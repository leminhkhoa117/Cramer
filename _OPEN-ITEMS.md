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
- [ ] Timeouts on sync OpenRouter path (`OpenRouterClient.chat()` has no connect/read timeout).
- [ ] Retry backoff + `Retry-After` handling in `GenerationService.generatePart()`.
- [ ] Enforce `abts.streaming.part-timeout-ms` (currently declared, never used).
- [ ] Fix config binding drift in `application.properties` (`openrouter.generation-model`, `timeout-ms`, etc. bind to nothing; effective default is `deepseek-chat` not `deepseek-v4-flash`).
- [ ] `OpenRouterException` handler in `GlobalExceptionHandler` (structured 502/503).
- [ ] `RefinementService`: move inline prompt to prompt package, server-side round counter, stop swallowing revalidate errors (F12/F13/F7).
- [ ] Observability: actuator/micrometer, structured logging.
- [ ] Caching for read-heavy endpoints (courses, hashtags, quota views).
- [ ] Delete dead code: 3 full schemas in `PromptSchemaBuilder`, `contextCache`/`webSearch` paths, legacy frontend event branches.
- [ ] `ValidationResult` shape vs SPEC-23 §1.1 (`schemaErrors/contentErrors/businessRuleErrors` split) — decide: fix code or update spec.

## Phase 3 — Database (production data must be preserved; incremental migrations only)
- [ ] Add indexes on `test_attempts` (user_id, exam_source/test_number/skill, status) and check `speaking_sessions` watchdog index.
- [ ] Decide + implement consolidation of `user_quotas`/`skill_quotas`/`user_subscriptions` counters.
- [ ] Archive plan for dead tables (`chatbot_usage`, `abts_templates`, `model_runtime_status`, 6 speaking legacy) — export backup, then drop (no aggressive drops without backup).
- [ ] Fix doc drift in `DATABASE_SCHEMA.md` (migration count 52 vs 53, missing 4 migrations, `FILL_BLANK` vs `FILL_IN_BLANK`, `GRADED` vs `COMPLETED`, skill case conventions).
- [ ] Enum hygiene: `CreditCategory`/`WritingStatus`/skill case — align code or migrate data.
- [ ] `sections.exam_source/test_number` legacy shim + dual-path repositories — plan removal (big; may move to Phase 4).

## Phase 4 — Deep refactor (optional later)
- [ ] Merge 3 API client stacks (`lib/api`, `admin/api`, `admin/services/abtsApi`) into one token-acquisition path.
- [ ] Unify admin CSS tokens (`admin-variables.css` vs dead `tokens.css`), kill duplicate modal systems.
- [ ] Remove ~100 default React imports; convert 2 remaining Contexts to Zustand.
- [ ] Port story: reconcile 3000 vs 5173 across `vite.config.js`, `docker-compose.yml`, `.env`, AGENTS.md.
- [ ] `AdminRouteGuard` hardcoded `ADMIN_USER_IDS` → env var.

## Deferred polish
- [ ] **Coverage gate.** Removed aspirational 50/40/50/50 thresholds (Vitest 4 format drift + actual ~0.6% coverage). Restore thresholds once suites grow; CI currently reports coverage without failing. Added 16/08/2026.
- [ ] **springdoc Swagger UI public in prod** (`swagger-ui.enabled=true` unconditional) — gate behind profile. Added 16/08/2026.
- [ ] **`SAVE` endpoint has no server-side validation** (`AbtsSaveService` trusts client payloads; `parseType` can write NULL question_type). Added 16/08/2026.
- [ ] **Dead config keys** (`openrouter.regeneration-model`, `json-fix-model`, `streaming-enabled`, `site-url/site-name`) — remove or wire. Added 16/08/2026.

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
