# FairFold — Shared Context (read this first, every session)

Version 1.2 · 21 November 2026 (database is MariaDB; the earlier PostgreSQL/MySQL drafts are obsolete)

> Everything an AI coding assistant needs to know about FairFold is in this file plus ONE chunk prompt (`01` to `06`). **Do not read the PRD, architecture, design or plan files.** If something is unclear or missing, ask the user and write the answer in `docs/progress.md` under Decisions.

## 1. How a session works

1. Read this file, the chunk prompt you were pointed to, and `docs/progress.md`.
2. Build **only the part named by the user** (for example "Part 2.1"). Do not start the next part.
3. Work in slices, one requirement at a time: migration → entity and repository → service → controller (with springdoc annotations) → test → frontend API module → page.
4. A part is finished when: all tests pass, `scripts/check-design.sh` passes (once the frontend exists), `docs/openapi.json` is re-exported, `docs/progress.md` is updated, and the work is committed as `Part N.M: <summary>`.
5. If you must deviate from the prompt, write the deviation and the reason in `docs/progress.md` under Decisions.

## 2. Product in brief

FairFold is a recruitment web app with two portals.

- **Employers** post jobs, receive **anonymised** applications, and get a ranked list where every score is explained with quotes from the candidate's own resume. They shortlist; only then are the candidate's name and contact details revealed.
- **Candidates** build a profile from a PDF resume, apply, see how they were scored, prove skills with assessments, practise interviews in text, and see a career journey.
- **AI is advisory.** Humans decide. Nothing personal is ever sent to the AI provider.
- Roles: `candidate`, `employer_hr`, `employer_manager`, `interviewer`, `admin`. One role per account.

Non-goals: Bangla resumes, local ML or vector search, video or audio interviews, scraping, courses, automatic hiring decisions, candidate paid plans, native apps, recurring billing, calendar files, light theme.

## 3. Stack and run commands

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.x (Maven), Spring Web, Security, Data JPA, Validation, Flyway (`flyway-core` and `flyway-mysql`), MariaDB driver (`org.mariadb.jdbc:mariadb-java-client`), Mail |
| API docs | springdoc-openapi `springdoc-openapi-starter-webmvc-ui` 2.8.x (the 3.x line is for Spring Boot 4) |
| Added later | PDFBox, jjwt (JWT), Bucket4j (rate limit), a TOTP library (chunk 5) |
| Database | MariaDB (verified on 13.0.2; InnoDB, `utf8mb4`, collation `utf8mb4_0900_as_ci`), Flyway migrations. Installed locally for now; Docker comes after chunk 6. |
| Frontend | React 18, TypeScript, Vite, React Router, Axios, TanStack Query, React Hook Form (built-in rules only), Tailwind (dark only), react-chartjs-2 with chart.js, lucide-react, `@fontsource-variable/inter` |
| AI | DeepSeek (OpenAI-compatible chat API) behind `AiProvider`, plus `RuleBasedFallbackProvider` |
| Payments | Stripe Checkout (test mode) behind `PaymentProvider`, plus `FakePaymentProvider` |
| Files | Local disk behind `StorageService` |
| Tests | JUnit 5, Mockito, Spring Boot Test + MockMvc on a local MariaDB database `fairfold_test` (Testcontainers with `mariadb:13.0` only in chunk 6), Vitest for key frontend logic |

| What | Command | URL |
|---|---|---|
| Backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | http://localhost:8080 |
| Swagger UI / JSON | (local profile only) | http://localhost:8080/swagger-ui.html , /v3/api-docs |
| Frontend | `cd frontend && npm run dev` | http://localhost:5173 (Vite proxies `/api` to 8080) |
| Backend tests | `cd backend && ./mvnw test` | uses database `fairfold_test` |
| Design check | `./scripts/check-design.sh` | |
| Export API contract | `curl http://localhost:8080/v3/api-docs -o docs/openapi.json` | |

**Local database setup (once, before chunk 1; no Docker yet).** Install MariaDB on the host, then run as root (`sudo mariadb`, or `mariadb -u root -p`):

```sql
CREATE DATABASE fairfold      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE DATABASE fairfold_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE USER 'fairfold'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON fairfold.*      TO 'fairfold'@'%';
GRANT ALL PRIVILEGES ON fairfold_test.* TO 'fairfold'@'%';
```

JDBC URL pattern: `jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:fairfold}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8` with driver class `org.mariadb.jdbc.Driver`. Full-text search note: InnoDB ignores words shorter than `innodb_ft_min_token_size` (default 3), so the job search also uses an exact skill-name match and a `LIKE` fallback (chunk 1).

Everything configurable comes from environment variables with a safe local default (`application-local.yml`). Never hard-code paths, URLs or keys. The frontend calls relative URLs only (`/api/...`).

## 4. Repository layout

```text
fairfold/
  backend/src/main/java/com/fairfold/
    common/      errors, paging, security helpers, audit, notification, mail, storage
    config/      security, openapi, async, properties
    auth/ user/ company/ job/ application/ resume/ screening/ ai/
    assessment/ coaching/ interview/ message/ team/ billing/ admin/
  backend/src/main/resources/ db/migration/  application*.yml  seed/
  backend/src/test/java/...  backend/src/test/resources/
  frontend/src/
    app/ (router, providers, guards)   api/   features/<feature>/{pages,components,queries.ts,types.ts}
    components/ui/   components/layout/   hooks/   lib/   styles/
  docs/   (PRD, architecture, design, plan, roadmap, prompts/, progress.md, openapi.json)
  scripts/ (check-design.sh)   data/ (git-ignored resume files)   .env.example
```

Feature packages hold controller, service, repository, entity, dto (Java records). Entities are never returned from controllers.

## 5. Conventions

### 5.1 API

- Base path `/api`. JSON, camelCase. IDs are UUID strings. Times are ISO-8601 UTC.
- Auth: `Authorization: Bearer <accessToken>`. Access token JWT (15 minutes). Refresh token in an HttpOnly cookie (7 days, rotated).
- Errors: `{ "timestamp", "status", "error", "code", "message", "errors": [{ "field", "message" }] }`.
  - 400 validation, 401 not signed in, 403 wrong role, 404 not found (also for another company's data), 409 rule conflict (machine `code`, for example `JOB_NOT_DRAFT`), 422 missing required reason, 423 account locked (message holds minutes left), 429 with `Retry-After`, 500.
- Paging: `?page=0&size=20` (size max 100). Response `{ items, page, size, totalItems, totalPages }`.
- Every employer query is scoped to the caller's `company_id`. Another company's record returns 404.
- Controllers are thin; services are `@Transactional`; use Jakarta Validation on DTOs.
- Annotate with springdoc: `@Tag` on controllers, `@Operation(summary)` on endpoints.
- Audit: `AuditService.record(actorId, action, resourceType, resourceId, detailsJson)`. Notifications: `NotificationService.notify(userId, type, title, body, link)`. Email: `MailService` (console implementation in `local`).
- Configuration through `@ConfigurationProperties("app")`.

### 5.2 Database

- MariaDB, InnoDB, snake_case, character set `utf8mb4`, collation `utf8mb4_0900_as_ci` (case-insensitive, accent-sensitive). Because of the collation, `UNIQUE (email)` is already case-insensitive; still store emails lower-case.
- **Types:** primary keys are `char(36) NOT NULL DEFAULT (UUID())` holding UUID strings. In Java generate ids with `@UuidGenerator` and put `@JdbcTypeCode(SqlTypes.CHAR)` on every `UUID` id and foreign-key field (Hibernate would otherwise use `binary(16)`). Times are `datetime(3)` in **UTC**: `created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)`, `updated_at` the same plus `ON UPDATE CURRENT_TIMESTAMP(3)`. Use `java.time.Instant`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` and `connectionTimeZone=UTC` in the JDBC URL. Flexible lists and objects are `json` (string arrays too; map with `@JdbcTypeCode(SqlTypes.JSON)`). Money and cost are `decimal`. Booleans are `boolean` (`tinyint(1)`).
- **The word `text` in the table lists** is shorthand: use `varchar(255)` for short values and `TEXT` for long free text (description, requirements, bio, stripped_text, quote, body, answer, comments, explanation, note, excerpt, error, reasons). Fixed rules: `email varchar(254)`, `token_hash char(64)`, `storage_key varchar(255)`, `skills.name` and `skill_aliases.alias varchar(100)`, `strip_terms.term varchar(150)`, `url` and `meeting_link varchar(2048)`. Any column inside a UNIQUE or ordinary index must be `varchar` or `char`, never `TEXT`.
- **Lists of allowed values** use `varchar` plus a `CHECK (col IN (...))`, never `ENUM`. CHECK is enforced in MariaDB 10.2+. A CHECK may not use a column that has a foreign-key referential action (CASCADE or SET NULL).
- **Defaults on `TEXT` and `json` must be expressions** in parentheses: `DEFAULT ('')`, `DEFAULT (JSON_ARRAY())`, `DEFAULT (JSON_OBJECT())`.
- **MariaDB has no partial indexes, no arrays and no GIN.** Use a VIRTUAL generated column plus a UNIQUE index (NULLs never collide), `json` columns, and `FULLTEXT` indexes. Never use a STORED generated column over a column with a CASCADE or SET NULL foreign key.
- **DDL is not transactional in MariaDB.** Keep each Flyway file small (one table group). If a migration fails half way, drop the leftovers by hand and run `flyway repair`.
- **Flyway owns the schema.** Set `spring.jpa.hibernate.ddl-auto=none`, not `validate`: the MariaDB JDBC driver reports `json` columns as `longtext`, so Hibernate's JSON type check fails under `validate`. MariaDB stores `json` as `longtext` with a `json_valid` CHECK; keep declaring the columns as `json`.
- Reserved words: if a column name is a MariaDB reserved word, wrap it in backticks in SQL and in `@Column(name = "`...`")`. None are known so far.
- Flyway files: `V<chunk>_<part>_<n>__name.sql`, for example `V1_1_1__auth_tables.sql`. Never edit an applied migration; add a new one.
- Foreign keys to `users`: data owned by the user (profile, skills, resumes, applications as candidate, notifications, tokens, attempts, practice sessions) uses `ON DELETE CASCADE`. Columns that only record who did something (`created_by`, `decided_by`, `changed_by`, `sender_id`, `interviewer_id`) are nullable with `ON DELETE SET NULL`. `audit_log.actor_id` has no foreign key.
- Candidate identity (name, email, phone) lives only in `users` and `candidate_profiles`. No other table copies it.

### 5.3 Frontend

- Query keys are arrays starting with the resource: `['jobs', filters]`, `['employer','job',jobId,'applications',filters]`. Mutations invalidate affected keys.
- Page number and filters live in the URL query string. Server-side paging, size 20.
- Session: access token in memory only (never `localStorage`). Axios interceptor: on 401 call refresh once (single shared promise), retry once, else go to `/login?next=`. On app start call `/api/auth/refresh`.
- Login redirect by role: candidate `/candidate`, HR and manager `/employer`, interviewer `/employer/interviews`, admin `/admin`.
- Error mapping: 400 sets field errors; 403 forbidden page; 404 not-found state; 409 toast with the server message and refetch; 423 shows minutes left; 429 toast with retry time; 5xx inline error with Retry.
- Every list, table and form has loading (skeleton), empty and error states.
- Polling: unread notifications 60 s; resume status 2 s; screening progress 3 s; messages 10 s while a thread is open. Stop when the condition is met or the tab is hidden.
- Money `৳1,500`. Dates in the browser's time zone, for example `12 Nov 2026, 3:30 PM`.

### 5.4 Testing

- Every requirement gets at least one test of its acceptance criteria. Name tests after the requirement, for example `Fr003_lockout_after_five_failures`.
- Test profile uses `fairfold_test`; clean it between tests (Flyway clean-and-migrate once, then per test `SET FOREIGN_KEY_CHECKS=0`, `TRUNCATE` every table, `SET FOREIGN_KEY_CHECKS=1`). Do not wrap tests in a rolled-back transaction: InnoDB full-text indexes see rows only after commit.
- A shared test `NoIdentityLeakTest` (from chunk 1) fails if any employer endpoint response contains a candidate's name or email before shortlist.

## 6. Roles and access

| Capability | Guest | Cand. | HR | Mgr | Interv. | Admin |
|---|---|---|---|---|---|---|
| Browse public jobs | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Profile, journey, resume, apply, assessments, practice, resources | | ✓ | | | | |
| Messaging on a shortlisted application | | ✓ | ✓ | ✓ | | |
| Create, edit, publish jobs; run screening | | | ✓ | | | |
| Ranked list, rationale, shortlist, reject, schedule, hire | | | ✓ | ✓ | | |
| Interview pack: edit | | | ✓ | ✓ | | |
| Interview pack: read, feedback | | | ✓ | ✓ | ✓ | |
| Analytics | | | ✓ | ✓ | | |
| Company profile, team, billing | | | ✓ | | | |
| Users, audit log, content | | | | | | ✓ |

## 7. Status models

| Entity | Values |
|---|---|
| Job | `draft`, `active`, `paused`, `closed` (draft→active, active↔paused, active or paused→closed; closed is final; only drafts are editable or deletable) |
| Application | `applied`, `screening`, `screened`, `not_matched`, `shortlisted`, `interview`, `hired`, `rejected` |
| Screening item | `queued`, `running`, `done`, `failed` |
| Assessment gate | `not_required`, `pending`, `passed`, `failed` |
| Bias status | `clear`, `flagged` |
| Resume | `uploaded`, `processing`, `ready`, `failed` |

Application transitions (anything else returns 409; every change writes `application_status_history`):

| From | To | Trigger |
|---|---|---|
| (new) | applied | Candidate applies |
| applied | screening | Employer confirms Screen All |
| screening | screened / not_matched | Item scored / a hard filter failed |
| not_matched | screening | Pull back (skip filters for this candidate, queue one scoring item, audit, count in analytics) |
| screened | shortlisted | Shortlist (gate must be `passed` or `not_required`; reason if score below cut-off) |
| screened, not_matched | rejected | Reject (reason if score above cut-off) |
| shortlisted | interview | Interview scheduled |
| shortlisted, interview | rejected | Reject |
| interview | hired | Mark hired |

`hired` and `rejected` are final.

## 8. Global rules

1. **No personal data to AI.** Only stripped text goes to the AI provider, after a final scan for the candidate's known identifiers (names, email, phone). If a match is found, block the call, mark the item failed, write an audit entry.
2. **Employers never see identity before shortlist.** Employer DTOs for applications contain an alias (or "Applicant n" in chunk 1), never name, email, phone or photo. Identity appears only after shortlist, with an `identity_reveal` audit entry.
3. **The backend computes every score.** The AI returns verdicts and quotes; the backend validates quotes and calculates the number.
4. **No pay-to-rank.** Plans, payments and coaching activity never change a score or rank.
5. **Coaching is isolated.** Practice tables have no foreign key to jobs or applications.
6. **Messages never go to AI.**
7. **Deactivate, never delete** assessments, practice questions and resources.
8. Email verification is **deferred**: the column `users.email_verified` and setting `app.auth.require-email-verification` (default `false`) exist, but the flow is not built.
9. Secrets and keys come from environment variables. Never log passwords, tokens or resume text.

## 9. Design rules (summary)

**Dark & Light mode support (via CSS variables, `ThemeContext`, and `ThemeToggle`). Smooth rounded corners across the UI (`rounded-md`, `rounded-lg`, `rounded-xl`, `rounded-2xl`, `rounded-full`). No gradients. Subtle shadows. Five-colour palette. One sans-serif font (Lato).**

| Token | Dark Mode | Light Mode | Use |
|---|---|---|---|
| `bg` | `#1C1B1F` | `#F7F6F9` | Page background |
| `surface` | `#26242A` | `#FFFFFF` | Cards, tables, sidebar, top bar |
| `raised` | `#302D36` | `#F0EDF5` | Dialogs, menus, hovered rows |
| `border` | `#4B4453` | `#E3DFE8` | Dividers, card borders |
| `border-strong` | `#8B8396` | `#BCB4C6` | Input and button outlines |
| `text` | `#F4F2F7` | `#191620` | Main text |
| `text-secondary` | `#B0A8B9` | `#5B5365` | Labels, captions |
| `text-subtle` | `#9A92A5` | `#898094` | Placeholders, disabled |
| `primary` | `#845EC2` | `#845EC2` | Primary button fill, active bar (white text on it) |
| `primary-hover` / `primary-pressed` | `#6F4BAA` / `#5C3D91` | `#714BB0` / `#5C3996` | Button states |
| `primary-text` | `#B79CF0` | `#6E3CAE` | Links, active nav text, focus outline, success text |
| `primary-subtle` | `#372E45` | `#F1ECF9` | Selected row, active nav bg, success badge bg |
| `danger` | `#C34A36` | `#C34A36` | Destructive fill, failed badge (white text) |
| `danger-subtle` | `#422B2C` | `#FCEEED` | Error banner bg |
| `accent` | `#FF8066` | `#D65135` | Warning and error text, flags |
| `accent-subtle` | `#4D3535` | `#FDF0ED` | Warning badge and banner bg |
| `highlight` | `#563837` | `#FEEAE4` | Evidence quote background |

- Tailwind config: all `borderRadius` = 0, `boxShadow` = none, `backgroundImage` = {}, `colors` limited to the tokens above (as CSS variables), `fontFamily.sans` only. No `rounded-*`, `shadow-*`, gradient classes or raw hex colours in components.
- Type: Lato only. h1 28/36 700, h2 20/28 600, h3 16/24 600, body 14/22, labels 12/16 600 uppercase tracking 0.06em, numbers with tabular figures. Never below 12 px.
- 1 px borders; depth through `bg` → `surface` → `raised`. Spacing 4 px scale. Control height 40 (44 on mobile).
- Focus: 2 px solid `primary-text`, offset 2 px, on every interactive element.
- Shells: PublicShell (top bar), PortalShell (top bar 56 px + sidebar 248 px from 1024 px, drawer below), FocusShell (assessment attempt), AuthShell (two columns from 900 px, left panel `primary-subtle`).
- Sidebar active item: `primary-subtle` bg, `primary-text`, 3 px `primary` bar on the left.
- Components (all in `components/ui`): Button (primary, secondary, danger, ghost), Input, Select, Textarea, Checkbox, Radio (square with inner square), Switch (rectangular), Badge, Tabs (2 px primary underline), Card, Table (sticky header, row 48 px), Pagination, Dialog, Toast, Banner (4 px left bar), Tooltip, Skeleton (opacity pulse, no shimmer), ProgressBar, ScoreMeter (number + flat 8 px bar, optional cut-off marker), FileDropZone (dashed square), Icon (lucide with `strokeLinecap="square"`, `strokeLinejoin="miter"`).
- Status badges (square, icon + text always; never colour alone), defined once in `lib/statusMap.ts`:

| Meaning | Style | Icon | Used for |
|---|---|---|---|
| Neutral | `raised` fill, 1 px `border`, `text-secondary` | `Minus` | Draft, Applied, None, Not matched |
| In progress | `primary-subtle`, `primary-text` | `Clock` | Screening, Processing, Interview, Pending |
| Success | `primary-subtle`, `primary-text` | `Check` | Active, Screened, Shortlisted, Passed, Ready, Hired, Clear, Met, Verified |
| Warning | `accent-subtle`, `accent` | `TriangleAlert` | Paused, Partial, Flagged, Rule-based |
| Failure | `danger` fill, white | `X` | Failed, Rejected, Broken, Not met |
| Closed | `raised`, `text-subtle`, 1 px `border` | `Lock` | Closed |

- Fairness UI: advisory banner on ranked lists ("Scores are advisory, you decide"); "Rule-based" warning badge when the fallback was used; aliases in a neutral chip; evidence quotes with `highlight` background and 2 px `accent` underline; shortlist dialog warns that identity will be revealed and logged; override dialog needs a written reason; Screen All dialog shows count and cost before running; proctoring signals in neutral chips; practice page says it is private.
- Charts: solid fills, no rounded bars, straight lines with square points, colours `#845EC2 #FF8066 #B0A8B9 #C34A36`, grid `#4B4453`, a "View as table" toggle on every chart.
- Accessibility: WCAG 2.1 AA, keyboard for everything, visible focus, labelled fields, errors as text under fields, `aria-live` for toasts and save status, touch targets 44 px.
- `scripts/check-design.sh` must pass: it fails on `rounded`, `gradient`, `shadow`, `border-radius`, `box-shadow` (except `rounded-none` and radius 0) and on hex colours outside `styles/tokens.css` and `charts/defaults.ts`.

## 10. Pages (36)

| ID | Page | Route | Who | Chunk |
|---|---|---|---|---|
| P01 | Jobs (browse) | `/jobs` | Everyone | 1 |
| P02 | Job detail | `/jobs/:jobId` | Everyone | 1 |
| P02a | Public company profile | `/companies/:companyId` | Everyone | 1 |

`/` is the guest landing page (hero, fairness pillars, latest jobs, CTAs). Signed-in users are redirected to their dashboard (candidate `/candidate`, employer `/employer`, interviewer `/employer/interviews`, admin `/admin`).
| P03 | Login (+ MFA code step) | `/login` | Guest | 1, 5 |
| P04 | Register | `/register` | Guest | 1 |
| P05 | Verify email | `/verify-email?token=` | Guest | later |
| P06 | Forgot password | `/forgot-password` | Guest | 1 |
| P07 | Reset password | `/reset-password?token=` | Guest | 1 |
| P08 | Accept invite | `/accept-invite?token=` | Guest | 4 |
| P09 | Notifications | `/notifications` | Signed in | 1 |
| P10 | Account settings | `/account`, `/account/password` | Signed in | 1, 5 |
| P11 | Not found / No access | `*`, `/403` | Everyone | 1 |
| P12 | Candidate dashboard | `/candidate` | Candidate | 1 |
| P13 | Profile | `/candidate/profile` | Candidate | 1, 2, 3 |
| P14 | Resume | `/candidate/resume` | Candidate | 1, 2 |
| P15 | My applications | `/candidate/applications` | Candidate | 1 |
| P16 | Application detail | `/candidate/applications/:applicationId` | Candidate | 1, 2, 3, 4 |
| P17 | Assessments | `/candidate/assessments` | Candidate | 3 |
| P18 | Assessment attempt | `/candidate/assessments/:assessmentId/attempt` | Candidate | 3 |
| P19 | Interview practice | `/candidate/practice` | Candidate | 4 |
| P20 | Employer dashboard | `/employer` | HR, Mgr | 2 |
| P21 | Company profile | `/employer/company` | HR | 1 |
| P22 | Jobs | `/employer/jobs` | HR, Mgr | 1 |
| P23 | Job editor | `/employer/jobs/new`, `/employer/jobs/:jobId/edit` | HR | 1, 2, 3 |
| P24 | Job workspace | `/employer/jobs/:jobId` | HR, Mgr | 1, 2, 4 |
| P25 | Application review | `/employer/jobs/:jobId/applications/:applicationId` | HR, Mgr | 2, 3, 4 |
| P26 | Interviews | `/employer/interviews` | HR, Mgr, Interv. | 4 |
| P27 | Interview detail | `/employer/interviews/:interviewId` | HR, Mgr, Interv. | 4 |
| P28 | Analytics | `/employer/analytics` | HR, Mgr | 5 |
| P29 | Team | `/employer/team` | HR | 4 |
| P30 | Billing | `/employer/billing` | HR | 5 |
| P31 | Admin dashboard | `/admin` | Admin | 5 |
| P32 | Users | `/admin/users` | Admin | 5 |
| P33 | Audit log | `/admin/audit` | Admin | 5 |
| P34 | Content | `/admin/content` | Admin | 3, 4 |
| P35 | Assessment editor | `/admin/assessments/new`, `/admin/assessments/:assessmentId` | Admin | 3 |
| P36 | Journey | `/candidate/journey` | Candidate | 4 |

Sidebar items: Candidate: Dashboard, Profile, Journey, Resume, Applications, Assessments, Practice, Jobs. HR: Dashboard, Jobs, Interviews, Analytics, Team, Company, Billing. Manager: Dashboard, Jobs, Interviews, Analytics. Interviewer: Interviews. Admin: Dashboard, Users, Audit log, Content. All signed-in roles: notification bell and account menu in the top bar.

## 11. Schema map (all tables)

Full column definitions are in the chunk prompt that creates each table. This map shows what exists, so you can reference tables from earlier chunks. All tables have `id char(36) PK` unless noted.

| Table | Chunk | Key columns and foreign keys |
|---|---|---|
| companies | 1 | name, industry, size, show_name_publicly, tagline, about, website, logo_url, linkedin_url, founded_year, headquarters, contact_email, phone, benefits |
| users | 1 (+4, +5) | email, password_hash, first_name, last_name, role, company_id→companies, email_verified, failed_login_count, locked_until, consent_at; +disabled_at (4); +mfa_enabled, mfa_secret_enc, mfa_pending_secret_enc (5) |
| refresh_tokens | 1 | user_id→users, token_hash, expires_at, revoked_at |
| password_reset_tokens | 1 | user_id→users, token_hash, expires_at, used_at |
| candidate_profiles | 1 | user_id PK→users, phone, title, bio, location, remote_preference, total_experience_years, education_level, previous_job_titles |
| skills | 1 | name |
| candidate_skills | 1 | user_id→users, skill_id→skills, level, years, verified, verified_at |
| resumes | 1 | user_id→users, storage_key, status, stripped_text, is_current |
| jobs | 1 | company_id→companies, created_by→users, title, description, requirements, experience_level, city, country, remote, status, hf_min_years, hf_country, cutoff_score |
| job_skills | 1 | job_id→jobs, skill_id→skills, kind, must_have |
| applications | 1 (+2, +3) | job_id→jobs, candidate_id→users, resume_id→resumes, status; +score, scoring_mode, bias_status, bias_note, missing_skills, not_matched_rule, pulled_back_at, shortlisted_at, decided_by, decision_reason, override_direction, gate_status (2) |
| application_status_history | 1 | application_id→applications, from_status, to_status, changed_by→users, reason |
| notifications | 1 | user_id→users, type, title, body, link, read_at |
| audit_log | 1 | actor_id (no FK), action, resource_type, resource_id, details json |
| skill_aliases | 2 | skill_id→skills, alias |
| strip_terms | 2 | kind, term |
| resume_suggestions | 2 | resume_id→resumes, provider, data json |
| resume_placeholder_map | 2 | resume_id→resumes, placeholder, kind, original_value, start_month, end_month, is_current |
| strip_reports | 2 | resume_id→resumes, user_id→users, note, excerpt |
| work_history | 2 | user_id→users, title, employer, start_month, end_month, is_current, skills_used |
| candidate_aliases | 2 | company_id→companies, candidate_id→users, alias |
| ai_calls | 2 | purpose, user_id, application_id, model, prompt_tokens, completion_tokens, cost, status |
| job_screening_questions | 2 | job_id→jobs, position, text |
| screening_runs | 2 | job_id→jobs, started_by→users, mode, total, status, estimated_cost |
| screening_items | 2 | run_id→screening_runs, application_id→applications, status, error, attempts |
| application_claims | 2 | application_id→applications, requirement_key, requirement_text, kind, verdict, quote, weight, source |
| assessments | 3 | title, skill_id→skills, duration_minutes, pass_mark, active |
| assessment_questions | 3 | assessment_id→assessments, position, text, options json, correct_option_id |
| assessment_attempts | 3 | assessment_id, user_id→users, started_at, ends_at, submitted_at, answers json, score, passed, status, proctoring_consent, explanation |
| proctor_events | 3 | attempt_id→assessment_attempts, type, occurred_at, duration_seconds |
| job_required_assessments | 3 | job_id→jobs, assessment_id→assessments, min_score |
| resources | 3 | skill_id→skills, title, url, cost, language, level, active, broken, last_checked_at |
| certifications | 3 | user_id→users, name, issuer, issued_on, expires_on, credential_id |
| team_invites | 4 | company_id→companies, email, role, token_hash, invited_by→users, expires_at, accepted_at |
| interview_packs | 4 | job_id→jobs (unique), questions json, rubric json |
| interviews | 4 | application_id→applications, scheduled_at, duration_minutes, meeting_link, created_by→users |
| interview_interviewers | 4 | interview_id→interviews, user_id→users |
| interview_feedback | 4 | interview_id→interviews, interviewer_id→users, scores json, comments, recommendation |
| messages | 4 | application_id→applications, sender_id→users, body, read_at |
| practice_questions | 4 | text, topic, active |
| practice_sessions | 4 | user_id→users, question_id→practice_questions, answer, feedback json, rubric_version, rating |
| plans | 5 | name, monthly_screenings, price_bdt, duration_days, active |
| company_subscriptions | 5 | company_id→companies, plan_id→plans, payment_id→payments, starts_at, ends_at |
| payments | 5 | company_id→companies, plan_id→plans, paid_by→users, amount_bdt, provider, provider_payment_id, status, paid_at |

## 12. Environment variables (grow as chunks add them)

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 bytes), `STORAGE_DIR`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` (chunk 1) · `AI_PROVIDER` (`fallback` or `deepseek`), `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL`, `DEEPSEEK_MODEL`, `AI_PRICE_INPUT_PER_M`, `AI_PRICE_OUTPUT_PER_M` (chunk 2) · `ASSESSMENT_COOLDOWN_HOURS` (chunk 3) · `MFA_ENCRYPTION_KEY`, `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_BASE_URL`, `PAYMENT_CURRENCY`, `PAYMENT_PROVIDER` (`fake` or `stripe`), `APP_BASE_URL` (chunk 5) · `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM`, `DB_POOL_MAX`, `DEMO_PASSWORD` (chunk 6) · `DB_ROOT_PASSWORD` (Docker only, chunk 6). Keep `.env.example` current.
