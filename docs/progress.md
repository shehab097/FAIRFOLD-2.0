# FairFold — Progress and History

Last updated: 21 November 2026 · Updated by: prompt author (before coding starts)

> Read this after `00_context.md` at the start of every session. Update it at the end of every session. Keep entries short. Newest history entry goes at the top of the History section.

## 1. Status

Legend: `[ ]` not started · `[~]` in progress · `[x]` done (tests green, design check passed, OpenAPI exported, committed)

| Part | Prompt file | Scope | Status | Commit | Date |
|---|---|---|---|---|---|
| 1.1 | 01_core_platform.md | Foundation and authentication | [ ] | | |
| 1.2 | 01_core_platform.md | Company, jobs, public job board | [ ] | | |
| 1.3 | 01_core_platform.md | Candidate profile, resume upload, applying, notifications | [ ] | | |
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

**Current focus:** Part 1.1 (setup complete, no features built yet). Backend (Spring Boot 3.5.16 + MariaDB + Flyway + springdoc) and frontend (Vite + React 18 + Tailwind) skeletons both run and pass `check-design.sh`. Next: build Part 1.1 from `docs/prompts/01_core_platform.md`.

**Next session start message:** `Build Part 1.1 from docs/prompts/01_core_platform.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

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
| D-01 | 21 Nov 2026 | all | Database is **MySQL 8.4 LTS** (InnoDB, `utf8mb4`, `utf8mb4_0900_as_ci`), not PostgreSQL. | Owner's choice. |
| D-02 | 21 Nov 2026 | all | UUID ids are `char(36)` with `DEFAULT (UUID())`; Hibernate maps with `@UuidGenerator` and `@JdbcTypeCode(SqlTypes.CHAR)`. | MySQL has no native uuid type; readable ids match the API. |
| D-03 | 21 Nov 2026 | all | Times are `datetime(3)` in UTC (`Instant`, JDBC `connectionTimeZone=UTC`). | MySQL `datetime` has no zone; one rule avoids bugs. |
| D-04 | 21 Nov 2026 | all | Arrays (`text[]`) become `json` string arrays; `jsonb` becomes `json`. | MySQL has neither type. |
| D-05 | 21 Nov 2026 | 1.2 | Job search uses an InnoDB `FULLTEXT` index plus exact skill match plus a `LIKE` fallback for queries under 3 characters. | Replaces PostgreSQL `tsvector`; InnoDB ignores words under 3 characters by default. |
| D-06 | 21 Nov 2026 | 1.3 | "One current resume per user" uses a VIRTUAL generated column with a UNIQUE index. | MySQL has no partial indexes. |
| D-07 | 21 Nov 2026 | all | Case-insensitive uniqueness (email, skill name, alias, strip term) comes from the collation instead of `lower()` indexes. | Simpler and portable in MySQL 8. |
| D-08 | 21 Nov 2026 | tests | Tests clean tables with `FOREIGN_KEY_CHECKS=0` + `TRUNCATE`; no rolled-back transactions around full-text tests. | InnoDB full-text sees rows only after commit. |
| D-09 | 21 Nov 2026 | 5, 6 | Chunk prompts 05 and 06 were **written from references in chunks 0 to 4**, without the PRD. Their rules marked *(Decision)* are proposals. | PRD was not available to the author. **Owner to review 05 and 06 before Part 5.1.** |
| D-10 | 21 Nov 2026 | 5.1 | Proposed: MFA optional for every role, no recovery codes (admin resets MFA). | Keeps scope small. Confirm. |
| D-11 | 21 Nov 2026 | 5.2 | Proposed: Free plan 20 AI screenings a month; Starter ৳1,500 / 200; Growth ৳4,500 / 1,000; rule-based screening does not use quota; failed items refunded; no recurring billing. | Quota numbers are placeholders. Confirm. |
| D-12 | 21 Nov 2026 | 5.3 | Proposed: analytics groups under 3 items return no average. | Prevents inferring one person. |
| D-13 | 21 Nov 2026 | 6.3 | Proposed: Docker files are **not** created in chunk 6; only `docs/deployment.md` hand-off notes. | Owner will set up Docker after the project is built. |
| D-14 | (from chunk 3) | 3.2 | Verified badge on pass creates the candidate skill at level `beginner`, years null, if missing. | Noted in prompt 03; keep here when built. |
| D-15 | 21 Nov 2026 | all | Database is **MariaDB** (13.0.2), not MySQL 8.4. JDBC driver `org.mariadb.jdbc:mariadb-java-client` (URL `jdbc:mariadb://…`); Flyway stays on `flyway-mysql` (supports MariaDB). Supersedes D-01. | Owner's environment: MariaDB 13 installed locally, user `shehab`/`1234`, db `fairfold`. Verified MariaDB 13 supports `utf8mb4_0900_as_ci`, `DEFAULT (UUID())`, JSON defaults, CHECK, VIRTUAL generated columns and InnoDB FULLTEXT, so all MySQL DDL conventions hold unchanged. |
| D-16 | 21 Nov 2026 | all | Added `me.paulschwarz:springboot3-dotenv` 5.1.0 (optional) so the backend loads `.env` from the repo root; real env vars still win. Configured `springdotenv.directory=..`. | Stack lists no `.env` loader and the owner asked for an `.env` file with frictionless `./mvnw` runs. |
| D-17 | 21 Nov 2026 | all | Maven 3.9.16 installed to `~/.local/maven` (no system Maven, no sudo); project uses the Maven wrapper `./mvnw` (script-only). | Reproducible builds without root. |

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
| (none yet) | | | | | |

## 7. History (newest first)

| Date | Event |
|---|---|
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
| Backend tests on a clean MySQL container (chunk 6, needs Docker) | `cd backend && ./mvnw test -Dtest.db=container` |
| Design check | `./scripts/check-design.sh` |
| Secrets check (chunk 6) | `./scripts/check-secrets.sh` |
| Export API contract | `curl http://localhost:8080/v3/api-docs -o docs/openapi.json` |
| Reset local dev database | `mysql -u root -p -e "DROP DATABASE fairfold; CREATE DATABASE fairfold CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;"` |
| Repair a half-applied Flyway migration | fix or drop the partial objects by hand, then `./mvnw flyway:repair` |
