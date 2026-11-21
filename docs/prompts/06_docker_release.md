# Prompt 06 — Package and Deploy with Docker (days 9.5 to 10)

Read first: `docs/prompts/00_context.md`, this file, `docs/progress.md`. Do not read the PRD or any other document.

**Start this chunk only when chunks 1 to 5 are finished and all backend and frontend tests pass locally.** Docker was deliberately left out until now: do not change application behaviour in this chunk, only packaging, production configuration and tests infrastructure.

**Goal:** the same app starts with one command, `docker compose up --build`, and the smoke test passes.

**Parts:**
- **Part 6.1** Backend image and production configuration
- **Part 6.2** Frontend image, Nginx and Docker Compose
- **Part 6.3** Testcontainers, smoke test, backups and README

Start message: `Build Part 6.1 from docs/prompts/06_docker_release.md. Follow 00_context.md. Stop when the Definition of Done is met and update docs/progress.md.`

## Target layout

```text
fairfold/
  docker-compose.yml
  .env.example                 (production variables, no real secrets)
  backend/Dockerfile           backend/.dockerignore
  frontend/Dockerfile          frontend/.dockerignore    frontend/nginx.conf
  scripts/smoke.sh   scripts/sample.pdf   scripts/backup.sh   scripts/restore.sh
  README.md
```

Containers: `postgres` (PostgreSQL 17), `backend` (Spring Boot, port 8080, internal only), `web` (Nginx serving the React build and proxying `/api` to the backend, the only published port). Volumes: `pgdata`, `resumes`.

## Production configuration (`application-prod.yml`)

| Setting | Value |
|---|---|
| Datasource | `jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:fairfold}`, user `${DB_USER:fairfold}`, password `${DB_PASSWORD}` |
| `app.storage-dir` | `${STORAGE_DIR:/data/resumes}` |
| `app.cookie-secure` | `${COOKIE_SECURE:true}` (**set `false` only when testing over plain `http://localhost`**; browsers drop Secure cookies on plain HTTP in some browsers, which breaks login) |
| `app.mail.mode` | `smtp` with `spring.mail.host/port/username/password` from `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, and the sender `MAIL_FROM` |
| `app.base-url` / `APP_BASE_URL` | public URL, used in links and bKash redirects |
| `app.ai.provider` | `${AI_PROVIDER:fallback}` |
| Springdoc | `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false` |
| JPA | `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false` |
| Server | `server.forward-headers-strategy=framework`, `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=20s` |
| Uploads | `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=11MB` |
| Logging | console only; never log passwords, tokens, resume text or message text |
| Required env (fail fast if missing) | `JWT_SECRET`, `DB_PASSWORD`, `MFA_ENCRYPTION_KEY`, `APP_BASE_URL`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` |

Flyway runs automatically at startup. The admin seed runs from `ADMIN_EMAIL` and `ADMIN_PASSWORD`. `DemoContentSeeder` and the fake payment endpoints must be **inactive in `prod`** (they are `local`-only); `PAYMENT_PROVIDER` defaults to `fake` and in `prod` the fake pay endpoint must return 404.

## `.env.example` (production variables)

```bash
# Database
DB_NAME=fairfold
DB_USER=fairfold
DB_PASSWORD=change-me
# Security (generate: openssl rand -base64 48 / openssl rand -base64 32)
JWT_SECRET=change-me-at-least-32-bytes
MFA_ENCRYPTION_KEY=change-me-base64-32-bytes
COOKIE_SECURE=true
# App
APP_BASE_URL=https://your-domain.example
HTTP_PORT=80
ADMIN_EMAIL=admin@your-domain.example
ADMIN_PASSWORD=change-me
# Mail
MAIL_HOST=
MAIL_PORT=587
MAIL_USER=
MAIL_PASSWORD=
MAIL_FROM=no-reply@your-domain.example
# AI
AI_PROVIDER=fallback
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=
AI_PRICE_INPUT_PER_M=
AI_PRICE_OUTPUT_PER_M=
# Payments
PAYMENT_PROVIDER=fake
BKASH_BASE_URL=
BKASH_APP_KEY=
BKASH_APP_SECRET=
BKASH_USERNAME=
BKASH_PASSWORD=
```

Never commit a real `.env`. Confirm `.gitignore` excludes it.

## Files to create

### `backend/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 app && mkdir -p /data/resumes && chown app /data/resumes
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

`.dockerignore`: `target`, `.git`, `*.md`, `data`.

### `frontend/Dockerfile`

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

`.dockerignore`: `node_modules`, `dist`, `.git`.

### `frontend/nginx.conf`

```nginx
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;

  client_max_body_size 11m;
  gzip on;
  gzip_types text/css application/javascript application/json image/svg+xml;

  add_header X-Content-Type-Options "nosniff" always;
  add_header X-Frame-Options "DENY" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'" always;

  location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 90s;
  }

  location /assets/ {
    expires 1y;
    add_header Cache-Control "public, immutable";
    try_files $uri =404;
  }

  location / {
    add_header Cache-Control "no-cache";
    try_files $uri /index.html;
  }
}
```

Note: the `add_header` lines are not inherited into a `location` that defines its own `add_header`; repeat the security headers there if needed (or use an `include` snippet). Swagger paths are not proxied.

### `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: ${DB_NAME:-fairfold}
      POSTGRES_USER: ${DB_USER:-fairfold}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-fairfold} -d ${DB_NAME:-fairfold}"]
      interval: 5s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  backend:
    build: ./backend
    env_file: .env
    environment:
      DB_HOST: postgres
      STORAGE_DIR: /data/resumes
    depends_on:
      postgres: { condition: service_healthy }
    volumes: [resumes:/data/resumes]
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:8080/api/health"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 40s
    restart: unless-stopped

  web:
    build: ./frontend
    ports: ["${HTTP_PORT:-80}:80"]
    depends_on:
      backend: { condition: service_healthy }
    restart: unless-stopped

volumes:
  pgdata:
  resumes:
```

Do not publish the Postgres or backend ports.

---

## Part 6.1 — Backend image and production configuration

**Tasks**
1. Create `application-prod.yml` per the table; add fail-fast validation of required variables (a `@ConfigurationProperties` validation or a startup check with clear messages).
2. Make sure the `local`-only beans (`DemoContentSeeder`, fake pay endpoint, Swagger paths in the security config, console mail) are guarded with `@Profile("local")` or the equivalent property so they are absent in `prod`.
3. Write `backend/Dockerfile` and `.dockerignore`.
4. Build and run the image against a locally running Postgres (for example `docker run` with `--network host` or a temporary compose with only `postgres` and `backend`).
5. Confirm: `/api/health` returns ok, Flyway migrations apply on an empty database, the admin seed creates the admin, `/swagger-ui.html` and `/v3/api-docs` return 404, the container runs as a non-root user.

**Definition of done:** `docker build` succeeds; the container starts against an empty Postgres with only environment variables; health is ok; Swagger is off; startup with a missing `JWT_SECRET` fails with a clear message.

---

## Part 6.2 — Frontend image, Nginx and Compose

**Tasks**
1. Write `frontend/Dockerfile`, `.dockerignore`, `nginx.conf`.
2. Write `docker-compose.yml` and the production `.env.example` as above; create a real `.env` from it locally with test values and `COOKIE_SECURE=false` for plain HTTP testing.
3. `docker compose up --build`; open `http://localhost`.
4. Check in the browser: the app loads; a deep link such as `/employer/jobs` reloads correctly (SPA fallback); login works and the refresh cookie is set (and kept across a reload); resume upload of a PDF near 10 MB works through Nginx; a 12 MB file is rejected; the browser console shows no CSP errors (adjust the CSP if a library needs it, keeping it as strict as possible); fonts load from the same origin; `http://localhost/swagger-ui.html` does not show Swagger.
5. Restart the stack (`docker compose down` then `up`) and confirm data and uploaded resumes persist.

**Definition of done:** one command starts everything; the demo path from chunk 1 works in the browser at `http://localhost`; data survives a restart.

---

## Part 6.3 — Testcontainers, smoke test, backups and README

**Tasks**
1. **Testcontainers:** add `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter` (test scope). Create `AbstractIntegrationTest` with a shared `PostgreSQLContainer<>("postgres:17")` (static, reused across the test class hierarchy) using Spring Boot's `@ServiceConnection`. Switch all integration tests to it and remove the dependency on the local `fairfold_test` database. Docker must be running for tests; note it in the README.
2. **Smoke test** `scripts/smoke.sh` (curl and jq; `BASE_URL` defaults to `http://localhost`): health check through Nginx → register an employer and a candidate with timestamped emails → employer completes the company and creates and activates a job → candidate uploads `scripts/sample.pdf` (a small valid PDF with a few sentences of resume text) and waits until the resume is `ready` → candidate applies → employer starts screening with the fallback provider and polls until finished → employer lists applicants and checks that no response contains the candidate's name or email → employer shortlists and checks the identity appears → print PASS or FAIL per step and exit non-zero on any failure.
3. **Backup scripts:** `scripts/backup.sh` (`docker compose exec -T postgres pg_dump -U "$DB_USER" "$DB_NAME" | gzip > backups/db-$(date +%F).sql.gz` and a tar of the `resumes` volume via a throwaway `alpine` container) and `scripts/restore.sh` (the reverse). Test a restore into a fresh stack once.
4. **README.md** at the repository root: what FairFold is, requirements (Docker, Docker Compose), first run (`cp .env.example .env`, edit secrets, `docker compose up --build`), first admin login, how to run locally without Docker (reference to 00_context section 3), how to run tests, how to back up and restore, how to enable email verification later (it is deferred), and a short production checklist: HTTPS in front of the stack (set `COOKIE_SECURE=true`), real SMTP, real keys, bKash merchant approval, backups scheduled, Swagger off.
5. Update `docs/progress.md` with the final status and any open issues.

**Definition of done (whole chunk):** `docker compose up --build` from a clean clone with a copied `.env` brings the app up; `scripts/smoke.sh` prints PASS for every step; `./mvnw test` passes using Testcontainers; a backup can be restored into a fresh stack; the README is accurate. After this chunk, the first version is complete; remaining work (email verification, GitHub Actions, polish) is in the roadmap.
