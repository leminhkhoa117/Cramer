# Experiment Refactor Run Report

Date: 03/06/2026
Branch: `dev/experiment-refactor`

## Running Evidence

- Git safety: switched from `main` to new branch `dev/experiment-refactor` without reverting existing dirty worktree changes.
- Supabase MCP: VS Code deferred Supabase tools did not load via `tool_search`; OpenCode reported `supabase_cramer` connected and project-scoped MCP calls returned `https://jpocdgkrvohmjkejclpl.supabase.co` plus public table metadata.
- Local runtime: upgraded frontend can run on `127.0.0.1:3001` when port `3000` is occupied by another project; upgraded backend is listening on `8080`.
- Backend runtime evidence: Spring Boot `3.5.14` started successfully on port `8080`.
- Source inventory: backend has 301 Java source files under `backend/src/main/java/com/cramer`; frontend route tree is centralized in `frontend/src/router.jsx`.
- Oversized text/source files: 32 tracked or non-ignored untracked text files currently exceed 1,000 lines, led by `frontend/package-lock.json`, large CSS files, ABTS backend services, and admin ABTS/frontend modules.
- Supabase RLS recheck: OpenCode MCP `supabase_cramer_list_tables` confirmed `public.sections`, `public.questions`, and `public.subscription_tiers` are the only 3 public tables with RLS disabled out of 35 public tables.
- Supabase RLS migration applied: `20260603_enable_content_tier_rls` enabled RLS for `sections`, `questions`, and `subscription_tiers`; anon/authenticated can read public content/active tiers and cannot read `questions.correct_answer`.

## Changes Made

- Enforced server-side admin authorization for `/api/admin/**` in Spring Security.
- Derived `ROLE_ADMIN` from `profiles.is_admin` for valid JWT requests to admin paths.
- Added focused JWT filter tests for admin and non-admin profiles.
- Added a MockMvc admin authorization security test that exercises the real Bearer-token path through `JwtAuthFilter` and `ProfileRepository`.
- Updated dashboard/finance admin MVC tests to send `ROLE_ADMIN` in mock JWTs.
- Upgraded backend dependency slice to Spring Boot `3.5.14`, Java release `21`, springdoc `2.8.17`, JJWT `0.13.0`, Lombok `1.18.46`, Hypersistence Utils `3.15.2`, WireMock `3.13.2`, Awaitility `4.3.0`, compiler plugin `3.15.0`, and Surefire `3.5.6`.
- Fixed the Spring Boot 3.5/Jackson/Lombok boolean DTO regression so only `isFree`/`isPremium` JSON keys are emitted.
- Upgraded frontend dependency slice to React `19.2.7`, React Router DOM `7.16.0`, Vite `8.0.16`, plugin-react `6.0.2`, Vitest `4.1.8`, jsdom `29.1.1`, Supabase JS `2.107.0`, Axios `1.16.1`, DOMPurify `3.4.7`, uuid `14.0.0`, Three `0.184.0`, React Three Fiber `9.6.1`, Drei `10.7.7`, Tailwind/PostCSS `4.3.0`, Recharts `3.8.1`, Framer Motion `12.40.0`, Zustand `5.0.14`, and React Resizable Panels `4.11.2`.
- Migrated `react-resizable-panels` usage to v4 `Group`/`Panel`/`Separator`, explicit percentage sizes, and v4 imperative panel APIs.
- Added React Router 7 `HydrateFallback` support and removed deprecated `RouterProvider fallbackElement` usage.
- Removed vulnerable `xlsx` dependency and replaced admin spreadsheet exports with a small internal Excel-compatible `.xls` table writer.
- Split `WritingResultPage.jsx` by extracting `WritingFeedbackSections`; the touched page is now 953 lines.
- Added guarded RLS migration `docs/ops/migrations/20260603_enable_content_tier_rls.sql` and updated canonical DB schema docs to live table count/RLS state.
- Added PromptBuilder characterization tests and extracted shared prompt fragments into `PromptFragments` without changing public facade behavior.
- Centralized ABTS generation/save payload construction to fix single selected-part generation, selected-part save filtering, and generation metadata persistence.
- Added missing `testsApi.create` for admin test-set creation flow and compatibility tests for admin API exports.

## Validation

- Passed: `cd backend; ./mvnw.cmd "-Dtest=JwtAuthFilterTest,AdminDashboardControllerTest,AdminFinanceControllerTest" test`
  - Result: 35 tests, 0 failures, 0 errors.
- Passed: `cd backend; ./mvnw.cmd "-Dtest=AdminAuthorizationSecurityTest" test`
  - Result: 4 tests, 0 failures, 0 errors.
- Passed: `cd backend; ./mvnw.cmd test`
  - Result: 636 tests, 0 failures, 0 errors.
- Passed: `cd backend; ./mvnw.cmd clean package -DskipTests`
  - Result: build success; Boot `3.5.14` runtime started on port `8080`.
- Passed: `cd frontend; npm run test -- --run`
  - Result: 15 files, 277 tests, 0 failures.
- Passed: `cd frontend; npm run build`
  - Result: Vite `8.0.16` build success; remaining warning is large chunks over 500 kB.
- Passed: `cd frontend; npm audit --omit=dev`
  - Result: 0 vulnerabilities after removing `xlsx`.
- Passed: `cd backend; ./mvnw.cmd test`
  - Latest result after RLS/prompt changes: 643 tests, 0 failures, 0 errors.
- Passed: `cd backend; ./mvnw.cmd "-Dtest=PromptBuilderServiceTest" test`
  - Result: 7 tests, 0 failures, 0 errors.
- Passed: `cd frontend; npm run test -- --run src/__tests__/admin/utils/abtsGenerationPayload.test.js src/__tests__/admin/utils/abtsSavePayload.test.js src/__tests__/admin/stores/useABTSStore.test.js`
  - Result: 39 tests, 0 failures.
- Passed: `cd frontend; npm run test -- --run src/__tests__/admin/api/adminApi.test.js`
  - Result: 2 tests, 0 failures.
- Browser smoke: public `/`, `/about`, `/pricing` render on upgraded Vite/React without page errors; protected routes redirect to `/login` because the local browser session is no longer authenticated.
- Browser smoke fixed: React Router 7 `No HydrateFallback` warning no longer appears after router cleanup.

## Subagent Usage Log

- Round 1, backend inventory: neutral read-only worker, no file edits.
- Round 1, frontend inventory: neutral read-only worker, no file edits.
- Round 1, upgrade research: neutral read-only worker, no file edits.
- Review round 1, backend change review: neutral read-only worker, no file edits.
- Review round 1, frontend change review: neutral read-only worker, no file edits; caught the React Resizable Panels v4 numeric-size regression.
- Review round 1, risk review: neutral read-only worker, no file edits.
- Subagent round 2, RLS policy design: neutral read-only worker, no file edits.
- Subagent round 2, PromptBuilder refactor: neutral read-only worker, no file edits.
- Subagent round 2, ABTS admin UX: neutral read-only worker, no file edits.
- Subagent round 2, Admin frontend refactor: neutral read-only worker, no file edits.
- Subagent round 2, 1K enforcement: neutral read-only worker, no file edits.
- Subagent round 2, Boot 4 / Java 25 path: neutral read-only worker, no file edits.
- Subagent round 3, Prompt refactor next-slice review: neutral read-only worker, no file edits.
- Subagent round 3, ABTS payload review: neutral read-only worker, no file edits; caught selected-part/persistence bugs.
- Subagent round 3, Admin API slice: neutral read-only worker, no file edits; identified missing `testsApi.create`.

## Current Risks

- Supabase RLS risk is addressed for `public.sections`, `public.questions`, and `public.subscription_tiers`; remaining work is authenticated app smoke and monitoring for unexpected direct Supabase clients.
- Spring Boot 4 was not attempted in this pass; current blocker is large test migration from deprecated `@MockBean` to Spring Framework `@MockitoBean`, plus Spring Security 7/Hibernate 7/Jackson 3 migration risk.
- Local machine has Java 21 and Java 24 installed, but not Java 25 LTS. Backend compile target was raised to Java 21, which is the latest locally installed LTS.
- Speaking has backend support but no verified user-facing frontend route/store/API implementation in `frontend/src`.
- ABTS backend and admin frontend modules exceed the 1,000-line target and mix multiple responsibilities.
- Browser validation for authenticated dashboard/profile/test-taking/admin flows is blocked until the user re-authenticates locally; no credentials were printed or stored.
- Large chunk warning remains in Vite build; likely related to shared app bundle and chart/export/admin dependencies.

## Tool Limits

- `rg` is not installed in the Windows terminal; searches used PowerShell and workspace search tools instead.
- Supabase CLI is installed but not authenticated with `SUPABASE_ACCESS_TOKEN`; Supabase checks used OpenCode MCP instead.
- Port `3000` may belong to another local project. Do not kill or reuse it blindly; start Cramer frontend with `npm run dev -- --host 127.0.0.1 --port 3001 --strictPort` when needed.