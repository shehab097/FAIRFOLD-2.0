# Prompt 02 — Anonymised AI Screening (days 2.5 to 4.5)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document. Chunk 1 must be finished.

**Goal:** resumes are read, stripped of personal details, and turned into profile suggestions. Employers screen all applicants and get a ranked, explained list with aliases only. Shortlisting reveals identity; overrides need a reason.

**Parts:**
- **Part 2.1** Resume processing: text extraction, stripping, AI gateway, profile suggestions, work history, skill aliases
- **Part 2.2** Screening engine: aliases, hard filters, runs, scoring, bias check, fallback provider
- **Part 2.3** Employer and candidate views: ranked list, review page, shortlist, reject, reveal, override, pull back, dashboard, screening questions

Start message: `Build Part 2.1 from docs/prompts/02_ai_screening.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

## Requirements covered

| ID | What it must do |
|---|---|
| FR-011 PII stripping | After extraction the text is stripped per BR-05. The stripped text is the only resume text kept for employers and AI use; the private placeholder mapping is stored separately for the candidate. Audit entry `resume_stripped`. |
| FR-012 Text extraction | PDFBox extracts text. If under 200 characters, or Latin letters are under 85% of all letters, mark the resume `failed` with a reason (`SCANNED_OR_EMPTY` or `NOT_ENGLISH`) and a clear message. |
| FR-013 Profile suggestions | Stripped text goes to the AI to extract skills, years per skill, total experience, education level, job titles and work history (title, skills used). Employer names come from the private placeholder mapping; start and end months are parsed by rules on the server. The candidate reviews and edits. If AI is down, use rule-based suggestions (dictionary skills and education only) and let the candidate fill the rest by hand. |
| FR-015 Skill de-duplication | Aliases (JS, JavaScript) map to one canonical skill. |
| FR-024 Match score visibility | After screening the candidate sees score (0 to 100), the quoted evidence and the missing skills. |
| FR-025 (AI button) | "Suggest screening questions" on the job editor; the employer edits the list; it is saved with the job and shown later in the interview pack tab. |
| FR-028 Screening trigger | "Screen All" shows count and cost estimate (zero in fallback mode). On confirm, applications are queued and progress is shown. Failed items can be retried. |
| FR-029 Result display | Ranked list shows alias, score, status, bias badge and gate status. No name, photo or contact. `not_matched` candidates are behind a toggle with the excluding rule, and can be pulled back. |
| FR-030 Rationale and reveal | Detail view shows each claim with its quote highlighted in the stripped resume, missing skills and bias status. Name and contact are revealed only on shortlist, to that employer only, with an audit entry. |
| FR-046 Employer dashboard | Open jobs, new applications, awaiting screening, bias flags (interviews this week and quota arrive later). |
| FR-052 Override record | Shortlisting below the job's cut-off, or rejecting at or above it, needs a written reason (enforced by a database CHECK). Audit entry `ai_decision`. Overrides are never blocked. |
| FR-054 Candidate alias | Each employer company sees a random alias (for example `C-7H2X9`) per candidate. Same company, same alias across jobs. Different companies cannot link aliases. Not derived from personal data. |
| FR-057 Proxy stripping | Institutions, employers, links, photos and age-revealing dates are replaced per BR-05. Candidate can preview the stripped resume and report wrong strips. |

## Business rules

### BR-05 Stripping (implement as `ResumeStripper`, pure and unit-testable)

Input: extracted text, the candidate's known identifiers (first name, last name, full name, email, phone from profile). Output: stripped text plus a list of placeholder mappings. Steps, in this order:

1. **Known identifiers:** remove the account's name tokens (3+ letters, case-insensitive, also the full name), email and phone wherever they appear → `[NAME]`, `[EMAIL]`, `[PHONE]`.
2. **Header name:** if the first non-empty line is 2 to 4 title-case words with no digits, replace it with `[NAME]`.
3. **Personal-detail lines:** delete whole lines that start (case-insensitive) with: `Date of Birth`, `DOB`, `Born`, `Age`, `Gender`, `Sex`, `Marital Status`, `Nationality`, `Religion`, `Father`, `Mother`, `Spouse`, `NID`, `National ID`, `Passport`, `Address`, `Present Address`, `Permanent Address`, `Blood Group`, `Photo`.
4. **Patterns:** emails (`[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}`), phone numbers (7+ digits allowing `+`, spaces, dashes, parentheses), URLs (`https?://\S+`, `www\.\S+`, and `linkedin.com/...`, `github.com/...`) → `[EMAIL]`, `[PHONE]`, `[URL]`.
5. **Institutions and employers:** replace terms from `strip_terms` (seed lists) and pattern matches: institutions `([A-Z][\w&.'-]*( [A-Z][\w&.'-]*)* )?(University|College|Institute|School|Academy)( of [A-Z][\w ]*)?`; employers `[A-Z][\w&.'-]*( [A-Z][\w&.'-]*)*,? (Ltd\.?|Limited|Inc\.?|LLC|Corp\.?|Co\.|Company|PLC|Technologies|Solutions|Systems|Bank|Group)`. Replacements are numbered and consistent: the same original (case-insensitive) always gets the same placeholder, for example `[EMPLOYER_1]`, `[INSTITUTION_2]`. Save each mapping in `resume_placeholder_map`.
6. **Dates:** (a) education lines (lines containing an institution placeholder or a degree word such as BSc, B.Sc, MSc, M.Sc, Bachelor, Master, PhD, HSC, SSC, Diploma, GPA, CGPA): replace any 4-digit year or year range with `[YEAR]`. (b) Work date ranges (`Mon YYYY – Mon YYYY`, `Mon YYYY – Present`, `YYYY – YYYY`) are parsed to start and end months, replaced by a duration such as `(3 yrs 2 mos)`, and the parsed dates are saved on the nearest employer placeholder row (same line, or the line before or after) as `start_month`, `end_month`, `is_current`. Unparseable dates are left as `[DATE]`.
7. **Images** are never read (text extraction only).
8. **Final scan before every AI call** (`IdentifierScan`): the outgoing text must not contain the candidate's name tokens, email or the last 7 digits of the phone (case-insensitive). If it does: do not call, mark the item or resume failed with `IDENTIFIER_LEAK`, write an `ai_call_blocked` audit entry and an `ai_calls` row with status `blocked`.

Steps 5 and 6 are best effort; quality is measured by the golden test (target: at least 95% of "must not appear" strings removed over the 30 fixtures).

### BR-20 Scoring (implement as `ScoringService`, pure and unit-testable)

- The backend builds the requirement list: one item per job skill (`required` weight 3, `nice_to_have` weight 1, key `skill:<skillId>`) plus one `experience` item (weight 1, key `experience`, text "Experience level: <level>").
- The AI (or fallback) returns a verdict per key: `met`, `partial` or `not_met`, with a quote.
- **Quote validation:** for AI results, a `met` or `partial` verdict needs a quote of 10 to 300 characters that appears in the stripped text after normalising (lower-case, collapse whitespace). Otherwise the verdict becomes `not_met` and the quote is cleared. Missing or unknown verdicts become `not_met`.
- Credit: met 1, partial 0.5, not_met 0. **Score = round(100 × Σ(weight × credit) ÷ Σ(weight)).**
- `missing_skills` = names of `required` skills with verdict `not_met`.
- Fallback (rule-based) results have `source = rule_based` and are labelled "Rule-based" everywhere.

### BR-21 Bias status

After scoring, scan each claim's requirement text and quote (claims with verdict `met` or `partial`) for protected traits and proxies using `resources/bias_terms.txt` (one regex per line, word boundaries): gender words, age words (`years old`, `age`, `born`), marital and family words, religion words, nationality and ethnicity words, disability words, and any `[INSTITUTION_` placeholder. If a claim matches: delete that claim, recompute the score from the remaining claims, set `bias_status = flagged` and `bias_note = "Removed N claim(s) that referenced a protected trait or proxy"`. Otherwise `clear`. Keep the term list in the file, easy to edit.

### Cost estimate

`estimatedCostUsd = Σ over applications ( (len(strippedText)/4 + 600) × AI_PRICE_INPUT_PER_M + 400 × AI_PRICE_OUTPUT_PER_M ) / 1,000,000`. Zero when the run mode is `rule_based`.

### Hard filters (BR-03, rule-based, no AI), evaluated first

Fail only on **known contradicting data** (unknown passes, so missing data never excludes anyone):
- `hf_min_years`: fails if the candidate's `total_experience_years` is set and lower.
- `hf_country`: fails if the candidate's `location` is set and does not contain the country (case-insensitive).
- Must-have skills: fail if the skill is in neither the candidate's profile skills nor found (with aliases) in the stripped text.
The failed rule text is stored in `not_matched_rule` (for example `Minimum experience: 3 years`).

## AI contract

### Architecture

```java
public interface AiProvider {
  String name();                 // "deepseek" or "rule_based"
  boolean available();
  ProfileSuggestions extractProfile(String strippedText);
  List<Verdict> screen(ScreeningRequest request);        // requirements + strippedText
  List<String> suggestQuestions(JobText job);            // no personal data involved
  // later chunks add: interview pack, STAR feedback, story
}
```

`AiGateway` (service) is the only entry point for the rest of the app: it selects the provider (`app.ai.provider`, falls back to rule-based when the key is missing or the call fails), runs `IdentifierScan` on text from candidates, records every call in `ai_calls` (purpose, model, token counts, cost, status; **never the prompt or response text**), and writes audit entries.

`DeepSeekProvider`: OpenAI-compatible chat completions. `POST {DEEPSEEK_BASE_URL}/chat/completions`, header `Authorization: Bearer <key>`, body `{model, messages:[system,user], temperature:0, response_format:{type:"json_object"}, max_tokens}`; read `choices[0].message.content` as JSON and `usage` for tokens. Timeout 60 s, one retry on 5xx or timeout. **Check DeepSeek's current API documentation before coding; do not rely on memory for URLs or parameters.** Use Spring `RestClient`.

### Prompts (keep in `resources/prompts/*.txt`)

Profile extraction, system: `You extract structured data from a resume that has had personal details removed. Tokens like [EMPLOYER_1] and [INSTITUTION_2] are intentional placeholders; keep them as given. Use only information in the text. Reply with JSON only.` Output: `{"skills":[{"name":string,"level":"beginner|intermediate|expert","years":number|null}],"totalExperienceYears":number|null,"educationLevel":"high_school|diploma|bachelor|master|phd|other"|null,"jobTitles":[string],"workHistory":[{"title":string,"employerPlaceholder":string|null,"skillsUsed":[string]}]}`.

Screening, system: `You are screening a resume (personal details removed) against requirements. For each requirement key return a verdict: "met", "partial" or "not_met". For met or partial, "quote" must be an exact contiguous excerpt (10 to 300 characters) copied from the resume that supports it; otherwise null. Do not consider gender, age, nationality, religion, family status or the name of any school or employer. Reply with JSON only.` User message: the requirement list `[{"key","text","kind"}]` and the resume text. Output: `{"verdicts":[{"key":string,"verdict":"met|partial|not_met","quote":string|null}]}`.

Suggest questions, system: `Suggest 6 concise screening questions for this job. Reply with JSON only: {"questions":[string]}.`

### Rule-based provider (`RuleBasedFallbackProvider`)

- Skills: word-boundary, case-insensitive match of the skill name or any alias in the text. Verdict `met`, quote = the containing sentence (trim to at most 250 characters; it must be an exact substring of the text). Not found: `not_met`.
- Experience item: use `resume_suggestions.total_experience_years` or else the profile value. Thresholds `entry` 0, `mid` 2, `senior` 5, `lead` 8 years: `met` if at or above, `partial` if at least half and above 0, else `not_met`. No quote needed (`source = rule_based`); requirement text carries the figure, for example `Experience level: senior (profile: 4.5 years)`.
- Profile suggestions: dictionary skills, highest education keyword found, total experience as the sum of `(N yrs M mos)` durations if present; empty work history.
- Suggest questions: one question per required skill, up to 6: `Tell us about your experience with <skill>.`

## Database

### Part 2.1 migrations (`V2_1_1__resume_processing.sql`, `V2_1_2__seed_aliases_terms.sql`)

| Table | Columns |
|---|---|
| skill_aliases | skill_id → skills CASCADE; alias varchar(100) NOT NULL UNIQUE (case-insensitive through the collation) |
| strip_terms | kind text CHECK (`institution`,`employer`); term varchar(150) NOT NULL; UNIQUE (kind, term) |
| resume_suggestions | resume_id → resumes CASCADE UNIQUE; provider text CHECK (`ai`,`rule_based`); data json NOT NULL |
| resume_placeholder_map | resume_id → resumes CASCADE; placeholder text NOT NULL (`EMPLOYER_1`); kind text CHECK (`employer`,`institution`); original_value text NOT NULL; start_month date NULL; end_month date NULL; is_current boolean NOT NULL DEFAULT false; UNIQUE (resume_id, placeholder). Candidate-only: never returned to employers or the AI. |
| strip_reports | resume_id → resumes CASCADE; user_id → users CASCADE; note text NOT NULL; excerpt text NULL |
| work_history | user_id → users CASCADE; title text NOT NULL; employer text NULL; start_month date NULL; end_month date NULL; is_current boolean NOT NULL DEFAULT false; skills_used json NOT NULL DEFAULT (JSON_ARRAY()) |
| ai_calls | purpose text CHECK (`profile_extraction`,`screening`,`screening_questions`,`interview_pack`,`practice_feedback`,`story`); user_id char(36) NULL → users SET NULL; application_id char(36) NULL → applications SET NULL; model text; prompt_tokens int; completion_tokens int; cost decimal(10,6) NOT NULL DEFAULT 0; status text CHECK (`ok`,`error`,`blocked`); created_at; index (created_at) |

Seeds: about 60 skill aliases (JS→JavaScript, TS→TypeScript, Node→Node.js, NodeJS, Postgres→PostgreSQL, ReactJS and React.js→React, K8s→Kubernetes, Golang→Go, Py→Python, MS SQL→SQL Server, Springboot→Spring Boot, and so on), and about 100 institution and 100 employer terms (well-known universities and companies, including Bangladeshi ones).

### Part 2.2 migrations (`V2_2_1__screening.sql`)

| Table | Columns |
|---|---|
| candidate_aliases | company_id → companies CASCADE; candidate_id → users CASCADE; alias text NOT NULL; UNIQUE (company_id, candidate_id); UNIQUE (company_id, alias). Alias: `C-` + 5 random characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` using `SecureRandom`; retry on collision. |
| screening_runs | job_id → jobs CASCADE; started_by char(36) NULL → users SET NULL; mode text CHECK (`ai`,`rule_based`); total int; done_count int NOT NULL DEFAULT 0; failed_count int NOT NULL DEFAULT 0; estimated_cost decimal(10,4) NOT NULL DEFAULT 0; status text CHECK (`running`,`finished`); finished_at datetime(3) NULL |
| screening_items | run_id → screening_runs CASCADE; application_id → applications CASCADE; status text NOT NULL DEFAULT 'queued' CHECK (`queued`,`running`,`done`,`failed`); error text NULL; attempts int NOT NULL DEFAULT 0; skip_filters boolean NOT NULL DEFAULT false; UNIQUE (run_id, application_id) |
| application_claims | application_id → applications CASCADE; requirement_key text; requirement_text text; kind text CHECK (`required`,`nice_to_have`,`experience`); verdict text CHECK (`met`,`partial`,`not_met`); quote text NULL; weight int; source text CHECK (`ai`,`rule_based`) |

`ALTER TABLE applications` add: `score int NULL CHECK (0 to 100)`, `scoring_mode text NULL CHECK (ai, rule_based)`, `bias_status text NULL CHECK (clear, flagged)`, `bias_note text NULL`, `missing_skills json NOT NULL DEFAULT (JSON_ARRAY())`, `not_matched_rule text NULL`, `pulled_back_at datetime(3) NULL`, `pulled_back_by char(36) NULL → users SET NULL`, `shortlisted_at datetime(3) NULL`, `decided_by char(36) NULL → users SET NULL`, `decision_reason text NULL`, `override_direction text NOT NULL DEFAULT 'none' CHECK (none, shortlisted_below_cutoff, rejected_above_cutoff)`, `gate_status text NOT NULL DEFAULT 'not_required' CHECK (not_required, pending, passed, failed)`, and a table CHECK: `override_direction = 'none' OR (decision_reason IS NOT NULL AND CHAR_LENGTH(TRIM(decision_reason)) >= 10)`.

### Part 2.3 migrations (`V2_3_1__screening_questions.sql`)

| Table | Columns |
|---|---|
| job_screening_questions | job_id → jobs CASCADE; position int NOT NULL; text text NOT NULL; UNIQUE (job_id, position) |

## API

### Part 2.1

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/candidate/resume` | Cand. | (changed) sets `processing` and starts async processing; on startup requeue resumes still `processing` |
| GET `/api/candidate/resume` | Cand. | adds `strippedText` (when ready) and `droppedPlaceholders` summary; poll every 2 s |
| GET `/api/candidate/resume/suggestions` | Cand. | `{provider, skills[{name, skillId?, level, years}], totalExperienceYears, educationLevel, jobTitles[], workHistory[{title, employer, startMonth, endMonth, isCurrent, skillsUsed[]}]}`; employers restored from `resume_placeholder_map` |
| POST `/api/candidate/resume/suggestions/apply` | Cand. | body is the reviewed set; upserts skills (map names through aliases; create a new skill row for an unknown name: trimmed, at most 40 characters, letters, digits, `+ # . -` only), updates profile experience fields, **replaces** work history with the submitted list |
| POST `/api/candidate/resume/strip-reports` | Cand. | `{note, excerpt?}` |
| GET, POST, PUT, DELETE `/api/candidate/work-history` | Cand. | CRUD |
| PUT `/api/candidate/profile` | Cand. | (changed) also `totalExperienceYears`, `educationLevel`, `previousJobTitles` |
| GET `/api/skills/suggest?q=` | Signed in | (changed) also matches aliases and returns the canonical skill |

### Part 2.2

| Method and path | Who | Notes |
|---|---|---|
| POST `/api/employer/jobs/{id}/screening/estimate` | HR | `{count, mode, estimatedCostUsd}`; count = applications with status `applied` and a `ready` resume |
| POST `/api/employer/jobs/{id}/screening` | HR | 202 `{runId}`; 409 `SCREENING_RUNNING`, `NOTHING_TO_SCREEN`; job must not be a draft; sets applications to `screening` and creates items |
| GET `/api/employer/jobs/{id}/screening` | HR, Mgr | latest run `{runId, status, mode, total, done, failed, startedAt}` |
| POST `/api/employer/jobs/{id}/screening/retry` | HR | requeue failed items of the latest run |

Worker: `@Async("screeningExecutor")` with 4 threads; one running run per job; each item processed in its own transaction; on startup reset `running` items to `queued`. Per item: hard filters → (`not_matched` and done) or → AI or fallback → validate quotes → score → bias scan → save claims and application fields → `ApplicationStatusService.changeStatus(..., screened)` → item done → update run counters → finish the run when nothing is queued or running. An exception marks the item `failed` (message without personal data); the application stays `screening`.

### Part 2.3

| Method and path | Who | Notes |
|---|---|---|
| GET `/api/employer/jobs/{id}/applications?status&gate&bias&includeNotMatched&page` | HR, Mgr | (changed) item `{id, alias, label, score, status, biasStatus, gateStatus, scoringMode, notMatchedRule, appliedAt}`; default order score descending (nulls last) then `appliedAt`; `not_matched` only when `includeNotMatched=true`; `label` equals `alias` |
| GET `/api/employer/applications/{id}` | HR, Mgr | `{id, jobId, jobTitle, alias, status, score, scoringMode, biasStatus, biasNote, gateStatus, cutoffScore, missingSkills[], claims[{requirement, kind, verdict, quote, source}], strippedText, notMatchedRule, history[], identity?}`; `identity {firstName, lastName, email, phone}` only when `shortlisted_at` is set |
| POST `/api/employer/applications/{id}/shortlist` | HR, Mgr | `{reason?}`; from `screened` only; 409 `GATE_NOT_PASSED` when gate is `pending` or `failed`; 422 `REASON_REQUIRED` when score is below a set cut-off and no reason (min 10 characters); sets `override_direction`, `decision_reason`, `decided_by`, `shortlisted_at`; audits `identity_reveal` and, for an override, `ai_decision`; returns the review object with identity |
| POST `/api/employer/applications/{id}/reject` | HR, Mgr | `{reason?}`; from `screened` or `not_matched`; 422 when score is at or above a set cut-off and no reason; audits `ai_decision` for an override |
| POST `/api/employer/applications/{id}/pull-back` | HR, Mgr | only `not_matched`; sets `pulled_back_at/by`; creates a one-item run with `skip_filters = true`; status back to `screening`; audit `pull_back` |
| GET `/api/candidate/applications/{id}` | Cand. | (changed) adds `score`, `scoringMode`, `claims[{requirement, kind, verdict, quote}]`, `missingSkills`, `notMatchedRule`, `gateStatus`. **Never** bias fields or stripped text. |
| GET `/api/employer/dashboard` | HR, Mgr | `{openJobs, newApplications (created in last 7 days), awaitingScreening (status applied), biasFlags (flagged and still screened), interviewsThisWeek: null, quota: null}` |
| POST `/api/employer/jobs/{id}/suggest-questions` | HR | `{questions[]}` through `AiGateway` (rule-based fallback) |
| PUT `/api/employer/jobs/{id}/screening-questions` | HR | `{questions[]}` replaces the list; drafts only; `GET /api/employer/jobs/{id}` now includes `screeningQuestions` |

Alias use: from now on employer DTOs use `AliasService.aliasFor(companyId, candidateId)` (get or create).

## Pages

| Page | Part | Shows and does |
|---|---|---|
| P14 Resume (full) | 2.1 | States: none, processing (steps: Uploaded, Reading text, Removing personal details, Extracting profile; polling), failed (reason, upload again), ready with two tabs. **Stripped preview:** text with placeholders drawn as chips, "Report a wrong strip" button. **Extracted details:** suggested skills, years, total experience, education level, job titles, work history, each with checkboxes and edit fields; "Save to profile" (warns that work history is replaced). If provider is `rule_based` show a banner that AI was unavailable. |
| P13 Profile | 2.1 | Adds sections *Experience and education* (total years, education level, previous titles) and *Work history* (title, employer, start and end month, skills used; add, edit, delete) |
| P24 Applicants tab | 2.3 | Advisory banner; filters (status, gate, bias); switch "Show not matched"; ranked table (rank, alias chip, ScoreMeter with cut-off marker, status badge, bias badge, gate badge, "Rule-based" badge); not matched rows show the rule and a Pull back button; **Screen All** (HR only) opens a dialog with count, estimated cost, mode badge and Confirm; then a progress bar (done of total, failed) polled every 3 s with Retry for failures |
| P25 Application review | 2.3 | Left: stripped resume with the quotes highlighted (click a claim to scroll). Right panel: alias, status, ScoreMeter, bias badge and note, gate badge, claims (requirement, verdict badge, quote), missing skills chips, history. Actions: Shortlist (reveal-confirm dialog; reason box when below cut-off; disabled with tooltip when gate fails), Reject (reason box when above cut-off), Pull back. After shortlist: identity card (name, email, phone). Mobile: panel above the resume, sticky action bar. |
| P16 Application detail | 2.3 | Adds score, claims with quotes, missing skills, a "Rule-based result" tag when relevant, and for `not_matched` the rule instead of a score |
| P20 Employer dashboard | 2.3 | Stat blocks with links (hide blocks whose value is null); `/employer` now shows this page |
| P23 Job editor | 2.3 | "Suggest screening questions" button, editable list, saved with the draft |

## Test data (create in Part 2.1)

- `backend/src/test/resources/resumes/`: 30 fake English resumes as `.txt` (fast strip tests) plus 5 real PDFs (extraction tests): 2 image-only PDFs (expected `SCANNED_OR_EMPTY`), 1 Bangla-language PDF (expected `NOT_ENGLISH`), 2 normal PDFs. Include Bangladeshi CV fields (Father's Name, Religion, NID, Present Address), varied layouts, many universities and companies, date formats, and links.
- `expected.json`: per file, `mustNotAppear` (names, emails, phones, institutions, employers, birth dates) and `mustAppear` (skills, titles).
- `StripperGoldenTest` asserts at least 95% of all `mustNotAppear` items are gone overall, and that all `mustAppear` items remain.

---

## Part 2.1 — Resume processing

**Tasks**
1. Migrations `V2_1_1`, `V2_1_2`; entities and repositories.
2. `ResumeTextExtractor` (PDFBox) with the length and Latin-letter checks and failure reasons.
3. `ResumeStripper` and `IdentifierScan` exactly as BR-05, with unit tests per step. `StripTermsRepository` seeded.
4. `AiProvider`, `AiGateway`, `DeepSeekProvider`, `RuleBasedFallbackProvider` (profile extraction part now), `ai_calls` recording, config `app.ai.*`. Start with `AI_PROVIDER=fallback`.
5. Resume processing pipeline (async executor, status transitions `processing → ready | failed`, restart recovery). Suggestions saved to `resume_suggestions`. Placeholder mapping saved.
6. Skill aliases: `SkillCanonicalizer`; update the suggest endpoint.
7. Endpoints in the Part 2.1 table, including apply-suggestions and work history CRUD.
8. Frontend: P14 full (all states, two tabs, report dialog, save to profile), P13 new sections.
9. Create the golden test data and `StripperGoldenTest`.

**Tests:** `Fr011_*` (each step, mapping consistency, audit), `Fr012_*` (3 failure cases and success), `Fr013_*` (ai and rule-based paths with a fake provider), `Fr015_*`, `Fr057_*`, final-scan block test, golden test.

**Definition of done:** upload a PDF as a candidate; within seconds see the stripped preview and suggestions; save them to the profile; the golden test passes at 95% or better; with the fallback provider everything works without a key.

---

## Part 2.2 — Screening engine

**Tasks**
1. Migration `V2_2_1`. Entities for runs, items, claims, aliases.
2. `AliasService` and update employer DTOs to use aliases.
3. `HardFilterService` (BR-03), `ScoringService` (BR-20), `BiasScanner` (BR-21) with `bias_terms.txt`.
4. Finish `RuleBasedFallbackProvider.screen` and `DeepSeekProvider.screen`.
5. `ScreeningService` (estimate, start, progress, retry) and `ScreeningWorker` as described, using `ApplicationStatusService` for every status change.
6. Employer endpoints for estimate, start, progress, retry.

**Tests:** `Fr054_*` (same company same alias across jobs, different company different alias, format), `Fr028_*` (estimate zero in fallback, one run at a time, retry only failed), unit tests for `ScoringService` (all met = 100, all not met = 0, a mixed table of weights, partial credit), quote validation (invented quote downgrades), `BiasScanner` (flagged claim removed, score recomputed), hard filters (unknown data passes, known contradiction fails), restart recovery of `running` items, and an end-to-end test with the fake provider.

**Definition of done:** with 10 applications and the fallback provider, a run completes; each application has claims, a score and `screened` or `not_matched`; with a fake AI provider returning an invented quote, that claim is downgraded; no identity appears in employer responses (extend `NoIdentityLeakTest`).

---

## Part 2.3 — Employer and candidate views

**Tasks**
1. Migration `V2_3_1`. Extend applicant list and review endpoints; shortlist, reject, pull-back with all rules; audit entries; candidate application detail changes; dashboard endpoint; suggest-questions and save-questions endpoints.
2. Frontend: P24 Applicants tab (ranking, filters, Screen All dialog and progress), P25 review page with highlighted quotes and all dialogs, P16 additions, P20, P23 additions. Add nav entry Dashboard for employers.
3. Seed a **demo dataset** script (`scripts/seed-demo.sh` or a `local`-profile runner): one company, one job with 6 skills, 10 candidates with varied resumes, so the demo is quick.

**Tests:** `Fr029_*` (ordering, toggle, filters), `Fr030_*` (identity only after shortlist; audit entry written), `Fr052_*` (reason required below cut-off and above cut-off, DB CHECK rejects a short reason, override never blocked, `ai_decision` audit), pull-back flow, gate 409 (set `gate_status` manually in the test), `Fr024_*` (candidate sees claims but not bias), `Fr046_*`, a final `NoIdentityLeakTest` sweep across all employer endpoints before shortlist.

**Definition of done (whole chunk):** demo path: ten candidates applied → HR clicks Screen All, sees the cost, confirms → ranked list with aliases → opens one, sees highlighted quotes → shortlists a low scorer and must write a reason → name and contact appear → the candidate sees their score and quotes. Tests green, design check passes, `docs/openapi.json` re-exported, `docs/progress.md` updated.
