# Prompt 05 — Operations (days 8 to 9.5)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunks 1 to 4 must be finished.

**Goal:** an admin can see and audit everything; users can export and delete their data; employers see analytics, pay for a plan through bKash (sandbox), and anyone can turn on two-step login.

**Parts:**
- **Part 5.1** Admin and compliance: dashboard, users, audit log, data export, account deletion
- **Part 5.2** Analytics and billing: analytics page, plans, quota, payments (fake provider first, then bKash sandbox)
- **Part 5.3** MFA: TOTP setup, login code step, admin reset

Start message: `Build Part 5.1 from docs/prompts/05_operations.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

If time is short, build billing last, and start it with the fake provider.

## Requirements covered

| ID | What it must do |
|---|---|
| FR-037 Admin dashboard | User, job and application counts, AI calls and estimated AI cost this month. |
| FR-038 User management | Search, filter, change role, unlock, trigger a password reset. |
| FR-039 Audit log | Filter by action, resource, user and date. Export to CSV. |
| FR-040 Data export | A signed-in user clicks "Download my data" and receives a JSON file immediately. |
| FR-041 Account deletion | A user confirms with their password and the account is deleted. Personal data and files are removed. Audit rows stay, with the actor cleared. The last admin and the last HR of a company cannot delete themselves (409). |
| FR-035 Analytics | Pipeline funnel, time-to-hire, a table of overrides with reasons, and the count of `not_matched` candidates pulled back. Each chart has a table view. |
| FR-048 Billing | The employer sees plan, usage and payment history and buys a 30-day plan through bKash. |
| FR-056 / BR-30 bKash payment | Employer picks a plan, the backend creates the payment, the employer approves on bKash, the backend verifies and executes it, and the plan is active for 30 days. No auto-renewal. Sandbox only until bKash approves a merchant account. |
| FR-005 MFA enable | A TOTP secret and QR code are generated. The secret is stored encrypted with a key from the environment. MFA is optional for every role. Turning it off needs the password and a valid code. |
| FR-006 MFA verify | When MFA is on, login needs a valid 6-digit code after the password. Five wrong codes count as failed logins (lockout rules of chunk 1). |

## Rules and decisions

- **Admin role changes:** allowed only among `employer_hr`, `employer_manager`, `interviewer`. Candidates and admins cannot be changed here (409 `ROLE_CHANGE_NOT_ALLOWED`); an admin cannot change their own role (409 `CANNOT_CHANGE_SELF`); demoting the last HR of a company returns 409 `LAST_HR`. Admin accounts come from the seed only.
- **Every admin action is audited:** `admin_role_changed`, `admin_unlock`, `admin_password_reset`, `mfa_reset`, `data_export`, `account_deleted`, `audit_exported`.
- **Audit details never contain personal data** (no names, emails, resume text, message text). Add a test that scans a sample of audit rows.
- **Data export** includes: user (without password hash, tokens or MFA secrets), profile, skills, work history, certifications, resumes (metadata and stripped text), placeholder mapping, applications (status, score, claims), assessment attempts, practice sessions, messages the user sent, notifications, consent time. For employers: company, jobs they created. Response header `Content-Disposition: attachment; filename="fairfold-data-<date>.json"`.
- **Account deletion** (`AccountErasureService`, one transaction, after verifying the password and the typed confirmation `DELETE`):
  1. Guards: last admin 409 `LAST_ADMIN`; last HR of the company 409 `LAST_HR`.
  2. Delete the user's resume files through `StorageService`.
  3. Blank messages the user sent: `UPDATE messages SET body = NULL WHERE sender_id = :id` (the foreign key then nulls the sender).
  4. `UPDATE audit_log SET actor_id = NULL WHERE actor_id = :id` (rows stay).
  5. Delete the user row; foreign keys cascade or set null as defined in 00_context section 5.2.
  6. Write an `account_deleted` audit row with `details = {role}` only.
  *Note:* when a **candidate** is deleted, their applications are deleted by cascade, so the threads and interviews attached to those applications disappear for the employer too. Record this in progress.md as a known behaviour.
- **Quota:** monthly screenings used = count of `screening_items` with status `done` in the current calendar month (UTC) belonging to runs with `mode = 'ai'` on the company's jobs. Rule-based runs are free and do not count. Starting an AI run when `used + count > quota` returns 409 `QUOTA_EXCEEDED` with `{used, quota}`; the UI shows a buy-a-plan link. *(Proposal, record it in progress.md.)*
- **Effective plan:** the paid subscription with `starts_at <= now < ends_at`, otherwise the Free plan. A new purchase while one is active ends the old one now and starts the new one now (unused time is not carried over). *(Decision.)*
- **No pay-to-rank:** plans only change quota. They never change a score or rank.
- **MFA security:** TOTP RFC 6238 (SHA-1, 6 digits, 30-second step, accept the previous and next step), secrets encrypted with AES-256-GCM using `MFA_ENCRYPTION_KEY` (base64, 32 bytes; random 12-byte IV stored with the ciphertext). The same time step cannot be used twice (`mfa_last_step`). The login step 1 response when MFA is on is `{mfaRequired:true, mfaToken}` where `mfaToken` is a JWT with claim `mfa=true`, 5-minute expiry, and **must be rejected by the normal access-token filter**.

## Database

### Part 5.1

No new tables. Optional migration `V5_1_1__admin_indexes.sql`: indexes on `users(role)`, `users(lower(email))` if missing, `audit_log(resource_type)`, `ai_calls(created_at)`.

### Part 5.2 migration (`V5_2_1__billing.sql`)

| Table | Columns |
|---|---|
| plans | name text NOT NULL UNIQUE; monthly_screenings int NOT NULL; price_bdt numeric(10,2) NOT NULL; duration_days int NULL (NULL for the Free plan); active boolean NOT NULL DEFAULT true |
| company_subscriptions | company_id → companies CASCADE; plan_id → plans; starts_at timestamptz NOT NULL; ends_at timestamptz NOT NULL; index (company_id, ends_at DESC) |
| payments | company_id → companies CASCADE; plan_id → plans; amount_bdt numeric(10,2) NOT NULL; provider text NOT NULL CHECK (`fake`,`bkash`); provider_payment_id text NULL UNIQUE; merchant_invoice text NOT NULL UNIQUE; status text NOT NULL DEFAULT 'initiated' CHECK (`initiated`,`completed`,`failed`,`cancelled`); completed_at timestamptz NULL |

Seed plans (**placeholders; the owner must confirm names, quotas and prices before launch**): Free (20 screenings a month, 0 BDT, duration NULL), Standard (200, 1500 BDT, 30 days), Pro (1000, 5000 BDT, 30 days).

### Part 5.3 migration (`V5_3_1__mfa.sql`)

`ALTER TABLE users` add: `mfa_enabled boolean NOT NULL DEFAULT false`, `mfa_secret_enc text NULL`, `mfa_pending_secret_enc text NULL`, `mfa_last_step bigint NULL`.

## API

### Part 5.1 (Admin and account)

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/admin/stats` | Admin | `{usersByRole:{...}, jobsByStatus:{...}, applicationsTotal, aiCallsThisMonth, aiCostThisMonthUsd}` from `ai_calls` |
| GET `/api/admin/users?q&role&locked&page` | Admin | item `{id, name, email, role, companyId, locked, disabled, mfaEnabled, createdAt}`; `q` matches name or email |
| PATCH `/api/admin/users/{id}/role` | Admin | `{role}` with the rules above |
| POST `/api/admin/users/{id}/unlock` | Admin | resets `failed_login_count` and `locked_until` |
| POST `/api/admin/users/{id}/password-reset` | Admin | creates a reset token and mails the link (console in local) |
| GET `/api/admin/audit?action&resourceType&actorId&from&to&page&size` | Admin | item `{id, createdAt, actorId, actorEmail, action, resourceType, resourceId, details}` |
| GET `/api/admin/audit?...&format=csv` | Admin | streams all matching rows (cap 50,000) as CSV with header row; audits `audit_exported` |
| GET `/api/account/export` | Signed in | JSON file download |
| DELETE `/api/account` | Signed in | `{password, confirmation:"DELETE"}` → 204; 400 wrong password; 409 `LAST_ADMIN`, `LAST_HR` |

### Part 5.2

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/employer/analytics?jobId&from&to` | HR, Mgr | dates ISO, default last 90 days; response below |
| GET `/api/employer/billing` | HR | `{plan:{name, monthlyScreenings, endsAt}, used, quota, plans:[{id, name, monthlyScreenings, priceBdt, durationDays}]}` |
| GET `/api/employer/billing/payments?page` | HR | `{id, planName, amountBdt, status, createdAt}` |
| POST `/api/employer/billing/checkout` | HR | `{planId}` → `{redirectUrl}`; Free plan is not purchasable (400) |
| GET `/api/payments/bkash/callback?paymentID&status` | Public | verify, execute, then 302 to `${APP_BASE_URL}/employer/billing?payment=success\|failed\|cancelled` |
| GET `/api/payments/fake/pay?paymentId&outcome` | Public, `local` profile only | simulates the provider and calls the same completion code |
| (changed) `POST /api/employer/jobs/{id}/screening` | HR | 409 `QUOTA_EXCEEDED` |
| (changed) `GET /api/employer/dashboard` | HR | `quota: {used, quota, planName, endsAt}` for HR only (null for managers) |

Analytics response: `{funnel:[{stage, count}], timeToHire:[{month, hires, avgDays}], overrides:[{date, userName, jobTitle, alias, direction, reason}], pullBacks}`.
- Funnel stages (counted once per application, using `application_status_history`, for applications created in range): `applied` (all), `screened` (ever `screened`), `shortlisted`, `interview`, `hired`.
- Time-to-hire: for applications that reached `hired`, days from the `applied` history row to the `hired` row, grouped by the month of hiring.
- Overrides: applications with `override_direction <> 'none'` in range; `userName` is the employer staff member who decided (null becomes "Former user"); show the **alias**, never the candidate's name.
- `pullBacks`: applications with `pulled_back_at` in range.

`PaymentProvider` interface: `CreatedPayment createPayment(BigDecimal amountBdt, String merchantInvoice, String callbackUrl)` returning `{providerPaymentId, redirectUrl}`; `ExecutionResult execute(String providerPaymentId)` returning `{success, transactionId}`. Select with `PAYMENT_PROVIDER=fake|bkash` (default `fake`).

Callback handling (idempotent): find the payment by `provider_payment_id` (404 if unknown); if already `completed` just redirect; if `status` is `success` call `execute`, check the amount equals the payment row, then mark `completed`, set `completed_at`, end any active subscription now and create the new subscription (`starts_at = now`, `ends_at = now + duration_days`); otherwise mark `failed` or `cancelled`; audit `payment_completed` or `payment_failed`.

`BkashProvider` (tokenized checkout, sandbox): grant token, create payment, execute payment, query status; cache the token until it expires. Env: `BKASH_BASE_URL`, `BKASH_APP_KEY`, `BKASH_APP_SECRET`, `BKASH_USERNAME`, `BKASH_PASSWORD`. As I remember it, create takes `{mode:"0011", payerReference, callbackURL, amount (string, 2 decimals), currency:"BDT", intent:"sale", merchantInvoiceNumber}` and returns `paymentID` and `bkashURL`. **Confirm every endpoint path, header name and field against bKash's current developer documentation before coding; do not trust memory.** The callback is a browser redirect, so `localhost` works in the sandbox.

### Part 5.3

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/account/mfa/setup` | Signed in | 409 if already enabled; generates a 20-byte secret, stores it encrypted as pending → `{secret, otpauthUri, qrDataUri}` (`otpauth://totp/FairFold:<email>?secret=...&issuer=FairFold`) |
| POST `/api/account/mfa/enable` | Signed in | `{code}` against the pending secret → moves it to the active secret, `mfa_enabled = true`; audit `mfa_enabled` |
| POST `/api/account/mfa/disable` | Signed in | `{password, code}`; audit `mfa_disabled` |
| (changed) POST `/api/auth/login` | Guest | when MFA is on and the password is right: 200 `{mfaRequired:true, mfaToken}` |
| POST `/api/auth/mfa/verify` | Guest | `{mfaToken, code}` → session like login; 401 `MFA_CODE_INVALID` with `triesLeft`; wrong codes increase `failed_login_count` and lock at 5 |
| POST `/api/admin/users/{id}/reset-mfa` | Admin | clears all MFA columns; audit `mfa_reset` *(proposal, record it in progress.md)* |

A TOTP library such as `dev.samstevens.totp:totp` can generate secrets, QR codes and verify codes; check its current version, or implement RFC 6238 directly.

## Pages

| Page | Part | Shows and does |
|---|---|---|
| P31 Admin dashboard | 5.1 | Stat blocks: users by role, jobs by status, applications total, AI calls and estimated AI cost this month. `/admin` now shows this page. |
| P32 Users | 5.1, 5.3 | Search, role and locked filters; table (name, email, role, status, MFA, created); row actions: change role (dialog with confirm), unlock, send password reset, reset MFA (confirm). |
| P33 Audit log | 5.1 | Filters (action, resource, user, date range); table (time, actor, action, resource, details); "Export CSV" for the current filters. |
| P10 Account settings | 5.1, 5.3 | Adds "Download my data" (fetch as blob and save the file), "Delete my account" block with password and typed `DELETE` (danger style, shows 409 explanations), and the **Two-step login (MFA)** card |
| P28 Analytics | 5.2 | Filters (job, date range); funnel as a horizontal bar chart; time-to-hire as bars of average days per month; overrides table (date, user, job, alias, direction, reason); pull-back count; every chart has "View as table" |
| P30 Billing | 5.2 | Current plan and expiry, quota meter (ProgressBar), plan cards with Buy (redirects to `redirectUrl`), payment history, result banner from `?payment=` then refetch |
| P20 Dashboard | 5.2 | Quota block for HR only |
| P03 Login | 5.3 | After password, if `mfaRequired`: 6-digit `CodeInput` step with a link back; shows tries left |
| P24 Applicants tab | 5.2 | If Screen All returns `QUOTA_EXCEEDED`, show the used and quota numbers and a link to Billing |

Nav: admin gets Dashboard, Users, Audit log, Content; HR adds Analytics, Billing; manager adds Analytics.

---

## Part 5.1 — Admin and compliance

**Tasks**
1. Admin stats, users (search, filters, role change with guards, unlock, password reset), audit query and CSV export with streaming.
2. `AccountExportService` and `AccountErasureService` exactly as specified; update the chunk 1 `GET /api/auth/me` only if needed.
3. Frontend: P31, P32, P33, P10 additions (export download, delete block). Complete the admin nav.
4. Add a test that calls `DELETE /api/account` for a candidate and an employer with data in every table and asserts nothing personal remains.

**Tests:** `Fr037_*`, `Fr038_*` (role rules and every 409, unlock, reset token created), `Fr039_*` (filters, CSV shape and header, audit entry for export), `Fr040_*` (contains the listed sections, excludes password hash, tokens and secrets), `Fr041_*` (wrong password 400, last admin 409, last HR 409, files deleted, messages blanked with sender null, audit rows kept with `actor_id` null, candidate cascade behaviour), audit-details-contain-no-personal-data test.

**Definition of done:** the admin can find a user, change an employer's role, unlock a locked account, and export an audit CSV; a candidate downloads their data and then deletes their account, after which they cannot log in and their files are gone.

---

## Part 5.2 — Analytics and billing

**Tasks**
1. Migration `V5_2_1` with seed plans.
2. `AnalyticsService` queries from `application_status_history` and `applications`; endpoint.
3. `PaymentProvider`, `FakePaymentProvider` (local only, with the fake pay endpoint), `BillingService` (checkout, callback, subscriptions, effective plan), `QuotaService`; wire the quota check into the screening start; dashboard quota; endpoints.
4. Frontend: P28 with `ChartWithTable`, P30, P20 quota block, quota error handling on P24. Chart defaults per `charts/defaults.ts`.
5. Implement `BkashProvider` last, selectable with `PAYMENT_PROVIDER=bkash`, after checking bKash's docs. Keep the fake provider as the default.

**Tests:** `Fr035_*` (funnel counts, time-to-hire average with seeded history, overrides show alias not name, pull-back count, date and job filters, manager access, other company's data never included), `Fr048_*` (effective plan, expiry returns to Free, new purchase replaces the old), `Fr056_*` (fake provider happy path, failed, cancelled, callback idempotency, amount mismatch rejected, unknown payment 404), quota (`QUOTA_EXCEEDED`, rule-based runs free, monthly reset by date).

**Definition of done:** HR opens Analytics and sees the funnel and tables for the demo data; HR buys Standard with the fake provider, returns to Billing with a success banner, and the quota meter shows the new limit; a quota-exceeded run shows the buy link.

---

## Part 5.3 — MFA

**Tasks**
1. Migration `V5_3_1`.
2. `MfaService` (secret generation, AES-GCM encryption, TOTP verify with ±1 step and `mfa_last_step` replay protection), endpoints for setup, enable, disable, verify; login change; `mfaToken` rejected by the access-token filter; admin reset.
3. Frontend: `CodeInput` (square cells, numeric keyboard, paste support), P03 code step, P10 MFA card (setup dialog with QR image, secret as text and a code field; disable dialog), P32 "Reset MFA".
4. Add `MFA_ENCRYPTION_KEY` to `.env.example` and the local config (a dev default is acceptable in `local` only).

**Tests:** `Fr005_*` (setup, enable with a good code, rejects a bad code, secret stored encrypted and never returned after enable, disable needs password and code), `Fr006_*` (login returns `mfaRequired`, valid code logs in, wrong codes increase the failure counter and lock at 5, replayed code rejected, `mfaToken` cannot call normal endpoints), admin reset clears MFA, encryption round trip.

**Definition of done (whole chunk):** demo path: admin views the dashboard and audit log → a user turns on MFA and logs in with a code → HR sees Analytics → HR buys a plan with the fake provider → a candidate exports and deletes their account. Tests green, design check passes, `docs/openapi.json` re-exported, `docs/progress.md` updated with decisions (cascade behaviour on candidate deletion, quota rule, plan replacement, MFA reset).
