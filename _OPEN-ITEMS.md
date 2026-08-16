# Open Items, Cramer Revival

_Last reviewed: 16/08/2026_

## Blocked on external input
- [ ] **Supabase MCP sign-in (`supabase_cramer`).** Owner: user (Hugo). Needed before Phase 3 (DB cleanup: index, dead tables, data audit). User said they will sign in. Added 16/08/2026.
- [ ] **Decide on secrets rotation.** Root `.env` + `frontend/.env` hold production secrets (SUPABASE keys, DEEPSEEK/OPENROUTER/PAYOS keys, `DEBUG_SECRET_KEY=12345678`). Owner: user — decide whether to rotate now or later; P0 hygiene. Added 16/08/2026.
- [ ] **Decide fate of repo clutter dirs.** `.orig-ref/` (30 snapshot files), `fixes/*.md` (7 notes), `.tmp/` — delete or keep as recovery material? Owner: user. Added 16/08/2026.

## Phase 1 — ABTS rescue
- [x] **Wire `frontend/src/lib/api/abts.js`** (contract-correct client) into `useABTSStore.js`; deleted old `admin/services/abtsApi.js`. Per user decision 16/08/2026. Resolved 16/08/2026.
- [x] **Fix contract breaks B1-B12**: save payload (`sections[]` snake_case + audioUrl), `?skill=` on validate/regenerate, `AI_THINKING` delta from `event.data`, refine `issueIds`/`ModelConfig`/hunks-array, apply `acceptedHunks`. Resolved 16/08/2026.
- [x] **Fix backend F1**: `ReadingValidator` reads `section.passage_text`. Resolved 16/08/2026.
- [x] **Fix B5 reasoning trace** end-to-end (`openAbtsStream` raw events + store reads `event.data`). Resolved 16/08/2026.

## Phase 2 — Backend hardening
- [x] Timeouts on the sync OpenRouter/DeepSeek path (JDK request factory with read timeout). Resolved 16/08/2026.
- [x] Retry backoff with jitter + Retry-After handling in `GenerationService.generatePart()`. Resolved 16/08/2026.
- [x] Enforce `abts.streaming.part-timeout-ms` (per-part deadline → PART_TIMEOUT). Resolved 16/08/2026.
- [x] Fix config binding drift in `application.properties` (default-generation-model, api-timeout-ms, site-url/site-name wired; dead keys removed; default model now deepseek-v4-flash). Resolved 16/08/2026.
- [x] `OpenRouterException` handler in `GlobalExceptionHandler` (structured 503 with error code). Resolved 16/08/2026.
- [x] `RefinementService`: prompt moved to `RefinementPromptBuilder` + schema-constrained, server-side round tracking per content, revalidate errors no longer swallowed. Resolved 16/08/2026.
- [x] Observability: actuator added (health, info, metrics). Resolved 16/08/2026.
- [x] Delete dead code: 3 full schemas in `PromptSchemaBuilder`, `OpenRouterChatRequest.jsonSchema` factory, `webSearch`/`contextCache` paths, `DeepSeekClient.timeout()`. Resolved 16/08/2026.
- [x] Caching for read-heavy endpoints: Caffeine + Spring Cache on course browse (`listPublishedSets`, `testsForCourse`, `setDetails`) and hashtag reads; evictions on admin/draft mutations. Resolved 16/08/2026.
- [x] `ValidationResult` shape vs SPEC-23 §1.1: synced the spec to the implemented `ValidationView` shape (`issues[]` + `errors`/`warnings` + counts). Resolved 16/08/2026.

## Phase 3 — Database (production data must be preserved; incremental migrations only)
- [x] Add indexes on `test_attempts` (user+status, lock-key), `user_answers` (user), `speaking_sessions` (status+updated_at for watchdog). Applied 16/08/2026 via migration `performance_indexes_20260816`. Resolved 16/08/2026.
- [x] Archive dead tables. Moved 9 tables (`chatbot_usage`, `abts_templates`, `model_runtime_status`, 6 speaking legacy) to the `archive` schema. Data preserved in place; RLS policies moved with tables. Migration `archive_dead_tables_20260816`. Resolved 16/08/2026.
- [x] Fix doc drift in `DATABASE_SCHEMA.md` (migration count 52→66, `FILL_BLANK`→`FILL_IN_BLANK`, `GRADED`→`COMPLETED`, `is_system`/`is_admin` clarified, archive status, missing migration rows). Resolved 16/08/2026.
- [x] Enum hygiene: live data checked — zero legacy enum rows (`GRADED`, `FILL_BLANK`, lowercase `completed`, lowercase `skill_quotas.skill`). No data migration needed. Resolved 16/08/2026.
- [ ] `sections.exam_source/test_number` legacy shim + dual-path repositories — plan removal (big; moves to Phase 4). Added 16/08/2026.

## Phase 4 — Deep refactor (optional later)
- [x] Merge the admin API stack into `lib/api` (all 8 admin modules use the shared client; `core.js` deleted; 401 handling + token injection now shared). Resolved 16/08/2026.
- [x] Unify admin CSS tokens: deleted the dead duplicate `admin/css/tokens.css`; `admin-variables.css` is the single loaded file. Resolved 16/08/2026.
- [x] Remove ~100 unused default React imports (verified: the 22 remaining files all use the `React.` identifier). Resolved 16/08/2026.
- [x] Convert the 2 remaining Contexts to Zustand: `HighlightContext` and the admin `Toast` context. Resolved 16/08/2026.
- [x] Delete the `utils/toast.js` shim; 3 consumers now use `ui/toast` directly. Resolved 16/08/2026.
- [x] `AdminRouteGuard` hardcoded `ADMIN_USER_IDS` → `VITE_ADMIN_USER_IDS` env var; remove the inline `<style>` injection. Resolved 16/08/2026.
- [x] Port story: dev server is 3000 everywhere (vite.config, .env PayOS URLs, AGENTS.md). Resolved 16/08/2026.
- [ ] Consolidate the 3 overlapping usage counters (`user_quotas`, `skill_quotas`, `user_subscriptions` counters). Needs a data-audit + design decision first. Added 16/08/2026.
- [ ] `sections.exam_source/test_number` legacy shim + dual-path repositories — plan removal (touches attempts resume/lock + publish cascade). Added 16/08/2026.

## Deferred polish
- [ ] **Coverage gate.** Removed aspirational 50/40/50/50 thresholds (Vitest 4 format drift + actual ~0.6% coverage). Restore thresholds once suites grow; CI currently reports coverage without failing. Added 16/08/2026.
- [x] **springdoc Swagger UI public in prod** — now gated by `SWAGGER_ENABLED` (default true; set false in prod). Resolved 16/08/2026.
- [x] **`SAVE` endpoint has no server-side validation** — `AbtsSaveService` now rejects unknown question types, missing/zero question numbers, and question-less reading/listening sections (400). Plus 3 new tests. Resolved 16/08/2026.
- [x] **Dead config keys** — removed with the Phase 2 binding fix. Resolved 16/08/2026.

## Watch items
- [ ] **Backend saveDraft drops testName/difficulty/hashtags.** `AbtsSaveService` only passes setCode/setId/testNumber/testId/generationMetadata to `ContentDraftPort`; test is named "AI Test N" regardless of the modal's testName. Wire testName/difficulty/hashtags through the port. Added 16/08/2026.
- [ ] **Regenerate single part >1 numbers questions 1..N (no canonical renumber).** `regenerateQuestions` uses `aggregate(..., totalParts=1)` so parts 2/3 produce local numbering. Open item if UI exposes per-part regeneration. Added 16/08/2026.
- [ ] **Multi-part validation.** `RefinementService.revalidate` validates the whole document; multi-part `{sections:[...]}` has no root `questions[]`, so revalidation reports phantom errors. Added 16/08/2026.
- [ ] **Spring Boot 4.0.x OSS support ends Dec 2026.** Plan 4.1.x upgrade before EOL. Watching. Added 16/08/2026.
- [ ] **AGENTS.md env-var list incomplete** (GEMINI_API_KEY, PAYOS_WEBHOOK_URL, DEBUG_*, SPEAKING_* missing) — update during docs sync. Added 16/08/2026.

## Resolved
- [x] **Phase 1 — ABTS rescue.** Wired lib/api client, fixed all contract breaks B1-B12, backend validator F1, snake_case content adaptation (StepPreview/contentAdapter/aiStudioStatus), store-backed imageUrls, multi-part question aggregation, refinement loop end-to-end. Frontend 275 tests pass, build OK; backend 145 tests pass. Resolved 16/08/2026.
- [x] **Phase 0 — dead code sweep.** 22 dead files deleted (verified by grep), `__patch_probe.tmp` removed, `frontend/html/` + Eclipse files untracked. Resolved 16/08/2026.
- [x] **Phase 0 — dep bumps/removals.** Spring Boot 4.0.0→4.0.7, axios→1.19.0, React→19.2.8, Vite→8.2.1, Zustand→5.0.15, Tailwind→4.3.3; removed jjwt/totp/sendgrid/mail/hypersistence/websocket (backend), bootstrap/react-bootstrap/maath/baseline-browser-mapping (frontend); npm audit 0 vulns. Resolved 16/08/2026.
- [x] **Phase 0 — Docker/CI.** Dockerfiles fixed (JDK 25, node 24, postcss copy), docker-compose env complete + nginx port fix, CI frontend job added. Resolved 16/08/2026.
- [x] **Phase 0 — stale tests.** Rewrote `useCourseStore.test.js` (16 fails → pass) and fixed `adminApi.test.js` X-User-Id expectations (pre-existing drift). 273/273 frontend tests pass; backend 145 tests pass. Resolved 16/08/2026.
