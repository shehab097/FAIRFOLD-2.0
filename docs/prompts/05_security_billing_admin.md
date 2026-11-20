# Prompt 05 — Account Security, Billing, Analytics and Admin (days 9 to 11)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunks 1 to 4 must be finished.

**Goal:** make the product safe to hand to real users and sellable to employers: MFA, data export and account deletion; plans, payments and a screening quota; analytics for employers; the admin console (dashboard, users, audit log).

**Parts:**
- **Part 5.1** Account security and privacy: TOTP MFA, data export, account deletion (blank messages)
- **Part 5.2** Billing: plans, bKash sandbox and fake provider, subscriptions, screening quota, P30
- **Part 5.3** Analytics and admin: P28 analytics, P31 admin dashboard, P32 users, P33 audit log

Start message: `Build Part 5.1 from docs/prompts/05_security_billing_admin.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

If this chunk runs long, build analytics last.

> **Authoring note:** this prompt was written from the references in chunks 0 to 4 (MFA columns, `plans`, `company_subscriptions`, `payments`, pages P10, P28 to P33, `blank-on-delete`, quota on the dashboard). The PRD was not available when writing it. Where a rule below is marked *(Decision)*, it is a reasonable default; confirm or change it in `docs/progress.md` before building that part.

## Requirements covered

| ID | What it must do |
|---|---|
| MFA | Any signed-in user may turn on TOTP two-factor sign-in (authenticator app). Login then needs a 6-digit code. Admin sees a banner on P31 if their own MFA is off. |
| DATA-EXPORT | A signed-in user downloads a JSON file with all of their own data. Never includes anyone else's data. |
| ACCOUNT-DELETE | A signed-in user deletes their own account after confirming their password. Their data is removed, their messages are blanked for the other side, and an audit entry without personal data is written. |
| FR-046 (rest) | Employer dashboard shows `interviewsThisWeek` (done in chunk 4) and now `quota`. |
| BILLING-PLANS | HR sees plans and the company's current plan, buys a plan through bKash sandbox (or the fake provider), and sees payment history. |
| QUOTA | Screening is limited by the plan's `monthly_screenings`. Plans never change a score or rank (global rule 4). |
| ANALYTICS | HR and managers see hiring numbers for their company: funnel, score distribution, overrides, pull-backs, bias flags, time to shortlist, hires. Counts only, never identity. |
| ADMIN-DASH | Admin sees platform numbers: users by role, companies, applications, AI calls and cost, failed screening items, broken resources. |
| ADMIN-USERS | Admin searches users, disables and enables them, resets their MFA. |
| ADMIN-AUDIT | Admin browses and filters the audit log. |

## Rules and decisions

### MFA (Part 5.1)
- Algorithm TOTP (RFC 6238): SHA-1, 6 digits, 30-second step, allowed drift of one step either side. Use a small, maintained library (for example `dev.samstevens.totp:totp-spring-boot-starter` or `com.eatthepath:java-otp`). Secret is 20 random bytes, shown as Base32 and as an `otpauth://totp/FairFold:<email>?secret=...&issuer=FairFold` URI. Render the QR code **in the browser** from the URI (library `qrcode.react`), so the secret is never sent to a third party.
- Secrets are stored encrypted with AES-256-GCM using the key `MFA_ENCRYPTION_KEY` (32 bytes, Base64). Format `base64(iv || ciphertext || tag)`. Never log secrets or codes.
- **Setup:** `POST /api/account/mfa/setup` stores the new secret in `users.mfa_pending_secret_enc` and returns the URI and Base32 secret. `POST /api/account/mfa/enable {code}` verifies against the pending secret, then moves it to `mfa_secret_enc`, sets `mfa_enabled = true`, clears pending. Wrong code: 400 `MFA_CODE_INVALID`.
- **Disable:** `POST /api/account/mfa/disable {password, code}`; both must be right.
- **Login with MFA:** `POST /api/auth/login` with correct password and `mfa_enabled` returns `200 {mfaRequired: true, mfaToken}` and no session. `mfaToken` is a JWT with claim `purpose=mfa`, valid 5 minutes, accepted only by `/api/auth/login/mfa`. `POST /api/auth/login/mfa {mfaToken, code}` returns the normal session. Wrong code increments `failed_login_count` (same lockout as passwords, BR from chunk 1); reuse of the same code within its window is rejected (remember the last used time step in memory per user).
- The refresh flow does not ask for a code again (the session already passed MFA).
- Lost authenticator: an admin resets MFA from P32 (clears all three MFA columns, revokes refresh tokens, audit `mfa_reset`). No recovery codes in this version *(Decision)*. An admin cannot reset their own MFA through the admin endpoint.
- MFA is optional for every role *(Decision)*. Audit: `mfa_enabled`, `mfa_disabled`, `mfa_reset`.

### Data export (Part 5.1)
- `GET /api/account/export` → file download `fairfold-export-<date>.json` (`Content-Disposition: attachment`). Built synchronously. Rate limit 3 per hour per user (Bucket4j, 429).
- Content by role. **Candidate:** account (name, email, role, createdAt), profile, skills (with verified flag), work history, certifications, resumes (metadata only: filename, uploadedAt, status; no files, no stripped text), applications (job title, company name only if the job shows it publicly, status, score, scoring mode, claims with quotes the candidate may already see), assessment attempts (score, passed, startedAt, focus summary), practice sessions (question text, answer, feedback, rating), messages the user sent, notifications. **Employer roles:** account, and for HR the company profile; no applicant data, no other users' data. **Admin:** account only.
- Never include: password hash, tokens, MFA secrets, other users' names or emails, `resume_placeholder_map` originals of other people, `ai_calls`. Audit entry `data_exported`.

### Account deletion (Part 5.1)
- `DELETE /api/account` with body `{password, confirm: "DELETE"}`; 400 or 401 if either is wrong. MFA users also send `code`.
- Blocked with 409 for: `admin` role (`ADMIN_CANNOT_DELETE`); the last `employer_hr` of a company (`LAST_HR`; the company must be handed over or the HR must remove other members first).
- Steps, all in one service method (files are deleted after the commit succeeds):
  1. `UPDATE messages SET body = NULL WHERE sender_id = :userId` (messages stay, text gone; the API then returns `senderName: "Deleted user"` and `body: null`, and the UI shows "Message removed").
  2. Delete the user row. Foreign keys do the rest: `ON DELETE CASCADE` removes profile, skills, work history, certifications, resumes rows, applications as candidate, tokens, attempts, proctor events, practice sessions, notifications; `ON DELETE SET NULL` keeps `created_by`, `decided_by`, `changed_by`, `interviewer_id`, `sender_id` columns for other people's records.
  3. After commit: delete the user's resume files from storage (list the `storage_key`s before deleting).
  4. Audit `account_deleted` with `resourceId` = the old user id and `details = {"role": "..."}` only. **No name, no email.** Remove this user's `candidate_aliases` rows (they cascade) so the employer view shows "Applicant n" or the alias nowhere new.
- Employers deleting an account (not the last HR) simply lose access; their jobs and decisions remain with the company (`created_by` becomes null).
- Revoke all refresh tokens first. Send no email (the user is gone). Test with a real database, because cascades are the point.

### Billing and quota (Part 5.2)
- Seed plans (`V5_2_1`): `Free` (0 BDT, 0 days = never expires, 20 AI screenings a month), `Starter` (1,500 BDT, 30 days, 200 a month), `Growth` (4,500 BDT, 30 days, 1,000 a month). Values live in the seed only; changing them later is a new migration *(Decision)*. No recurring billing: each purchase is one payment for `duration_days`.
- **Effective plan** of a company = the subscription row with `ends_at > now` and the latest `starts_at`, else the `Free` plan. Buying while a paid plan is active starts the new plan **after** the current one ends (`starts_at = max(now, latest ends_at)`) *(Decision)*.
- **Quota period** = the current calendar month in UTC. **Used** = number of `screening_items` created this month for jobs of the company, in runs with `mode = 'ai'`, whose status is not `failed` *(Decision: rule-based screening is free and does not consume quota; failed items are refunded)*. **Limit** = effective plan `monthly_screenings`.
- **Enforcement:** when HR confirms Screen All (`POST .../screening`), retries failed items, or pulls back a candidate into an AI run, if `used + itemsToQueue > limit`, return 409 `QUOTA_EXCEEDED` with `{used, limit, requested, periodEnd}` and queue nothing. The Screen All dialog (chunk 2) already shows count and cost; extend it to show "Quota: used of limit" and, when exceeded, a link to P30. Check and queue inside one transaction with a lock on the company row (`SELECT ... FOR UPDATE` on `companies`) so two runs cannot overspend.
- Quota never changes scoring, order or filters (global rule 4). Add a test that fails if any scoring code reads billing tables.
- **Payment flow.** `PaymentProvider` interface: `createPayment(paymentId, amountBdt, callbackUrl)` → `{providerPaymentId, redirectUrl}` and `executePayment(providerPaymentId)` → `{success, rawStatus}`. Implementations: `BkashSandboxPaymentProvider` (bKash tokenized checkout sandbox: grant token, create, execute; token cached until expiry) and `FakePaymentProvider` (selected by `PAYMENT_PROVIDER=fake`, default locally; `redirectUrl` points straight to the callback with `status=success`; a query flag `?fail=1` on checkout simulates failure).
  1. `POST /api/employer/billing/checkout {planId}` (HR) → creates `payments` row `pending`, calls the provider, returns `{paymentId, redirectUrl}`. `Free` cannot be bought (409 `PLAN_NOT_PURCHASABLE`).
  2. The browser goes to `redirectUrl`; the provider sends the user back to `GET /api/billing/callback?paymentId=...&status=...` (public path, no token; the payment id is a random UUID and the result is **verified server-side** by calling `executePayment`, never trusted from the query string).
  3. On success: payment `paid`, `paid_at = now`, subscription row created, notification `payment_succeeded` and a plain email to the paying HR, audit `payment_succeeded`. On failure or cancel: `failed` or `cancelled`, audit `payment_failed`. Redirect the browser to `${APP_BASE_URL}/employer/billing?result=success|failed|cancelled`.
  4. **Idempotent:** calling the callback again for a `paid` payment does nothing and redirects the same way.
- All amounts are whole BDT integers (`amount_bdt int`); show `৳1,500`. Keep provider secrets (`BKASH_*`) out of logs and out of the frontend. Store only `provider_payment_id` and status, never card or wallet numbers.

### Analytics (Part 5.3)
- Company-scoped, read-only, **aggregates only**: no candidate names, emails, aliases or free text. Filters: `from`, `to` (ISO dates, default last 30 days, max 366 days), optional `jobId` (must belong to the company, else 404). Dates use `applications.created_at` for cohorts.
- Metrics returned: `funnel` (count of applications per current status), `scoreBuckets` (10 buckets 0 to 9 … 90 to 100, only applications with a score), `averageScore`, `overrides` (`shortlistedBelowCutoff`, `rejectedAboveCutoff`), `pullBacks` (count with `pulled_back_at` in range), `biasFlags` (currently flagged), `scoringMode` (`ai` vs `rule_based` counts), `timeToShortlistDays` (average and median of `shortlisted_at − created_at`, null when no shortlisted), `interviews` (count scheduled in range), `hires` (count `hired` in range via `application_status_history`), `quota` (used, limit, periodEnd).
- Groups with fewer than 3 items show the count but no average or median (return `null`), so a single person cannot be inferred *(Decision)*.

### Admin (Part 5.3)
- `GET /api/admin/dashboard`: `{usersByRole, companies, applications30d, aiCalls30d: {count, cost, blocked, errors}, failedScreeningItems30d, brokenResources, disabledUsers, adminMfaEnabled}`. AI numbers come from `ai_calls` (never prompt text).
- **Users:** `GET /api/admin/users?q&role&status=active|disabled&page` (search matches email or name, contains, case-insensitive). Row `{id, name, email, role, companyName, disabled, mfaEnabled, createdAt}`. Admin may see names and emails (platform operator). `PATCH /api/admin/users/{id}/disabled {disabled}`: sets or clears `disabled_at`, revokes tokens when disabling, audit `user_disabled` or `user_enabled`. Cannot disable yourself (409 `CANNOT_DISABLE_SELF`), cannot disable the last active admin (409 `LAST_ADMIN`), cannot disable the last active HR of a company (409 `LAST_HR`). `POST /api/admin/users/{id}/reset-mfa`. **Admin cannot read passwords, tokens, resumes, scores or messages.**
- **Audit log:** `GET /api/admin/audit?action&actorId&resourceType&from&to&page` newest first; item `{id, createdAt, actorId, actorEmail (null if deleted), action, resourceType, resourceId, details}`. `details` is shown as stored. Read-only: no update or delete endpoints exist, and no code path edits `audit_log` rows *(test it)*.
- Add `AuditService` calls for every new action listed below. Sensitive values never appear in `details`.

## Database

### Part 5.1 migration (`V5_1_1__mfa.sql`)

| Table | Columns |
|---|---|
| (alter) users | add `mfa_enabled boolean NOT NULL DEFAULT false`; `mfa_secret_enc varchar(512) NULL`; `mfa_pending_secret_enc varchar(512) NULL` |

### Part 5.2 migrations (`V5_2_1__billing.sql`, `V5_2_2__seed_plans.sql`)

| Table | Columns |
|---|---|
| plans | name varchar(50) NOT NULL UNIQUE; monthly_screenings int NOT NULL CHECK (>= 0); price_bdt int NOT NULL CHECK (>= 0); duration_days int NOT NULL CHECK (>= 0) (0 = no expiry, only for the Free plan); active boolean NOT NULL DEFAULT true; created_at |
| company_subscriptions | company_id → companies CASCADE; plan_id → plans; starts_at datetime(3) NOT NULL; ends_at datetime(3) NOT NULL; payment_id char(36) NULL → payments SET NULL; index (company_id, ends_at DESC) |
| payments | company_id → companies CASCADE; plan_id → plans; paid_by char(36) NULL → users SET NULL; amount_bdt int NOT NULL; provider varchar(20) NOT NULL CHECK (`bkash`,`fake`); provider_payment_id varchar(100) NULL; status varchar(20) NOT NULL DEFAULT 'pending' CHECK (`pending`,`paid`,`failed`,`cancelled`); paid_at datetime(3) NULL; UNIQUE (provider, provider_payment_id); index (company_id, created_at DESC) |

`payments` is created before `company_subscriptions` because of the foreign key. Seed `Free`, `Starter`, `Growth` as described under Rules.

### Part 5.3

No migration. Add indexes only if `EXPLAIN` shows a full scan on a large table; if needed, create `V5_3_1__analytics_indexes.sql` (for example `applications (created_at)`, `application_status_history (to_status, created_at)`).

## API

### Part 5.1

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/account/mfa/setup` | Signed in | → `{otpauthUri, secret}`; overwrites any pending secret |
| POST `/api/account/mfa/enable` | Signed in | `{code}` → 204; 400 `MFA_CODE_INVALID` |
| POST `/api/account/mfa/disable` | Signed in | `{password, code}` → 204; 401 or 400 on wrong values |
| GET `/api/account/mfa` | Signed in | `{enabled}` |
| (changed) POST `/api/auth/login` | Guest | may return `200 {mfaRequired:true, mfaToken}` |
| POST `/api/auth/login/mfa` | Guest | `{mfaToken, code}` → session; 401 `MFA_TOKEN_INVALID`; 400 `MFA_CODE_INVALID`; 423 when locked |
| GET `/api/account/export` | Signed in | JSON download; 429 |
| DELETE `/api/account` | Signed in | `{password, confirm, code?}` → 204; 409 `ADMIN_CANNOT_DELETE`, `LAST_HR` |
| (changed) GET `/api/applications/{id}/messages` | as before | deleted sender → `senderName: "Deleted user"`, `body: null` |

Audit added: `mfa_enabled`, `mfa_disabled`, `mfa_reset`, `data_exported`, `account_deleted`.

### Part 5.2

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/employer/billing` | HR | `{currentPlan:{name, monthlyScreenings, endsAt}, quota:{used, limit, periodEnd}, plans:[{id, name, monthlyScreenings, priceBdt, durationDays}], payments:[{id, planName, amountBdt, status, createdAt, paidAt}]}` (payments paged to the last 20) |
| POST `/api/employer/billing/checkout` | HR | `{planId}` → 201 `{paymentId, redirectUrl}`; 409 `PLAN_NOT_PURCHASABLE` |
| GET `/api/billing/callback` | Public | verifies the payment server-side, redirects the browser; idempotent |
| (changed) POST `/api/employer/jobs/{id}/screening` (Screen All), POST `.../screening/retry` and POST `/api/employer/applications/{id}/pull-back` | HR, Mgr | 409 `QUOTA_EXCEEDED`; retry and pull-back count against the quota like new items |
| (changed) POST `/api/employer/jobs/{id}/screening/estimate` | HR | adds `quota:{used, limit, periodEnd}` and `withinQuota` (boolean) to the response |
| (changed) GET `/api/employer/dashboard` | HR, Mgr | `quota` now `{used, limit, periodEnd}` |

Notification type added: `payment_succeeded`. Audit: `payment_created`, `payment_succeeded`, `payment_failed`.

### Part 5.3

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/employer/analytics?from&to&jobId` | HR, Mgr | metrics as described |
| GET `/api/admin/dashboard` | Admin | as described |
| GET `/api/admin/users?...`, PATCH `/api/admin/users/{id}/disabled`, POST `/api/admin/users/{id}/reset-mfa` | Admin | as described |
| GET `/api/admin/audit?...` | Admin | as described |

Audit added: `user_disabled`, `user_enabled`.

## Pages

| Page | Part | Shows and does |
|---|---|---|
| P03 Login | 5.1 | After a correct password, if `mfaRequired`, a second step with a 6-digit code field (numeric, autofocus, paste friendly), Back link, minutes left when locked |
| P10 Account settings | 5.1 | Adds **Two-factor sign-in** (status, Set up dialog with QR from the URI and the Base32 secret as text, enter code to enable; Disable dialog asks password and code), **Download my data** button (shows "Preparing" then saves the file), **Delete my account** (danger zone, dialog lists what is removed and asks for password and the word DELETE; shows the 409 messages) |
| P30 Billing | 5.2 | Current plan card, quota meter (`ProgressBar`, used of limit, resets date), plan cards with Buy (redirects to the provider), result banner from `?result=`, payments table. HR only |
| P20 Employer dashboard | 5.2 | Quota tile (used of limit) linking to P30 |
| P28 Analytics | 5.3 | Date range and job filter (in the URL); tiles (average score, hires, pull-backs, overrides, bias flags); **Funnel** horizontal bars; **Score distribution** vertical bars; each chart has a "View as table" toggle; "Not enough data" for groups under 3 |
| P31 Admin dashboard | 5.3 | Tiles and a small table for AI usage; banner if the signed-in admin has no MFA. `/admin` now routes here instead of `/admin/content` |
| P32 Users | 5.3 | Search, role and status filters, paging; row actions Disable or Enable, Reset MFA (confirm dialogs, the server message on 409) |
| P33 Audit log | 5.3 | Filters (action select, actor email, date range), table with an expandable details row, paging |

Nav: HR keeps Billing and Analytics (already listed); admin gets Dashboard, Users, Audit log, Content (Dashboard becomes the home).

---

## Part 5.1 — Account security and privacy

**Tasks**
1. Migration `V5_1_1`; `MfaService` (secret generation, AES-GCM encryption, code verification with drift and replay guard); `MFA_ENCRYPTION_KEY` in `.env.example` and the properties class (fail to start if missing and any user has MFA on).
2. Setup, enable, disable endpoints; login split into password step and MFA step with `mfaToken`; lockout shared with passwords.
3. Data export service (one query per section, streamed JSON) and endpoint with the rate limit.
4. Account deletion service as described (blank messages, cascade, file removal after commit, audit). Update the messages endpoint for deleted senders.
5. Frontend: P03 second step, P10 sections (MFA dialogs with `qrcode.react`, export, delete).

**Tests:** `Mfa_*` (setup does not enable; wrong code rejected; correct code enables; secret stored encrypted and never returned after setup; login returns `mfaRequired` without a session; `mfaToken` rejected on other endpoints and after 5 minutes; code reuse rejected; five wrong codes lock the account; disable needs password and code), `Export_*` (candidate export contains own data and no other user's name or email; no password hash or token; rate limit 429), `Delete_*` (cascades remove profile, applications and attempts; messages remain with `body` null and sender null; files removed from storage; last HR and admin get 409; audit entry has no name or email; disabled-login and token revocation).

**Definition of done:** a candidate turns on MFA with a real authenticator app, signs out, signs in with a code, downloads their data file, applies to a job and exchanges a message, then deletes the account; the employer still sees the thread with "Message removed" and no identity.

---

## Part 5.2 — Billing

**Tasks**
1. Migrations `V5_2_1`, `V5_2_2`; entities; `BillingService` (effective plan, quota calculation, subscription creation).
2. `PaymentProvider`, `FakePaymentProvider`, `BkashSandboxPaymentProvider` (read `BKASH_BASE_URL`, `BKASH_APP_KEY`, `BKASH_APP_SECRET`, `BKASH_USERNAME`, `BKASH_PASSWORD`); checkout and callback endpoints; payment notification, email and audit.
3. Quota enforcement in Screen All and pull-back with the company row lock; dashboard `quota`; extend the Screen All dialog.
4. Frontend: P30, dashboard tile, quota text in the Screen All dialog.

**Tests:** `Quota_*` (Free limit 20: 20 items allowed, 21st returns 409 with nothing queued; rule-based run does not use quota; failed items are refunded; month rollover with the test `Clock`; paid plan raises the limit; expired subscription falls back to Free; two concurrent runs cannot overspend), `Billing_*` (checkout creates `pending`; callback with the fake provider creates the subscription once; second callback does nothing; failure leaves no subscription; Free not purchasable; HR of another company gets 404 on another company's payment; query-string status is not trusted), `NoPayToRank_*` (scoring and ranking code does not reference `plans`, `payments` or subscription entities; a company on Growth and one on Free get identical scores and order for the same applications).

**Definition of done:** with `PAYMENT_PROVIDER=fake`, HR on the Free plan exhausts the quota, sees the 409 message, buys Starter, and screens again; payment history shows the row; the dashboard quota changes. With sandbox credentials (optional, if the user has them) the bKash flow completes the same way.

---

## Part 5.3 — Analytics and admin

**Tasks**
1. `AnalyticsService` with plain SQL or JPQL aggregates, unit-tested against seeded data; endpoint with filters and the under-3 rule.
2. Admin dashboard endpoint; users endpoints with the guards; reset MFA; audit log endpoint with filters (use a `Specification` or a small query builder; page size max 100).
3. Frontend: P28 with charts (`charts/defaults.ts` style, table toggle), P31, P32, P33; change the `/admin` redirect; admin MFA banner.

**Tests:** `Analytics_*` (funnel counts match a seeded set; buckets sum to scored applications; filters by date and job; another company's `jobId` is 404; under-3 groups return null; response contains no names, emails or aliases, extend `NoIdentityLeakTest`), `AdminUsers_*` (non-admin 403; disable blocks login with 403 `ACCOUNT_DISABLED` and revokes tokens; self, last admin and last HR guards; reset MFA clears the columns and audits; admin cannot reach resumes, scores or messages endpoints), `AdminAudit_*` (filters; newest first; no mutation endpoints exist; no repository method updates or deletes audit rows).

**Definition of done (whole chunk):** demo path: candidate enables MFA, exports data → HR buys a plan and runs Screen All within quota → HR opens Analytics and sees the funnel and score buckets with table toggles → admin sees the dashboard numbers, disables a user (their login is refused), finds the `user_disabled` entry in the audit log. Tests green, design check passes, `docs/openapi.json` re-exported, `docs/progress.md` updated.
