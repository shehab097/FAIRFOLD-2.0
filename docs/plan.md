# FairFold — 10-Day Build Plan (local first, Docker last)

Based on PRD v2.1 and the Architecture file. Build and run everything directly on your machine. Docker, Nginx and deployment come in the last chunk.

## 0. Ground rules

- **Thin first.** In every chunk, build the backend rules properly and keep the screens plain. Polish later, if there is time.
- **Demo gate.** Do not start the next chunk until the demo of the current one works end to end.
- **Honest budget.** 10 days has no buffer. Treat days 11 and 12 as spare. Chunks 5 and 6 are the ones most likely to spill over.
- **AI helps, rules stay yours.** Scoring, stripping and status transitions are small, well-tested services that you read line by line.
- **Design rules.** Every screen follows `design.md`: Dark & Light mode support via CSS tokens, smooth rounded corners, no gradients, the five-colour palette. Run `scripts/check-design.sh` before each commit.

| Chunk | Theme | Days | Running total |
|---|---|---|---|
| 1 | Core platform | 2.5 | 2.5 |
| 2 | Anonymised AI screening | 2 | 4.5 |
| 3 | Skills proof | 1.5 | 6 |
| 4 | Interviews and candidate growth | 2 | 8 |
| 5 | Operations | 1.5 | 9.5 |
| 6 | Package and deploy | 0.5 | 10 |

Chunk 1 is half a day shorter than before because there is no Docker setup to debug. Chunk 6 gets that half day back.

Email verification, GitHub Actions and polish are **outside the 10 days** (section 9). Days 11 and 12 are for those and for anything that slipped.

## 1. Local setup (before day 1 starts, about 1 hour)

### 1.1 Install on Arch

```bash
sudo pacman -S --needed jdk21-openjdk maven nodejs npm mariadb git
sudo archlinux-java set java-21-openjdk
java -version        # must say 21
node -v              # 20 or newer
```

### 1.2 MariaDB

```bash
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
sudo systemctl enable --now mariadb
sudo mariadb-secure-installation
sudo mariadb
```

Then, in the MariaDB prompt:

```sql
CREATE DATABASE fairfold      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE DATABASE fairfold_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE USER 'fairfold'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON fairfold.*      TO 'fairfold'@'%';
GRANT ALL PRIVILEGES ON fairfold_test.* TO 'fairfold'@'%';
FLUSH PRIVILEGES;
```

The project was verified on **MariaDB 13.0.2**. Use a recent MariaDB release that supports the `utf8mb4_0900_as_ci` collation (the current stable is fine); check `mariadb --version` and use the same major version in production later.

### 1.3 Project layout

```text
fairfold/
  backend/        Spring Boot (Maven)
  frontend/       React + Vite + TypeScript
  docs/           PRD, architecture, design, this plan, roadmap, openapi.json (generated)
  data/           resume files in development (git-ignored)
  .env.example    every setting the app reads
```

Generate the backend at start.spring.io (Maven, Java 21; dependencies Web, Security, Data JPA, Validation, Flyway, MariaDB Driver, Mail). Add `org.flywaydb:flyway-mysql` (it provides MariaDB support). Add springdoc-openapi on day 1 (section 1.7). Add PDFBox, a JWT library, a TOTP library and Bucket4j by hand when their chunk needs them.

### 1.4 Run it

| What | Command | URL |
|---|---|---|
| Backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | http://localhost:8080 |
| Frontend | `cd frontend && npm run dev` | http://localhost:5173 |

Vite replaces Nginx during development by proxying `/api` to the backend. The browser sees one origin, so the HttpOnly refresh cookie works with no CORS setup, exactly as in production.

```ts
// frontend/vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
});
```

```yaml
# backend/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/fairfold
    driver-class-name: org.mariadb.jdbc.Driver
    username: fairfold
    password: ${DB_PASSWORD}
app:
  storage-dir: ${STORAGE_DIR:./data/resumes}
  cookie-secure: false          # true in production
  ai:
    provider: ${AI_PROVIDER:fallback}   # fallback until the key is added
    api-key: ${DEEPSEEK_API_KEY:}
    model: ${DEEPSEEK_MODEL:deepseek-v4.1-flash}
  mail:
    mode: console               # prints links to the terminal
  auth:
    require-email-verification: false   # turn on after day 10
```

### 1.5 Development choices that save time

- **Email:** the `local` profile prints reset and invite links in the terminal. No mail server is needed. Email verification is **deferred**: add the `email_verified` column and the `app.auth.require-email-verification` setting (default `false`) now, and build the verification flow after day 10 (section 9). Add Mailpit or Mailtrap only if you want to see real emails.
- **AI:** start with `AI_PROVIDER=fallback`. Everything works without a key; switch to DeepSeek in chunk 2 when the screening flow is ready.
- **Backend tests:** use the local `fairfold_test` database instead of Testcontainers, because Testcontainers needs Docker. Switch to Testcontainers in chunk 6 when Docker is installed.
- **Stripe:** hosted Checkout returns through a browser redirect, so `localhost` works in test mode. Start with a fake `PaymentProvider`, then use Stripe test keys.

### 1.6 Docker-ready habits (so chunk 6 takes half a day)

1. Every setting comes from an environment variable with a safe local default. Never hard-code a path, URL or key.
2. The frontend calls only relative URLs (`/api/...`), never `http://localhost:8080`.
3. Files go through `StorageService`, and the folder comes from `app.storage-dir`.
4. Add a health endpoint now (`/api/health`) so the container health check is ready later.
5. Log to the console, not to files.
6. Keep a `.env.example` up to date as you add settings.
7. Commit Flyway migrations from the very first table. Never edit an old migration; add a new one.

### 1.7 API documentation (springdoc-openapi)

springdoc reads your controllers and DTOs and produces the OpenAPI document plus Swagger UI, so the API docs are never out of date. Version rule: **the 2.8.x line is for Spring Boot 3.x; springdoc 3.x is for Spring Boot 4.** If you generate the project with Spring Boot 4 instead of 3.x, use springdoc 3.x.

```xml
<!-- backend/pom.xml -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.8.14</version> <!-- use the newest 2.8.x on Maven Central -->
</dependency>
```

```java
// config/OpenApiConfig.java  (shows the Authorize button for the JWT access token)
@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI fairFoldOpenApi() {
    return new OpenAPI()
      .info(new Info().title("FairFold API").version("v1"))
      .components(new Components().addSecuritySchemes("bearerAuth",
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
      .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
```

```yaml
# application-local.yml
springdoc:
  swagger-ui:
    persist-authorization: true

# application-prod.yml  (documentation off in production)
springdoc:
  api-docs: { enabled: false }
  swagger-ui: { enabled: false }
```

- **Where to look in development:** `http://localhost:8080/swagger-ui.html` (the UI) and `http://localhost:8080/v3/api-docs` (the JSON). Open them on port 8080 directly, not through the Vite proxy.
- **Security:** allow `/v3/api-docs/**`, `/swagger-ui/**` and `/swagger-ui.html` in the Spring Security config **only in the `local` profile**.
- **Annotate as you go:** `@Tag` on each controller, `@Operation(summary = "...")` on each endpoint, and `@Schema` on DTO fields that need a description. Ask the AI to add these with every controller.
- **Export the contract:** after each slice run `curl http://localhost:8080/v3/api-docs -o docs/openapi.json` and commit it. Give that file to the AI when it builds the frontend calls.
- **Optional:** generate TypeScript types from the same document with `openapi-typescript` (`npx openapi-typescript http://localhost:8080/v3/api-docs -o frontend/src/api/schema.d.ts`), so frontend calls cannot drift from the backend.

## 2. How to work with AI inside each chunk

1. At the start of the chunk, give the AI the requirement rows from the PRD and the page specs from the architecture file, and `design.md` for how it must look. Ask for the controllers and request and response DTOs **first** (with springdoc annotations), then export `docs/openapi.json` and use it as the contract for the frontend.
2. Build **one slice at a time**, in this order: migration → entity → service → controller → test → page. One requirement is one slice.
3. Paste the acceptance criteria and ask for the test **together** with the code.
4. Commit after each slice with the requirement ID in the message, for example `REQ-FR-003 account lockout`.
5. At the end of the day, run all tests and write three lines in `docs/progress.md`: done, broken, next.

## 3. Chunk 1: Core platform (days 1 to 2.5)

**Goal:** an employer posts a job, a candidate applies, and both see the status.

**Requirements:** 001, 002, 003, 004, 007, 008, 009, 010, 014, 022, 023, 025, 026, 027, 042, 045.

**Pages:** P01 to P04, P06, P07 (P05 is deferred; P08 needs team invites, so it comes in chunk 4), P09 (basic list), P10 (password change only), P11, P12 to P16 (basic versions), P21 to P24 (basic versions).

- [ ] Backend skeleton: Spring Boot, Flyway, `local` profile, health endpoint
- [ ] springdoc-openapi: Swagger UI and `/v3/api-docs` on port 8080 in the `local` profile, JWT bearer scheme, switched off in `prod` (section 1.7)
- [ ] Frontend skeleton: Vite, Tailwind, router, `PublicShell`, `PortalShell`, role guards
- [ ] (Optional) `npm run gen:api` to generate TypeScript types from the OpenAPI document
- [ ] Design system from `design.md`: CSS variables, Tailwind config (zero radius, no gradients), fonts, the base components Button, Input, Select, Table, Badge, Modal, Toast, and `scripts/check-design.sh`
- [ ] Auth: register (active at once), login, lockout, reset, refresh with rotation, logout. Add the `email_verified` column and the switch now, but no verification flow yet
- [ ] Session on the frontend: in-memory token, silent refresh, role redirects
- [ ] Company profile and the "company required to activate" rule
- [ ] Jobs: create, edit (drafts only), lifecycle (BR-01), duplicate as draft
- [ ] Job board: full-text search, filters, pagination, public job page
- [ ] Candidate profile and skills (add, edit, delete)
- [ ] Resume upload: PDF signature check, 10 MB limit, UUID file name, `StorageService`
- [ ] Apply (needs an uploaded resume), status history, basic notifications

**Demo:** register as employer and log in at once, create and activate a job. Register as candidate, upload a PDF, apply. Both sides see the notification and the status.

## 4. Chunk 2: Anonymised AI screening (days 2.5 to 4.5)

**Goal:** screen 10 sample resumes and see ranked, explained, anonymised results.

**Requirements:** 011, 012, 013, 015, 024, 028, 029, 030, 046, 052, 054, 057, and the AI button in 025.

**Pages:** full P14, P16 (score and evidence), P20, P24 (applicants tab and Screen All), P25.

- [ ] PDFBox text extraction with the 200-character and 85%-Latin checks
- [ ] Stripping pipeline (BR-05) with the final identifier scan before any AI call
- [ ] A test set of about 30 resumes with expected strips (this is goal G1)
- [ ] `AiProvider` interface and `RuleBasedFallbackProvider` first, then DeepSeek
- [ ] Profile extraction (skills, years, titles, work history) and the review screen on P14
- [ ] Private placeholder mapping (employer, institution) and rule-based date parsing, used to restore work history names and months (BR-05)
- [ ] Work history section on P13 (edit and confirm what was extracted)
- [ ] Skill de-duplication from a seeded alias list
- [ ] Hard filters (BR-03) and `not_matched` with the rule shown and pull-back
- [ ] Candidate alias per employer organisation
- [ ] Screening run: estimate, confirm, queue, progress polling, retry
- [ ] Scoring (BR-20) with word-for-word quote validation, then bias status (BR-21)
- [ ] Ranked list, evidence view with highlighted quotes, shortlist and identity reveal, audit entries
- [ ] Override reason (FR-052) with the database CHECK
- [ ] "Suggest screening questions" button
- [ ] Employer dashboard (thin: cards for features that do not exist yet stay hidden until their chunk)

**Demo:** screen the sample resumes. The list shows aliases only. Open one, see the quotes highlighted. Shortlist one and see the name appear. Shortlist a low scorer and be forced to give a reason.

## 5. Chunk 3: Skills proof (days 4.5 to 6)

**Goal:** a candidate fails an assessment, studies, retakes and passes. The employer's shortlist button is blocked until then.

**Requirements:** 019, 020, 044, 049, 051, 055, 061, 063.

**Pages:** P17, P18, P34, P35, plus the focus signals in P25.

- [ ] Admin: assessments with multiple-choice questions, reorder, deactivate
- [ ] Admin: resources per skill, weekly link check job
- [ ] Attempt: server end time, countdown, 2-second autosave, auto-submit, scheduled sweeper
- [ ] Scoring, pass mark, Verified badge
- [ ] Required assessment per job and the gate (button disabled, API returns 409)
- [ ] Retake loop: fail, resources, 48-hour cooldown, retake
- [ ] Proctoring: consent, focus events, explanation, neutral display for the employer
- [ ] Certifications

**Demo:** attach an assessment to a job, apply, try to shortlist (blocked), take the assessment, fail, see resources, retake after the cooldown (shorten it with a setting while testing), pass, shortlist.

## 6. Chunk 4: Interviews and candidate growth (days 6 to 8)

**Goal:** one full hiring story from shortlist to message, interview and feedback, plus the candidate growth features.

**Requirements:** 016, 017, 018, 031, 032, 033, 043, 047, 058, 059, 060, 062.

**Pages:** P08, P19, P26, P27, P29, P36, plus the story and messages tabs in P16 and P25.

- [ ] Team: invites, accept-invite page, roles, the last-HR rule (interviewers are picked from the team)
- [ ] Interview pack: AI generation, edit, save
- [ ] Scheduling with interviewers, in-app notification and plain email
- [ ] Feedback form and recommendation, then hired or rejected
- [ ] Messaging: opens after shortlist, polling every 10 seconds, rate limit, never sent to AI
- [ ] Practice questions, STAR feedback with a versioned rubric, coaching isolation (no keys to jobs or applications), star rating
- [ ] Journey timeline and skill evolution charts with table views
- [ ] Job-specific story with local placeholder restore

**Demo:** invite an interviewer, then shortlist a candidate, message them, schedule with that interviewer, submit feedback as the interviewer, mark hired. If chunk 4 runs long, do messaging last. As the candidate: practise an answer and get STAR feedback, open the journey charts, generate a story.

## 7. Chunk 5: Operations (days 8 to 9.5)

**Goal:** an admin can audit everything, an employer can manage a team and pay.

**Requirements:** 005, 006, 035, 037, 038, 039, 040, 041, 048, 056.

**Pages:** P28, P30 to P33, plus the MFA parts of P03 and P10.

- [ ] Admin dashboard, user management, audit log with CSV export
- [ ] Data export (instant JSON) and self-service deletion with the last-admin and last-HR guards
- [ ] Analytics: funnel, time-to-hire, overrides table, pull-back count, table toggles
- [ ] MFA: setup with QR, login code step, turn off
- [ ] Billing with the fake `PaymentProvider`, then Stripe Checkout test mode

**Demo:** run analytics, turn on MFA and log in with a code, buy a plan through Stripe test mode. If time runs out, billing goes last.

## 8. Chunk 6: Package and deploy (days 9.5 to 10)

**Goal:** the same app, running from `docker compose up`.

- [ ] Backend `Dockerfile` (multi-stage: build with Maven, run on a JRE 21 image)
- [ ] Frontend build, served by Nginx with a fallback to `index.html` and `/api` proxied to the backend
- [ ] `docker-compose.yml` with `nginx`, `backend` and `db` (MariaDB; volumes for the database and the resume files)
- [ ] A `prod` profile: `cookie-secure: true`, real SMTP, real keys from `.env`, springdoc switched off
- [ ] Switch backend tests to Testcontainers
- [ ] Smoke test: register, apply, screen, pay in the sandbox
- [ ] Backup note: how to dump MariaDB (`mariadb-dump --single-transaction`) and where the resume volume lives

```yaml
# docker-compose.yml (outline)
services:
  db:
    image: mariadb:13.0
    environment:
      MARIADB_DATABASE: fairfold
      MARIADB_USER: fairfold
      MARIADB_PASSWORD: ${DB_PASSWORD}
      MARIADB_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_ci
    volumes: [dbdata:/var/lib/mysql]
  backend:
    build: ./backend
    env_file: .env
    depends_on: [db]
    volumes: [resumes:/data/resumes]
  nginx:
    build: ./frontend        # builds the React app and copies it into an nginx image
    ports: ["80:80"]
    depends_on: [backend]
volumes: { dbdata: {}, resumes: {} }
```

## 9. After day 10 (outside the 10-day target)

- [ ] **Email verification** (about 2 hours): token table, verify endpoint, resend link, the P05 page, the "verify your email" login message; then turn on `app.auth.require-email-verification` with real SMTP
- [ ] **GitHub Actions:** build, test and `scripts/check-design.sh` on every push
- [ ] **Polish:** loading, empty and error states, a mobile pass at 360 px, an accessibility pass
- [ ] Anything that slipped from chunks 1 to 6

## 10. Risks and what to do

| Risk | Plan |
|---|---|
| Behind schedule at the end of a chunk | Finish the chunk's backend and rules, leave plain screens, and move on. Use days 11 and 12 for polish. |
| Stripping quality is below the 95% target | Add the missed patterns to the test set and fix the rules. Do not move on from chunk 2 until the test set passes. |
| DeepSeek key or quota problems | Stay on the fallback provider. The product still works and every result is labelled rule-based. |
| Stripe test keys or webhook access are unavailable | Keep the fake provider; the interface is the same. |
| AI-written code that looks right but is wrong | Review the scoring, stripping and status services line by line, and keep their tests green. |
| Frontend and backend disagree on field names | Export `docs/openapi.json` after each slice and build the frontend calls from it (or from the generated types). |
| AI writes rounded corners, gradients or shadows | Run `scripts/check-design.sh` before every commit. It fails on `rounded`, `gradient` and `shadow` classes and on colours outside the tokens. |
| Works locally, breaks in Docker | Follow the habits in section 1.6, and test the container build on day 9, not day 10. |

## 11. Daily routine

1. Morning: pick the next five slices from the chunk checklist.
2. Build slice by slice with tests; commit each one.
3. Late afternoon: run the whole test suite and click through the demo path.
4. Evening: write the three lines in `docs/progress.md`.
