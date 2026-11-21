# Prompt 01 — Core Platform (days 1 to 2.5)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document.

**Goal:** an employer registers, creates and activates a job. A candidate registers, builds a profile, uploads a PDF resume and applies. Both see the status and notifications.

**Parts (do one at a time, in order):**
- **Part 1.1** Foundation and authentication
- **Part 1.2** Company, jobs and the public job board
- **Part 1.3** Candidate profile, resume upload, applying, status and notifications

Message to start a part: `Build Part 1.1 from docs/prompts/01_core_platform.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

## Requirements covered (acceptance criteria)

| ID | What it must do |
|---|---|
| FR-001 Registration | Valid email, password of at least 10 characters, first and last name, role (`candidate` or `employer`) and a consent tick create an account that can log in at once and return a session. `employer` becomes `employer_hr` with a new empty company. Admins come only from the seed. `email_verified` defaults to `true` (the verification flow is not built). Duplicate email returns 409 `EMAIL_TAKEN`. |
| FR-002 Login | Correct credentials return a 15-minute access token and a 7-day refresh token (HttpOnly cookie, `SameSite=Strict`, `Path=/api/auth`, `Secure` from `app.cookie-secure`). Counter resets. Wrong credentials return a generic 401 `INVALID_CREDENTIALS`. |
| FR-003 Lockout | Five consecutive failures lock the account for 15 minutes (`locked_until`). While locked, return 423 `ACCOUNT_LOCKED` with minutes left (check lock before checking the password). |
| FR-004 Password recovery | `forgot-password` always returns 202 (never reveals whether the email exists). A reset link valid 1 hour, usable once, is sent (console mail in local). After a reset, revoke all refresh tokens of the user and clear the lockout. |
| FR-007 Token refresh | Valid refresh cookie returns a new access token and a new refresh cookie; the old token row gets `revoked_at`. Reusing a revoked token returns 401. |
| FR-008 Logout | Revokes the current refresh token and clears the cookie (204). |
| FR-009 Profile | Candidate saves phone, title, bio, location, remote preference. |
| FR-010 Resume upload | PDF only, at most 10 MB, checked by file signature (`%PDF-`), not by extension. Stored under a random UUID name through `StorageService`. A new upload makes the old row `is_current = false`. |
| FR-014 Skills | Candidate adds, edits and deletes skills with level (`beginner`, `intermediate`, `expert`) and years (0 to 60). Same skill twice returns 409 `SKILL_EXISTS`. |
| FR-022 Apply | Once per active job. Needs a resume with status `uploaded` or `ready` (409 `RESUME_REQUIRED`), job `active` (409 `JOB_NOT_ACTIVE`), not already applied (409 `ALREADY_APPLIED`). Status `applied`. Both sides get an in-app notification. |
| FR-023 Status tracking | Candidate sees the current status and the history. Each change creates a notification. |
| FR-025 Job creation | Employer saves a draft: title, description, requirements, required and nice-to-have skills (with optional must-have flag), experience level, city, country, remote, optional hard filters (`hf_min_years`, `hf_country`), optional `cutoff_score` (0 to 100). *(The "Suggest screening questions" button is chunk 2. Required assessments are chunk 3.)* |
| FR-026 Job editing | Only drafts can be edited or deleted; otherwise 409 `JOB_NOT_DRAFT`. Every edit is audited. "Duplicate as draft" copies a job. |
| FR-027 Lifecycle | Activate (needs a complete company profile, else 409 `COMPANY_INCOMPLETE`), pause, resume, close per BR-01. Each action is audited. |
| FR-042 Job board | Anyone sees active jobs newest first. Keyword search over title, description and skills. Filters: location, remote, level. Paging. Company name is hidden unless `show_name_publicly`. Every active job has a stable public URL `/jobs/:id`. |
| FR-045 Company profile | Name, industry, size, "show name publicly" switch, tagline, about (Markdown), website, LinkedIn, logo URL, founded year, headquarters, contact email, phone, benefits (Markdown). Required (name, industry, size) before a job can be activated. |
| FR-046 Markdown | Job description/requirements and company about/benefits are Markdown. Editor: toolbar (heading, bold, italic, code, link, lists, quote, hr) + live preview. Renderer builds React elements (no raw HTML), links only `http(s)`/`mailto`. |

Business rule BR-01 (job lifecycle): `draft → active ↔ paused → closed`; only `active` accepts applications; `closed` is final.
Business rule BR-03 (hard filters): stored on the job now (`hf_min_years`, `hf_country`, must-have skills); they are applied in chunk 2.

## Database

Create tables in this order. Follow the MariaDB type rules in 00_context section 5.2: `id char(36) NOT NULL DEFAULT (UUID())` primary keys, and `created_at`/`updated_at` `datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)` (`updated_at` also `ON UPDATE CURRENT_TIMESTAMP(3)`). Engine InnoDB, `utf8mb4`, collation `utf8mb4_0900_as_ci`.

### Part 1.1 migrations (`V1_1_1__auth_tables.sql`)

| Table | Columns |
|---|---|
| companies | name text NULL; industry text NULL; size text NULL CHECK (`1-10`,`11-50`,`51-200`,`201-1000`,`1000+`); show_name_publicly boolean NOT NULL DEFAULT false; *(V1_4_1)* tagline varchar(160); about TEXT (Markdown); website, logo_url, linkedin_url varchar(2048); founded_year smallint CHECK 1800–2100; headquarters varchar(255); contact_email varchar(254); phone varchar(40); benefits TEXT (Markdown) — all NULL |
| users | email varchar(254) NOT NULL (store lower-case; `UNIQUE (email)`, case-insensitive through the collation); password_hash text NOT NULL; first_name text NOT NULL; last_name text NOT NULL; role text NOT NULL CHECK (`candidate`,`employer_hr`,`employer_manager`,`interviewer`,`admin`); company_id char(36) NULL → companies; email_verified boolean NOT NULL DEFAULT true; failed_login_count int NOT NULL DEFAULT 0; locked_until datetime(3) NULL; consent_at datetime(3) NOT NULL |
| refresh_tokens | user_id char(36) NOT NULL → users CASCADE; token_hash text NOT NULL UNIQUE (SHA-256 of the token); expires_at datetime(3) NOT NULL; revoked_at datetime(3) NULL |
| password_reset_tokens | user_id → users CASCADE; token_hash text UNIQUE; expires_at; used_at NULL |
| audit_log | actor_id char(36) NULL (no FK); action text NOT NULL; resource_type text NOT NULL; resource_id text NULL; details json NOT NULL DEFAULT (JSON_OBJECT()); created_at; indexes on (created_at DESC), (actor_id), (action) |

### Part 1.2 migrations (`V1_2_1__skills_jobs.sql`, `V1_2_2__seed_skills.sql`)

| Table | Columns |
|---|---|
| skills | name varchar(100) NOT NULL UNIQUE (case-insensitive through the collation); created_at |
| jobs | company_id char(36) NOT NULL → companies; created_by char(36) NULL → users SET NULL; title text NOT NULL; description text NOT NULL; requirements text NOT NULL DEFAULT (''); experience_level text NOT NULL CHECK (`entry`,`mid`,`senior`,`lead`); city text; country text NOT NULL; remote boolean NOT NULL DEFAULT false; status text NOT NULL DEFAULT 'draft' CHECK (`draft`,`active`,`paused`,`closed`); hf_min_years int NULL; hf_country text NULL; cutoff_score int NULL CHECK (0 to 100); a `FULLTEXT` index `ft_jobs_search` on (title, description, requirements) (InnoDB); published_at datetime(3) NULL; closed_at datetime(3) NULL; indexes on (company_id, status), (status, published_at DESC) |
| job_skills | job_id → jobs CASCADE; skill_id → skills; kind text CHECK (`required`,`nice_to_have`); must_have boolean NOT NULL DEFAULT false; UNIQUE (job_id, skill_id); must_have only allowed when kind = `required` (CHECK) |

Seed about 100 common skills (languages, frameworks, databases, cloud, tools, soft skills such as Communication, Leadership, Project Management) in `V1_2_2__seed_skills.sql`.

### Part 1.3 migrations (`V1_3_1__candidate_applications.sql`)

| Table | Columns |
|---|---|
| candidate_profiles | user_id char(36) PK → users CASCADE; phone text NULL; title text NULL; bio text NULL; location text NULL; remote_preference text NOT NULL DEFAULT 'any' CHECK (`onsite`,`hybrid`,`remote`,`any`); total_experience_years decimal(4,1) NULL; education_level text NULL CHECK (`high_school`,`diploma`,`bachelor`,`master`,`phd`,`other`); previous_job_titles json NOT NULL DEFAULT (JSON_ARRAY()); updated_at |
| candidate_skills | user_id → users CASCADE; skill_id → skills; level text CHECK (`beginner`,`intermediate`,`expert`); years decimal(3,1) NULL; verified boolean NOT NULL DEFAULT false; verified_at datetime(3) NULL; UNIQUE (user_id, skill_id) |
| resumes | user_id → users CASCADE; original_filename text; storage_key text NOT NULL UNIQUE; size_bytes bigint; status text NOT NULL DEFAULT 'uploaded' CHECK (`uploaded`,`processing`,`ready`,`failed`); failure_reason text NULL; stripped_text text NULL; is_current boolean NOT NULL DEFAULT true; `current_user_key char(36) GENERATED ALWAYS AS (CASE WHEN is_current = 1 THEN user_id END) VIRTUAL` with a `UNIQUE` index on it (MariaDB has no partial indexes; NULLs do not collide, so each user has at most one current resume) |
| applications | job_id → jobs; candidate_id → users CASCADE; resume_id → resumes; status text NOT NULL DEFAULT 'applied' CHECK (the eight statuses in 00_context); UNIQUE (job_id, candidate_id); index (job_id, status) |
| application_status_history | application_id → applications CASCADE; from_status text NULL; to_status text NOT NULL; changed_by char(36) NULL → users SET NULL; reason text NULL; created_at |
| notifications | user_id → users CASCADE; type text; title text; body text; link text NULL; read_at datetime(3) NULL; index (user_id, read_at, created_at DESC) |

## API

### Part 1.1

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/auth/register` | Guest | Body `{email, password, firstName, lastName, role, consent}` → 201 `{accessToken, user}` + refresh cookie |
| POST `/api/auth/login` | Guest | → 200 `{accessToken, user}`; 401 `INVALID_CREDENTIALS`; 423 `ACCOUNT_LOCKED` |
| POST `/api/auth/refresh` | Cookie | → `{accessToken, user}` + new cookie |
| POST `/api/auth/logout` | Any | → 204 |
| POST `/api/auth/forgot-password` | Guest | `{email}` → 202 always |
| POST `/api/auth/reset-password` | Guest | `{token, newPassword}` → 204; 400 `TOKEN_INVALID` |
| GET `/api/auth/me` | Signed in | `{id, email, firstName, lastName, role, companyId}` |
| POST `/api/account/change-password` | Signed in | `{currentPassword, newPassword}` → 204 |
| GET `/api/health` | Public | `{status:"ok"}` |

`user` object: `{id, email, firstName, lastName, role, companyId}`.

### Part 1.2

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/skills/suggest?q=` | Signed in | up to 10 skills `{id, name}` by prefix or contains |
| GET `/api/jobs?q&location&remote&level&page&size` | Public | active jobs only; item `{id, title, city, country, remote, experienceLevel, topSkills[≤5], publishedAt, companyName?}` |
| GET `/api/jobs/{id}` | Public | active only (else 404); adds description, requirements, requiredSkills[], niceToHaveSkills[] |
| GET `/api/employer/company` | HR | |
| PUT `/api/employer/company` | HR | `{name, industry, size, showNamePublicly, tagline?, about?, website?, logoUrl?, linkedinUrl?, foundedYear?, headquarters?, contactEmail?, phone?, benefits?}` (URLs must be http/https) |
| GET `/api/employer/jobs?status&page` | HR, Mgr | item `{id, title, status, applicationsCount, awaitingScreeningCount, createdAt}` |
| POST `/api/employer/jobs` | HR | create draft |
| GET `/api/employer/jobs/{id}` | HR, Mgr | full job incl. hard filters and cut-off |
| PUT `/api/employer/jobs/{id}` | HR | drafts only |
| DELETE `/api/employer/jobs/{id}` | HR | drafts only |
| POST `/api/employer/jobs/{id}/activate`, `/pause`, `/resume`, `/close`, `/duplicate` | HR | lifecycle; duplicate returns the new draft |

Job body: `{title, description, requirements, experienceLevel, city, country, remote, requiredSkillIds[], niceToHaveSkillIds[], mustHaveSkillIds[], hfMinYears, hfCountry, cutoffScore}`.

Search rule: `q` matches `MATCH(title, description, requirements) AGAINST (:q IN NATURAL LANGUAGE MODE)` **or** an exact case-insensitive skill name on the job **or**, when `q` is shorter than 3 characters (InnoDB ignores such words), `title LIKE %q%` with `%`, `_` and `\\` escaped. Full-text results are visible only after commit, so search tests must not use a rolled-back transaction. `location` matches city or country (case-insensitive contains).

### Part 1.3

| Method and path | Who | Notes |
|---|---|---|
| GET, PUT `/api/candidate/profile` | Cand. | all profile fields |
| GET `/api/candidate/skills`; POST; PUT, DELETE `/api/candidate/skills/{id}` | Cand. | `{skillId, level, years}` |
| POST `/api/candidate/resume` | Cand. | multipart field `file` → `{id, filename, sizeBytes, status, failureReason, uploadedAt}` |
| GET `/api/candidate/resume` | Cand. | current resume, or 404 `RESUME_NOT_FOUND` |
| POST `/api/jobs/{id}/applications` | Cand. | → 201 application |
| GET `/api/candidate/applications?status&page` | Cand. | item `{id, jobId, jobTitle, companyName?, status, appliedAt}` |
| GET `/api/candidate/applications/{id}` | Cand. | adds job summary and `history[{status, at}]` |
| GET `/api/candidate/dashboard` | Cand. | `{checklist:{profileSaved, resumeUploaded, skillsAdded}, latestApplications[5]}` (profileSaved = title and location filled) |
| GET `/api/employer/jobs/{id}/applications?page` | HR, Mgr | item `{id, label:"Applicant 1", status, appliedAt}`; the label is the ordinal by `created_at` within the job. **No name, email or phone.** |
| GET `/api/notifications?page`; GET `/unread-count`; POST `/{id}/read`; POST `/read-all` | Signed in | |

Notification types now: `application_submitted` (to the candidate), `application_received` (to the job's creator), `status_changed`.

## Pages

| Page | Part | Shows and does |
|---|---|---|
| P03 Login | 1.1 | Email, password, links to register and forgot; shows minutes left when locked; redirect by role or `?next=` |
| P04 Register | 1.1 | First and last name, email, password (hint: minimum 10), role choice, consent checkbox; signs in on success |
| P06 Forgot password | 1.1 | Email field; always shows "If the account exists, we sent a link" |
| P07 Reset password | 1.1 | New password and confirmation from `?token=` |
| P11 Not found / No access | 1.1 | Large code, one sentence, button to the user's home |
| P01 Jobs (home) | 1.2 | Search bar, filters (location, remote, level), job rows, paging; filters in the URL |
| P02 Job detail | 1.2 | Title, company (if allowed), details, skills, copy link; Apply button (wired in 1.3) |
| P21 Company profile | 1.2 | Sections Basics (name, industry, size, tagline, founded, HQ, show-name switch), Contact and links, About and benefits (Markdown editors); Save |
| P22 Jobs | 1.2 | Table, status filter, New job, row menu (Open, Edit, Duplicate, Activate, Pause, Resume, Close, Delete), banner if the company profile is incomplete |
| P23 Job editor | 1.2 | One form: basics, skills (required, nice-to-have, must-have via suggest), level and location, hard filters, cut-off score; "Leave without saving?" prompt; drafts only |
| P24 Job workspace | 1.2, 1.3 | Back link, header with status badge, lifecycle buttons (HR), copy public link (active only); tabs **Overview** (default: rendered Markdown + "at a glance" sidebar with status, applicant count, level, location, skills, screening) and **Applicants (n)** |
| P09 Notifications | 1.3 | List with unread dot, click marks read and opens the link; "Mark all read"; top-bar bell with count and last five |
| P10 Account settings | 1.3 | Name and email read-only, change password form (export, delete, MFA come in chunk 5) |
| P12 Candidate dashboard | 1.3 | Setup checklist, latest five applications |
| P13 Profile | 1.3 | Sections *Basics* (phone, title, bio, location, remote) and *Skills* (table, add and edit dialog with suggest, delete with confirm); other sections arrive later |
| P14 Resume | 1.3 | Drop zone (PDF, 10 MB, "English resumes only" notice), shows current file, status chip |
| P15 My applications | 1.3 | Table with status filter, paging |
| P16 Application detail | 1.3 | Job summary and status timeline |

Employer home `/employer` redirects to `/employer/jobs` until P20 exists (chunk 2). Sidebar entries appear only for pages that exist (config array with an `enabled` flag per item).

---

## Part 1.1 — Foundation and authentication

**Tasks**
1. **Backend skeleton** (Spring Initializr: Maven, Java 21, Web, Security, Data JPA, Validation, Flyway Migration, MariaDB Driver, Mail; then add `org.flywaydb:flyway-mysql`, which provides the MariaDB/MySQL support). Packages from 00_context. `application.yml`, `application-local.yml` (datasource from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` with the JDBC URL from 00_context section 3, driver `org.mariadb.jdbc.Driver`, `hibernate.jdbc.time_zone=UTC`, `ddl-auto=none`, `app.storage-dir`, `app.cookie-secure: false`, `app.mail.mode: console`, `app.auth.require-email-verification: false`, JWT secret from env), `application-test.yml` (`fairfold_test`). Add springdoc 2.8.x, an `OpenApiConfig` bean with a bearer JWT scheme. Swagger paths are open **only in the `local` profile**.
2. Common: `GlobalExceptionHandler` producing the error body, paging DTO, `AuditService`, `MailService` (+ console implementation that prints links), `Clock` bean for testable time.
3. Migration `V1_1_1`. Entities and repositories for companies, users, refresh tokens, reset tokens, audit log.
4. Security: stateless filter chain, JWT service (HS256, claims `sub`, `role`, `companyId`, 15 min), BCrypt, method security. Permit `/api/auth/**` (except `me` and `change-password` need auth), `GET /api/jobs/**`, `/api/health`; everything else authenticated.
5. Auth endpoints per the table. Refresh tokens: 256-bit random, stored hashed, rotated. Cookie `refresh_token`.
6. Rate limit with Bucket4j: 10 requests per minute per IP on login, register and forgot-password (429 with `Retry-After`).
7. Startup admin seed from `ADMIN_EMAIL` and `ADMIN_PASSWORD` when no such user exists.
8. **Frontend skeleton:** Vite + React + TypeScript, Tailwind configured exactly as in 00_context section 9 (zero radius, no shadows, no gradients, token colours as CSS variables), `styles/tokens.css`, Inter via `@fontsource-variable/inter`, `scripts/check-design.sh`, Vite proxy for `/api`.
9. Base components (square, token colours): Button, Input, Select, Textarea, Checkbox, Badge (+ `statusMap.ts`), Card, Table, Dialog, Toast, Banner, Skeleton, Tabs, Icon. Build only what pages need now.
10. Layouts: PublicShell, AuthShell, PortalShell (sidebar with `enabled` flags, top bar placeholder for the bell and account menu).
11. Router with lazy portals, `RequireAuth`, `RequireRole`, auth context with `bootstrapSession()`, Axios client with the single-flight refresh interceptor, error mapping.
12. Pages P03, P04, P06, P07, P11. After login, show a simple "Welcome" page for each role at its home route.

**Tests (backend)** `Fr001_*` (valid, duplicate email, short password, no consent, employer gets company), `Fr002_*`, `Fr003_lockout_after_five_failures_and_423_minutes_left`, `Fr004_*` (always 202, token single use, tokens revoked), `Fr007_*` (rotation, reuse returns 401), `Fr008_*`, rate-limit test. **Frontend (Vitest):** route-guard decision function, API error mapper.

**Definition of done:** you can register, log in, refresh, log out and reset a password in both Swagger UI and the browser; the lock message works; `./mvnw test` is green; `check-design.sh` passes; `docs/openapi.json` exported.

---

## Part 1.2 — Company, jobs and the public job board

**Tasks**
1. Migrations `V1_2_1` and `V1_2_2` (skills seed).
2. Company: GET and PUT (HR). Completeness = name, industry and size all set.
3. Jobs: DTOs and validation (title ≤ 200 characters, description required, country required, cut-off 0 to 100, must-have skills must be a subset of required). Create, read, update (draft only), delete (draft only), lifecycle actions with BR-01 transitions, duplicate. All actions call `AuditService` (`job_created`, `job_updated`, `job_activated`, `job_paused`, `job_resumed`, `job_closed`, `job_duplicated`, `job_deleted`). Activation sets `published_at` on first activation and requires a complete company (409 `COMPANY_INCOMPLETE`).
4. Public board: search and filters as specified, paging, company name hidden unless `show_name_publicly`. Skills suggest endpoint.
5. Frontend API modules and queries for jobs, company, skills.
6. Pages P21, P22, P23, P24 (Applicants tab shows "No applicants yet" for now), P01, P02 (Apply button visible for candidates, wired in 1.3; guests go to login with `next`).
7. Add the employer nav items (Jobs, Company) with `enabled` flags.

**Tests:** `Fr025_*` (create, validation), `Fr026_edit_only_in_draft_409`, `Fr027_*` (every transition, illegal transitions 409, activation needs company), `Fr042_*` (search by text, by skill name, filters, hidden company name, inactive jobs not listed), `Fr045_*`, a company-scope test (another company's job returns 404).

**Definition of done:** an HR user fills the company profile, creates a job with skills, activates it, and finds it on the public board; pause hides it, resume shows it, close removes it.

---

## Part 1.3 — Candidate profile, resume, applying, status, notifications

**Tasks**
1. Migration `V1_3_1`.
2. `StorageService` interface (`save`, `load`, `delete`) with `LocalDiskStorageService` using `app.storage-dir`. Resume upload with signature check and size limit; previous resume gets `is_current = false`.
3. Profile and skills endpoints; skills deduplicated per user.
4. Apply flow with all checks; writes the history row (`null → applied`), notifications for both sides, audit `application_created`.
5. Candidate application list and detail; employer applicant list with ordinal labels. Build the DTO layer so employer views cannot include identity; add `NoIdentityLeakTest` that registers a candidate with a distinctive name and email, applies, and asserts the employer response body contains neither.
6. Notifications endpoints and the `NotificationService`. Status changes (any later chunk) must call it; write a helper `ApplicationStatusService.changeStatus(application, to, actor, reason)` that updates the status, writes history and notifies. **All later chunks must use this helper.**
7. Candidate dashboard endpoint.
8. Frontend: pages P09 (+ bell in the top bar, polling the count every 60 s), P10, P12, P13 (Basics and Skills), P14, P15, P16; wire the Apply button on P02 (confirm dialog; map 409 codes to friendly messages and a link to P14); fill the Applicants tab on P24.
9. Add candidate nav items (Dashboard, Profile, Resume, Applications, Jobs).

**Tests:** `Fr009_*`, `Fr010_*` (rejects non-PDF with a `.pdf` name, rejects over 10 MB, accepts a valid PDF, replaces old), `Fr014_*`, `Fr022_*` (all 409 cases), `Fr023_*` (history order, notification created), `NoIdentityLeakTest`.

**Definition of done (whole chunk):** demo path works end to end: employer registers, completes company, creates and activates a job → candidate registers, completes profile and skills, uploads a PDF, applies → candidate sees status `applied` and a notification; employer sees a notification and "Applicant 1" in the list. All tests green, design check passes, `docs/openapi.json` committed, `docs/progress.md` updated with decisions and any open issues.
