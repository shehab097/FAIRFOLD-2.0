# FairFold — Roadmap

Version 1.0 · 12 November 2026

`plan.md` says **how** to build each chunk. This file says **what you have and when**, what must be true before moving on, and what comes after the first version. Days are counted from the day you start coding.

## 1. Overview

| Stage | What exists at the end | By day | Gate to pass |
|---|---|---|---|
| **M1 Core platform** | Employer posts a job, candidate applies, status and notifications work | 2.5 | The chunk 1 demo runs end to end |
| **M2 Anonymised screening** | Resumes are stripped, screened, ranked and explained; shortlist reveals identity | 4.5 | The 30-resume test set meets the 95% strip target, and every score shows valid quotes |
| **M3 Skills proof** | Assessments, gate, retake loop with resources, light proctoring | 6 | A candidate fails, studies, retakes and passes; the shortlist button is blocked until then |
| **M4 Interviews and growth** | Team, interviews, feedback, messaging, practice, journey charts, story | 8 | One full hiring story works from shortlist to hired |
| **M5 Operations** | MFA, admin tools, audit, export and deletion, analytics, billing | 9.5 | Admin can audit every AI call, reveal and override; a sandbox payment succeeds |
| **M6 Release candidate** | The same app starts with `docker compose up` | 10 | The smoke test passes inside Docker |

## 2. Timeline

```text
Day   1    2    3    4    5    6    7    8    9    10
      |-------- M1 --------|
                 |------ M2 ------|
                                |--- M3 ---|
                                         |------ M4 ------|
                                                         |--- M5 ---|
                                                                  |M6|
```

There is no spare time inside these 10 days. Days 11 and 12 are your buffer, and they are also when the work in section 3 starts.

## 3. Right after the first version (days 11 to 12 and beyond)

| Item | Size | Why it waits |
|---|---|---|
| Email verification (FR-001 second half, the verify page, resend) | about 2 hours | You can build and test everything without it, because the setting is off by default |
| GitHub Actions (build, test, design check) | about 1 hour | Useful once there is code worth protecting |
| Polish: loading, empty and error states, a 360 px mobile pass, an accessibility pass | 0.5 to 1 day | Chunks are built "thin" first |
| Anything that slipped from M1 to M6 | varies | |

## 4. Before real users (production readiness)

- [ ] Real SMTP provider and domain for reset and invite emails, then turn on email verification
- [ ] Stripe live keys and webhook verification (test mode is not real money)
- [ ] HTTPS and a domain, `cookie-secure: true`, strong secrets in `.env`
- [ ] MariaDB backups and a tested restore, and a backup of the resume files volume
- [ ] Non-functional targets written down (response times, availability, accessibility), because the original file did not include them
- [ ] A privacy review: what is stored, how long, who can see it, and the deletion path
- [ ] A bias review of the AI prompts and the bias-status rule with your test set
- [ ] A check that no personal data appears in AI request logs
- [ ] Swagger UI and API docs confirmed off in production
- [ ] Rate limits tested on login, reset, messaging and screening

## 5. First pilot

1. Invite a small number of employers and candidates you trust.
2. Collect every wrong strip (the "report a wrong strip" button) and add it to the test set.
3. Watch the override reasons: they show where the ranking disagrees with human judgement.
4. Check AI cost per screening run against the estimate shown to employers.
5. Review audit entries for anything unexpected.

## 6. Later ideas (currently out of scope)

These are the PRD's non-goals. Revisit them only when the pilot shows a need.

- Bangla and other non-English resume parsing
- S3 or MinIO file storage instead of local disk
- A separate search engine if MariaDB full-text search becomes slow
- Recurring billing and stored payment methods
- Calendar files (`.ics`) for interviews
- A light theme or theme switcher
- Native mobile apps
- Redis-based rate limiting if you run more than one server

## 7. Dependencies to start early

| Dependency | Needed by | Action now |
|---|---|---|
| Test resumes and expected strips | M2 (day 2.5) | Generate about 30 fake PDF resumes with the AI |
| Seed lists (skills, aliases, institutions, employers) | M2 | Ask the AI to draft them, then review |
| DeepSeek API key | M2 | Create the account; start on the fallback provider meanwhile |
| Stripe test keys | M5 (day 8) | Create a test-mode account; no merchant approval needed for the demo |
| Plan names, quotas and prices | M5 | Decide before day 8 |
| Original business rules, if you have them | M2 | Send them so the rebuilt rules can be checked |

## 8. Decision log

| Decision | Choice |
|---|---|
| Build order | Local first, Docker last |
| Frontend | React 18, TypeScript, Vite, Tailwind, one sans-serif font (Inter) |
| Look | Dark & Light mode support (with switcher & system detection), rounded corners, no gradients, five-colour palette |
| Backend | Java 21, Spring Boot 3.x, Maven, springdoc-openapi 2.8.x |
| Email verification | Deferred until after day 10 |
| Kept as core features | MFA, messaging, proctoring, journey and skill evolution, job story, screening-question suggestions, resources, rating, retake loop |
| Live updates | Polling, no WebSockets |
