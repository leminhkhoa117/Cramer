## Quick context for AI code agents

This repository is a two-part web app: a Spring Boot backend (Maven, Java 21) and a React + Vite frontend. Primary data lives in a Supabase Postgres instance (see `docs/backend/supabase-backend.md`). The backend exposes REST APIs under `/api/**` and uses JWTs (Supabase tokens) for authentication.

Keep changes small, well-scoped and documented. If you modify DB schema, update `docs/backend/*` and coordinate with the Supabase project.

## Where to look first (high-value files)
- Root README: `README.md` — project overview and links to backend docs.
- Backend main: `backend/src/main/java/com/cramer/CramerBackendApplication.java`.
- Backend config: `backend/src/main/java/com/cramer/config/` — especially `SecurityConfig.java`, `JwtAuthFilter.java`, `ConditionalDataSourceConfig.java`, `WebConfig.java`.
- Backend env & run: `backend/.env` (example), `backend/run-local.cmd`, `backend/run-app.ps1`, `backend/BUILD_INSTRUCTIONS.md` and `backend/pom.xml`.
- Backend properties: `backend/src/main/resources/application.properties` (datasource, supabase keys, swagger path).
- API surface and client: `frontend/src/api/backendApi.js` — shows axios usage, token provider (`setupApiClient`) and endpoints.
- Frontend: `frontend/package.json`, `frontend/README.md`, Vite/Tailwind config files.
- Docker: `docker-compose.yml` and `docker-compose.dev.yml` for containerized runs.

## Runtime & build essentials (exact commands)
- Backend (Windows, preferred):
  - Start dev app (uses root `.env`): `backend\run-local.cmd` (CMD) or `backend\run-app.ps1` (PowerShell).
  - Build jar: `cd backend && .\mvnw.cmd clean package -DskipTests` then run `java -jar target\cramer-backend-0.0.1-SNAPSHOT.jar`.
  - Maven run: `cd backend && .\mvnw.cmd spring-boot:run`.
  - Swagger UI: http://localhost:8080/swagger-ui.html (use Authorize with a Supabase JWT).

- Frontend:
  - Install: `cd frontend && npm install`
  - Dev server: `npm run dev` (default port 5173)
  - Important env vars: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, optional `VITE_API_BASE_URL`.

- Docker: use `docker-compose.yml` or `docker-compose.dev.yml` at repo root.

## Authentication & API expectations
- All `/api/**` routes are guarded by Spring Security: `SecurityConfig` configures `.requestMatchers("/api/**").authenticated()` and inserts `JwtAuthFilter` before username/password filter.
- `JwtAuthFilter` extracts `userId` from Supabase JWTs and grants `ROLE_USER` to validated tokens — controllers expect authenticated requests with an `Authorization: Bearer <token>` header.
- Frontend sets Authorization with the `setupApiClient` token provider (see `frontend/src/api/backendApi.js`).
- JWT authentication is fully functional as of 2025-12-04.

## Data & DB patterns
- Schema lives in Supabase (Postgres). Backend uses `spring-data-jpa` + `jsonb` payloads for question content.
- **RLS (Row Level Security)**: Tables `test_attempts` and `user_answers` have RLS enabled. Service role policies have been added to allow backend operations (DELETE, etc.).
- Conditional DB wiring: `ConditionalDataSourceConfig` only creates the `DataSource` if `spring.datasource.url` is set.

## Test Attempt Flow
- `POST /api/test-attempts/start` — creates or resumes an IN_PROGRESS attempt.
- `POST /api/test-attempts/{id}/progress` — saves answers, timeLeft, currentPart.
- `POST /api/test-attempts/{id}/submit` — submits and scores the attempt (marks as COMPLETED).
- `POST /api/test-attempts/{id}/cancel` — **deletes** the attempt and all associated answers (fixed 2025-12-04).
- `DELETE /api/test-attempts/{id}` — alternative delete endpoint.
- Frontend uses `hasFetchedRef` to prevent duplicate API calls in React StrictMode.

## Writing Test Attempt Flow (with forceNew parameter)
- `POST /api/test-attempts/start?forceNew=false` (default):
  - If IN_PROGRESS exists → return it (resume)
  - If COMPLETED exists → return COMPLETED (frontend shows choice modal)
  - If CANCELLED exists → create new attempt
- `POST /api/test-attempts/start?forceNew=true`:
  - Cancel all IN_PROGRESS attempts and create new
- Frontend `location.state.forceNew` controls this behavior:
  - Dashboard "Làm lại" button → `forceNew=true`
  - Direct URL access → `forceNew=false` → shows modal if COMPLETED
- `ResumeConfirmationModal` handles both IN_PROGRESS ("Tiếp tục") and COMPLETED ("Xem kết quả") states.

## Conventions & patterns to follow
- API routes: controllers are under `backend/src/main/java/com/cramer/controller/` and use `/api/{resource}` conventions.
- Use Springdoc OpenAPI (Swagger) for new endpoints.
- Repository delete methods should use explicit `@Query` annotations with `@Modifying(clearAutomatically = true, flushAutomatically = true)` for reliable deletion.

## Edit & testing rules for agents
- Small PRs only: change one component at a time.
- After making backend changes, always rebuild: `cd backend && .\mvnw.cmd clean package -DskipTests` and restart.
- Don't commit secrets: `.env` at repo root is local only.

## Known Issues (Resolved)
- ✅ Test cancellation now properly deletes attempts (fixed 2025-12-04).
- ✅ RLS policies added for service_role on test_attempts and user_answers tables.
- ✅ Duplicate API calls in React StrictMode prevented with refs.

## Useful quick links in repo
- API client: `frontend/src/api/backendApi.js`
- Test page: `frontend/src/pages/TestPage.jsx`
- Test attempt service: `backend/src/main/java/com/cramer/service/TestAttemptService.java`
- Swagger config: `backend/src/main/resources/application.properties`
- Docs: `docs/backend/supabase-backend.md`, `backend/BUILD_INSTRUCTIONS.md`
