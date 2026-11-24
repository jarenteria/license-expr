# PLAN: License Expiration Window Status

Status: **Approved (2026-11-24)**
Implements: [`SPEC.md`](SPEC.md)

## 1. Approach

Build the job as a non-web Spring Boot command-line app inside the existing project. One run starts the app, loads and validates every license, sends the notifications that are due, records them and exits.

The status rule is a pure function with no I/O. Three pieces sit behind interfaces so each can be replaced without touching the rule or the no-repeat logic:

- **where licenses come from** (`LicenseSource`)
- **where notifications go** (`NotificationPublisher`, SPEC §6.4)
- **where sent notifications are remembered** (`SentNotificationStore`)

```
LicenseJobRunner (parses args)
  └─ LicenseExpirationJob   (regular vs forecast, no-repeat logic)
       ├─ LicenseSource          → CsvLicenseSource              (now) / DB, API… (later)
       ├─ WindowStatusCalculator (pure rule, SPEC §3)
       ├─ SentNotificationStore  → CsvSentNotificationStore      (now) / DB (later)
       └─ NotificationPublisher  → ConsoleNotificationPublisher  (now) / event publisher (later)
```

## 2. Tech decisions

| Decision | Choice | Why |
|---|---|---|
| Runtime | Existing Spring Boot 4.2.0, Java 17, Maven wrapper | Already set up. Gives dependency injection, argument parsing and test support. |
| App type | Non-web. Replace `spring-boot-starter-webmvc` with `spring-boot-starter`, and `spring-boot-starter-webmvc-test` with `spring-boot-starter-test`. | The job doesn't serve HTTP. Without the web starters, no server starts and the app exits when the run is done. |
| Entry point | An `ApplicationRunner` bean | Spring Boot's standard hook for running work at startup. It gets parsed `--key=value` arguments. |
| Dates | `java.time.LocalDate`. Days left = `ChronoUnit.DAYS.between(asOf, expirationDate)` | Calendar dates with no time of day. Counts whole days (SPEC §3). |
| "Today" | Injected `java.time.Clock` bean = `Clock.systemDefaultZone()`, and `LocalDate.now(clock)` | Local time zone of the machine running the job (SPEC §4). Tests can supply a fixed clock. |
| Regular vs forecast | The job compares the as-of date with `LocalDate.now(clock)` | SPEC §4 defines a forecast run as any as-of date other than today |
| License data | A CSV file whose path is passed as an argument (§4) | The simplest stand-in for "the system's set of licenses", with no extra libraries. Hidden behind `LicenseSource`. |
| Sent-notification memory | A local CSV file. One line is appended and flushed right after each successful publish. | No extra libraries. Appending each line as it's sent means that if the run fails partway, what was already sent stays recorded (SPEC §6.2). Hidden behind `SentNotificationStore`. |
| Output | One line per notification on stdout (§5) | SPEC §6.4 says writing output is the destination for now. Hidden behind `NotificationPublisher`. |
| Tests | JUnit 5, AssertJ and Mockito (all from `spring-boot-starter-test`), `@ParameterizedTest`, `@TempDir` | Already included in the test starter. The SPEC §7 tables become test cases. |

## 3. Components

All components go under `org.license.licenseexpr`.

| Component | Kind | Responsibility |
|---|---|---|
| `Clinician` | record (`id`, `List<License> licenses`) | Domain (SPEC §2) |
| `License` | record (`id`, `LocalDate expirationDate`) | Domain. The constructor rejects a blank ID or a null date. |
| `WindowStatus` | enum `OK, EXPIRING_90, EXPIRING_60, EXPIRING_30, EXPIRED` | Each value has an output label: `OK`, `expiring_90`, `expiring_60`, `expiring_30`, `expired` |
| `WindowStatusCalculator` | stateless class, `WindowStatus statusOf(LocalDate expirationDate, LocalDate asOf)` | The SPEC §3 table |
| `Notification` | record (`clinicianId`, `licenseId`, `WindowStatus status`, `LocalDate expirationDate`) | The four fields from SPEC §6.1. Its `key()` method returns a `NotificationKey`. |
| `NotificationKey` | record (`licenseId`, `LocalDate expirationDate`, `WindowStatus status`) | The repeat identity from SPEC §6.2 |
| `LicenseSource` | interface, `List<Clinician> loadClinicians()` | Port for the set of licenses. Must load and validate everything before returning. |
| `CsvLicenseSource` | implementation | Reads the CSV, validates every row and groups rows by clinician ID |
| `SentNotificationStore` | interface, `Set<NotificationKey> loadSent()`, `void recordSent(NotificationKey key)` | Port for what's remembered between runs |
| `CsvSentNotificationStore` | implementation | Loads the file (a missing file means nothing has been sent yet). `recordSent` appends one line and flushes immediately. |
| `NotificationPublisher` | interface, `void publish(Notification n)` | Port for the destination. Throws an exception if delivery fails. |
| `ConsoleNotificationPublisher` | implementation | Prints one line per notification |
| `LicenseExpirationJob` | service, `void run(LocalDate asOf)` | The algorithm in §6 |
| `LicenseJobRunner` | `ApplicationRunner` | Parses the arguments, works out the as-of date and calls the job |

## 4. Inputs and files

**Arguments**

| Argument | Required | Format | Default |
|---|---|---|---|
| `--licenses-file` | yes | path to a CSV file | none (the run fails if it's missing) |
| `--as-of` | no | `YYYY-MM-DD` | today, from the `Clock` |
| `--sent-file` | no | path to a CSV file | `sent-notifications.csv` in the working directory |

**Licenses CSV**: UTF-8, a header row, one row per license. Rows with the same `clinician_id` belong to the same clinician.

```
clinician_id,license_id,expiration_date
C-1,L-7,2026-01-31
C-1,L-8,2026-04-01
C-2,L-9,2027-06-30
```

**Sent-notifications CSV**: written only by the job. It's created with a header row on the first notification that's recorded.

```
license_id,expiration_date,status
L-7,2026-01-31,expiring_30
L-8,2026-04-01,expiring_90
```

**Run commands**

```
./mvnw spring-boot:run -Dspring-boot.run.arguments="--licenses-file=licenses.csv"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--licenses-file=licenses.csv --as-of=2026-04-01"   # forecast
java -jar target/license-expr-0.0.1-SNAPSHOT.jar --licenses-file=licenses.csv
```

## 5. Output

Each notification is one line on stdout, with fields in SPEC §6.1 order:

```
clinician_id=C-1 license_id=L-7 status=expiring_30 expiration_date=2026-01-31
```

A run with nothing to notify prints nothing. Errors go to stderr, and log output doesn't mix with notification lines on stdout.

## 6. Job algorithm (`LicenseExpirationJob.run(asOf)`)

1. `forecast = !asOf.equals(LocalDate.now(clock))`
2. `clinicians = licenseSource.loadClinicians()`. Any invalid row throws here, so nothing has been printed yet (SPEC §2).
3. `sent = forecast ? empty : sentStore.loadSent()`
4. For each clinician and each license:
   1. `status = calculator.statusOf(license.expirationDate, asOf)`. If `OK`, skip it (SPEC §6.1).
   2. Build the `Notification n`. If `!forecast` and `sent` contains `n.key()`, skip it (SPEC §6.2).
   3. `publisher.publish(n)`. If this throws, the run stops right away (SPEC §6.2, failed sends).
   4. If `!forecast`, call `sentStore.recordSent(n.key())`. Forecast runs record nothing (SPEC §6.3).

## 7. Errors

All of these stop the run with a message on stderr and a non-zero exit code (Spring Boot's default when the runner throws).

| Situation | When it's detected |
|---|---|
| `--licenses-file` missing or unreadable, or `--as-of` invalid | Before anything is loaded |
| A licenses CSV row with a missing ID or an empty or invalid `expiration_date`. The message names the line number. | While loading, before any notification |
| The sent-notifications file is unreadable or has a malformed line | While loading, before any notification |
| Publishing fails | During the loop. Notifications already sent stay recorded. |

**Assumption:** only one run happens at a time. The job is started by hand, so two runs at once aren't guarded against.

## 8. Testing strategy

| Test | Kind | Covers |
|---|---|---|
| `LicenseTest`, `NotificationTest` | unit | Validation, and that `key()` uses license ID + expiration date + status |
| `WindowStatusTest` | unit | Output labels match the SPEC names exactly |
| `WindowStatusCalculatorTest` | unit, parameterized | Every row of SPEC §7.1 |
| `ConsoleNotificationPublisherTest` | unit | The exact line format |
| `CsvSentNotificationStoreTest` | unit, `@TempDir` | Missing file means nothing sent, round trip, append without rewriting, malformed line fails |
| `LicenseExpirationJobTest` | unit, with a fixed clock, an in-memory source and store, and a recording publisher | The SPEC §7.2 run sequence, including renewal. The §7.3 forecast. `OK` is skipped. A skipped window notifies only the current status. A publisher failure on the second notification keeps the first one recorded. |
| `CsvLicenseSourceTest` | unit, `@TempDir` | Parsing and grouping. Every kind of invalid row fails with its line number. |
| `LicenseJobRunnerTest` | unit, with a fixed clock and a mocked job | `--as-of` used when given, otherwise today. Errors for bad arguments. |
| `LicenseExprApplicationTests` | `@SpringBootTest`, fixture CSV, `@TempDir` sent file | End to end: the first run prints the expected lines, and a second run on the same day prints nothing |

## 9. Traceability

| SPEC section | Where it's implemented |
|---|---|
| §2 Domain, invalid data fails the run | `Clinician`, `License`, `CsvLicenseSource` |
| §3 Status rules | `WindowStatusCalculator`, `WindowStatus` |
| §4 As-of date, regular vs forecast | `LicenseJobRunner`, `Clock`, `LicenseExpirationJob` step 1 |
| §5 Manual job, all licenses | `LicenseExpirationJob`, `LicenseJobRunner` |
| §6.1 What gets notified, fields | `Notification`, job step 4.1 |
| §6.2 No repeats, renewal, failed sends | `NotificationKey`, `SentNotificationStore`, job steps 3, 4.2–4.4 |
| §6.3 Forecast runs | Job steps 3 and 4.4 |
| §6.4 Replaceable destination | `NotificationPublisher`, `ConsoleNotificationPublisher` |
| §7 Examples | `WindowStatusCalculatorTest`, `LicenseExpirationJobTest` |

## 10. Decisions

| Question | Decision |
|---|---|
| First license source | CSV file passed with `--licenses-file` |
| Output format | `key=value` line per notification (§5) |
| Default sent-notifications file | `sent-notifications.csv` in the working directory, overridable with `--sent-file`. Running from another directory starts with no memory. |
