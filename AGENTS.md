# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: Spring Boot (Java 21, Maven) REST API that talks to Supabase Postgres. See `backend/README.md` and `backend/BUILD_INSTRUCTIONS.md`.
- `frontend/`: React + Vite client styled with Tailwind and React Bootstrap. See `frontend/README.md` and `frontend/SETUP_INSTRUCTIONS.md`.
- `docs/`: Backend schema and operational docs, especially `docs/backend/supabase-backend.md`.
- Root: orchestration and meta files (`docker-compose*.yml`, `.env*`, `DEVELOPMENT_NOTES.md`, `GEMINI.md`) and CI under `.github/`.

## Build, Test, and Development Commands

- Full stack (recommended): from repo root run `docker-compose up --build`.
- Backend dev (Windows, typical flow): `cd backend; ./run-app.ps1`. Alternatives: `backend\run-local.cmd` or `cd backend && .\mvnw.cmd spring-boot:run`.
- Backend build: `cd backend && .\mvnw.cmd clean package`.
- Frontend dev (typical flow): `cd frontend; npm install; npm run dev`.
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

## Recent Agent Notes

- 2025-11-16: Verified the two-pane Test layout (passage/questions) and confirmed that dragging the resize handle fully left does not remove the passage panel when the layout is configured correctly; no code change required beyond the existing safeguards.
