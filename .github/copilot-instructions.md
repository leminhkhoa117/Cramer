## Quick context for AI code agents

This repository is a two-part web app: a Spring Boot backend (Maven, Java 21) and a React + Vite frontend. Primary data lives in a Supabase Postgres instance (see `docs/backend/supabase-backend.md`). The backend exposes REST APIs under `/api/**` and uses JWTs (Supabase tokens) for authentication.

Keep changes small, well-scoped and documented. If you modify DB schema, update `docs/backend/*` and coordinate with the Supabase project.

## Where to look first (high-value files)
- Root README: `README.md` — project overview and links to backend docs.
- Backend main: `backend/src/main/java/com/cramer/CramerBackendApplication.java`.
- Backend config: `backend/src/main/java/com/cramer/config/` — especially `SecurityConfig.java`, `JwtAuthFilter.java`, `ConditionalDataSourceConfig.java`, `WebConfig.java`.
- Backend env & run: `backend/.env` (example), `backend/run-local.cmd`, `backend/run-app.ps1`, `backend/BUILD_INSTRUCTIONS.md` and `backend/pom.xml`.
- Backend properties: `backend/src/main/resources/application.properties` (datasource, supabase keys, swagger path).
- API surface and client: `frontend/src/api/backendApi.js` — shows axios usage, token provider (`setupApiClient`) and endpoints like `/sections/:id/full`.
- Frontend: `frontend/package.json`, `frontend/README.md`, Vite/Tailwind config files.
- Docker: `docker-compose.yml` and `docker-compose.dev.yml` for containerized runs.

## Runtime & build essentials (exact commands)
- Backend (Windows, preferred):
  - Start dev app (uses local `.env`): `backend\run-local.cmd` (CMD) or `backend\run-app.ps1` (PowerShell).
  - Build jar: `cd backend && .\mvnw.cmd clean package -DskipTests` then run `java -jar target\cramer-backend-0.0.1-SNAPSHOT.jar`.
  - Maven run: `cd backend && .\mvnw.cmd spring-boot:run`.
  - Swagger UI: http://localhost:8080/swagger-ui.html (use Authorize with a Supabase JWT).

- Frontend:
  - Install: `cd frontend && npm install`
  - Dev server: `npm run dev` (default port 3000)
  - Important env vars: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, optional `VITE_API_BASE_URL`.

- Docker: use `docker-compose.yml` or `docker-compose.dev.yml` at repo root. Inspect those files for container names and ports before editing.

## Authentication & API expectations
- All `/api/**` routes are guarded by Spring Security: `SecurityConfig` configures `.requestMatchers("/api/**").authenticated()` and inserts `JwtAuthFilter` before username/password filter.
- `JwtAuthFilter` extracts `userId` from tokens and grants `ROLE_USER` to validated tokens — controllers generally expect authenticated requests with an `Authorization: Bearer <token>` header.
- Frontend sets Authorization with the `setupApiClient` token provider (see `frontend/src/api/backendApi.js`). Use that pattern when adding client calls.

## Data & DB patterns
- Schema lives in Supabase (Postgres). Backend uses `spring-data-jpa` + `jsonb` payloads for question content — prefer storing question payloads in `jsonb` when adding new question types.
- Conditional DB wiring: `ConditionalDataSourceConfig` only creates the `DataSource` if `spring.datasource.url` is set — keep `.env` values correct when running locally.

## Conventions & patterns to follow
- API routes: controllers are under `backend/src/main/java/com/cramer/controller/` and use `/api/{resource}` conventions (e.g., `/api/sections`, `/api/questions`). Mirror these patterns for new resources.
- Use Springdoc OpenAPI (Swagger) for new endpoints — they automatically appear at `/swagger-ui.html`.
- Error logging: backend uses standard Spring logging; frontend logs API errors in `backendApi.js` interceptors — keep messages structured like existing examples.
- Auth flow: do not bypass `JwtAuthFilter` — prefer validating tokens with `JwtUtil` helper (`com.cramer.util.JwtUtil`) used across the codebase.

## Edit & testing rules for agents
- Small PRs only: change one component at a time (e.g., add one controller + DTO + repository). Include a short README note if behavior or schema changes.
- When editing backend runtime behavior, test manually with Swagger or curl and mention commands used in the PR description (e.g., how to obtain a Supabase JWT for testing).
- Don't commit secrets: `.env` at repo root is expected locally; do not commit real Supabase keys.

## Useful quick links in repo
- API client: `frontend/src/api/backendApi.js`
- Swagger config: `backend/src/main/resources/application.properties` (`springdoc.swagger-ui.path`)
- Build scripts: `backend/run-local.cmd`, `backend/run-app.ps1`, `backend/.mvnw*`
- Docs: `docs/backend/supabase-backend.md`, `backend/BUILD_INSTRUCTIONS.md`

If anything here looks wrong or you want me to expand examples (e.g., a sample curl JWT request + expected JSON), tell me which part to expand and I will update the file.
