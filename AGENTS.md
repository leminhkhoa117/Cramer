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

- All `/api/**` routes are JWT-protected; the JWT subject is the Supabase user UUID and validated using `supabase.jwt.secret`.
- **Authentication is fully functional** as of 2025-12-04 — JWT signature validation works correctly.
- Profile API: `GET`/`PUT /api/profiles/{id}` — frontend handles profile creation via Supabase triggers.
- Dashboard API: `GET /api/dashboard/summary/{userId}` — aggregates test progress, targets, and stats.
- Target endpoints: `POST /api/dashboard/target` saves user learning targets.
- Test attempt flow is fully implemented with start, progress, submit, cancel, and delete endpoints.

## Frontend Notes

- Use `.env.local` with `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, and `VITE_API_BASE_URL`.
- Dev server runs on port **5173** (Vite default).
- Supabase Storage bucket `userImages` is expected with folders `avatars/`, `hero-backgrounds/`, and `backgrounds/`.
- Routes: tests at `/test/:source/:testNum/:skill`, course details at `/courses/:courseName`.
- Writing test at `/test/writing/:source/:testNum`, review at `/test/writing/review/:attemptId`.
- `TestPage.jsx` uses `hasFetchedRef` to prevent duplicate API calls in React StrictMode.
- `WritingResultPage.jsx` uses `react-resizable-panels` for 3-column layout with collapsible scores bar.

## Database Notes

- **RLS (Row Level Security)** is enabled on `test_attempts`, `user_answers`, `writing_submissions`, `profiles`, and `target` tables.
- Service role policies have been added to allow backend DELETE operations.
- When adding new tables with RLS, ensure service_role policies are created for backend operations.

## Recent Agent Notes

- 2025-12-10: **Major Security Audit & Fixes** — Comprehensive security hardening:
  - **IDOR Fixes (CRITICAL)**: `ProfileController` and `DashboardController` now validate ownership via JWT authentication. Dashboard endpoint changed from `/summary/{userId}` to `/summary`.
  - **XSS Prevention (CRITICAL)**: Installed DOMPurify and created `src/utils/sanitize.js`. All 18 `dangerouslySetInnerHTML` usages now sanitized.
  - **Race Condition Fix**: `TestAttemptService.startOrGetAttempt` now uses pessimistic locking via `findAndLockByUserIdAndExamSourceAndTestNumberAndSkill`.
  - **Null Check Fix**: `GeminiGradingService.parseAndApplyGradingResults` validates Gemini API response structure before accessing arrays.
  - **Transaction Fix**: `WritingSubmissionService` async grading now uses `TransactionSynchronizationManager.afterCommit()` to prevent reading uncommitted data.
  - **RLS Enabled**: `profiles` and `target` tables now have RLS with user-scoped and service_role policies.
  - **GlobalExceptionHandler**: Added `AccessDeniedException` handler returning HTTP 403.
  - **Frontend API Update**: `dashboardApi.getSummary()` no longer takes userId parameter.
- 2025-12-05: **Fixed ghost IN_PROGRESS attempts for Writing tests** — when user completes a Writing test and navigates back, the system no longer creates phantom IN_PROGRESS attempts. Changes:
  - `TestAttemptService.java`: When `forceNew=false` and latest attempt is COMPLETED, return the COMPLETED attempt instead of creating new
  - `WritingTestPage.jsx`: Detect COMPLETED status and show choice modal
  - `ResumeConfirmationModal.jsx`: Added `attemptStatus` prop to show different messages for COMPLETED vs IN_PROGRESS
  - Flow: COMPLETED → show "Xem kết quả" / "Làm bài mới" choice → user decides
- 2025-12-05: **Implemented IELTS Writing Test with AI Grading** — comprehensive feature including:
  - Async grading service using gemini-2.5-pro (Gemini 2.5 Pro) API
  - WritingResultPage with resizable 3-column layout (react-resizable-panels)
  - Top collapsible scores bar with 4 IELTS band criteria
  - Essay highlighting with color-coded error types (grammar, spelling, vocabulary, punctuation, coherence)
  - Click-to-scroll from highlighted text to analysis sections
  - Detailed feedback: sentence corrections, paragraph rewrites, sample essays (band+1, band 9), word analysis
  - User can save Gemini API key in profile for personalized AI grading
  
    Added files and notable points for the Writing feature commit (2025-12-05):
    - Backend additions: controllers, DTOs, entity, repository, and services:
      - `backend/src/main/java/com/cramer/controller/WritingController.java`
      - `backend/src/main/java/com/cramer/entity/WritingSubmission.java`
      - `backend/src/main/java/com/cramer/repository/WritingSubmissionRepository.java`
      - `backend/src/main/java/com/cramer/dto/WritingSubmitDTO.java`
      - `backend/src/main/java/com/cramer/dto/WritingSubmissionDTO.java`
      - `backend/src/main/java/com/cramer/dto/WritingReviewDTO.java`
      - `backend/src/main/java/com/cramer/service/AsyncGradingService.java`
      - `backend/src/main/java/com/cramer/service/GeminiGradingService.java`
      - `backend/src/main/java/com/cramer/service/WritingSubmissionService.java`

    - Frontend additions and updates:
      - `frontend/src/pages/WritingTestPage.jsx`
      - `frontend/src/pages/WritingResultPage.jsx`
      - `frontend/src/css/WritingTestPage.css`
      - `frontend/src/css/WritingResultPage.css`
      - Updates to `ResumeConfirmationModal.jsx` and `TestPageContent.jsx` to support the writing attempt flow and modal behavior

    - Docs & migrations:
      - `docs/backend/migrations/001_writing_feature.sql`
      - `docs/backend/IELTS Cambridge 17_T1_W.sql`
      - `docs/marking_criteria/IELTS_W_Band_Descriptors.md`
      - Sample assets added under `docs/test_materials/` and `docs/marking_criteria/` (image and PDF)

    - Backend behavior updates:
      - `TestAttemptService` updated to return COMPLETED attempts on `forceNew=false` instead of creating new attempts to avoid ghost `IN_PROGRESS` attempts.
      - Repository delete methods corrected with explicit `@Query` and `@Modifying` annotation to support reliable delete semantics.
- 2025-12-04: **Fixed test cancellation bug** — attempts are now properly deleted when user clicks "Huỷ bài". Required fixes:
  - Added explicit `@Query` annotations to repository delete methods.
  - Added RLS policies for service_role on test_attempts and user_answers tables.
  - Added `hasFetchedRef` to prevent duplicate API calls from React StrictMode creating orphan attempts.
- 2025-12-04: JWT authentication confirmed working — no more signature mismatch issues.
- 2025-11-16: Verified the two-pane Test layout (passage/questions) works correctly.
