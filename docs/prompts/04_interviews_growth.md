# Prompt 04 — Interviews and Candidate Growth (days 6 to 8)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunks 1 to 3 must be finished.

**Goal:** one full hiring story from shortlist to message, interview, feedback and hire, plus the candidate growth features (practice, journey, job story).

**Parts:**
- **Part 4.1** Team and interviews: invites, roles, interview pack, scheduling, feedback, hire
- **Part 4.2** Messaging and interview practice: threads after shortlist, STAR feedback, rating, practice question bank
- **Part 4.3** Journey and story: career timeline, skill evolution, job-specific story

Start message: `Build Part 4.1 from docs/prompts/04_interviews_growth.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

If this chunk runs long, build messaging last.

## Requirements covered

| ID | What it must do |
|---|---|
| FR-047 Team | HR invites staff by email as manager, HR or interviewer, changes roles, removes members. The last HR cannot be removed or demoted (409 `LAST_HR`). |
| FR-031 Interview pack | The AI creates 6 to 10 structured questions and a scoring rubric from the job. The employer edits and saves. |
| FR-032 Interview scheduling | Employer sets date, duration, a pasted meeting link and one or more interviewers from the team. Candidate and interviewers get an in-app notification and a plain email. |
| FR-033 Interview feedback | Interviewer submits a score per rubric item, comments and a recommendation (`strong_yes`, `yes`, `no`, `strong_no`). Afterwards HR or a manager marks the candidate `hired` or `rejected`. |
| FR-043 Messaging | After shortlist, candidate and employer can message inside the application thread. Never sent to AI. A deleted user's messages are blanked for the other side. |
| FR-058 Text practice | Candidate picks a question from the admin bank and types an answer. No audio or video. |
| FR-059 STAR feedback | AI grades the answer against a versioned STAR rubric. Each point quotes the candidate's own words. The rubric version is shown. Dialect and fluency are not penalised. |
| FR-060 Coaching isolation | Practice answers are private. Practice tables have no foreign key to jobs or applications. Practice never affects ranking. Answers are anonymised before any AI call. |
| FR-062 Helpfulness rating | A 1 to 5 star rating after each coaching session. |
| FR-016 Journey timeline | A career timeline chart from work history, with a table view. Only the candidate sees employer names. |
| FR-017 Skill evolution | A chart of when each skill was first used, with a table view. |
| FR-018 Job-specific story | For a chosen application the AI writes a short narrative of the candidate's relevant experience from the stripped text. Placeholders are restored by FairFold before display. |
| FR-049 (practice part) | Admin manages the practice question bank (deactivate, never delete). |

## Rules and decisions

- **Roles in the team:** invitable roles are `employer_manager`, `employer_hr`, `interviewer`. An invite for an email that already has an account returns 409 `EMAIL_TAKEN`. A new invite for the same pending email replaces the old one. Invite token: 256-bit random, stored hashed, valid `app.invite.valid-days` (default 7). Link `${APP_BASE_URL}/accept-invite?token=...` (printed by the console mailer).
- **Removing a member** sets `users.disabled_at = now()` and revokes their refresh tokens; login for a disabled user returns 403 `ACCOUNT_DISABLED` (update the chunk 1 login). A member cannot remove themselves (409 `CANNOT_REMOVE_SELF`). The last HR cannot be removed or demoted (409 `LAST_HR`). Disabled users never appear in pickers or lists.
- **Interview rules:** scheduling needs application status `shortlisted` or `interview`; the first interview moves `shortlisted → interview` through `ApplicationStatusService`. `scheduledAt` must be in the future; duration 15 to 240 minutes; meeting link must be an `https://` URL; interviewers must be active members of the same company (any employer role). Rubric scores are 1 to 5 per rubric item (all items required when a pack exists).
- **Reject after shortlist** (extends chunk 2): HR or manager may reject from `shortlisted` or `interview`; no reason needed (override rules apply only to `screened` and `not_matched`). Mark hired only from `interview`.
- **Messaging rules (BR-14):** a thread is readable once `shortlisted_at` is set; sending is allowed while status is `shortlisted`, `interview` or `hired` (409 `THREAD_CLOSED` otherwise). Participants: the candidate, and HR and managers of the job's company. Body 1 to 2000 characters. Rate limit 10 messages per minute per user (Bucket4j). Notification to the other side with type `message`; do not create a new one while an unread `message` notification for the same application already exists. Opening the thread marks the other side's messages as read. Message text is never passed to AI and never appears in audit details.
- **Coaching isolation:** `practice_questions` and `practice_sessions` have no foreign key, direct or through another practice table, to `jobs` or `applications`. No code in `application`, `screening` or `job` packages may import the coaching package.
- **Practice anonymisation:** before the AI call, anonymise the answer with the same `ResumeStripper` steps for known identifiers, patterns, institutions and employers (not the date or line-deletion steps), run `IdentifierScan`, keep the placeholder mapping only in memory, and restore it in the quotes before saving feedback. STAR rubric `star-v1` lives in `resources/prompts/star-v1.txt`; it says to judge structure and content only and not to penalise dialect, accent, grammar or fluency.
- **Journey data:** computed from `work_history` and `candidate_skills`, no new table.

## AI contract additions

Extend `AiProvider` and `AiGateway` (record every call in `ai_calls`; same fallback rule: unavailable provider means a clear message, never silent fake output except where stated).

- `generateInterviewPack(JobText job, List<String> screeningQuestions)` → `{"questions":[string],"rubric":[{"criterion":string,"description":string}]}` with 6 to 10 questions and 3 to 6 rubric items. System prompt: `Create structured interview questions and a scoring rubric for this job. Questions must be job-related and free of questions about age, family, religion, nationality or health. Reply with JSON only.` **Fallback:** one question per required skill (up to 8, pad to 6 with generic ones: "Describe a challenging project you finished.", "How do you handle disagreement in a team?", "Why are you interested in this role?") and a default rubric: Technical skills, Problem solving, Communication, Role fit.
- `gradeStar(String anonymisedAnswer, String question)` → `{"situation":{"rating":1-5,"points":[{"comment":string,"quote":string}]},"task":{...},"action":{...},"result":{...},"summary":string}`. Backend validation: clamp ratings to 1 to 5; keep a point only if its quote appears in the anonymised answer after normalising. No fallback: when unavailable, save the session with `feedback = null` and answer `feedbackAvailable: false`.
- `writeStory(String strippedResume, JobText job)` → `{"story":string}` (120 to 180 words, first person, only facts from the text, placeholders kept unchanged). No fallback (503 `AI_UNAVAILABLE`).

## Database

### Part 4.1 migration (`V4_1_1__team_interviews.sql`)

| Table | Columns |
|---|---|
| (alter) users | add `disabled_at datetime(3) NULL` |
| team_invites | company_id → companies CASCADE; email text NOT NULL; role text CHECK (`employer_hr`,`employer_manager`,`interviewer`); token_hash text NOT NULL UNIQUE; invited_by char(36) NULL → users SET NULL; expires_at datetime(3) NOT NULL; accepted_at datetime(3) NULL; index on (email) (email is varchar(254)) |
| interview_packs | job_id → jobs CASCADE UNIQUE; questions json NOT NULL (`[{"id","text"}]`); rubric json NOT NULL (`[{"id","criterion","description"}]`); updated_by char(36) NULL → users SET NULL; updated_at |
| interviews | application_id → applications CASCADE; scheduled_at datetime(3) NOT NULL; duration_minutes int NOT NULL CHECK (15 to 240); meeting_link text NOT NULL; created_by char(36) NULL → users SET NULL; index (scheduled_at) |
| interview_interviewers | interview_id → interviews CASCADE; user_id → users CASCADE; PRIMARY KEY (interview_id, user_id) (no `id` column) |
| interview_feedback | interview_id → interviews CASCADE; interviewer_id char(36) NULL → users SET NULL; scores json NOT NULL DEFAULT (JSON_OBJECT()) (`{"<rubricId>":1-5}`); comments text NULL (max 2000); recommendation text NOT NULL CHECK (`strong_yes`,`yes`,`no`,`strong_no`); UNIQUE (interview_id, interviewer_id) |

### Part 4.2 migration (`V4_2_1__messages_practice.sql`)

| Table | Columns |
|---|---|
| messages | application_id → applications CASCADE; sender_id char(36) NULL → users SET NULL; body text NULL (set to NULL when the sender is deleted); read_at datetime(3) NULL; created_at; index (application_id, created_at) |
| practice_questions | text text NOT NULL; topic text NOT NULL; active boolean NOT NULL DEFAULT true |
| practice_sessions | user_id → users CASCADE; question_id → practice_questions; answer text NOT NULL; feedback json NULL; rubric_version text NOT NULL DEFAULT 'star-v1'; provider text NULL; rating smallint NULL CHECK (1 to 5). **No other foreign keys.** |

### Part 4.3

No migration.

## API

### Part 4.1

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/employer/team` | HR | `{members:[{id, name, email, role}], invites:[{id, email, role, expiresAt}]}` |
| POST `/api/employer/team/invites` | HR | `{email, role}` → 201; 409 `EMAIL_TAKEN` |
| DELETE `/api/employer/team/invites/{id}` | HR | cancel |
| PATCH `/api/employer/team/members/{id}` | HR | `{role}`; 409 `LAST_HR` |
| DELETE `/api/employer/team/members/{id}` | HR | 409 `LAST_HR`, `CANNOT_REMOVE_SELF` |
| GET `/api/auth/invites/{token}` | Guest | `{companyName, role, email}` or 404 `INVITE_INVALID` |
| POST `/api/auth/accept-invite` | Guest | `{token, firstName, lastName, password, consent}` → 201 session (like register) |
| GET `/api/employer/jobs/{id}/interview-pack` | HR, Mgr | `{questions, rubric, screeningQuestions}` or empty |
| POST `/api/employer/jobs/{id}/interview-pack/generate` | HR, Mgr | proposal only, not saved |
| PUT `/api/employer/jobs/{id}/interview-pack` | HR, Mgr | `{questions:[{id,text}], rubric:[{id,criterion,description}]}` |
| POST `/api/employer/applications/{id}/interviews` | HR, Mgr | `{scheduledAt, durationMinutes, meetingLink, interviewerIds[]}` |
| GET `/api/interviews?tab=upcoming\|past&page` | HR, Mgr, Interv. | interviewers see only theirs; item `{id, scheduledAt, candidateName, jobTitle, interviewers[], myFeedbackSubmitted}` |
| GET `/api/interviews/{id}` | HR, Mgr, Interv. | `{scheduledAt, durationMinutes, meetingLink, candidate{firstName,lastName}, applicationId, jobTitle, interviewers[], pack, myFeedback, allFeedback}`; `allFeedback` for HR and manager only, interviewers get their own |
| PUT `/api/interviews/{id}/feedback` | assigned users | `{scores, comments, recommendation}` (create or update) |
| POST `/api/employer/applications/{id}/hire` | HR, Mgr | `interview → hired`, audit, notify the candidate |
| (changed) `POST .../reject` | HR, Mgr | now also from `shortlisted` and `interview` |
| (changed) `GET /api/candidate/applications/{id}` | Cand. | adds `interviews:[{scheduledAt, durationMinutes, meetingLink}]` |
| (changed) `GET /api/employer/dashboard` | HR, Mgr | `interviewsThisWeek` now a number |

Notification types added: `interview_scheduled`, `hired`. Audit: `team_invited`, `team_role_changed`, `team_member_removed`, `interview_scheduled`, `interview_pack_saved`, `application_hired`.

### Part 4.2

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/applications/{id}/messages?afterId=` | Cand. owner, HR, Mgr of the company | `[{id, mine, senderName, body, createdAt}]`; marks the other side's messages read; 403 or 404 before shortlist |
| POST `/api/applications/{id}/messages` | same | `{body}` → 201; 409 `THREAD_CLOSED`; 429 |
| GET `/api/admin/practice-questions?page`, POST, PUT `/{id}`, PATCH `/{id}/active` | Admin | `{text, topic, active}` |
| GET `/api/practice/questions?topic` | Cand. | active only |
| POST `/api/practice/sessions` | Cand. | `{questionId, answer}` (20 to 3000 characters) → `{id, feedbackAvailable, feedback?, rubricVersion}` |
| GET `/api/practice/sessions?page`, GET `/api/practice/sessions/{id}` | Cand. | own only |
| POST `/api/practice/sessions/{id}/rating` | Cand. | `{rating}` 1 to 5 |

### Part 4.3

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/candidate/journey` | Cand. | `{timeline:[{id, title, employer, startMonth, endMonth, isCurrent}], skills:[{skillId, name, level, verified, firstUsedMonth, lastUsedMonth, roleTitles[]}]}` |
| POST `/api/candidate/applications/{id}/story` | Cand. owner | `{story, provider}`; not stored; 503 `AI_UNAVAILABLE` |

Journey calculation: timeline = the user's `work_history` sorted by `start_month` (null starts last). For each skill name in `skills_used`, map to a canonical skill with `SkillCanonicalizer`; `firstUsedMonth` = earliest `start_month` of entries listing it, `lastUsedMonth` = latest `end_month` (or today for a current job), `roleTitles` = titles of those entries. Profile skills never mentioned in history appear with `firstUsedMonth = null`.

## Pages

| Page | Part | Shows and does |
|---|---|---|
| P29 Team | 4.1 | Members and pending invites table (name, email, role, status). Invite dialog (email, role), change role (select + confirm), remove (confirm), cancel invite. Shows the 409 messages. HR only. |
| P08 Accept invite | 4.1 | Company name and role from the token, first name, last name, password, consent; signs in. Invalid token message. |
| P24 Interview pack tab | 4.1 | Saved screening questions as a reference list; "Generate with AI" (fills an editable proposal); editable questions (add, remove, Move up and down) and rubric items (criterion, description); Save. Banner when the fallback template was used. |
| P25 Review | 4.1, 4.2 | After shortlist: *Schedule interview* dialog (date and time, duration, meeting link, interviewers multi-select); after interviews, feedback summary list and *Mark hired* and *Reject*; a **Messages** tab (shortlisted and later). |
| P26 Interviews | 4.1 | Tabs Upcoming and Past; table (date and time, candidate name, job, interviewers, my feedback status); paging |
| P27 Interview detail | 4.1 | Candidate name, job, date, duration, meeting link button, interviewers; question pack and rubric (read-only); feedback form (score 1 to 5 per rubric item, comments, recommendation); HR and manager see all feedback |
| P16 Application detail | 4.1, 4.2, 4.3 | Interview card (date, duration, link); **Messages** tab (shown once shortlisted; before that: "Messaging opens if the employer shortlists you"); **Story** panel |
| P19 Interview practice | 4.2 | Note: "Practice is private and never affects your applications." Question picker (topic filter), text area with character counter, STAR feedback blocks (Situation, Task, Action, Result with rating and points quoting the answer, summary) with the rubric version, optional 1 to 5 star rating, history list; message when feedback is unavailable |
| P34 Content | 4.2 | Adds tab **Practice questions** (list, add and edit dialog, active switch) |
| P36 Journey | 4.3 | *Career timeline* (one square bar per role) and *Skill evolution* (one bar per skill from first to last use), each with "View as table"; empty state with links to P14 and P13 |
| Messages component | 4.2 | `MessageThread`: list (own messages `primary-subtle`, others `raised`), composer with counter, Send; polls every 10 s while open |
| Story panel | 4.3 | "Write my story for this job" button; shows the text with the tag "AI draft, check before using", Copy and Regenerate; error state when AI is unavailable |

Nav: HR adds Team and Interviews; manager adds Interviews; interviewer gets only Interviews; candidate adds Practice and Journey.

---

## Part 4.1 — Team and interviews

**Tasks**
1. Migration `V4_1_1`; entities. Update login to reject disabled users.
2. Team service and endpoints with all rules and the last-HR guard; invite and accept flow (console mail).
3. Interview pack: `AiProvider.generateInterviewPack` (+ fallback template), endpoints, saved pack.
4. Interviews: schedule (status change, notifications, plain email via `MailService`, audit), list with role scoping, detail, feedback upsert, hire, reject extension, dashboard count, candidate DTO addition.
5. Frontend: P29, P08, P24 pack tab, P25 schedule, feedback summary and hire, P26, P27, P16 interview card. Add nav items.

**Tests:** `Fr047_*` (invite, accept, role change, last HR 409 on remove and demote, self-removal 409, disabled login 403, existing email 409), `Fr031_*` (proposal not saved until PUT; fallback template shape: 6 to 8 questions, 4 rubric items), `Fr032_*` (status moves to `interview`, only shortlisted or interview allowed, past date rejected, interviewers must be company members, notifications created), `Fr033_*` (feedback upsert, only assigned users, interviewers see only their own, rubric completeness), hire only from `interview`, company scoping (404 for another company's interview).

**Definition of done:** HR invites an interviewer, the interviewer accepts and logs in; HR saves a pack, shortlists a candidate, schedules an interview with that interviewer; the interviewer sees it under Interviews, submits feedback; HR sees it and marks hired; the candidate sees the interview card and a `hired` notification.

---

## Part 4.2 — Messaging and interview practice

**Tasks**
1. Migration `V4_2_1`; entities.
2. Messaging service and endpoints with all rules (access, thread-open, closed, rate limit, notification collapsing, read marking). Blank-on-delete is implemented in chunk 5 (account deletion); add the nullable columns now.
3. `AiProvider.gradeStar`, practice anonymisation, validation, `star-v1.txt`, endpoints for sessions and rating; admin practice question endpoints.
4. Frontend: `MessageThread` plus tabs on P16 and P25, P19, P34 practice tab. Add nav item Practice.
5. `CoachingIsolationTest`.

**Tests:** `Fr043_*` (not readable before shortlist, can send in allowed statuses, `THREAD_CLOSED` after reject, rate limit 429, notification collapsing, unread marking, message text never present in `ai_calls` or audit details), `Fr058_*`, `Fr059_*` (fake provider returns a point with an invented quote → dropped; ratings clamped; placeholders restored in saved quotes; unavailable provider saves the answer with `feedbackAvailable:false`), `Fr060_*` (schema test: no foreign key path from `practice_*` to `jobs` or `applications`; import test for the coaching package), `Fr062_*`.

**Definition of done:** after shortlist, the candidate and HR exchange messages in both UIs with live refresh; a candidate answers a practice question and gets STAR feedback with quotes (use a fake AI provider if no key); the rating saves.

---

## Part 4.3 — Journey and story

**Tasks**
1. `JourneyService` and endpoint (pure calculation, unit-tested).
2. `AiProvider.writeStory`, story endpoint with placeholder restoration from `resume_placeholder_map` (candidate-owned), `IdentifierScan`, audit through `ai_calls`.
3. Frontend: P36 with two charts and table toggles. Use a numeric x scale (months since the earliest start) with tick labels rendered as `MMM YYYY`, so no date adapter library is needed; floating horizontal bars; square style from `charts/defaults.ts`. `StoryPanel` on P16.
4. Add candidate nav item Journey.

**Tests:** `Fr016_*` and `Fr017_*` (timeline order, first and last use, current job uses today, aliases map to canonical skills, skills without history have null first use), `Fr018_*` (candidate-only access, final scan blocks on leak, placeholders restored, nothing stored, 503 when unavailable).

**Definition of done (whole chunk):** demo path: invite and accept an interviewer → shortlist → message → schedule → feedback → hire; candidate practises an answer, opens the journey charts, and generates a story for an application. Tests green, design check passes, `docs/openapi.json` re-exported, `docs/progress.md` updated.
