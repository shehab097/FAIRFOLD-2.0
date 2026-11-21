# FairFold — Product Requirements Document

Version 2.1 · 12 November 2026 · Stack revised 21 November 2026 · Spring Boot + React + MariaDB + bKash + DeepSeek

## 0. What changed from v2.0

v2.0 already removed the heavy infrastructure. This version simplifies the parts that were hard to build or test, **but keeps every core feature of the product**: MFA, messaging, light proctoring, the journey timeline, skill evolution, the job-specific story, AI screening-question suggestions, learning resources, the helpfulness rating and the full retake loop. These are the project's main focus and are not cut. Requirement IDs (REQ-FR-xxx) are kept, so nothing is renumbered.

| Area | v2.0 | v2.1 | Why |
|---|---|---|---|
| Assessment scoring (FR-020) | Multiple choice and short text | **Multiple choice only.** | Short-text auto-scoring is unreliable and hard to defend. |
| Practice questions (FR-058) | Admin bank or generated from a job | **Admin bank only.** | Generating from jobs links coaching to jobs, which FR-060 forbids. |
| Interview scheduling (FR-032) | Email with `.ics` file | **Date, duration, meeting link and interviewers; in-app notification and plain email.** | Calendar file generation adds little. |
| Job share link (FR-036) | Separate feature | **Merged into FR-042.** Every active job already has a public URL. | |
| Certifications (FR-044) | Credential id stored encrypted | **Plain text.** | Not sensitive enough to justify key management. |
| Data export (FR-040) | Background build, download link valid 7 days | **Generated on request and downloaded immediately** (JSON). | Per-user data is small. |
| Account deletion (FR-041) | Request, then admin approval | **Self-service with password confirmation.** | Removes an approval queue and an admin page. |
| Analytics (FR-035) | Funnel, time-to-hire, override rate by user and job | **Three charts and one table:** funnel, time-to-hire, override list with reasons, and a pull-back count. | |
| Proxy stripping (FR-057) | Undefined detection of institutions and employers | **Rule-based:** seeded lists plus patterns, previewed by the candidate (BR-05). | Predictable and testable. |
| Scoring | Undefined | **One simple formula** (BR-20). | Explainable and easy to test. |
| Billing | Plans and bKash | **bKash sandbox, one-time purchase of 30 days, no auto-renew.** | No recurring billing. |
| Email verification (FR-001, 002) | Verification email at sign-up, verified users only | **Deferred.** Accounts are active at sign-up. The `email_verified` column and the switch `app.auth.require-email-verification` (default off) exist from day one; the verification flow is built last. | Lets you build and test everything locally without a mail server. |
| Visual design | Not defined | **Dual-theme (Dark & Light mode with system preference and switcher), smooth rounded corners, no gradients, a five-colour palette.** Full rules in `design.md`. | Clean, modern, accessible style system. |
| Phase of Team (FR-047) | P4 | **P3** | Interview scheduling (FR-032) needs interviewers from the team, and the accept-invite page depends on it. |
| API documentation | Not defined | **springdoc-openapi** generates the OpenAPI document and Swagger UI from the code. | Gives you, the frontend and your AI coding tool an exact, always-current API contract. |
| Size | 59 FRs | **58 FRs** (only FR-036 is merged) | |

**Important note about the source file.** The uploaded v2.0 file ends at section 4.8. Sections 5 to 12 were not in it (business rules, non-functional requirements, data model, bKash section 9, phases). Because requirements refer to those sections, section 5 to 7 below **rebuild the minimum needed** from what the requirements imply. Treat them as proposals and replace them with your original text if you have it.

## 1. Product overview

### 1.1 Summary

FairFold is a recruitment web platform with two portals. Employers post jobs, receive anonymised applications, and get a ranked list in which every score is explained with quotes from the candidate's own resume. Candidates build a profile from their resume, apply to jobs, see how they were scored, take skill assessments and practise interviews in text. A human always makes the final hiring decision.

### 1.2 Problem

- Early screening is where bias and favouritism do the most damage. Even automated tools can discriminate: Amazon's recruiting model learned to downgrade women's colleges, and a hard-coded age filter at iTutorGroup led to a legal settlement.
- Removing a name is not enough. Institution names, employer names and dates act as proxies for gender, age and background.
- Black-box scores cannot be explained to candidates or auditors.
- A fixed CV template hides what candidates can actually do.

### 1.3 Goals

| ID | Goal | Target |
|---|---|---|
| G1 | Personal data stays away from the AI provider | 100% of AI requests pass a final scan for the candidate's known identifiers. On a 30-resume test set, at least 95% of emails, phone numbers and names are stripped. |
| G2 | Every score is explainable | 100% of scores show claims with quotes that passed validation, or are labelled rule-based. |
| G3 | Every decision is auditable | An audit entry exists for each AI call, filter result, identity reveal and override. |
| G4 | Low cost for employers | A cost estimate is shown before every screening run; fallback mode costs nothing. |
| G5 | Skills count, not only tenure | Candidates can take assessments and practise interviews, and verified skills show on the profile. |
| G6 | Humans stay in charge | AI is advisory. No candidate is rejected automatically. |

### 1.4 Non-goals

Bangla or other non-English CV parsing; local ML models, embeddings or vector search; video or audio interviews and any voice, accent or face analysis; scraping LinkedIn or other sites; building courses (only external links); automatic hiring decisions; candidate paid plans; native mobile apps; recurring billing or card storage; calendar file generation; gradients in the interface; real money payments until bKash approves a merchant account.

### 1.5 Users

| User | Need |
|---|---|
| Candidate | Fair evaluation, clear feedback, a way to show real skills. |
| Employer HR | Screen many applications quickly and defensibly. |
| Hiring manager | See the ranked list and reasons, shortlist, view analytics. |
| Interviewer | Question pack and a simple feedback form. |
| Platform admin | Manage users, content, resources and the audit log. |
| Guest | Browse public jobs. |

## 2. Technology and key decisions

| Layer | Choice | Notes |
|---|---|---|
| Backend | Java 21, Spring Boot 3.x (current stable), Spring Web, Security, Data JPA, Validation | One application, packaged by feature (modular monolith). |
| Database | MariaDB (verified 13.0.2; InnoDB, `utf8mb4`, collation `utf8mb4_0900_as_ci`) | Flyway migrations. InnoDB full-text search for the job board. |
| Frontend | React 18, **TypeScript**, Vite, React Router, Axios, TanStack Query, React Hook Form, Tailwind (Dark & Light themes via CSS variables, rounded corners), in-house Markdown editor/renderer, react-chartjs-2 with chart.js, lucide-react icons, one self-hosted sans-serif font, Inter (`@fontsource-variable/inter`) | TypeScript is now the decision, not an option. Visual rules are in `design.md`. |
| AI | DeepSeek HTTP API, model `deepseek-v4.1-flash` | Model id, key and prices come from environment variables. Behind an `AiProvider` interface with a `RuleBasedFallbackProvider`. |
| Payments | bKash sandbox | Behind a `PaymentProvider` interface. |
| Files | Local disk volume behind a `StorageService` interface | Can be swapped for S3 or MinIO later. |
| Resume text | Apache PDFBox | PDF only. |
| Email | SMTP (links printed in the terminal in local development) | Reset, invites, notifications. Verification emails come later (FR-001). |
| Background work | `@Async` with a thread pool, `@Scheduled` jobs | State lives in database columns, so work survives a restart. |
| Auth | JWT access token (kept in memory), refresh token in an HttpOnly cookie, BCrypt, optional TOTP (RFC 6238) | Same origin through Nginx, so no CORS setup. |
| Live updates | **Polling** with TanStack Query (messages every 10 s while a thread is open) | No WebSocket or SSE. |
| Rate limiting | Bucket4j, in memory | Fine for one server. |
| API docs | springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | Generates the OpenAPI document and Swagger UI from the controllers. Use the 2.8.x line with Spring Boot 3.x; the 3.x line of springdoc is for Spring Boot 4. On in development, off in production. |
| Testing | JUnit 5, Mockito, Testcontainers (MariaDB), Vitest for key frontend logic | |
| Deployment | Docker Compose: nginx (serves React build, proxies /api), backend, MariaDB (`mariadb:13.0`) | CI: GitHub Actions build and test. |

Deliberately not used: Redis, MinIO as a requirement, ClamAV, Prometheus, Kubernetes, Spring AI, embeddings, microservices, WebSockets.

## 3. Roles and permissions

Each account has one role. A user cannot be both candidate and employer. Roles: `candidate`, `employer_hr`, `employer_manager`, `interviewer`, `admin`.

| Capability | Guest | Candidate | HR | Manager | Interviewer | Admin |
|---|---|---|---|---|---|---|
| Browse public jobs | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Profile, journey, resume, apply, assessments, practice, resources | | ✓ | | | | |
| Messaging on a shortlisted application | | ✓ | ✓ | ✓ | | |
| Create, edit, publish jobs; run screening | | | ✓ | | | |
| View ranked list and rationale; shortlist or reject | | | ✓ | ✓ | | |
| Analytics | | | ✓ | ✓ | | |
| Team and billing | | | ✓ | | | |
| Interview pack and feedback | | | ✓ | ✓ | ✓ | |
| Users, audit log, content (assessments, practice questions, resources) | | | | | | ✓ |

## 4. Functional requirements

Priority: Must, Should, Could. Phase: P1 to P4 (section 7). Each acceptance criterion is checked by an automated test.

### 4.1 Authentication

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-001 | Registration | Must | P1 | Valid email, password (at least 10 characters), first and last name, role (candidate or employer) and a consent tick create an account that can log in at once. **Email verification is deferred:** `users.email_verified` and the setting `app.auth.require-email-verification` (default `false`) exist from day one. When the setting is turned on later, new accounts start unverified, a verification link is emailed, and login is blocked until it is used (with a resend option). Admin accounts come from a seed script, never from sign-up. Invited team members register through their invite link (FR-047). |
| REQ-FR-002 | Login | Must | P1 | A user with correct credentials (and, once verification is turned on, a verified email) gets a 15-minute access token and a 7-day refresh token (HttpOnly cookie). The failed-attempt counter resets. |
| REQ-FR-003 | Account lockout | Must | P1 | Five consecutive failures lock the account for 15 minutes. The error shows the time left. |
| REQ-FR-004 | Password recovery | Must | P1 | A reset link, valid for 1 hour and usable once, is emailed. After a reset, all refresh tokens of that user are revoked. |
| REQ-FR-005 | MFA enable | Should | P4 | A TOTP secret and QR code are generated. The secret is stored encrypted with a key from the environment. MFA is optional for every role. Turning it off needs the password and a valid code. |
| REQ-FR-006 | MFA verify | Should | P4 | When MFA is on, login needs a valid 6-digit code after the password. Five wrong codes count as failed logins (FR-003). |
| REQ-FR-007 | Token refresh | Must | P1 | A valid refresh token returns a new access token and a new refresh token. The old one is marked revoked in the database. Reusing it returns 401. |
| REQ-FR-008 | Logout | Must | P1 | The current refresh token is revoked and the cookie cleared. |

### 4.2 Candidate portal

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-009 | Profile | Must | P1 | Candidate saves title, bio, location and remote preference. Profile view displays all candidate attributes (title, location, phone, remote preference, total experience, education level, bio in Markdown, skills and current resume link) with an Overview and Edit view. |
| REQ-FR-010 | Resume upload & preview | Must | P1 | PDF only, up to 10 MB, checked by file signature (not by extension). Stored under a random UUID name through `StorageService`. A new upload replaces the old resume. Candidate can preview the uploaded PDF inline (`iframe`) or open it in a new tab (`GET /api/candidate/resume/file`). |
| REQ-FR-011 | PII stripping | Must | P2 | After extraction the text is stripped per BR-05. The stripped text is the only resume text kept for employers and for AI use; the private mapping (BR-05) is stored separately and only for the candidate. An audit entry is written. |
| REQ-FR-012 | Text extraction | Must | P2 | PDFBox extracts the text. If there are fewer than 200 characters or under 85% Latin letters, the resume is marked failed with a clear reason (scanned PDF or non-English). The upload page states that only English PDFs are supported. |
| REQ-FR-013 | Structured profile from resume | Must | P2 | The stripped text is sent to the AI to extract skills, years per skill, total experience, education level, job titles and work history (title, skills used). Employer names and start and end months do not come from the AI: employer names are restored from the private placeholder mapping (BR-05), months are parsed by rules from the original text on the server, and the candidate confirms both on the profile. The result pre-fills the profile and the candidate can edit everything. If AI is down, the candidate fills the form by hand. |
| REQ-FR-014 | Skills | Must | P1 | Add, edit and delete skills with level (beginner, intermediate, expert) and years. |
| REQ-FR-015 | Skill de-duplication | Should | P2 | Aliases such as JS and JavaScript map to one canonical skill from a seeded list. |
| REQ-FR-016 | Journey timeline | Should | P3 | A career timeline chart is built from work history, with a table view. Only the candidate sees employer names here. |
| REQ-FR-017 | Skill evolution | Could | P3 | A chart shows when each skill was first used, with a table view. |
| REQ-FR-018 | Job-specific story | Could | P3 | For a chosen application the AI writes a short narrative of the candidate's relevant experience from stripped text. Placeholders are restored locally before display. |
| REQ-FR-019 | Assessment taking | Should | P3 | A timer starts, answers autosave, and the attempt auto-submits at timeout. |
| REQ-FR-020 | Assessment scoring | Should | P3 | Multiple-choice questions are scored automatically and the total is stored. Coding and free-text questions are out of scope. |
| REQ-FR-022 | Apply | Must | P1 | A candidate applies once per active job and needs an uploaded resume (`uploaded` or `ready`; `processing` or `failed` blocks it with 409). Status is `applied`. Both sides get an in-app notification. |
| REQ-FR-023 | Status tracking | Must | P1 | The candidate sees the current status and history. Each change creates a notification. |
| REQ-FR-024 | Match score visibility | Must | P2 | After screening, the candidate sees the score (0–100), the quoted evidence and the missing skills for their application. |
| REQ-FR-044 | Certifications | Could | P3 | Add, edit and delete certifications (name, issuer, dates, credential id as plain text). |

### 4.3 Employer portal and jobs

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-025 | Job creation | Must | P1 | Employer saves a draft with title, description, requirements, required and nice-to-have skills, experience level, city, country, remote flag and optional hard-filter settings (BR-03), an optional cut-off score (used by FR-052) and optional required assessments (FR-051). A 'Suggest screening questions' button calls the AI (P2); the employer edits the suggestions and they are saved with the job and shown in its interview pack tab. |
| REQ-FR-026 | Job editing | Must | P1 | Only drafts can be edited or deleted. Other states return 409. Every edit is audited. 'Duplicate as draft' covers changes after publishing. |
| REQ-FR-027 | Job lifecycle actions | Must | P1 | Activate (needs a company profile), pause and close follow BR-01. Each action is audited. |
| REQ-FR-028 | Screening trigger | Must | P2 | 'Screen All' shows the candidate count and a cost estimate (zero in fallback mode). On confirm, applications are queued and progress is shown. Failed items can be retried. |
| REQ-FR-029 | Result display | Must | P2 | The ranked list shows alias, score, status, bias badge and assessment gate status. No name, photo or contact detail is shown. `not_matched` candidates stay visible behind a toggle with the rule that excluded them, and the employer can pull any of them back into review. |
| REQ-FR-030 | Evidence-cited rationale and reveal | Must | P2 | The detail view shows each claim with its quote highlighted in the stripped resume, the missing skills and the bias status. The real name and contact details are revealed only when the employer shortlists, to that employer only, with an audit entry. |
| REQ-FR-031 | Interview pack | Should | P3 | The AI creates 6 to 10 structured questions and a scoring rubric from the job. The employer edits and saves. |
| REQ-FR-032 | Interview scheduling | Should | P3 | Employer sets date, duration, a pasted meeting link and one or more interviewers from the team. The candidate and interviewers get an in-app notification and an email with the details. |
| REQ-FR-033 | Interview feedback | Should | P3 | Interviewer submits scores per rubric item, comments and a recommendation (`strong_yes`, `yes`, `no`, `strong_no`). Afterwards HR or a manager marks the candidate `hired` or `rejected`. |
| REQ-FR-035 | Analytics | Should | P4 | Pipeline funnel, time-to-hire, a table of overrides with reasons, and the count of `not_matched` candidates pulled back. Each chart has a table view. |
| REQ-FR-045 | Company profile | Must | P1 | Name, industry and size (required before a job can be activated) plus optional tagline, about (Markdown), website, LinkedIn, logo URL, founded year, headquarters, contact email, phone and benefits (Markdown). |
| REQ-FR-046 | Markdown content | Must | P1 | Job description/requirements and company about/benefits are Markdown, written in an editor with a quick-format toolbar (heading, bold, italic, code, link, lists, quote, rule) and live preview. Rendered without raw HTML; only `http(s)`/`mailto` links. |
| REQ-FR-046 | Employer dashboard | Should | P2 | Open jobs, new applications, awaiting screening, interviews this week, quota meter and bias flags. Each links to its list. |
| REQ-FR-047 | Team | Should | P3 | HR invites staff by email as manager, HR or interviewer, changes roles and removes members. The last HR cannot be removed or demoted (409). |
| REQ-FR-048 | Billing | Must | P4 | The employer sees plan, usage and payment history and buys a 30-day plan through bKash (FR-056). |

### 4.4 Job discovery and messaging

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-042 | Job browse, search and public link | Must | P1 | Anyone, including guests, sees active jobs newest first. Keyword search over title, description and skills. Filters for location, remote and experience level. Pagination. The company name is hidden unless the employer opted in. Every active job has a stable public URL that the employer can copy. |
| REQ-FR-064 | Public company profile | Must | P1 | When an employer enables “show name publicly”, candidates and guests can open a public company page from the company name on job cards, job details and application rows. The page shows the company logo, name, tagline, industry, size, headquarters, founded year, website, LinkedIn, about and benefits, plus the company's active jobs. Companies that hide their name have no public page (404), are not linked, and cannot be used as a `companyId` job filter. |
| REQ-FR-043 | Messaging | Should | P3 | After shortlist (BR-14), candidate and employer can message inside the application thread. Messages are never sent to AI. When a user is deleted, their messages are blanked, not removed, for the other side. |

### 4.5 Administration and compliance

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-037 | Admin dashboard | Should | P4 | User, job and application counts, AI calls and estimated AI cost this month. |
| REQ-FR-038 | User management | Must | P4 | Search, filter, change role, unlock and trigger a password reset. |
| REQ-FR-039 | Audit log | Must | P4 | Filter by action, resource, user and date. Export to CSV. |
| REQ-FR-040 | Data export | Should | P4 | A signed-in user clicks 'Download my data' and receives a JSON file immediately. |
| REQ-FR-041 | Account deletion | Should | P4 | A user confirms with their password and the account is deleted. Personal data and files are removed. Audit rows stay, with the actor cleared. The last admin and the last HR of a company cannot delete themselves (409). |
| REQ-FR-049 | Content management | Should | P3 | Admin creates, edits and reorders assessments and their questions, manages the practice question bank, and curates learning resources (BR-15). Assessments and practice questions can be deactivated but never deleted. |

### 4.6 Fairness and integrity

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-051 | Required assessment | Should | P3 | Employer attaches assessments with a minimum score to a job. Applicants without a pass have gate status `pending` or `failed` and cannot be shortlisted: the button is disabled and the API returns 409. Jobs with no requirement use `not_required`. |
| REQ-FR-052 | Override record | Must | P2 | Shortlisting someone below the employer's cut-off score, or rejecting someone above it, needs a written reason, enforced by a database CHECK. An `ai_decision` audit entry is written. Overrides are never blocked. |
| REQ-FR-054 | Candidate alias | Must | P2 | Each employer organisation sees a random alias (for example C-7H2X9) for a candidate. The same employer sees the same alias across jobs. Different employers cannot link aliases. The alias is not derived from personal data. |
| REQ-FR-055 | Light proctoring | Could | P3 | Consent is taken before an attempt. Tab-switch and window-blur events are recorded and shown to the employer as neutral signals. They never fail an attempt or change a score. The candidate can add an explanation. |
| REQ-FR-056 | bKash payment | Must | P4 | See BR-30. |
| REQ-FR-057 | Proxy stripping | Must | P2 | Institution names, employer names, links, photos and age-revealing dates are replaced per BR-05. The candidate can preview the stripped resume and report wrong strips. |

### 4.7 Coaching

| ID | Requirement | Priority | Phase | Acceptance criteria |
|---|---|---|---|---|
| REQ-FR-058 | Text practice | Should | P3 | The candidate picks a question from the admin bank and types an answer. No audio or video. |
| REQ-FR-059 | STAR feedback | Should | P3 | The AI grades the answer against a versioned STAR rubric. Each feedback point quotes the candidate's own words. The rubric version is shown. Dialect and fluency are not penalised. |
| REQ-FR-060 | Coaching isolation | Must | P3 | Practice answers are private. Coaching tables have no foreign key to applications or jobs. Coaching data never affects ranking. Answers are stripped before any AI call. |
| REQ-FR-061 | Resources | Could | P3 | Admin curates links per skill (cost, language, level). They appear when an assessment shows a gap. A weekly job marks broken links. |
| REQ-FR-062 | Helpfulness rating | Could | P3 | A 1 to 5 star rating after each coaching session. |
| REQ-FR-063 | Retake loop | Could | P3 | Score below pass mark, then resources, then a 48-hour cooldown, then a retake. A pass adds a Verified badge to the skill. |

### 4.8 Removed or merged

| ID | Item | Reason |
|---|---|---|
| REQ-FR-021 | Interview coaching | Merged into FR-058 and FR-059. |
| REQ-FR-034 | AI offer letter | Needs candidate personal data; offers are written by hand. |
| REQ-FR-036 | Job share link | Merged into FR-042. |
| REQ-FR-050 | Broadcast announcements | High risk for little value. |
| REQ-FR-053 | Paid discovery rule | Replaced by BR-09, because there are no candidate paid plans. |

## 5. Business rules (rebuilt, please confirm)

Numbers BR-01, 03, 05, 09, 14 and 15 are the ones the requirements point to. New rules start at BR-20 so they cannot clash with original numbers.

| ID | Rule |
|---|---|
| BR-01 | **Job lifecycle.** `draft → active ↔ paused → closed`. Activation needs a company profile. Only drafts can be edited or deleted. Applications are accepted only while `active`. `closed` is final. |
| BR-03 | **Hard filters (optional, per job, no AI).** Minimum years of experience, required country, and a list of must-have skills. A candidate who fails a filter becomes `not_matched` and the failed rule is stored and shown. The employer can pull them back. |
| BR-05 | **Stripping, in this order.** (1) Remove the account's own name, email and phone wherever they appear. (2) Replace emails, phone numbers and URLs by pattern. (3) Replace institutions and employers using seeded lists plus word patterns such as University, College, Ltd, Inc. (4) Remove date of birth and education years; keep work experience as durations where the dates can be parsed. (5) Images are never read, because only text is extracted. (6) Before every AI call, scan the outgoing text for the candidate's known identifiers and block the call if one is found. Steps 3 and 4 are best-effort and measured on the test set. Placeholders are numbered (for example `[EMPLOYER_1]`, `[INSTITUTION_2]`). The mapping from each placeholder to the original value is stored in a candidate-only table. It is never sent to AI, never shown to employers, and is used only to restore employer names in the work history, the journey charts and the job story (FR-013, 016, 018). |
| BR-09 | **No pay-to-rank.** Payments, plans and coaching activity never change a score or rank. Only employers pay. |
| BR-14 | **Messaging.** A thread exists per application and opens only after shortlist. Both sides can write; messages are never sent to AI and are never read by the scoring code. A deleted user's messages are blanked for the other side. Messages are rate limited with Bucket4j. |
| BR-15 | **Resources.** A resource is a link tied to a skill, with cost, language and level. It is shown to a candidate only when an assessment on that skill is below the pass mark. A weekly job marks links that no longer respond; broken links are hidden from candidates. |
| BR-20 | **Scoring.** The AI returns, for each job requirement, a verdict (`met`, `partial`, `not_met`) and a quote. The backend checks that the quote appears word for word in the stripped resume; a verdict without a valid quote becomes `not_met`. Score = 100 × Σ(weight × credit) ÷ Σ(weight), where weight is 3 for required and 1 for nice-to-have skills and experience items, and credit is 1, 0.5 or 0. In fallback mode the same formula runs on keyword matches and every claim is labelled rule-based. |
| BR-21 | **Bias status.** After scoring, the AI output is scanned for mentions of protected traits or proxies (gender, age, religion, nationality, institution names). Status is `clear`, or `flagged` when found; the offending claim is removed and the employer is advised to review manually. |
| BR-30 | **bKash payment.** The employer picks a plan, the backend creates the payment, the employer approves on bKash, the backend verifies and executes it, and the plan is active for 30 days. No auto-renewal. Plans (name, monthly screening quota, price in BDT) are rows in a `plans` table set by the owner. Sandbox only until bKash approves a merchant account. |

## 6. Status models

| Entity | Values |
|---|---|
| Job | `draft`, `active`, `paused`, `closed` |
| Application | `applied`, `screening`, `screened`, `not_matched`, `shortlisted`, `interview`, `hired`, `rejected` |
| Screening item | `queued`, `running`, `done`, `failed` (a failed item keeps the application in `screening` until it is retried) |
| Assessment gate | `not_required`, `pending`, `passed`, `failed` |
| Bias status | `clear`, `flagged` |
| Resume | `uploaded`, `processing`, `ready`, `failed` |

Every application status change writes a history row, which feeds FR-023 and the time-to-hire chart.

### Application transitions

| From | To | Trigger |
|---|---|---|
| (new) | `applied` | Candidate applies (FR-022) |
| `applied` | `screening` | Employer confirms Screen All (FR-028) |
| `screening` | `screened` | Item scored (BR-20) |
| `screening` | `not_matched` | A hard filter failed (BR-03) |
| `not_matched` | `screening` | Employer pulls the candidate back: filters are skipped for this candidate, one scoring item is queued, an audit entry is written and the pull-back is counted in analytics |
| `screened` | `shortlisted` | Shortlist. Needs gate `passed` or `not_required`, and a written reason if the score is below the cut-off (FR-051, 052) |
| `screened`, `not_matched` | `rejected` | Reject. Needs a written reason if the score is above the cut-off |
| `shortlisted` | `interview` | Interview scheduled (FR-032) |
| `shortlisted`, `interview` | `rejected` | Reject |
| `interview` | `hired` | Employer marks hired (FR-033) |

`hired` and `rejected` are final. Any other transition returns 409.

## 7. Phases

Derived from the phase column of the requirements.

| Phase | Theme | Requirements |
|---|---|---|
| P1 | Accounts, jobs and applying | 001, 002, 003, 004, 007, 008, 009, 010, 014, 022, 023, 025, 026, 027, 042, 045 |
| P2 | Anonymised AI screening | 011, 012, 013, 015, 024, 028, 029, 030, 046, 052, 054, 057 |
| P3 | Skills, interviews and coaching | 016, 017, 018, 019, 020, 031, 032, 033, 043, 044, 047, 049, 051, 055, 058, 059, 060, 061, 062, 063 |
| P4 | Operations and money | 005, 006, 035, 037, 038, 039, 040, 041, 048, 056 |

Deferred outside the phases: email verification (the second half of FR-001, the verify-email page and the resend link). It is built after the other chunks.

## 8. Still missing from the source file

Non-functional targets (performance, availability, accessibility), the full data model and table list, the API contract, and the original business rules BR-02, 04, 06 to 08 and 10 to 13 (BR-14 and BR-15 were rebuilt above). Two behaviours are also undefined and are proposed here: what happens when the screening quota is used up (proposal: new screening runs return 409 and the page shows the buy-a-plan link), and how a lost authenticator is reset (proposal: an admin *Reset MFA* action with an audit entry). The architecture file proposes the API calls each page needs; from chunk 1 the real contract is generated by springdoc from the code, and those proposals are only the starting point.
