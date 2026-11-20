# Prompt 06 — Hardening, Quality and Release Readiness (days 12 to 14)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunks 1 to 5 must be finished.

**Goal:** prove the product is safe, correct and ready to run anywhere. This chunk adds **no new features**. It verifies the global rules end to end, runs the whole suite on a clean MariaDB container, fixes performance and accessibility problems, adds demo data, and prepares everything the owner needs to add Docker afterwards.

**Parts:**
- **Part 6.1** Security and privacy verification: authorisation matrix, identity-leak and AI-isolation sweeps, log and secret checks
- **Part 6.2** Quality: clean-database run with Testcontainers (`mariadb:13.0`), query performance, accessibility and responsive review, frontend logic tests, demo data
- **Part 6.3** Release readiness: production profile, health checks, documentation, Docker hand-off notes, final contract export

Start message: `Build Part 6.1 from docs/prompts/06_hardening_release.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

> **Authoring note:** like chunk 5, this prompt was written from the rules in chunks 0 to 4 (global rules, Testcontainers only in chunk 6, design check, `docs/openapi.json`, deferred email verification). Confirm scope in `docs/progress.md` before starting.

## Scope rules

- **No new product features.** Fix bugs and gaps that the checks below reveal. Every fix gets a test and a line in `docs/progress.md`.
- **Do not create Dockerfiles or `docker-compose` files.** The owner will add Docker after this chunk. Part 6.3 only prepares and documents what Docker will need.
- Do not edit applied migrations. Index or constraint fixes go into `V6_<part>_<n>__name.sql`.
- Email verification (P05) stays deferred; do not build it.

## Requirements covered

| ID | What it must do |
|---|---|
| RULE-1 | No personal data reaches the AI provider on any code path. |
| RULE-2 | Employers never see identity before shortlist, on any endpoint. |
| RULE-3 | Every score is computed by the backend and cannot be influenced by the AI beyond verdicts and validated quotes. |
| RULE-4 | Plans, payments and coaching never change a score or rank. |
| RULE-5 | Coaching tables have no foreign-key path to jobs or applications. |
| RULE-6 | Messages never reach the AI and never appear in audit details or `ai_calls`. |
| RULE-9 | Passwords, tokens, MFA secrets and resume text never appear in logs. |
| AUTHZ | Every endpoint enforces its role and company scope exactly as the role table in 00_context says. |
| A11Y | WCAG 2.1 AA for all 36 pages that exist (P05 excluded), keyboard only, visible focus. |
| PERF | Main lists answer in under 300 ms on the demo data set with proper indexes. |
| OPS | The app starts from environment variables only, reports health, and can be containerised without code changes. |

## Part 6.1 — Security and privacy verification

### Authorisation matrix test
- Build `AuthorizationMatrixTest`. It reads the OpenAPI document from the running test context (`/v3/api-docs`, enable it in the `test` profile only) or reflects over `@RestController` mappings, and for every endpoint calls it as each of: guest, candidate, HR, manager, interviewer, admin, **and HR of a second company**.
- Expected result per endpoint comes from a table in the test (`endpoint → allowed roles`), written from the role table in 00_context section 6 and the API tables of chunks 1 to 5. Disallowed roles get 401 (guest) or 403; wrong-company resources get 404. The test fails when a new endpoint has no row in the table, so future work cannot skip it.
- Public endpoints allowed for guests: job board and job detail, skills suggest, register, login, MFA login, refresh, logout, forgot and reset password, invite lookup and accept, billing callback, health. Swagger paths only in `local`.

### Identity-leak sweep (`NoIdentityLeakTest` extended)
- Seed two candidates with distinctive names, emails and phone numbers, applied, screened, not shortlisted. Call **every** `/api/employer/**`, `/api/interviews/**`, analytics and message endpoint as HR, manager and interviewer, serialise the response, and assert that no name, email, phone, photo or resume placeholder original appears. After shortlist, the name and email appear only on the endpoints defined for reveal, and an `identity_reveal` audit row exists.
- Also assert that `candidate_aliases` and the `resume_placeholder_map` are never reachable by any employer or admin endpoint.

### AI isolation sweep
- A `RecordingAiProvider` test double captures every outgoing prompt. Drive every AI feature once (profile extraction, screening, screening-question suggestions, interview pack, STAR feedback, story) with candidates whose names, emails and phone numbers are distinctive, plus a message thread and a practice answer containing a name. Assert that no captured text contains any identifier, any message body, or any `[EMPLOYER_n]` original.
- Assert `ai_calls` rows contain no prompt or response text (column list check) and that audit `details` never contain message bodies.
- Assert `IdentifierScan` blocks a deliberately leaking text with `IDENTIFIER_LEAK`, writes `ai_call_blocked`, and the call is not made.

### Score integrity and isolation checks
- `ScoreIntegrityTest`: a fake AI that returns invented quotes, extra requirement keys, ratings out of range or a numeric `score` field cannot change the stored score; the score equals the backend formula for the validated claims.
- `NoPayToRankTest`: (from chunk 5) still green, plus a source scan that `screening`, `application` and `job` packages do not import `billing` or `coaching`.
- `CoachingIsolationTest`: walk `information_schema.KEY_COLUMN_USAGE` for `practice_questions` and `practice_sessions` and assert there is no path of foreign keys, in either direction, to `jobs` or `applications` (follow up to 5 hops).

### Log and secret checks
- `LogLeakTest`: run register, login, MFA enable, forgot password, resume upload, screening and payment callback with a captured log appender at TRACE for `com.fairfold` and `org.springframework.web`; assert the log contains no password, token, JWT, MFA secret, `BKASH_*` value, resume text or message body. Request and response logging filters must not exist or must redact.
- Secret scan: `scripts/check-secrets.sh` greps the repository (excluding `node_modules`, `target`, `data`) for key-like strings (`sk-`, `AKIA`, private key headers, long Base64 assigned to names containing `SECRET`, `KEY` or `PASSWORD`). `.env` must be git-ignored; `.env.example` has placeholders only.
- Startup validation: the app refuses to start in a non-`local` profile if `JWT_SECRET` is shorter than 32 bytes, `app.cookie-secure` is false, or `MFA_ENCRYPTION_KEY` is missing.

### HTTP hardening
- Security headers on every response: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy` empty for camera, microphone, geolocation, and a `Content-Security-Policy` for the API (`default-src 'none'; frame-ancestors 'none'`). Document the frontend CSP for the hosting layer in `docs/deployment.md`.
- CORS: none needed (same origin through the proxy). Verify no wildcard configuration exists.
- Cookie: refresh cookie is `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, `Secure` outside `local`.
- Rate limits verified by test: 10 per minute per IP on login, register, forgot password; messages 10 per minute per user; export 3 per hour; all return 429 with `Retry-After`.
- Upload checks: wrong signature, over 10 MB and path-like filenames are rejected; stored name is a random UUID; files are served only through authorised endpoints, never from a static path.
- Dependency audit: run `./mvnw org.owasp:dependency-check-maven:check` (or record that it could not run) and `npm audit --omit=dev`. Fix high and critical findings or list them in `docs/progress.md` with a reason.

**Tests:** all of the above are tests or scripts; none are manual.

**Definition of done:** `AuthorizationMatrixTest`, the sweeps, `LogLeakTest` and `CoachingIsolationTest` pass; `scripts/check-secrets.sh` passes; startup validation tested; findings and fixes recorded in `docs/progress.md`.

---

## Part 6.2 — Quality

### Clean-database run with Testcontainers
- Add `org.testcontainers:mariadb` and `junit-jupiter` (test scope). `MariaDbTestSupport` starts `mariadb:13.0` once per run with `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_ci` and points the datasource at it (driver `org.mariadb.jdbc.Driver`, URL `jdbc:mariadb://…`). Flyway runs **all** migrations from empty on it.
- Run the full backend suite twice: once on the local `fairfold_test` database (default) and once with `-Dtest.db=container`. Both must be green. Record the command in `docs/progress.md`. If Docker is not available on the machine yet, write the code, mark the container run as **pending until Docker is installed**, and note it; do not skip the local run.
- Migration check: a test that starts from empty, migrates, then asserts the schema against a list of expected tables, a few key constraints (unique emails, current-resume uniqueness, the override CHECK, the full-text index on jobs) and that `flyway validate` passes.

### Query performance
- Create the **demo data set** (below) and run `EXPLAIN` for: job board search (full-text and filters), employer ranked applicant list, candidate applications list, notifications unread count and list, interviews lists, analytics funnel, admin audit filters, admin users search.
- No query on these paths may do a full table scan of a table with more than a few hundred rows without a reason noted in `docs/progress.md`. Fix with a new migration `V6_2_1__performance_indexes.sql` (never edit applied ones). Watch for: `LIKE '%x%'` on large columns, `ORDER BY` without a usable index, N+1 queries (enable Hibernate statistics in one test and assert query counts on list endpoints: at most 5 queries per page).
- Pagination is server-side everywhere; sizes above 100 are clamped.
- Verify InnoDB full-text behaviour: words under 3 characters, stop words, and a skill name like `C` or `Go` still find jobs through the skill-name match.

### Demo data (`DemoDataSeeder`)
- Runs only in the `local` profile and only when the table `jobs` is empty (do not touch real data). Creates: 1 company with a complete profile, an HR, a manager and an interviewer; 3 active jobs with required skills, must-haves and cut-offs; 25 candidates with profiles, skills and work history and a stripped resume text each (hand-written text templates, no real people, no real companies except common public ones); applications spread across all statuses; one attached assessment with attempts; a few messages and one interview. Password for all demo users comes from `DEMO_PASSWORD` (default printed once at startup in `local` only).
- Candidates must include Bangladeshi names and employers, and equal skills with different names and genders, so the bias and anonymity checks have something to chew on.

### Accessibility and responsive review
- Add `scripts/a11y.mjs` or Vitest + `jest-axe` tests that render each page component with mocked data and assert no `axe` violations of impact serious or critical. Pages with charts check the table toggle.
- Manual checklist recorded in `docs/progress.md` (tick each): keyboard-only path for login, apply, screen all, shortlist dialog, assessment attempt, schedule interview, buy plan; focus returns to the trigger when a dialog closes; focus outline visible on every control; colour is never the only signal (status badges have icon and text); `aria-live` regions for toasts and autosave; reduced-motion respected (skeleton pulse disabled); text never below 12 px; touch targets 44 px on mobile; layouts checked at 375, 768 and 1280 px; no horizontal scroll except inside tables.
- Run `scripts/check-design.sh` and fix every violation (rounded corners, shadows, gradients, stray hex colours).

### Frontend logic tests (Vitest)
- Cover: `statusMap`, money and date formatters, the Axios refresh interceptor (single shared refresh promise, one retry, redirect with `next`), error mapping (400, 403, 404, 409, 423, 429, 5xx), polling stop rules (condition met, hidden tab), route guards by role, journey chart data builder, assessment timer offset, quota display, character counters.
- `npm run build` has no TypeScript errors and no warnings that mean real problems; `npm run lint` is clean if configured.

**Tests:** the Testcontainers run, the migration check, the query-count test, the axe tests, the Vitest suite.

**Definition of done:** both database runs green (or the container run marked pending with the reason), `EXPLAIN` review recorded with any index migration, demo data seeds and the demo path works by hand, a11y tests and checklist done, design check passes.

---

## Part 6.3 — Release readiness

### Production configuration
- `application-prod.yml`: no Swagger, no console mail (`app.mail.mode: smtp` using `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM`), `app.cookie-secure: true`, JSON-safe logging at INFO, error responses without stack traces, `server.shutdown: graceful`, `spring.jpa.open-in-view: false`, Hikari pool settings from env (`DB_POOL_MAX`, default 10), `spring.flyway.validate-on-migrate: true`, `spring.jpa.hibernate.ddl-auto: none` (Flyway owns the schema; `validate` fails on MariaDB `json` columns, which the driver reports as `longtext`).
- Health: add Spring Boot Actuator with only `health` (and `info`) exposed. `/actuator/health/liveness` and `/readiness` public; readiness includes the database. No other actuator endpoints.
- Scheduled jobs (weekly link check from chunk 3, assessment auto-submit, expired-token cleanup if present) must be safe if two instances run: use a database lock row (`SELECT ... FOR UPDATE SKIP LOCKED` on a `scheduler_locks` row) or a `@Scheduled` guard configured by `app.scheduling.enabled`. Pick the simpler: **`app.scheduling.enabled` default true; document that production runs exactly one instance for now.**
- Time: confirm every timestamp is stored in UTC and the JVM default time zone does not matter (test by running one test with `-Duser.timezone=Asia/Dhaka`).
- Storage: `STORAGE_DIR` must be a configurable absolute path; the app fails fast if it is not writable.
- Frontend: production build served as static files; `/api` is proxied by whatever hosts it. Record the exact routing rules (SPA fallback to `index.html`, `/api` to the backend) in `docs/deployment.md`.

### Docker hand-off notes (documents only)
Write `docs/deployment.md` containing everything the owner needs to add Docker later, **without** creating any Docker file:

1. Services: `backend` (Java 21, port 8080, health `/actuator/health/readiness`), `frontend` (static build served by a web server, SPA fallback, proxy `/api`), `db` (`mariadb:13.0`).
2. MariaDB settings: `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_ci`, `--innodb-ft-min-token-size=2` is optional (document the trade-off), `--default-time-zone=+00:00`, persistent volume at `/var/lib/mysql`, healthcheck `healthcheck.sh --connect --innodb_initialized`, init SQL to create `fairfold` and the user (same statements as 00_context section 3). The image reads `MARIADB_DATABASE`, `MARIADB_USER`, `MARIADB_PASSWORD` and `MARIADB_ROOT_PASSWORD`.
3. Volumes: database data and `STORAGE_DIR` (resume files). Backup advice: nightly `mariadb-dump --single-transaction` plus a copy of the storage volume.
4. A table of every environment variable from 00_context section 12 and chunks 5 and 6 with: name, required or optional, default, example, secret yes or no.
5. Start order and migration behaviour (Flyway runs on backend start; backend waits for the database to be healthy), ports, and the exact build commands (`./mvnw -DskipTests package`, `npm ci && npm run build`).
6. Pre-release checklist: all tests green on local and container runs, `check-design.sh`, `check-secrets.sh`, `npm audit`, OpenAPI exported, secrets rotated for production, admin password changed, MFA enabled for admin, SMTP configured, bKash switched from sandbox only after the owner has production credentials.

### Documentation
- `README.md` at the repository root: what FairFold is (three sentences), prerequisites (Java 21, Node, MariaDB), one-time setup (database SQL from 00_context), how to run backend, frontend and tests, demo accounts and the demo path, project layout, link to `docs/deployment.md` and `docs/progress.md`.
- `docs/demo-script.md`: the full 15-minute demo, step by step, with the account to use at each step, covering resume to ranked list, shortlist and reveal, assessment gate, interview and hire, practice, journey, billing quota, admin audit.
- `docs/known-limitations.md`: honest list (no email verification, no recovery codes for MFA, single backend instance, local file storage, English resumes only, AI is advisory, quota rules decided in chunk 5).
- Final contract: run the backend, `curl http://localhost:8080/v3/api-docs -o docs/openapi.json`, commit.
- Update `docs/progress.md`: every part marked done with its commit, final decisions list, open items for after launch.
- Tag the release: `git tag v1.0.0`.

**Tests:** `Prod_*` (production profile starts with valid env and refuses invalid values; Swagger and console mail are off; actuator exposes only health and info; error body has no stack trace), `Timezone_*` (one run with `-Duser.timezone=Asia/Dhaka`), `Storage_*` (fails fast on a non-writable directory).

**Definition of done (whole chunk):** a new person can clone the repository, follow `README.md`, create the two databases, run the backend, frontend and tests, and walk through `docs/demo-script.md` without asking anything; `docs/deployment.md` lists everything Docker needs; all checks (tests, design, secrets, a11y, audit) are green or have a recorded reason; `docs/openapi.json` is current; `docs/progress.md` shows every part done; tag `v1.0.0` exists.
