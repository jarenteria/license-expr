# SPEC: License Expiration Window Status

Status: **Approved (2026-11-24)**

## 1. Purpose

Clinicians hold licenses that expire. This feature checks a set of licenses and reports which expiration window each one is in, so upcoming and past expirations are visible ahead of time. Each change is reported once, and later runs don't repeat it.

## 2. Domain

- **Clinician**: a person who holds one or more licenses. Identified by a clinician ID.
- **License**: belongs to exactly one clinician. Identified by a license ID.
- **Expiration date**: every license has one. It's a calendar date, no time of day, and it's **required**.
- A clinician has one or more licenses. Each license is evaluated on its own.
- **Invalid data**: if any license in the set is missing an ID or doesn't have a valid expiration date, the whole run fails before anything is reported. The error says which entry is invalid.

## 3. Window status rules

**Days left** = number of whole calendar days from the **as-of date** to the license's expiration date (expiration date − as-of date).

A license is expired from the start of its expiration date. So on the expiration date, days left = 0 and the status is `expired`.

| Days left | Window status |
|---|---|
| more than 90 | `OK` |
| 61 to 90 | `expiring_90` |
| 31 to 60 | `expiring_60` |
| 1 to 30 | `expiring_30` |
| 0 or less (on or after the expiration date) | `expired` |

- Every boundary is inclusive in the more urgent window: exactly 90 days left is `expiring_90`, exactly 60 is `expiring_60`, exactly 30 is `expiring_30`.
- The windows are fixed and the same for every license and clinician.
- Every license has exactly one of these five statuses. No other statuses exist.

## 4. As-of date (forecasting)

- The as-of date is the day the statuses are calculated for. If none is given, it defaults to **today**.
- "Today" is the current calendar date in the local time zone of the machine running the job. Because the job is started by hand, this matches the date of the person running it.
- A run whose as-of date is today is a **regular run**.
- A run whose as-of date is any other day is a **forecast run**. It previews future statuses (for example, "what will be expiring on 2027-03-01?") or looks back at past ones.
- The same license and as-of date always produce the same status.

## 5. The job

- The feature runs as a job that someone starts manually. It has no schedule or trigger.
- Each run evaluates every license in the system's set of licenses against one as-of date.
- The job never changes license data.
- Between runs, the job remembers only the notifications it has already sent (§6).

## 6. Notifications

### 6.1 What gets notified

- A license whose status is `expiring_90`, `expiring_60`, `expiring_30` or `expired` produces a notification.
- A license whose status is `OK` produces no notification.
- Each notification has exactly these fields:

  | Field | Meaning |
  |---|---|
  | clinician ID | the clinician who holds the license |
  | license ID | the license |
  | window status | the license's status (§3) |
  | expiration date | the license's expiration date |

- There's no summary or count per status.
- Notifications come in no particular order.

### 6.2 No repeats (regular runs)

- A notification is identified by **license ID + expiration date + window status**.
- In a regular run, if a notification with the same identity was already sent, it isn't sent again.
- So a license is notified once each time it enters a new window. For example, it's notified at `expiring_90`, stays quiet on later runs while still `expiring_90`, and is notified again when it reaches `expiring_60`.
- If a license skips windows between runs (for example, it goes from `expiring_90` straight to `expiring_30` because the job wasn't run in between), only the current status is notified.
- **Renewal:** if a license's expiration date changes, its earlier notifications no longer match. It's notified again as it enters each window for its new expiration date.
- **Failed sends:** a notification counts as sent only once it has actually been delivered. If delivery fails, the run stops and reports an error. Notifications already delivered in that run stay remembered, and the next regular run sends the rest.

### 6.3 Forecast runs

- A forecast run reports a notification for every license whose status on that date isn't `OK`, whether or not it was already sent.
- A forecast run doesn't remember anything, so it never stops a later regular run from sending a notification.

### 6.4 Destination

- For now, sending a notification means writing it as output.
- **Replaceable destination:** where notifications go must be swappable (for example, publishing an event per notification) without changing the status rules (§3), how licenses are evaluated (§5) or the no-repeat rules (§6.2).

## 7. Examples

### 7.1 Status rule

As-of date **2026-01-01**:

| Expiration date | Days left | Window status |
|---|---|---|
| 2026-04-02 | 91 | `OK` |
| 2026-04-01 | 90 | `expiring_90` |
| 2026-03-03 | 61 | `expiring_90` |
| 2026-03-02 | 60 | `expiring_60` |
| 2026-02-01 | 31 | `expiring_60` |
| 2026-01-31 | 30 | `expiring_30` |
| 2026-01-02 | 1 | `expiring_30` |
| 2026-01-01 | 0 | `expired` |
| 2025-12-31 | −1 | `expired` |

### 7.2 Regular runs, no repeats

Licenses: `L-7` (clinician `C-1`) expires 2026-01-31, `L-8` (clinician `C-1`) expires 2026-04-01, and `L-9` (clinician `C-2`) expires 2027-06-30.

| Run on | Notifications sent |
|---|---|
| 2026-01-01 | `L-7 expiring_30`, `L-8 expiring_90` (`L-9` is `OK`) |
| 2026-01-02 | none (`L-7` is still `expiring_30` and `L-8` is still `expiring_90`) |
| 2026-01-31 | `L-7 expired`, `L-8 expiring_60` |
| 2026-02-01 | none |

Then `L-7` is renewed to expire 2027-01-31:

| Run on | Notifications sent |
|---|---|
| 2026-02-02 | none (`L-7` is now `OK`) |
| 2026-04-01 | `L-8 expired` |
| 2026-11-02 | `L-7 expiring_90` (90 days before its new expiration date) |

### 7.3 Forecast run

Run on 2026-02-01 (before the renewal) with as-of date 2026-04-01: reports `L-7 expired` and `L-8 expired`, even though `L-7 expired` was already sent on 2026-01-31. Nothing is remembered, so a regular run on 2026-04-01 still sends `L-8 expired`.

## 8. Out of scope

- Real notification channels (email, events and so on). The destination only has to be replaceable.
- Scheduled or automatic runs
- Resetting what the job remembers, or forcing notifications to be sent again
- Any history beyond what's needed to avoid repeats (§6.2)
- Creating, editing, renewing or deleting licenses or clinicians
- Users, roles and access control
- Windows that can be configured or vary per license
- Days left, summaries or counts in the output

## 9. Decisions log

| Question | Decision |
|---|---|
| Status for 1–30 days left | `expiring_30` (not `expired_30`) |
| Boundaries at exactly 90 / 60 / 30 days | In the more urgent window (`expiring_90` / `expiring_60` / `expiring_30`) |
| When a license becomes expired | At the start of its expiration date |
| How days are counted | Whole calendar days |
| Time zone for "today" | Local time zone of the machine running the job (Claude's choice, as you delegated it) |
| Missing or invalid expiration date | Required. Any invalid entry fails the whole run before anything is reported. |
| Notification fields | Clinician ID, license ID, window status, expiration date |
| Notification order | No set order |
| What counts as a repeat | Same license ID + expiration date + window status |
| `OK` licenses | Never notified |
| Renewals | A new expiration date means notifications start fresh |
| Forecast runs | Report all non-`OK` licenses for that date and remember nothing |
| Failed delivery | Not counted as sent. The run stops, and the next regular run retries. |
| Resetting what's remembered | Out of scope |
