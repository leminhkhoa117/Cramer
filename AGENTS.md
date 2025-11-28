# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: Spring Boot (Java 21, Maven) REST API that talks to Supabase Postgres. See `backend/README.md` and `backend/BUILD_INSTRUCTIONS.md`.
- `frontend/`: React + Vite client styled with Tailwind and React Bootstrap. See `frontend/README.md` and `frontend/SETUP_INSTRUCTIONS.md`.
- `docs/`: Backend schema and operational docs, especially `docs/backend/supabase-backend.md`.
- Root: orchestration and meta files (`docker-compose*.yml`, `.env*`, `DEVELOPMENT_NOTES.md`, `GEMINI.md`) and CI under `.github/`.
- Root `README.md` is partly outdated (Supabase-only); use `GEMINI.md` and backend docs for the current Spring Boot + Supabase-auth architecture.

## Build, Test, and Development Commands

- Full stack (recommended): from repo root run `docker-compose up --build`.
- Backend dev (Windows, typical flow): `cd backend; ./run-app.ps1` (loads root `.env`, runs the built JAR; build first with `.\mvnw.cmd clean package -DskipTests` if `target/` is empty). Alternatives: `backend\run-local.cmd` or `cd backend && .\mvnw.cmd spring-boot:run`.
- Backend build: `cd backend && .\mvnw.cmd clean package`.
- Frontend dev (typical flow): `cd frontend; npm install; npm run dev` (Vite defaults to http://localhost:5173; set `VITE_API_BASE_URL`/Supabase keys in `.env.local`).
- Once tests exist, run backend tests with `cd backend && .\mvnw.cmd test`.

## Coding Style & Naming Conventions

- Match nearby code; do not introduce new libraries or patterns without discussion.
- Java: 4-space indentation, `com.cramer.*` packages, single responsibility classes, prefer Lombok where already used.
- React: function components in `PascalCase` (e.g., `TestPage.jsx`), helpers/hooks in `camelCase`, keep styling in Tailwind utility classes or local CSS files under `frontend/src`.

## Testing Guidelines

- Prefer adding focused tests alongside new backend services/controllers in `backend/src/test/java/com/cramer/...` using Spring Boot + JUnit 5.
- Frontend currently relies on manual testing; if you introduce a test runner, mirror the `src` structure with `*.test.jsx` files.
- Use Swagger (`http://localhost:8080/swagger-ui.html`) and real Supabase JWTs when changing authentication or API behavior.

## Commit & Pull Request Guidelines

- Use conventional commits (`feat:`, `fix:`, `docs:`, `refactor:`, etc.) and keep changes small and cohesive.
- Work on feature branches (`feature/<summary>`, `fix/<issue-id>`), and update `DEVELOPMENT_NOTES.md` for notable behavior or environment changes.
- PRs should describe the intent, main changes, affected endpoints/pages, testing steps (commands + URLs), and link any related issues.

## Security & Configuration

- Do not commit real secrets; use `.env.example` as a template and keep `.env` files local.
- When modifying schema, auth, or infrastructure behavior, also update `docs/backend/supabase-backend.md` and summarize the change in `DEVELOPMENT_NOTES.md`.

## Auth & API Notes

- All `/api/**` routes are JWT-protected; the JWT subject must be the Supabase user UUID and the secret comes from `supabase.jwt.secret` (see PROJECT_DIARY.md 2025-11-08 for the unresolved signature mismatch blocker).
- `SupabaseAdminService` expects a base URL without `/rest/v1`; the current `.env.example` points to `/rest/v1`, which breaks `/auth/v1` admin calls (email checks).
- Profile API surface is currently only `GET`/`PUT /api/profiles/{id}`; the frontend still tries `POST /api/profiles` when a profile is missing, so signup/profile creation will 404 until the backend is expanded.
- Target endpoints live at `/api/targets/me`, but the frontend posts to `/api/dashboard/target`; align before relying on dashboard target saves.
- Test attempt flow requires auth; `/api/test-attempts/{id}/progress` wipes and rewrites answers, and `/submit` recalculates scores and marks attempts `COMPLETED`.

## Frontend Notes

- Use `.env.local` with `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, and `VITE_API_BASE_URL`; `frontend/README.md` still mentions port 3000 (actual dev port is 5173).
- Supabase Storage bucket `userImages` is expected with folders `avatars/`, `hero-backgrounds/`, and `backgrounds/`; uploads delete prior files and resize avatars to 200x200 client-side.
- `App.jsx` imports `./pages/profile` (lowercase) while the file is `Profile.jsx`; this will break on case-sensitive filesystems.
- Routes: tests live at `/test/:source/:testNum/:skill`, course details at `/courses/:courseName`, profile page depends on the profile API notes above.

## Recent Agent Notes

- 2025-11-28: Skimmed repo - docs still diverge from the Spring Boot + Supabase setup; profile creation and dashboard target APIs are missing server handlers the frontend calls; Supabase admin base URL must exclude `/rest/v1`; auth remains blocked by Supabase JWT signature mismatch (see PROJECT_DIARY.md 2025-11-08).
- 2025-11-16: Verified the two-pane Test layout (passage/questions) and confirmed that dragging the resize handle fully left does not remove the passage panel when the layout is configured correctly; no code change required beyond the existing safeguards.
