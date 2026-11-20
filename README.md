# FairFold

FairFold is a recruitment web platform with two portals. Employers post jobs, receive **anonymised** applications, and get a ranked list where every score is explained with quotes from the candidate's own resume. Candidates build a profile from a PDF resume, apply, see how they were scored, prove skills with assessments and practise interviews. **AI is advisory — a human always makes the final hiring decision.**

---

## Table of contents

- [Overview](#overview)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [1. Install MariaDB](#1-install-mariadb)
- [2. Create the databases](#2-create-the-databases)
- [3. Configure the environment](#3-configure-the-environment)
- [4. Run the backend](#4-run-the-backend)
- [5. Run the frontend](#5-run-the-frontend)
- [Tests](#tests)
- [API documentation](#api-documentation)
- [Useful commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)

---

## Overview

| | |
|---|---|
| **Candidates** | Build a profile from a PDF resume, apply to jobs, see how they were scored, take skill assessments, practise interviews in text, and track a career journey. |
| **Employers** | Post jobs, receive anonymised applications, run AI screening, see a ranked list with evidence quotes, shortlist (only then is identity revealed), schedule interviews and message candidates. |
| **Interviewers** | Get a question pack and submit structured feedback. |
| **Admins** | Manage users, content, resources and the audit log. |
| **Guests** | Browse public jobs. |

**Core principles**

- Personal data (name, email, phone) is never sent to the AI provider — resumes are stripped before screening.
- Every score is explainable with validated quotes, or clearly labelled **rule-based**.
- Every AI call, filter result, identity reveal and override is auditable.
- Skills count, not only tenure.
- Humans stay in charge; nothing is rejected automatically.

**Roles:** `candidate`, `employer_hr`, `employer_manager`, `interviewer`, `admin` (one role per account).

**Project status:** the foundation and authentication are implemented (Part 1.1), with the job/skills schema landing in Part 1.2. See [`docs/progress.md`](docs/progress.md) for the live status and the full roadmap in [`docs/roadmap.md`](docs/roadmap.md).

---

## Tech stack

| Layer | Choice |
|---|---|
| **Backend** | Java 21, Spring Boot 3.5, Spring Web, Security, Data JPA, Validation, Flyway, Mail |
| **Database** | **MariaDB** (InnoDB, `utf8mb4`, collation `utf8mb4_0900_as_ci`) — driver `org.mariadb.jdbc:mariadb-java-client`, migrations via `flyway-mysql` |
| **Frontend** | React 18, TypeScript, Vite, React Router, Axios, TanStack Query, React Hook Form, Tailwind CSS (dark only), lucide-react, `@fontsource-variable/inter` |
| **API docs** | springdoc-openapi (Swagger UI + `/v3/api-docs`) |
| **Auth** | JWT access token (in memory), refresh token in an HttpOnly cookie, BCrypt, rate limiting (Bucket4j) |
| **AI** | DeepSeek behind an `AiProvider` interface, with a `RuleBasedFallbackProvider` (works with no key) |
| **Payments** | bKash sandbox behind a `PaymentProvider` interface, with a fake provider |
| **Files** | Local disk behind a `StorageService` interface |
| **Build** | Maven wrapper (`./mvnw`), npm |

**Deliberately not used:** Redis, MinIO, ClamAV, Prometheus, Kubernetes, Spring AI, embeddings, microservices, WebSockets.

---

## Repository layout

```text
fairfold/
├── backend/                 Spring Boot (Maven) — run from this directory
│   └── src/main/
│       ├── java/com/fairfold/
│       │   ├── auth/        register, login, refresh, reset, change password
│       │   ├── user/ company/
│       │   ├── common/      errors, security (JWT), mail, audit, paging, rate limiting
│       │   └── config/      security, OpenAPI, properties, data seeder
│       └── resources/
│           ├── db/migration/   Flyway migrations (V*__*.sql)
│           └── application*.yml
├── frontend/                React + Vite + TypeScript — run from this directory
│   └── src/
│       ├── app/             router, auth context, guards
│       ├── api/             Axios client + API modules
│       ├── features/        feature pages (auth, …)
│       ├── components/      ui/ primitives and layout shells
│       └── styles/          design tokens
├── docs/                    PRD, design system, plan, roadmap, prompts, progress
├── scripts/check-design.sh  fails on rounded corners / gradients / shadows
├── data/                    resume files in development (git-ignored)
├── .env.example             every setting the app reads
└── .env                     your local values (git-ignored — never commit)
```

---

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Node.js | 20 or newer | `node -v` |
| npm | comes with Node | `npm -v` |
| MariaDB | see below | `mariadb --version` |
| Git | any | `git --version` |

The backend ships a Maven wrapper (`backend/mvnw`), so you do **not** need a system Maven install.

---

## 1. Install MariaDB

You need a MariaDB server **and** client. The project was verified on **MariaDB 13.0.2**. The migrations use the `utf8mb4_0900_as_ci` collation, so use a MariaDB version that supports it (recent releases do; pick the current stable from the official site if unsure).

**Official downloads:** <https://mariadb.org/download/>

### Arch Linux

```bash
sudo pacman -S --needed mariadb
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
sudo systemctl enable --now mariadb
sudo mariadb-secure-installation
```

### Debian / Ubuntu

```bash
sudo apt update
sudo apt install -y mariadb-server
sudo systemctl enable --now mariadb
sudo mariadb-secure-installation
```

### Fedora / RHEL

```bash
sudo dnf install -y mariadb-server
sudo systemctl enable --now mariadb
sudo mariadb-secure-installation
```

### macOS (Homebrew)

```bash
brew install mariadb
brew services start mariadb
mariadb-secure-installation
```

### Windows

Download the MSI installer from <https://mariadb.org/download/>, run it, and keep the default port `3306`. Use the bundled **MariaDB Client** (or a GUI such as DBeaver/HeidiSQL) for the SQL below.

Verify the server is running:

```bash
mariadb --version
mariadb -u root -p -e "SELECT VERSION();"
```

---

## 2. Create the databases

FairFold needs two databases: one for the app and one for backend tests.

Log in as root:

```bash
sudo mariadb        # or: mariadb -u root -p
```

Run:

```sql
CREATE DATABASE fairfold      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;
CREATE DATABASE fairfold_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;

CREATE USER 'fairfold'@'%' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON fairfold.*      TO 'fairfold'@'%';
GRANT ALL PRIVILEGES ON fairfold_test.* TO 'fairfold'@'%';
FLUSH PRIVILEGES;
```

> Want a simpler local setup? You can use your own MariaDB user (for example the one you already log in with) and just point `DB_USER` / `DB_PASSWORD` at it in `.env`. The databases and grants above are the portable option.

Verify the connection:

```bash
mariadb -u fairfold -p -e "SHOW DATABASES;"
```

**You do not need to create tables.** Flyway runs the migrations in `backend/src/main/resources/db/migration/` automatically on backend startup.

> The backend connects with the MariaDB driver (`org.mariadb.jdbc.Driver`) and a `jdbc:mariadb://…` URL. See `backend/src/main/resources/application.yml`.

---

## 3. Configure the environment

Copy the template and fill in your values:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
# --- Database ---
DB_HOST=localhost
DB_PORT=3306
DB_NAME=fairfold
DB_USER=fairfold
DB_PASSWORD=change-me          # matches the password you set above

# --- Auth ---
JWT_SECRET=                    # at least 32 bytes; generate below
ADMIN_EMAIL=admin@fairfold.local
ADMIN_PASSWORD=AdminPass123!   # used to seed the first admin account

# --- Other ---
AI_PROVIDER=fallback           # works with no key
PAYMENT_PROVIDER=fake
APP_BASE_URL=http://localhost:5173
STORAGE_DIR=../data/resumes
```

Generate a strong JWT secret:

```bash
openssl rand -hex 48
```

The backend loads `.env` from the **repository root** via `springboot3-dotenv` (configured with `springdotenv.directory=..`, because the backend runs from `backend/`). Real environment variables still take precedence.

> `.env` is git-ignored. **Never commit it.** `.env.example` is the safe, shareable template.

Key settings (see `.env.example` for the full list):

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `fairfold` | MariaDB connection |
| `DB_USER` / `DB_PASSWORD` | `fairfold` / *(empty)* | MariaDB credentials |
| `JWT_SECRET` | *(empty)* | HS256 signing key (≥ 32 bytes) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@fairfold.local` / *(empty)* | Seeds the first admin on startup |
| `AI_PROVIDER` | `fallback` | `fallback` (no key) or `deepseek` |
| `DEEPSEEK_API_KEY` | *(empty)* | Only needed for `AI_PROVIDER=deepseek` |
| `PAYMENT_PROVIDER` | `fake` | `fake` or `bkash` |
| `STORAGE_DIR` | `../data/resumes` | Where uploaded resumes are stored |
| `APP_BASE_URL` | `http://localhost:5173` | Frontend base URL used in links |

---

## 4. Run the backend

From the **`backend/`** directory (so `../.env` resolves to the repo root):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- Backend: <http://localhost:8080>
- Health check: <http://localhost:8080/api/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

On first start, Flyway creates the schema and `DataSeeder` creates the admin account from `ADMIN_EMAIL` / `ADMIN_PASSWORD` (if it does not already exist).

---

## 5. Run the frontend

In a second terminal, from the **`frontend/`** directory:

```bash
cd frontend
npm install
npm run dev
```

- Frontend: <http://localhost:5173>

Vite proxies `/api` to `http://localhost:8080`, so the browser sees a single origin and the HttpOnly refresh cookie works with no CORS setup.

Open <http://localhost:5173>, register a candidate/employer, or sign in with the seeded admin (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).

**Production build:**

```bash
cd frontend
npm run build      # output in frontend/dist/
npm run preview    # serve the build locally
```

---

## Tests

```bash
# Backend — needs the fairfold_test database (uses application-test.yml)
cd backend
./mvnw test

# Frontend unit tests (Vitest)
cd frontend
npm test

# Design system guard (fails on rounded corners / gradients / shadows / off-token colours)
./scripts/check-design.sh
```

---

## API documentation

springdoc generates the docs from the controllers, so they are always current:

| What | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

Export the contract after a change:

```bash
curl http://localhost:8080/v3/api-docs -o docs/openapi.json
```

> Swagger UI and `/v3/api-docs` are enabled only in the `local` profile and are switched off in production.

---

## Useful commands

| What | Command |
|---|---|
| Run backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| Run frontend | `cd frontend && npm run dev` |
| Backend tests | `cd backend && ./mvnw test` |
| Frontend tests | `cd frontend && npm test` |
| Design check | `./scripts/check-design.sh` |
| Export API contract | `curl http://localhost:8080/v3/api-docs -o docs/openapi.json` |
| Reset the dev database | `mariadb -u root -p -e "DROP DATABASE fairfold; CREATE DATABASE fairfold CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;"` |
| Repair a half-applied migration | fix or drop the partial objects manually, then `cd backend && ./mvnw flyway:repair` |

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Access denied for user 'fairfold'@…` | The `DB_PASSWORD` in `.env` does not match the MariaDB user, or the user's host (`'%'` vs `'localhost'`) does not match how you connect. Re-check the `CREATE USER` statement and re-run `FLUSH PRIVILEGES`. |
| `Unknown collation: 'utf8mb4_0900_as_ci'` | Your MariaDB build is too old. Upgrade to a recent MariaDB release (the project was verified on 13.0.2). |
| `Communications link failure` / connection refused | MariaDB is not running: `sudo systemctl start mariadb` (Linux) or `brew services start mariadb` (macOS). |
| Flyway migration fails halfway | DDL is not transactional in MariaDB. Drop the partially created objects, then run `./mvnw flyway:repair` and start again. |
| Backend can't read `.env` | Start it from `backend/` so `springdotenv.directory=..` finds the repo-root `.env`. |
| Port already in use | Backend uses `8080`, frontend `5173`. Stop the other process or change `server.port` / the Vite port. |
| Frontend requests fail with 404 | Make sure the backend is running on `:8080`; Vite proxies `/api` there. Open the API on `:8080` directly, not through `:5173`. |

---

## Documentation

| File | Contents |
|---|---|
| [`docs/FairFold_PRD_v2_1.md`](docs/FairFold_PRD_v2_1.md) | Product requirements, goals, roles, functional requirements |
| [`docs/design.md`](docs/design.md) | Design system: dark-only, square corners, five-colour palette, components |
| [`docs/plan.md`](docs/plan.md) | 10-day build plan, local setup, chunk-by-chunk checklist |
| [`docs/roadmap.md`](docs/roadmap.md) | Milestones, timeline, production-readiness gates |
| [`docs/progress.md`](docs/progress.md) | Live status, decisions (including **D-15: MariaDB**), test log, history |
| [`docs/prompts/`](docs/prompts/) | Chunk-by-chunk build prompts and shared context |

---

*AI is advisory. Humans decide. Every score is explained, and every decision is auditable.*
