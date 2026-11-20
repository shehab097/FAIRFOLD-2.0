# Prompt 03 — Skills Proof (days 4.5 to 6)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunks 1 and 2 must be finished.

**Goal:** a candidate fails an assessment, sees learning resources, retakes it after the cooldown and passes, earning a Verified badge. An employer can require assessments per job; the shortlist button is blocked until the candidate passes.

**Parts:**
- **Part 3.1** Admin content: assessments, questions, resources, link check, admin shell
- **Part 3.2** Taking assessments: attempts, timer, autosave, scoring, Verified badge, retake loop
- **Part 3.3** Gate, proctoring and certifications: required assessments per job, gate logic, focus signals, employer view

Start message: `Build Part 3.1 from docs/prompts/03_skills_proof.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

## Requirements covered

| ID | What it must do |
|---|---|
| FR-049 Content management (assessments and resources part) | Admin creates, edits and reorders assessments and their questions, and curates learning resources (BR-15). Assessments and resources can be deactivated, never deleted. *(The practice question bank is added in chunk 4.)* |
| FR-019 Assessment taking | A timer starts, answers autosave, the attempt auto-submits at timeout. |
| FR-020 Assessment scoring | Multiple-choice questions are scored automatically and the total is stored. No coding or free-text questions. |
| FR-063 Retake loop | Score below pass mark → resources → 48-hour cooldown → retake. A pass adds a Verified badge to the skill. |
| FR-061 Resources | Admin curates links per skill (cost, language, level). They show when an assessment shows a gap. A weekly job marks broken links. |
| FR-051 Required assessment | Employer attaches assessments with a minimum score to a job. Applicants without a pass have gate status `pending` or `failed` and cannot be shortlisted: the button is disabled and the API returns 409 `GATE_NOT_PASSED` (already implemented in chunk 2; now the status is real). Jobs with no requirement use `not_required`. |
| FR-055 Light proctoring | Consent is taken before an attempt. Tab-switch and window-blur events are recorded and shown to the employer as neutral signals. They never fail an attempt or change a score. The candidate can add an explanation. |
| FR-044 Certifications | Add, edit, delete certifications (name, issuer, dates, credential id as plain text). |

## Rules and decisions

- **Questions are multiple choice only.** 2 to 6 options per question, exactly one correct. Option ids are short strings (`a`, `b`, `c`, …).
- **Assessment limits:** duration 5 to 180 minutes, pass mark 1 to 100, at least 1 question.
- **Questions are locked once attempts exist.** If an assessment has any attempt, changing questions returns 409 `ASSESSMENT_HAS_ATTEMPTS` (title, active, duration and pass mark can still change). To change content, deactivate and create a new assessment.
- **Active only:** candidates and job attachment see only active assessments and active, not-broken resources.
- **Attempt rules:**
  - Starting when an `in_progress` attempt exists for that assessment returns that same attempt (a reload resumes).
  - After a **failed** submitted attempt, a new start is allowed only after `submitted_at + app.assessment.cooldown-hours` (default 48; set 0 in tests and optionally in local). Otherwise 409 `COOLDOWN` with `retryAvailableAt`.
  - After a pass, a retake is allowed any time (an employer may require a higher minimum). The Verified badge is never removed.
  - `ends_at = started_at + duration`. Answers are accepted only while `status = in_progress` and `now <= ends_at + 5 seconds`.
  - Score = `round(100 × correct ÷ totalQuestions)`. `passed = score >= pass_mark`. Unanswered counts as wrong.
- **Verified badge:** on a pass, upsert `candidate_skills` for the assessment's skill with `verified = true`, `verified_at = now()`; if the candidate does not have the skill yet, create it with level `beginner` and `years = null`. *(Decision: note it in progress.md.)*
- **Auto-submit sweeper:** `@Scheduled(fixedDelay = 60s)` submits every `in_progress` attempt with `ends_at + 5 s < now`, using the saved answers, status `auto_submitted`.
- **Gate (BR for FR-051)** `GateService.recompute(applicationId)`:
  - No required assessments on the job → `not_required`.
  - Every required assessment has a submitted attempt by the candidate with `score >= min_score` → `passed`.
  - Else, if the candidate has any submitted attempt for any required assessment → `failed`.
  - Else → `pending`.
  Call it when a candidate applies, when an attempt is submitted (for all of that candidate's applications whose job requires that assessment), and when a job is duplicated.
- **Proctoring signals never touch scoring or the gate.** If consent was not given, record nothing; employers see "Not recorded".
- **BR-15 Resources:** shown to a candidate only after a failed attempt on that skill's assessment. Broken links are hidden from candidates until fixed. The weekly link check uses HEAD (GET on 405), 5-second timeout, follows redirects, only `http` and `https`, and refuses localhost and private IP ranges.

## Database

### Part 3.1 migration (`V3_1_1__content.sql`)

| Table | Columns |
|---|---|
| assessments | title text NOT NULL; skill_id → skills; duration_minutes int NOT NULL CHECK (5 to 180); pass_mark int NOT NULL CHECK (1 to 100); active boolean NOT NULL DEFAULT true |
| assessment_questions | assessment_id → assessments CASCADE; position int NOT NULL; text text NOT NULL; options json NOT NULL (`[{"id":"a","text":"..."}]`); correct_option_id text NOT NULL; UNIQUE (assessment_id, position) |
| resources | skill_id → skills; title text NOT NULL; url text NOT NULL; cost text NOT NULL DEFAULT 'free' CHECK (`free`,`paid`); language text NOT NULL DEFAULT 'English'; level text NOT NULL CHECK (`beginner`,`intermediate`,`advanced`); active boolean NOT NULL DEFAULT true; broken boolean NOT NULL DEFAULT false; last_checked_at datetime(3) NULL |

### Part 3.2 migration (`V3_2_1__attempts.sql`)

| Table | Columns |
|---|---|
| assessment_attempts | assessment_id → assessments; user_id → users CASCADE; started_at datetime(3) NOT NULL; ends_at datetime(3) NOT NULL; submitted_at datetime(3) NULL; answers json NOT NULL DEFAULT (JSON_OBJECT()) (`{"<questionId>":"<optionId>"}`); score int NULL; passed boolean NULL; status text NOT NULL DEFAULT 'in_progress' CHECK (`in_progress`,`submitted`,`auto_submitted`); proctoring_consent boolean NULL; explanation text NULL (max 500); indexes (user_id, assessment_id, started_at DESC), index on (status, ends_at) |

### Part 3.3 migration (`V3_3_1__gate_proctoring_certs.sql`)

| Table | Columns |
|---|---|
| proctor_events | attempt_id → assessment_attempts CASCADE; type text CHECK (`hidden`,`blur`); occurred_at datetime(3); duration_seconds int NOT NULL DEFAULT 0 CHECK (>= 0) |
| job_required_assessments | job_id → jobs CASCADE; assessment_id → assessments; min_score int NOT NULL CHECK (1 to 100); UNIQUE (job_id, assessment_id) |
| certifications | user_id → users CASCADE; name text NOT NULL; issuer text NOT NULL; issued_on date NULL; expires_on date NULL; credential_id text NULL |

## API

### Part 3.1 (Admin)

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/admin/assessments?page` | Admin | item `{id, title, skillName, questionCount, durationMinutes, passMark, active, attemptCount}` |
| GET `/api/admin/assessments/{id}` | Admin | with questions including `correctOptionId` |
| POST `/api/admin/assessments` | Admin | `{title, skillId, durationMinutes, passMark, active, questions:[{text, options:[{id,text}], correctOptionId}]}`; question order = array order |
| PUT `/api/admin/assessments/{id}` | Admin | same body; questions replaced as a whole unless attempts exist (409 `ASSESSMENT_HAS_ATTEMPTS` if the questions differ) |
| PATCH `/api/admin/assessments/{id}/active` | Admin | `{active}` |
| GET `/api/admin/resources?skillId&page` | Admin | |
| POST, PUT `/api/admin/resources[/{id}]` | Admin | `{skillId, title, url, cost, language, level, active}` |
| PATCH `/api/admin/resources/{id}/active` | Admin | |
| POST `/api/admin/resources/check` | Admin | runs the link check now → `{checked, broken}` |

Audit: `assessment_created`, `assessment_updated`, `assessment_activated`, `assessment_deactivated`, `resource_created`, `resource_updated`.

### Part 3.2 (Candidate)

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/assessments` | Cand. | active assessments `{id, title, skillName, questionCount, durationMinutes, passMark, bestScore, passed, lastAttemptAt, retryAvailableAt, inProgressAttemptId}` |
| GET `/api/candidate/attempts?page` | Cand. | history `{id, assessmentTitle, score, passed, status, submittedAt}` |
| POST `/api/assessments/{id}/attempts` | Cand. | → `{attemptId, assessmentTitle, endsAt, serverTime, questions:[{id, position, text, options:[{id,text}]}], answers}` (never the correct ids); 409 `COOLDOWN`, `ASSESSMENT_INACTIVE` |
| PUT `/api/attempts/{id}/answers` | Cand. | `{answers:{questionId:optionId}}` merged into saved answers; 409 `ATTEMPT_CLOSED` |
| POST `/api/attempts/{id}/submit` | Cand. | idempotent; → result |
| GET `/api/attempts/{id}` | Cand. | in progress: same as start; submitted: `{score, passed, passMark, correctCount, totalQuestions, status, skillVerified, retryAvailableAt, resources[] (only when failed)}` |
| GET `/api/resources?skillId=` | Cand. | active and not broken |

### Part 3.3

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/attempts/{id}/consent` | Cand. | `{given: boolean}` once, before answering |
| POST `/api/attempts/{id}/events` | Cand. | `[{type, occurredAt, durationSeconds}]`; ignored unless consent is true and the attempt is `in_progress`; cap 500 events per attempt |
| PUT `/api/attempts/{id}/explanation` | Cand. | `{text}` up to 500 characters; only when consent was given |
| GET, POST, PUT, DELETE `/api/candidate/certifications` | Cand. | CRUD |
| (changed) job body | HR | adds `requiredAssessments:[{assessmentId, minScore}]` (drafts only); `GET /api/employer/jobs/{id}` returns them with the assessment title; duplicate copies them |
| (changed) `GET /api/employer/applications/{id}` | HR, Mgr | adds `assessments:[{assessmentTitle, minScore, bestScore, passed, lastAttemptAt, focus:{recorded, leftCount, totalSeconds, explanation}}]` and `verifiedSkills:[name]`; no identity |
| (changed) `GET /api/candidate/applications/{id}` | Cand. | adds `requiredAssessments:[{assessmentId, title, minScore, bestScore, status}]` |

## Pages

| Page | Part | Shows and does |
|---|---|---|
| Admin shell | 3.1 | PortalShell with admin sidebar (Content only for now); `/admin` redirects to `/admin/content` until P31 exists (chunk 5) |
| P34 Content | 3.1 | Tabs **Assessments** (table, active switch, New assessment) and **Resources** (table with skill, cost, language, level, status OK or Broken, last check date; add and edit in a dialog; "Check links now"). The Practice questions tab is added in chunk 4. |
| P35 Assessment editor | 3.1 | Title, skill (suggest), duration, pass mark, questions list (text, 2 to 6 options, mark the correct one), Move up and Move down buttons, active switch, Save; shows a locked notice and disables question editing when attempts exist |
| P17 Assessments | 3.2 | Cards or rows for active assessments with Start or Resume or Retake (disabled with the retry time during cooldown), best score, Verified skills; attempt history |
| P18 Assessment attempt | 3.2, 3.3 | FocusShell. Consent dialog first (Agree or No thanks, both start the attempt). Timer from `endsAt` and the server-time offset (`offset = serverTime − clientNow` at load); question navigator (answered or not); one question at a time with Previous, Next, Submit (confirm with unanswered count); autosave 2 s after each change with a "Saved" indicator (`aria-live="polite"`); warnings at 5 minutes and 1 minute; auto-submit at zero. Result view: score, pass mark, Verified badge on pass; on fail the resources list and the retry time; optional explanation box when consent was given. Record `visibilitychange` and `blur` events (merge events under 1 s), send every 10 s and on submit. |
| P13 Profile | 3.3 | Adds *Certifications* section (name, issuer, dates, credential id; add, edit, delete) |
| P23 Job editor | 3.3 | Adds *Required assessments* (choose active assessments, minimum score each) |
| P25 Review | 3.3 | Adds an *Assessments* panel: per required assessment the title, best score against the minimum, Passed or Failed or Pending badge, and a neutral focus chip ("Left the tab 3 times, 1 min 20 s in total", the candidate's explanation, or "Not recorded") with the label "Does not affect the score"; Verified skills chips; Shortlist disabled with a tooltip when the gate is `pending` or `failed` |
| P16 Application detail | 3.3 | Adds *Required assessments* card with status and a Take button to P18 |

Add candidate nav item Assessments.

## Seed content (local profile only)

`DemoContentSeeder` (runs only in the `local` profile when no assessments exist): 3 assessments (for example JavaScript, SQL, Python) with 10 multiple-choice questions each, and about 15 resources across those skills. Real content is added by the admin.

---

## Part 3.1 — Admin content

**Tasks**
1. Migration `V3_1_1`; entities and repositories.
2. Admin CRUD for assessments with the validation rules and the locked-questions rule; active toggle; resources CRUD and active toggle.
3. `ResourceLinkChecker` with the schedule `app.resources.link-check-cron` (default `0 0 3 * * SUN`) and the manual endpoint; SSRF protection (scheme check, block localhost and private ranges).
4. Admin shell, nav, route guard for `admin`. Pages P34 (two tabs) and P35.
5. `DemoContentSeeder`.

**Tests:** `Fr049_*` (validation: one correct option, 2 to 6 options, min 1 question; deactivate instead of delete; locked questions with attempts is simulated by inserting an attempt), `Fr061_*` (checker marks a failing URL broken using a local test server or a mock `HttpClient`; refuses `http://localhost`), authorisation (non-admins get 403).

**Definition of done:** an admin creates an assessment with 3 questions and a resource, deactivates and reactivates it, runs the link check, and sees a broken link flagged.

---

## Part 3.2 — Taking assessments

**Tasks**
1. Migration `V3_2_1`; attempt entity.
2. `AttemptService`: start (resume, cooldown), autosave, submit (scoring, Verified badge upsert), result DTO with resources on failure. `AutoSubmitSweeper`.
3. `GateService.recompute` is created in 3.3, but call a stub now so 3.2 compiles.
4. Candidate endpoints; frontend API modules.
5. Pages P17 and P18 (without proctoring yet), `Countdown` hook using the server-time offset, `QuestionNavigator`, `SaveIndicator`.
6. Candidate nav item Assessments.

**Tests:** `Fr019_*` (autosave merge, rejects answers after `ends_at + 5 s`, sweeper submits expired attempts), `Fr020_*` (scoring table, unanswered counts as wrong, pass mark boundary), `Fr063_*` (cooldown blocks restart after a fail; `COOLDOWN` includes `retryAvailableAt`; pass creates the Verified badge and a missing skill row; retake allowed after a pass), resume of an in-progress attempt, correct ids never returned before submit.

**Definition of done:** a candidate starts an assessment, answers, reloads the page and continues, submits, fails, sees resources and a retry time, (cooldown set to 0 locally) retakes, passes, and sees the Verified badge on the profile skills.

---

## Part 3.3 — Gate, proctoring and certifications

**Tasks**
1. Migration `V3_3_1`.
2. Required assessments on jobs (draft only, copy on duplicate). `GateService.recompute` per the rules; call sites: apply, attempt submit, duplicate. Extend the applications endpoints and DTOs as in the API table.
3. Proctoring endpoints and client-side event recording; employer summary (count of events, total seconds, explanation, or "Not recorded").
4. Certifications CRUD and the profile section.
5. Frontend: P23 section, P25 panel, P16 card, P18 consent and events.
6. Extend `NoIdentityLeakTest` to the new employer fields.

**Tests:** `Fr051_*` (gate truth table: not_required, pending, failed, passed; shortlist returns 409 `GATE_NOT_PASSED` while pending or failed; works after passing; recompute triggered on submit), `Fr055_*` (events stored only with consent, never change score or gate, 500-event cap, explanation only with consent), `Fr044_*`, duplicate copies required assessments.

**Definition of done (whole chunk):** demo path: HR attaches an assessment (minimum 70) to a job → candidate applies, gate is `pending`, HR cannot shortlist → candidate takes the assessment, fails, studies the resources, retakes after the cooldown, passes → gate becomes `passed` → HR sees score and the neutral focus chip and shortlists. Tests green, design check passes, `docs/openapi.json` re-exported, `docs/progress.md` updated with decisions (Verified badge default level, retake after pass).
