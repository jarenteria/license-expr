# TASKS: License Expiration Window Status

Status: **Approved (2026-11-24)**
Implements: [`SPEC.md`](SPEC.md) · [`PLAN.md`](PLAN.md)

**Rules**
- Do one task per request.
- Within a task, write the failing test first, see it fail, then implement.
- A task is done only when its done condition holds and `./mvnw test` passes.

**Paths**: `main/…` = `src/main/java/org/license/licenseexpr/`, `test/…` = `src/test/java/org/license/licenseexpr/`

---

- [x] **T1: Make the app a non-web job**
  - Files: `pom.xml`
  - Steps:
    - Replace `spring-boot-starter-webmvc` with `spring-boot-starter`.
    - Replace `spring-boot-starter-webmvc-test` with `spring-boot-starter-test`.
  - Done when: `./mvnw test` passes, and `./mvnw spring-boot:run` starts and exits on its own without starting an embedded web server.

- [x] **T2: `WindowStatus` enum and output labels**
  - Files: `test/…/WindowStatusTest.java`, then `main/…/WindowStatus.java`
  - Test first: there are exactly five values, labelled `OK`, `expiring_90`, `expiring_60`, `expiring_30`, `expired`.
  - Done when: the test passes.

- [x] **T3: Domain records and notification key**
  - Files: `test/…/LicenseTest.java`, `test/…/NotificationTest.java`, then `main/…/License.java`, `main/…/Clinician.java`, `main/…/Notification.java`, `main/…/NotificationKey.java`
  - Test first:
    - `License` rejects a blank ID or a null date.
    - `Notification.key()` returns license ID + expiration date + status.
    - Two notifications that differ only in expiration date have different keys.
  - Done when: the tests pass. `Notification` has exactly the four SPEC §6.1 fields.

- [x] **T4: Status rule**
  - Files: `test/…/WindowStatusCalculatorTest.java`, then `main/…/WindowStatusCalculator.java`
  - Test first: a parameterized test with every row of SPEC §7.1.
  - Done when: all rows pass. The calculator is a pure function.

- [x] **T5: Notification publisher**
  - Files: `test/…/ConsoleNotificationPublisherTest.java`, then `main/…/NotificationPublisher.java`, `main/…/ConsoleNotificationPublisher.java`
  - Test first: publishing one notification writes exactly `clinician_id=C-1 license_id=L-7 status=expiring_30 expiration_date=2026-01-31` plus a newline to the output stream the publisher is given.
  - Done when: the test passes. The Spring bean writes to stdout.

- [x] **T6: Sent-notification store**
  - Files: `test/…/CsvSentNotificationStoreTest.java`, then `main/…/SentNotificationStore.java`, `main/…/CsvSentNotificationStore.java`
  - Test first, using `@TempDir`:
    - a missing file means nothing has been sent;
    - a key that's recorded is loaded back by a new store instance;
    - recording appends one line without rewriting earlier lines, and the file has its header;
    - a malformed line fails the load with its line number.
  - Done when: the tests pass, in the file format from PLAN §4.

- [x] **T7: The job, with no-repeat and forecast logic**
  - Files: `test/…/LicenseExpirationJobTest.java`, then `main/…/LicenseSource.java`, `main/…/LicenseExpirationJob.java`
  - Test first, using a fixed clock, an in-memory source and store, and a recording publisher:
    - the SPEC §7.2 run sequence gives exactly the notifications in its tables, including after the renewal;
    - the SPEC §7.3 forecast reports `L-7 expired` and `L-8 expired`, records nothing, and a later regular run on 2026-04-01 still sends `L-8 expired`;
    - `OK` licenses are never published;
    - a license that skipped windows publishes only its current status;
    - if the publisher throws on the second notification, the run fails and only the first notification is recorded.
  - Done when: the tests pass, and the job follows the steps in PLAN §6.

- [x] **T8: CSV license source (fails on invalid rows)**
  - Files: `test/…/CsvLicenseSourceTest.java`, then `main/…/CsvLicenseSource.java`
  - Test first, using `@TempDir`:
    - the header is skipped;
    - rows are grouped by `clinician_id`;
    - dates are parsed as `YYYY-MM-DD`;
    - a missing file gives a clear error;
    - an empty date, an invalid date or a blank ID each fails the load with the line number in the message.
  - Done when: the tests pass, and an invalid row anywhere means `loadClinicians()` returns nothing.

- [x] **T9: Command-line runner and clock**
  - Files: `test/…/LicenseJobRunnerTest.java`, then `main/…/LicenseJobRunner.java`, `main/…/LicenseExprApplication.java` (adds the `Clock` bean and wires the file paths)
  - Test first, using a fixed clock and a mocked job:
    - `--as-of=2026-01-01` runs the job for that date;
    - with no `--as-of`, the job runs for the clock's date;
    - a missing `--licenses-file` or an invalid `--as-of` throws a clear message before the job runs;
    - `--sent-file` defaults to `sent-notifications.csv`.
  - Done when: the tests pass. The `Clock` bean is `Clock.systemDefaultZone()`.

- [x] **T10: End-to-end run**
  - Files: `src/test/resources/licenses-fixture.csv`, `test/…/LicenseExprApplicationTests.java`, `CLAUDE.md` (adds the run commands)
  - Test first: two `@SpringBootTest` runs on the same fixed date, sharing a `@TempDir` sent file.
    - The first run prints exactly the expected notification lines.
    - The second run prints nothing.
  - Done when:
    - the test passes;
    - running the app by hand twice with `./mvnw spring-boot:run` behaves the same way;
    - the run commands from PLAN §4 are in `CLAUDE.md`.
