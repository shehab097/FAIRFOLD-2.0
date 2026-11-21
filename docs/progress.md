# FairFold — Progress and History

Last updated: 21 November 2026 · Updated by: prompt author (before coding starts)

> Read this after `00_context.md` at the start of every session. Update it at the end of every session. Keep entries short. Newest history entry goes at the top of the History section.

## 1. Status

Legend: `[ ]` not started · `[~]` in progress · `[x]` done (tests green, design check passed, OpenAPI exported, committed)

| Part | Prompt file | Scope | Status | Commit | Date |
|---|---|---|---|---|---|
| 1.1 | 01_core_platform.md | Foundation and authentication | [x] | | 21 Nov 2026 |
| 1.2 | 01_core_platform.md | Company, jobs, public job board | [x] | | 21 Nov 2026 |
| 1.3 | 01_core_platform.md | Candidate profile, resume upload, applying, notifications | [x] | | 21 Nov 2026 |
| 2.1 | 02_ai_screening.md | Resume processing, stripping, AI gateway | [ ] | | |
| 2.2 | 02_ai_screening.md | Screening engine, scoring, bias check | [ ] | | |
| 2.3 | 02_ai_screening.md | Ranked list, review, shortlist, reveal, override | [ ] | | |
| 3.1 | 03_skills_proof.md | Admin content: assessments, resources | [ ] | | |
| 3.2 | 03_skills_proof.md | Taking assessments, Verified badge | [ ] | | |
| 3.3 | 03_skills_proof.md | Gate, proctoring, certifications | [ ] | | |
| 4.1 | 04_interviews_growth.md | Team, interview pack, interviews, hire | [ ] | | |
| 4.2 | 04_interviews_growth.md | Messaging, interview practice | [ ] | | |
| 4.3 | 04_interviews_growth.md | Journey, job story | [ ] | | |
| 5.1 | 05_security_billing_admin.md | MFA, data export, account deletion | [ ] | | |
| 5.2 | 05_security_billing_admin.md | Plans, payments, screening quota | [ ] | | |
| 5.3 | 05_security_billing_admin.md | Analytics, admin dashboard, users, audit log | [ ] | | |
| 6.1 | 06_hardening_release.md | Security and privacy verification | [ ] | | |
| 6.2 | 06_hardening_release.md | Testcontainers run, performance, a11y, demo data | [ ] | | |
| 6.3 | 06_hardening_release.md | Production config, docs, Docker hand-off notes | [ ] | | |

**Current focus:** Part 1.3 done. Next: build Part 2.1 (resume processing, stripping, AI gateway) from `docs/prompts/02_ai_screening.md`.

**Next session start message:** `Build Part 2.1 from docs/prompts/02_ai_screening.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

## 2. Environment checklist (tick when true on your machine)

- [x] Java 21, Maven wrapper works (Maven 3.9.16 at `~/.local/maven`, wrapper `./mvnw`)
- [x] Node 20+ and npm (Node 26.13.1, npm 12.2.3)
- [x] MariaDB 13.0.2 running; databases `fairfold` and `fairfold_test` created (`utf8mb4` / `utf8mb4_0900_as_ci`); user `shehab` granted (see D-15)
- [x] `.env` created from `.env.example` (`DB_PASSWORD`, `JWT_SECRET` ≥ 32 bytes, `ADMIN_EMAIL`, `ADMIN_PASSWORD`)
- [x] `AI_PROVIDER=fallback` works with no key; DeepSeek key added later (optional)
- [ ] Docker: **not yet** (owner sets it up after chunk 6; see `docs/deployment.md` once written)

## 3. Decisions

Record every choice that is not already in the prompts, and every deviation. Format: `D-nn · date · part · decision · reason`.

| ID | Date | Part | Decision | Reason |
|---|---|---|---|---|
| D-01 | 21 Nov 2026 | all | Database is **MySQL 8.4 LTS** (InnoDB, `utf8mb4`, `utf8mb4_0900_as_ci`), not PostgreSQL. **Superseded by D-15.** | Owner's initial choice; the machine runs MariaDB instead. |
| D-02 | 21 Nov 2026 | all | UUID ids are `char(36)` with `DEFAULT (UUID())`; Hibernate maps with `@UuidGenerator` and `@JdbcTypeCode(SqlTypes.CHAR)`. | MariaDB has no native uuid type; readable ids match the API. |
| D-03 | 21 Nov 2026 | all | Times are `datetime(3)` in UTC (`Instant`, JDBC `connectionTimeZone=UTC`). | MariaDB `datetime` has no zone; one rule avoids bugs. |
| D-04 | 21 Nov 2026 | all | Arrays (`text[]`) become `json` string arrays; `jsonb` becomes `json`. | MariaDB has neither type. |
| D-05 | 21 Nov 2026 | 1.2 | Job search uses an InnoDB `FULLTEXT` index plus exact skill match plus a `LIKE` fallback for queries under 3 characters. | Replaces the original PostgreSQL `tsvector` design; InnoDB ignores words under 3 characters by default. |
| D-06 | 21 Nov 2026 | 1.3 | "One current resume per user" uses a nullable `current_user_key char(36)` column that holds the user id only while the resume is current, under a UNIQUE index (NULLs never collide). | MariaDB has no partial indexes and no CASE/IF in generated columns, so the MySQL VIRTUAL-column trick is not available. |
| D-07 | 21 Nov 2026 | all | Case-insensitive uniqueness (email, skill name, alias, strip term) comes from the collation instead of `lower()` indexes. | Simpler and portable in MariaDB. |
| D-08 | 21 Nov 2026 | tests | Tests clean tables with `FOREIGN_KEY_CHECKS=0` + `TRUNCATE`; no rolled-back transactions around full-text tests. | InnoDB full-text sees rows only after commit. |
| D-09 | 21 Nov 2026 | 5, 6 | Chunk prompts 05 and 06 were **written from references in chunks 0 to 4**, without the PRD. Their rules marked *(Decision)* are proposals. | PRD was not available to the author. **Owner to review 05 and 06 before Part 5.1.** |
| D-10 | 21 Nov 2026 | 5.1 | Proposed: MFA optional for every role, no recovery codes (admin resets MFA). | Keeps scope small. Confirm. |
| D-11 | 21 Nov 2026 | 5.2 | Proposed: Free plan 20 AI screenings a month; Starter ৳1,500 / 200; Growth ৳4,500 / 1,000; rule-based screening does not use quota; failed items refunded; no recurring billing. | Quota numbers are placeholders. Confirm. |
| D-12 | 21 Nov 2026 | 5.3 | Proposed: analytics groups under 3 items return no average. | Prevents inferring one person. |
| D-13 | 21 Nov 2026 | 6.3 | Proposed: Docker files are **not** created in chunk 6; only `docs/deployment.md` hand-off notes. | Owner will set up Docker after the project is built. |
| D-14 | (from chunk 3) | 3.2 | Verified badge on pass creates the candidate skill at level `beginner`, years null, if missing. | Noted in prompt 03; keep here when built. |
| D-15 | 21 Nov 2026 | all | Database is **MariaDB** (13.0.2); the earlier MySQL 8.4 decision is void. JDBC driver `org.mariadb.jdbc:mariadb-java-client` (URL `jdbc:mariadb://…`); Flyway stays on `flyway-mysql` (supports MariaDB). Supersedes D-01. | Owner's environment: MariaDB 13 installed locally, user `shehab`/`1234`, db `fairfold`. Verified MariaDB 13 supports `utf8mb4_0900_as_ci`, `DEFAULT (UUID())`, JSON defaults, CHECK, VIRTUAL generated columns and InnoDB FULLTEXT, so all the planned DDL conventions hold unchanged. |
| D-16 | 21 Nov 2026 | all | Added `me.paulschwarz:springboot3-dotenv` 5.1.0 (optional) so the backend loads `.env` from the repo root; real env vars still win. Configured `springdotenv.directory=..`. | Stack lists no `.env` loader and the owner asked for an `.env` file with frictionless `./mvnw` runs. |
| D-17 | 21 Nov 2026 | all | Maven 3.9.16 installed to `~/.local/maven` (no system Maven, no sudo); project uses the Maven wrapper `./mvnw` (script-only). | Reproducible builds without root. |
| D-18 | 21 Nov 2026 | all | `spring.jpa.hibernate.ddl-auto` is `none` (not `validate`). MariaDB's JDBC driver reports `json` columns as `longtext`, which fails Hibernate's JSON type check under `validate`. Flyway owns the schema. JSON columns stay `json` (MariaDB adds a `json_valid` CHECK). Flyway also warns "MariaDB 13.0 is newer than tested (11.2)" but migrations apply cleanly. | MariaDB `json` ≈ `longtext`; `validate` cannot match `@JdbcTypeCode(SqlTypes.JSON)`. |
| D-19 | 21 Nov 2026 | all | All documentation now describes **MariaDB** only. Removed the remaining MySQL/PostgreSQL setup, JDBC, Testcontainers, Docker and backup instructions from the PRD, plan, roadmap, prompts and README. | The project runs on MariaDB; stale MySQL instructions caused wrong commands and driver choices. |
| D-20 | 21 Nov 2026 | 1.3 | Resumes enforce "one current per user" with an explicit nullable `current_user_key` column + UNIQUE index instead of the prompt's `GENERATED ALWAYS AS (CASE …) VIRTUAL` column. The service sets `current_user_key = user_id` (current) or `NULL` (not current) and flushes before inserting the replacement. | MariaDB rejects `CASE`/`IF` expressions in generated columns (`ERROR 1901`); the nullable-unique-key pattern gives the same guarantee. |
| D-21 | 21 Nov 2026 | 1.3+ | Markdown is rendered by a ~80-line in-house component (`components/ui/Markdown.tsx`) that builds React elements, instead of adding `react-markdown`. Editor (`MarkdownEditor.tsx`) is a textarea + toolbar + live preview. | No new dependency; safe by construction (no `dangerouslySetInnerHTML`, only `http(s)`/`mailto` links); covers the needed syntax. |
| D-22 | 21 Nov 2026 | 1.3+ | UI navigation & profile overhaul: (1) signed-in users keep the full `PortalShell` (sticky left sidebar `lg:sticky lg:top-0 lg:h-screen` + sticky top bar) even on public routes like Browse Jobs (`/`). (2) Browse Jobs (`P01`) uses a two-column layout with results on the left and a sticky search/filter sidebar on the right. (3) Candidate Resume page (`P14`) supports inline PDF preview and open-in-new-tab via `GET /api/candidate/resume/file`. (4) Password change moved to a dedicated page `/account/password`, and Sign Out + Change Password buttons live inside `/account` instead of cluttering the nav bar. (5) Profile & company views display all attributes in clean `divide-y` containers without nested boxes, alongside a custom folded-corner SVG brand icon (`Logo.tsx` and `public/favicon.svg`). | Owner requested cleaner layout, sticky navigation, right-side job filters, resume preview, and dedicated password page. |
| D-23 | 21 Nov 2026 | 1.3+ | Local dummy data generator & database seed lives in git-ignored `dummy-data/` (`generate_dummy_data.py`, `users.csv`, `seed_data.sql`, `cvs/*.pdf`). Generates 10 users (6 candidates, 4 employer HRs across 4 companies, password `Password123!`), 20 active jobs with Markdown descriptions and skill mappings, and 5 valid PDF resumes copied to `data/resumes/<storage_key>`. | Owner requested 20 jobs, 10 users, 5 CVs seeded into MariaDB plus a git-ignored folder containing `users.csv`. |
| D-24 | 21 Nov 2026 | all | Added **Light Mode support** (`tokens.css` `:root.light` / `[data-theme="light"]`, `ThemeContext.tsx`, anti-flicker inline script in `index.html`, `ThemeToggle.tsx` in `PublicShell`, `PortalShell`, `AuthShell`, and theme selector in `AccountPage.tsx`) and enabled **rounded corners across the entire UI** (`sm: 6px` to `2xl: 20px` and `full: 9999px` in `tailwind.config.ts` and `tokens.css`, with subtle shadows). Updated `scripts/check-design.sh` and `docs/design.md`. | Owner requested light mode support and rounded corners all over the UI. |

## 4. Open questions (ask the owner, then move to Decisions)

- [ ] Real plan names, prices and quotas for chunk 5.
- [ ] Does rule-based (fallback) screening count against the quota? (Proposed: no.)
- [ ] Is MFA required for admin accounts? (Proposed: optional with a banner.)
- [ ] Do you have bKash sandbox credentials? (If not, use `PAYMENT_PROVIDER=fake`.)
- [ ] Which email provider will send real mail in production? (SMTP settings in 06.)
- [ ] Mapping of requirement names in chunk 5 and 6 to the PRD's FR numbers (names such as MFA, QUOTA, ANALYTICS are placeholders).

## 5. Deviations from prompts

None yet. Format: `Part · what differed · why · date`.

## 6. Test and quality log

Update after every part.

| Part | Backend tests | Frontend tests | `check-design.sh` | OpenAPI exported | Notes |
|---|---|---|---|---|---|
| 1.1 | 13 passed | 9 passed | passed | yes | Auth flow verified end-to-end via curl (register, login, lockout 423, reset, refresh rotation, logout). |
| 1.2 | 18 passed (31 total) | 9 passed | passed | yes | Company profile, job CRUD + lifecycle (BR-01), FULLTEXT/skill/LIKE search, filters, company scope. |
| 1.3 | 50 passed | 11 passed | passed | yes | Candidate profile + skills (FR-009/FR-014), resume upload & preview (`GET /api/candidate/resume/file`, FR-010, signature/10MB/replace), apply + status history + notifications (FR-022/FR-023), dashboard, anonymised employer applicant list (`NoIdentityLeakTest`). Decisions D-20–D-24. |

## 7. History (newest first)

| Date | Event |
|---|---|
| 21 Nov 2026 | **Light mode + Rounded corners across UI**: added light theme tokens in `tokens.css`, `ThemeContext.tsx` (Dark / Light / System with `localStorage` persistence), anti-flicker script in `index.html`, `ThemeToggle.tsx` in `PublicShell`, `PortalShell`, `AuthShell`, and theme switcher in `AccountPage.tsx`. Enabled full rounded corner scale (`rounded-md`, `rounded-lg`, `rounded-xl`, `rounded-2xl`, `rounded-full`) and subtle elevation shadows across all components and pages. Updated `design.md` and `scripts/check-design.sh`. Decision D-24. |
| 21 Nov 2026 | **Dummy data generator & MariaDB seed**: created git-ignored `dummy-data/` folder with `generate_dummy_data.py`, `users.csv` (10 users: 6 candidates, 4 employer HRs, password `Password123!`), 5 generated PDF CVs (`dummy-data/cvs/` and `data/resumes/`), and `seed_data.sql` (4 companies, 10 users, 6 candidate profiles, 5 resumes, 20 active jobs with Markdown and 65 job_skills). Seeded into local MariaDB `fairfold`. Decision D-23. |
| 21 Nov 2026 | **UI simplification, navigation, resume preview & brand icon**: kept `PortalShell` sticky sidebar visible for signed-in users on Browse Jobs; made sidebar sticky (`lg:h-screen`); refactored Browse Jobs (`P01`) with a right-hand sticky search/filter sidebar; added PDF resume preview endpoint (`GET /api/candidate/resume/file`) and inline/new-tab viewer on `ResumePage`; added dedicated `/account/password` page and moved Sign Out inside `/account`; simplified cards with `divide-y` dividers instead of nested boxes; created folded-corner SVG brand icon (`Logo.tsx`, `public/favicon.svg`). Decision D-22. |
| 21 Nov 2026 | **Company details + Markdown**: migration `V1_4_1` adds 10 company fields (tagline, about, website, logo, LinkedIn, founded year, HQ, contact email, phone, benefits); company API/page extended; Markdown editor with toolbar + live preview for job description/requirements and company about/benefits; Markdown rendering on public job and employer workspace; P24 redesigned (Overview + sidebar, applicant count, SPA navigation). Decision D-21. |
| 21 Nov 2026 | **Docs database pass**: converted the PRD, `plan.md`, `roadmap.md`, `00_context.md`, `01_core_platform.md`, `06_hardening_release.md` and `README.md` from MySQL/PostgreSQL to MariaDB (driver, JDBC URL, Testcontainers image/module, Docker service, dump command, setup SQL). Decision D-19. |
| 21 Nov 2026 | **Part 1.3 done**: migration `V1_3_1` (candidate_profiles, candidate_skills, resumes, applications, application_status_history, notifications); candidate profile + skills endpoints; `StorageService` + resume upload (PDF signature, 10 MB, replaces current); apply flow with `ApplicationStatusService.changeStatus` helper; notifications; candidate dashboard; anonymised employer applicant list. Frontend: candidate dashboard/profile/resume/applications/detail, notifications, account, top-bar bell + unread count, Apply button wired, Applicants tab filled. 49 backend tests green, design check + frontend build pass, `docs/openapi.json` exported. Decisions D-20. |
| 21 Nov 2026 | **Part 1.2 done**: skills/jobs/job_skills migrations + 100-skill seed, Job/Skill/JobSkill entities, company profile (GET/PUT + completeness), job CRUD + lifecycle (BR-01 + activation requires company + duplicate), public board with FULLTEXT/exact-skill/LIKE search + filters + hidden company name, company scoping. 18 new backend tests (31 total). Frontend: types/API/queries, Table/Tabs/Dialog/Toast/Textarea components, pages P01/P02/P21/P22/P23/P24, employer nav enabled. `docs/openapi.json` exported (20 paths). |
| 21 Nov 2026 | **Part 1.1 done**: auth tables migration, User/Company/token/audit entities, JWT stateless security, register/login/lockout/refresh(rotation)/logout/forgot/reset endpoints, rate limiting, admin seed, 13 backend tests + 9 frontend tests green, `check-design.sh` passes, `docs/openapi.json` exported. Frontend: base UI components, PublicShell/AuthShell/PortalShell, auth context + guards + refresh interceptor, pages P03/P04/P06/P07/P11 + welcome. Decisions D-18. |
| 21 Nov 2026 | Environment setup: scaffolded backend (Spring Boot 3.5.16, MariaDB driver, Flyway `flyway-mysql`, springdoc 2.8.17, spring-dotenv) and frontend (Vite + React 18 + TS + Tailwind + design tokens). Both run; health + OpenAPI + Vite proxy verified; `check-design.sh` passes. Decisions D-15–D-17. |
| 21 Nov 2026 | Replaced PostgreSQL with MySQL 8.4 in prompts 00 to 04 (types, indexes, search, tests, setup SQL, env vars, Flyway module). Wrote prompts 05 and 06 and this file. Confirmed prompt 04 was complete. |
| 20 Nov 2026 | Generated prompts 00 (shared context) and 01 to 04, each split into at most three parts. |

### Session log template (copy for each session)

```text
Date:
Part:
Done:
Not done / blocked:
Decisions added (IDs):
Commit:
Tests (count, pass/fail):
Next start message:
```

## 8. Handy commands

| What | Command |
|---|---|
| Run backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| Run frontend | `cd frontend && npm run dev` |
| Backend tests | `cd backend && ./mvnw test` |
| Backend tests on a clean MariaDB container (chunk 6, needs Docker) | `cd backend && ./mvnw test -Dtest.db=container` |
| Design check | `./scripts/check-design.sh` |
| Secrets check (chunk 6) | `./scripts/check-secrets.sh` |
| Export API contract | `curl http://localhost:8080/v3/api-docs -o docs/openapi.json` |
| Generate & seed dummy data (20 jobs, 10 users, 5 CVs) | `python3 dummy-data/generate_dummy_data.py && mariadb -u shehab -p1234 fairfold < dummy-data/seed_data.sql` |
| Reset local dev database | `mariadb -u root -p -e "DROP DATABASE fairfold; CREATE DATABASE fairfold CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;"` |
| Repair a half-applied Flyway migration | fix or drop the partial objects by hand, then `./mvnw flyway:repair` |
