# Cramer Backend — Service Layer (Vertical-Slice)

> **Last Updated:** 14/06/2026
> **Version:** 2.0.0 (regenerated from vertical-slice code)
> **Tech:** Spring Boot 4.0.0 · Java 25 · Spring Data JPA · OAuth2 resource server

This document **supersedes the legacy layered version** (the previous `controller` /
`service` / `service.implement` / `entity` / `dto` / `repository` packages were deleted in the
June 2026 rewrite and no longer exist). Every class name, FQN, file path, and method below was
verified by reading the actual `.java` source under `backend/src/main/java/com/cramer/`.

---

## Table of Contents

1. [Architecture & Conventions](#architecture--conventions)
2. [Cross-Module Ports & SPIs](#cross-module-ports--spis)
3. [Async & Scheduled Components](#async--scheduled-components)
4. [platform — Shared Kernel](#platform--shared-kernel)
5. [identity](#identity)
6. [catalog](#catalog)
7. [assessment](#assessment)
8. [writing](#writing)
9. [speaking](#speaking)
10. [billing](#billing)
11. [engagement](#engagement)
12. [admin](#admin)
13. [abts](#abts)
14. [Supporting Records & Value Types](#supporting-records--value-types)
15. [Per-Module Counts](#per-module-counts)

---

## Architecture & Conventions

The backend is organized into **10 vertical-slice modules**, each owning its own
`web/` (controllers + `web/dto` records), `service/` (services + cross-module `Port` interfaces),
`domain/` (Lombok JPA entities/enums), `repository/`, and (some) `config/`.

Modules:
`platform` · `identity` · `catalog` · `assessment` · `writing` · `speaking` · `billing` ·
`engagement` · `admin` · `abts`

Rules observed across the service layer (verified in source):

- **Module isolation** — a module never touches another module's repositories/entities. Cross-module
  collaboration goes through published **`Port` interfaces** (records/primitives only across the
  boundary).
- **Inbound SPIs** — a module may define an interface and let another module implement it (e.g.
  catalog's `TestDependencyGuard`, assessment's `AttemptCleanupParticipant`), injected via
  `ObjectProvider` so absence is tolerated.
- **Charge-after-success billing** — AI features (writing grading, chat, translation) bill only
  after a successful result; speaking deducts on completion and refunds on grading failure.
- **After-commit dispatch** — async grading is enqueued via
  `TransactionSynchronizationManager` so it runs only after the originating transaction commits.
- **`platform` depends on no business module** — its kernel services read across boundaries only via
  narrow `JdbcTemplate` queries (e.g. `AdminAuthorizationService`).

---

## Cross-Module Ports & SPIs

Published contracts injected across module boundaries. **Published port** = defined and implemented
by the owning module, consumed elsewhere. **Inbound SPI** = defined by the consumer, implemented by
the owner of the data.

| Interface (FQN) | Kind | Implemented by | Consumed by | Purpose |
| --- | --- | --- | --- | --- |
| `com.cramer.catalog.service.ContentLookupPort` | Published | `catalog.ContentLookupService` | assessment, writing, speaking | Resolve published sections/questions, gradable answers, review content, speaking bank |
| `com.cramer.catalog.service.ContentDraftPort` | Published | `catalog.ContentDraftService` | abts | Persist AI-generated content into catalog as **draft** |
| `com.cramer.catalog.service.TestDependencyGuard` | Inbound SPI | `assessment.AttemptTestDependencyGuard` | catalog | Veto deleting a test that has user data |
| `com.cramer.assessment.service.AttemptWriteBackPort` | Published | `assessment.AttemptWriteBackService` | writing | Drive the shared attempt shell without touching `test_attempts` |
| `com.cramer.assessment.service.AttemptCleanupParticipant` | Inbound SPI | `writing.WritingAttemptCleanup` | assessment | Clean attempt-scoped data before attempt delete/cancel |
| `com.cramer.billing.service.AttemptBillingPort` | Published | `billing.AttemptBillingService` | assessment | Charge attempt start vs. monthly quota / Lúa overage |
| `com.cramer.billing.service.UsageBillingPort` | Published | `billing.UsageBillingService` | writing | AI-grading billing (charge after success, refund) |
| `com.cramer.billing.service.SpeakingBillingPort` | Published | `billing.SpeakingBillingService` | speaking | Speaking session afford/deduct/refund |
| `com.cramer.billing.service.ChatBillingPort` | Published | `billing.ChatBillingService` | engagement | Chat allowance check + post-reply charge |
| `com.cramer.billing.service.TranslationBillingPort` | Published | `billing.TranslationBillingService` | engagement | Translation allowance check + post-success charge |
| `com.cramer.billing.service.FeatureAccessPort` | Published | `billing.FeatureAccessService` | catalog, engagement | Tier feature gating / premium check |
| `com.cramer.engagement.service.ActivityPort` | Published | `engagement.ActivityService` | assessment, billing, identity, admin | Append to user activity timeline |
| `com.cramer.admin.service.AuditPort` | Published | `admin.AuditService` | speaking, billing, admin | Write the admin audit trail |
| `com.cramer.speaking.service.SpeakingGradingTrigger` | Seam | `speaking.grading.SpeakingGradingDispatcher` | speaking (session service) | Enqueue async grading after completion |
| `com.cramer.abts.generation.PartGenerator` | Strategy | `abts` Reading/Listening/Writing generators | abts (`GenerationService`) | Generate one part/task through its phase pipeline |

---

## Async & Scheduled Components

| Component (FQN) | Mechanism | Trigger | Purpose |
| --- | --- | --- | --- |
| `writing.service.WritingGradingDispatcher` | `@Async(WritingAsyncConfig.EXECUTOR)` | After submit/regrade commit | Grade essays in parallel; bill once on full success |
| `speaking.grading.SpeakingGradingDispatcher` | `@Async("speakingGradingExecutor")` | After session completion commit | Off-thread enqueue → `SpeakingGradingWorker` |
| `speaking.grading.SpeakingGradingWorker` | Plain bean | Called by dispatcher/watchdog | Claim → OpenRouter grade (outside TX) → persist/fail+refund |
| `speaking.grading.SpeakingGradingWatchdog` | `@Scheduled(fixedDelayString=...)` (~60s) | Periodic | Fail stale `grading` (>10m); re-enqueue stale `completed` (>5m) |
| `billing.service.MonthlyResetScheduler` | `@Scheduled(cron="0 10 0 1 * *", Asia/Ho_Chi_Minh)` | Monthly | Reset counters; grant `monthly_lua_bonus` |
| `billing.service.SubscriptionExpiryScheduler` | `@Scheduled(cron="0 5 0 * * *", Asia/Ho_Chi_Minh)` | Daily | Flip expired `ACTIVE` subscriptions to `EXPIRED` |
| `abts.generation.StreamingGenerationService` | Bounded `ThreadPoolExecutor` + `SseEmitter` | Per stream request | Run generation/refinement off the request thread over SSE |
| `abts.service.ModelCatalogService` | `@EventListener(ApplicationReadyEvent)` | Startup + 5-min cache | Fetch/validate OpenRouter model catalog |
| `platform.config.StartupValidator` | `ApplicationRunner` | Startup | Log one-line config presence summary |

---

## platform — Shared Kernel

Cross-cutting services and thin integration clients. `platform` depends on no business module.

### AdminAuthorizationService
- **FQN:** `com.cramer.platform.security.AdminAuthorizationService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/platform/security/AdminAuthorizationService.java`
- **Responsibility:** Authorize `/api/admin/**` by checking `profiles.is_admin = true` via a narrow `JdbcTemplate` query (no dependency on `identity`).
- **Key methods:** `isAdmin(Authentication)` — resolve principal UUID then check; `isAdmin(UUID)` — true iff profile row has `is_admin = true`.

### RateLimiter
- **FQN:** `com.cramer.platform.ratelimit.RateLimiter` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/platform/ratelimit/RateLimiter.java`
- **Responsibility:** In-memory per-user, per-endpoint token-bucket limiter (Bucket4j). `grading` 5/min, `profile`/`auth` 10/min, default 60/min. Non-distributed (per-instance).
- **Key methods:** `tryConsume(UUID, String endpointType)` → boolean. Constants: `GRADING`, `PROFILE`, `AUTH`.

### DeepSeekClient
- **FQN:** `com.cramer.platform.integration.llm.DeepSeekClient` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/platform/integration/llm/DeepSeekClient.java`
- **Responsibility:** Thin client for DeepSeek's OpenAI-compatible `/chat/completions` (JSON-object mode). No business logic. Failures → `UpstreamServiceException` (503).
- **Key methods:** `isConfigured()`; `chatJson(model, systemPrompt, userPrompt, temperature, maxTokens)` → parsed `JsonNode` content.
- **Used by:** `writing.WritingGradingService`, `engagement.ChatService`, `engagement.VocabularyService`.

### OpenRouterClient
- **FQN:** `com.cramer.platform.integration.openrouter.OpenRouterClient` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/platform/integration/openrouter/OpenRouterClient.java`
- **Responsibility:** OpenRouter chat-completions + models client: JSON-schema-constrained non-streaming calls, true SSE streaming with cooperative cancellation, model listing. Errors normalized to `OpenRouterException` with a `retryable` flag.
- **Key methods:** `isConfigured()`; `chat(OpenRouterChatRequest)` → `OpenRouterChatResult`; `streamChat(request, OpenRouterStreamListener, BooleanSupplier cancelled)`; `listModels()`.
- **Used by:** abts generation/refinement, `speaking.grading.SpeakingGradingWorker`, `admin.AdminDashboardService`, `abts.ModelCatalogService`.

### SupabaseAdminClient
- **FQN:** `com.cramer.platform.integration.supabase.SupabaseAdminClient` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/platform/integration/supabase/SupabaseAdminClient.java`
- **Responsibility:** Thin client for the Supabase Auth Admin API (service-role key). Any transport/non-2xx → `UpstreamServiceException` (503).
- **Key methods:** `listUsersPage(int page, int perPage)` → `JsonNode` (`{ "users": [...] }`).
- **Used by:** `identity.EmailLookupService`.

### StartupValidator
- **FQN:** `com.cramer.platform.config.StartupValidator` (`@Component`, `ApplicationRunner`)
- **File:** `backend/src/main/java/com/cramer/platform/config/StartupValidator.java`
- **Responsibility:** Log a one-line config presence summary at startup (db / supabaseAdmin / deepSeek / openRouter); warn on missing datasource URL. Never logs secrets.
- **Key methods:** `run(ApplicationArguments)`.

---

## identity

Supabase-JWT auth + self-service profile.

### ProfileService
- **FQN:** `com.cramer.identity.service.ProfileService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/identity/service/ProfileService.java`
- **Responsibility:** Self-service profile read/update with IDOR guard (requester must equal target, else 403); missing profile → 404.
- **Key methods:** `getProfile(UUID requesterId, UUID targetId)`; `updateProfile(UUID requesterId, UUID targetId, UpdateProfileRequest)` (handles avatar/background URLs and LLM model/provider/api-key clear semantics).
- **Collaborators:** `ProfileRepository`.

### EmailLookupService
- **FQN:** `com.cramer.identity.service.EmailLookupService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/identity/service/EmailLookupService.java`
- **Responsibility:** Email-existence check over the Supabase Auth Admin API; paginates (50/page, max 200 pages) and matches case-insensitively. Propagates upstream failure (never fabricates `exists:false`).
- **Key methods:** `emailExists(String email)` → boolean.
- **Collaborators:** `platform.SupabaseAdminClient`.

---

## catalog

`test_sets → tests → sections → questions` + hashtags; admin CRUD, course browse, and the published content ports.

### TestSetService
- **FQN:** `com.cramer.catalog.service.TestSetService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/TestSetService.java`
- **Responsibility:** Admin CRUD + publish + reorder for test sets.
- **Key methods:** `listAll`, `getById`, `getByCode`, `create(CreateTestSetRequest, UUID)`, `update(Long, CreateTestSetRequest)`, `delete(Long)`, `publish(Long, boolean)`, `reorder(List<Long>)`.
- **Collaborators:** `TestSetRepository`, `TestRepository`.

### TestAdminService
- **FQN:** `com.cramer.catalog.service.TestAdminService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/TestAdminService.java`
- **Responsibility:** Admin operations on tests: create (auto `test_number = max+1`), update, publish/unpublish with section status cascade, duplicate (metadata + hashtags), hashtag replacement, guarded delete.
- **Key methods:** `listBySet`, `getById`, `lookup(setCode, testNumber)`, `create`, `update`, `setPublished(Long, boolean)`, `duplicate(Long, boolean includeSections)`, `replaceHashtags(Long, List<String>)`, `delete(Long, boolean force)`, `sectionsForTest(Long, String skill)`.
- **Collaborators:** `TestRepository`, `SectionRepository`, `TestHashtagRepository`, `HashtagService`, `TestSetRepository`, `ObjectProvider<TestDependencyGuard>`.

### TestDeliveryService
- **FQN:** `com.cramer.catalog.service.TestDeliveryService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/TestDeliveryService.java`
- **Responsibility:** Build **answer-free** test payloads for the test-taking UI; never reads `correct_answer`/`explanation` (answer-key safety enforced by construction).
- **Key methods:** `getTestData(String examSource, int testNumber, Skill)` → `List<TestSectionView>`.
- **Collaborators:** `SectionRepository`, `QuestionRepository`.

### SectionService
- **FQN:** `com.cramer.catalog.service.SectionService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/SectionService.java`
- **Responsibility:** Admin section CRUD; deleting a section removes its questions.
- **Key methods:** `get`, `create(SectionRequest)`, `update(Long, SectionRequest)`, `delete(Long)`.
- **Collaborators:** `SectionRepository`, `QuestionRepository`.

### QuestionService
- **FQN:** `com.cramer.catalog.service.QuestionService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/QuestionService.java`
- **Responsibility:** Admin question CRUD; admin views include answer key + explanation (delivery never does).
- **Key methods:** `get`, `listBySection(Long)`, `create(QuestionRequest)`, `update(Long, QuestionRequest)`, `delete(Long)`.
- **Collaborators:** `QuestionRepository`.

### HashtagService
- **FQN:** `com.cramer.catalog.service.HashtagService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/HashtagService.java`
- **Responsibility:** Hashtag management; soft delete (`is_active=false`); `findOrCreateByCodes` creates missing as category `topic` (max 20/test).
- **Key methods:** `listActive`, `byCategory`, `search`, `popular(int)`, `categories`, `codesByIds(List<Long>)`, `create`, `update`, `softDelete(Long)`, `findOrCreateByCodes(List<String>)`.
- **Collaborators:** `HashtagRepository`.

### CourseQueryService
- **FQN:** `com.cramer.catalog.service.CourseQueryService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/CourseQueryService.java`
- **Responsibility:** Read-only course browsing for end users; only published content is visible.
- **Key methods:** `listCourses(page, size, search)` → `PageResponse<String>`; `listPublishedSets`; `testsForCourse(String)`; `setDetails(String code)`.
- **Collaborators:** `SectionRepository`, `TestSetRepository`.

### ContentLookupService — implements `ContentLookupPort`
- **FQN:** `com.cramer.catalog.service.ContentLookupService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/ContentLookupService.java`
- **Responsibility:** Sole catalog-internal implementation of the published content port; consumers see records only.
- **Key methods:** `sectionsForTest(long, Skill)`, `sectionsForExam(String, int, Skill)`, `gradableQuestions(long sectionId)`, `reviewContent(String, int, Skill)`, `totalQuestions(String, int, Skill)`, `speakingBank(long testId, int partNumber)`.
- **Collaborators:** `SectionRepository`, `QuestionRepository`.

### ContentDraftService — implements `ContentDraftPort`
- **FQN:** `com.cramer.catalog.service.ContentDraftService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/catalog/service/ContentDraftService.java`
- **Responsibility:** Persist generated content as **draft**: resolve/create set + test, upsert each section by `(test_id, skill, part_number)`, replace its questions. Never publishes; computes `question_uid`.
- **Key methods:** `saveDraft(SaveDraftCommand)` → `SaveDraftResult`.
- **Collaborators:** `TestSetRepository`, `TestRepository`, `SectionRepository`, `QuestionRepository`.

### Ports & SPIs (catalog)
- **`ContentLookupPort`** — `…catalog/service/ContentLookupPort.java` — published content-resolution contract (incl. answer-bearing `gradableQuestions` for server-side scoring only).
- **`ContentDraftPort`** — `…catalog/service/ContentDraftPort.java` — published draft-persistence contract; carries the `SaveDraftCommand`/`DraftSection`/`DraftQuestion`/`SaveDraftResult` records.
- **`TestDependencyGuard`** — `…catalog/service/TestDependencyGuard.java` — inbound SPI; `hasUserData(long testId)`; implemented by assessment.

---

## assessment

`test_attempts`, `user_answers`, scoring, review. Owns the shared attempt shell.

### AttemptService
- **FQN:** `com.cramer.assessment.service.AttemptService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/assessment/service/AttemptService.java`
- **Responsibility:** Attempt lifecycle for Reading/Listening (and shared shell): start/resume with row-lock + quota, save progress, submit + grade, cancel (idempotent), regrade, delete. Reading/Listening charge at start; Writing bills at grading time.
- **Key methods:** `start(source, testNumber, skill, UUID, boolean forceNew)`, `saveProgress(Long, UUID, SaveProgressRequest)`, `submit(Long, UUID, SubmitAnswersRequest)` → `AttemptResultResponse`, `regrade(Long, UUID)`, `cancel(Long, UUID)`, `resume(Long, UUID)`, `delete(Long, UUID)`, `getAnswers(Long, UUID)`.
- **Collaborators:** `AttemptRepository`, `UserAnswerRepository`, `ContentLookupPort`, `ScoringService`, `AttemptBillingPort`, `ObjectProvider<AttemptCleanupParticipant>`.

### ScoringService
- **FQN:** `com.cramer.assessment.service.ScoringService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/assessment/service/ScoringService.java`
- **Responsibility:** Pure, stateless objective grading. Normalizes (underscore→space, trim, lowercase, collapse whitespace); multi-select compared as unordered sets.
- **Key methods:** `isCorrect(QuestionType, JsonNode correctAnswer, String userValue)` → boolean.

### AttemptReviewService
- **FQN:** `com.cramer.assessment.service.AttemptReviewService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/assessment/service/AttemptReviewService.java`
- **Responsibility:** Build owner-only attempt review: attempt + score overlaid with authored content (passage, correct answers, explanations). Only user-facing surface exposing answer keys.
- **Key methods:** `review(Long attemptId, UUID userId)` → `AttemptReviewView`.
- **Collaborators:** `AttemptRepository`, `UserAnswerRepository`, `ContentLookupPort`.

### AttemptWriteBackService — implements `AttemptWriteBackPort`
- **FQN:** `com.cramer.assessment.service.AttemptWriteBackService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/assessment/service/AttemptWriteBackService.java`
- **Responsibility:** The only place outside the attempt lifecycle that mutates `test_attempts`; lets writing complete an attempt (and cancel siblings) without touching the table.
- **Key methods:** `completeForGrading(long, UUID)` → `AttemptContext`; `requireOwnedContext(long, UUID)` → `AttemptContext`.
- **Collaborators:** `AttemptRepository`.

### AttemptTestDependencyGuard — implements catalog `TestDependencyGuard`
- **FQN:** `com.cramer.assessment.service.AttemptTestDependencyGuard` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/assessment/service/AttemptTestDependencyGuard.java`
- **Responsibility:** Lets catalog block destroying a test that has user data; resolves via `user_answers → questions → sections.test_id`.
- **Key methods:** `hasUserData(long testId)` → boolean.
- **Collaborators:** `UserAnswerRepository`.

### Ports & SPIs (assessment)
- **`AttemptWriteBackPort`** — `…assessment/service/AttemptWriteBackPort.java` — published; carries the `AttemptContext` record.
- **`AttemptCleanupParticipant`** — `…assessment/service/AttemptCleanupParticipant.java` — inbound SPI; `beforeAttemptDeletion(long attemptId)`; implemented by writing.

---

## writing

`writing_submissions` + async DeepSeek grading.

### WritingSubmissionService
- **FQN:** `com.cramer.writing.service.WritingSubmissionService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingSubmissionService.java`
- **Responsibility:** Writing submission lifecycle: draft save, submit (1–2 essays), regrade, status, review. Delegates attempt completion to `AttemptWriteBackPort`; dispatches grading **after commit**; rate-limited.
- **Key methods:** `saveDraft(long, int, String, UUID)`, `submit(long, Map<Integer,String>, UUID)` → `WritingStatusResponse`, `regrade(long, UUID)`, `status(long, UUID)`, `review(long, UUID)` → `WritingReviewView`, `rawSubmissions(long, UUID)`.
- **Collaborators:** `WritingSubmissionRepository`, `WritingGradingDispatcher`, `WritingBandCalculator`, `ContentLookupPort`, `AttemptWriteBackPort`, `RateLimiter`.

### WritingGradingDispatcher *(async worker)*
- **FQN:** `com.cramer.writing.service.WritingGradingDispatcher` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingGradingDispatcher.java`
- **Responsibility:** Asynchronously grade all tasks in parallel; if any fails, mark all `FAILED` and **bill nothing**; on full success charge once via `UsageBillingPort`. A post-grade billing error never hides a successful grade (logged for reconciliation).
- **Key methods:** `@Async gradeAttempt(long, UUID, String examSource, String testNumber)`; `gradeOne(long, Map<Integer,TaskPrompt>)` (own TX); `markAll(long, WritingStatus)`.
- **Collaborators:** `WritingSubmissionRepository`, `WritingGradingService`, `ContentLookupPort`, `UsageBillingPort`, `WritingAsyncConfig.EXECUTOR`.

### WritingGradingService
- **FQN:** `com.cramer.writing.service.WritingGradingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingGradingService.java`
- **Responsibility:** Grade a single essay: local shortcuts (empty→0, <20 words→1) without an API call; otherwise call DeepSeek and **recompute** overall band from criterion scores (model's own overall is never trusted).
- **Key methods:** `isAvailable()`; `grade(int taskNumber, String essay, String promptText, String imageDescription, String model)` → `GradingOutcome`; `parse(JsonNode)` (package-visible).
- **Collaborators:** `platform.DeepSeekClient`, `WritingBandCalculator`, `WritingPromptBuilder`.

### WritingBandCalculator
- **FQN:** `com.cramer.writing.service.WritingBandCalculator` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingBandCalculator.java`
- **Responsibility:** Pure IELTS Writing band math: per-task overall (avg of 4 criteria, rounded 0.5; ignores any `overall*` key), weighted overall (Task1·⅓ + Task2·⅔), local shortcut bands, word count.
- **Key methods:** `overallFromBandScores(JsonNode)`, `weightedOverall(Double, Double)`, `localShortcutBand(String)` → `OptionalDouble`, `countWords(String)`.

### WritingPromptBuilder
- **FQN:** `com.cramer.writing.service.WritingPromptBuilder` *(final utility, static methods — not a bean)*
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingPromptBuilder.java`
- **Responsibility:** Build the DeepSeek grading prompt (text-only; images via `imageDescription`); strict JSON output; Task 1 ≥150 words, Task 2 ≥250.
- **Key methods:** `static system()`, `static user(int taskNumber, String essay, String promptText, String imageDescription)`.

### WritingAttemptCleanup — implements `AttemptCleanupParticipant`
- **FQN:** `com.cramer.writing.service.WritingAttemptCleanup` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/writing/service/WritingAttemptCleanup.java`
- **Responsibility:** On attempt cancel/delete, remove the attempt's `writing_submissions` (no FK orphans).
- **Key methods:** `beforeAttemptDeletion(long attemptId)`.
- **Collaborators:** `WritingSubmissionRepository`.

*Supporting record:* `GradingOutcome` (`…writing/service/GradingOutcome.java`) — `(BigDecimal overallBand, JsonNode bandScores, JsonNode aiFeedback)`.

---

## speaking

Sessions, frozen blueprint, transcripts, async grading worker/watchdog, admin regrade.

### SpeakingSessionService
- **FQN:** `com.cramer.speaking.service.SpeakingSessionService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/speaking/service/SpeakingSessionService.java`
- **Responsibility:** Session lifecycle: create (build blueprint + Lúa check, no deduct), transcript upsert (tamper-proof vs. frozen blueprint), complete (gate on all turns + deduct + dispatch grading after commit), abandon (no charge), results/status/history.
- **Key methods:** `create(UUID, CreateSessionRequest)`, `get(UUID, long)`, `saveTranscript(UUID, long, SaveTranscriptRequest)`, `complete(UUID, long, Integer durationSeconds)`, `abandon(UUID, long)`, `results(UUID, long)`, `gradingStatus(UUID, long)`, `history(UUID, int, int, String status)`.
- **Collaborators:** `SpeakingSessionRepository`, `SpeakingTranscriptRepository`, `SpeakingBlueprintService`, `SpeakingBillingPort`, `SpeakingSessionProperties`, `ObjectProvider<SpeakingGradingTrigger>`.

### SpeakingBlueprintService
- **FQN:** `com.cramer.speaking.service.SpeakingBlueprintService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/speaking/service/SpeakingBlueprintService.java`
- **Responsibility:** Build the frozen session blueprint (`schemaVersion, testId, sessionMode, accent, speed, parts[]` with `turns[]`) using a heuristic planner over the published speaking bank.
- **Key methods:** `build(long testId, String sessionMode, String accent, String speed)` → `ObjectNode`.
- **Collaborators:** `ContentLookupPort`, `SpeakingSessionProperties`.

### AdminSpeakingService
- **FQN:** `com.cramer.speaking.service.AdminSpeakingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/speaking/service/AdminSpeakingService.java`
- **Responsibility:** Admin regrade: reset a session to a claimable `completed` state, clear attempts/last-error, and write a `SPEAKING_REGRADE` audit entry. Allowed from `grading_failed`, or `graded` with `force`.
- **Key methods:** `regrade(UUID adminId, long sessionId, String mode, boolean force, String reason)` → `long` (session id for post-commit dispatch).
- **Collaborators:** `SpeakingSessionRepository`, `AuditPort`.

### SpeakingGradingStore
- **FQN:** `com.cramer.speaking.grading.SpeakingGradingStore` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/speaking/grading/SpeakingGradingStore.java`
- **Responsibility:** Short transactional boundaries (row-locked) so the slow OpenRouter call runs outside any TX. Claim `completed→grading`; finish success (persist bands) or fail (`grading→grading_failed` + Lúa refund).
- **Key methods:** `claim(long)` → boolean, `load(long)`, `finishSuccess(long, SpeakingGradingResult, JsonNode rawResult)`, `finishFailure(long, String error)`.
- **Collaborators:** `SpeakingSessionRepository`, `SpeakingBillingPort`.

### SpeakingGradingWorker *(async grading worker)*
- **FQN:** `com.cramer.speaking.grading.SpeakingGradingWorker` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/speaking/grading/SpeakingGradingWorker.java`
- **Responsibility:** End-to-end grade: claim → build text-only prompt → call OpenRouter (default `google/gemini-2.5-flash`) up to 2 attempts → validate bands → persist or fail+refund. Audio multimodal not wired (text-only).
- **Key methods:** `grade(long sessionId)`.
- **Collaborators:** `SpeakingGradingStore`, `SpeakingTranscriptRepository`, `SpeakingGradingPromptBuilder`, `platform.OpenRouterClient`.

### SpeakingGradingDispatcher — implements `SpeakingGradingTrigger` *(async)*
- **FQN:** `com.cramer.speaking.grading.SpeakingGradingDispatcher` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/speaking/grading/SpeakingGradingDispatcher.java`
- **Responsibility:** Enqueue grading on `speakingGradingExecutor` so a completed session is graded off-thread.
- **Key methods:** `@Async("speakingGradingExecutor") enqueue(long sessionId)`.

### SpeakingGradingWatchdog *(scheduled)*
- **FQN:** `com.cramer.speaking.grading.SpeakingGradingWatchdog` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/speaking/grading/SpeakingGradingWatchdog.java`
- **Responsibility:** Every ~60s, fail+refund `grading` sessions stale >10 min and re-enqueue `completed` sessions stale >5 min (lost dispatches). Guarded by `speaking.evaluation.enabled`.
- **Key methods:** `@Scheduled sweep()`.
- **Collaborators:** `SpeakingSessionRepository`, `SpeakingGradingStore`, `SpeakingGradingWorker`.

### SpeakingGradingPromptBuilder
- **FQN:** `com.cramer.speaking.grading.SpeakingGradingPromptBuilder` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/speaking/grading/SpeakingGradingPromptBuilder.java`
- **Responsibility:** Build the Speaking grading prompt from blueprint + transcripts; text-only mode (pronunciation graded conservatively); strict JSON shape.
- **Key methods:** `systemPrompt()`, `userPrompt(SpeakingSession, List<SpeakingTranscript>)`.

### Helpers / Ports (speaking)
- **`SpeakingInputs`** — `…speaking/service/SpeakingInputs.java` *(final util)* — normalize/validate `mode`, `accent`, `speed`. Methods: `normalizeMode`, `normalizeAccent`, `normalizeSpeed`.
- **`AudioStoragePath`** — `…speaking/service/AudioStoragePath.java` *(final util)* — validate a relative audio object key. Methods: `isValid(String)`, `require(String)`.
- **`SpeakingGradingTrigger`** — `…speaking/service/SpeakingGradingTrigger.java` — seam; `enqueue(long sessionId)`.
- *Supporting record:* `SpeakingGradingResult` (`…speaking/grading/SpeakingGradingResult.java`) — parsed grade with `bandsValid()` and per-criterion band accessors.

---

## billing

Subscription, credit (Lúa), quota, payment (PayOS), feature gating, schedulers.

### SubscriptionService
- **FQN:** `com.cramer.billing.service.SubscriptionService` (`@Service`, `@Transactional`)
- **File:** `backend/src/main/java/com/cramer/billing/service/SubscriptionService.java`
- **Responsibility:** Subscription lifecycle; resolves the active subscription, auto-creating a free `cramerie` tier (and granting initial Lúa once). Activate paid / admin tier change with reset counters.
- **Key methods:** `getOrCreateActive(UUID)`, `tierOf`, `isPremium`, `save`, `listTiers`, `getTierByCode`, `gradingsRemaining`, `chatLimit`, `setAiGrading(UUID, boolean)`, `activatePaid(UUID, SubscriptionTier, String paymentReference)`, `adminSetTier(UUID, SubscriptionTier, int months)`.
- **Collaborators:** `UserSubscriptionRepository`, `SubscriptionTierRepository`, `UserCreditRepository`, `CreditService`.

### CreditService
- **FQN:** `com.cramer.billing.service.CreditService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/CreditService.java`
- **Responsibility:** Lúa balance management. Atomic (`PESSIMISTIC_WRITE` lock on `user_credits`), idempotent by `(user, reference, category)`, spend rejects on insufficient balance (402).
- **Key methods:** `balance`, `stats`, `transactions(UUID, Pageable)`, `hasTransaction(UUID, String, CreditCategory)`, `earn(...)`, `spend(...)`, `refund(...)` (each → `CreditResult`).
- **Collaborators:** `UserCreditRepository`, `CreditTransactionRepository`.

### QuotaService
- **FQN:** `com.cramer.billing.service.QuotaService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/billing/service/QuotaService.java`
- **Responsibility:** Tier-aware quota status reporting and a read-only attempt pre-check (no counter mutation).
- **Key methods:** `status(UUID)` → `QuotaStatusView`; `canAttempt(UUID, String skill, boolean ai)` → `CanAttemptView`.
- **Collaborators:** `SubscriptionService`, `UserQuotaRepository`, `CreditService`.

### PaymentService
- **FQN:** `com.cramer.billing.service.PaymentService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/PaymentService.java`
- **Responsibility:** PayOS payment orders + grant. Concurrency-safe idempotent grant (row-lock + `PENDING→PAID` before granting). DB-driven Lúa packs; subscription grants delegate to `SubscriptionService`. Mock checkout URL when PayOS unconfigured.
- **Key methods:** `payosConfigured()`, `createSubscriptionOrder(UUID, Long, String)`, `createLuaOrder(UUID, String packCode)`, `getStatus(long, UUID)`, `history(UUID, Pageable)`, `grantOnSuccess(long orderCode)` → boolean.
- **Collaborators:** `PaymentOrderRepository`, `LuaPackService`, `CreditService`, `SubscriptionService`, `SubscriptionTierRepository`, `PayOsProperties`.

### LuaPackService
- **FQN:** `com.cramer.billing.service.LuaPackService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/billing/service/LuaPackService.java`
- **Responsibility:** DB-driven Lúa pack catalog (no hardcoded packs).
- **Key methods:** `listActive()` → `List<LuaPackView>`; `requireActiveByCode(String)` → `LuaPack`.
- **Collaborators:** `LuaPackRepository`.

### FeatureAccessService — implements `FeatureAccessPort`
- **FQN:** `com.cramer.billing.service.FeatureAccessService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/FeatureAccessService.java`
- **Responsibility:** Feature gating; single parser handles both `features` JSON shapes (array and object); free-tier defaults.
- **Key methods:** `hasFeature(UUID, String)`, `isPremium(UUID)`; `static featureEnabled(JsonNode, String, boolean premium)`.
- **Collaborators:** `SubscriptionService`.

### AttemptBillingService — implements `AttemptBillingPort`
- **FQN:** `com.cramer.billing.service.AttemptBillingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/AttemptBillingService.java`
- **Responsibility:** Charge attempt start; premium not charged; free users charged tier overage once both global and per-skill monthly caps are exceeded (under pessimistic locks).
- **Key methods:** `chargeAttemptStart(UUID, Skill, String referenceId)`.
- **Collaborators:** `SubscriptionService`, `UserQuotaRepository`, `SkillQuotaRepository`, `CreditService`.

### UsageBillingService — implements `UsageBillingPort`
- **FQN:** `com.cramer.billing.service.UsageBillingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/UsageBillingService.java`
- **Responsibility:** AI-grading billing; premium allowance grades free (counter++), else tier AI overage (canonical 20 Lúa), charged after success, idempotent; refund reverses an actual charge only.
- **Key methods:** `canGrade(UUID)`, `chargeAiGrading(UUID, String)`, `refund(UUID, String)`.
- **Collaborators:** `SubscriptionService`, `CreditService`.

### SpeakingBillingService — implements `SpeakingBillingPort`
- **FQN:** `com.cramer.billing.service.SpeakingBillingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/SpeakingBillingService.java`
- **Responsibility:** Speaking afford/deduct/refund, idempotent by session id (`session_{id}` / `refund_session_{id}`).
- **Key methods:** `canAfford(UUID, int)`, `deduct(UUID, long, int)`, `refund(UUID, long, int)`.
- **Collaborators:** `CreditService`.

### ChatBillingService — implements `ChatBillingPort`
- **FQN:** `com.cramer.billing.service.ChatBillingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/ChatBillingService.java`
- **Responsibility:** Chat billing; monthly `chatbot_used` vs. tier limit (`<0` = unlimited); within allowance → counter++, over → tier overage (`CHAT_EXTENSION`), charged after reply.
- **Key methods:** `canChat(UUID)`, `remaining(UUID)`, `chargeChat(UUID, String)`.
- **Collaborators:** `SubscriptionService`, `CreditService`.

### TranslationBillingService — implements `TranslationBillingPort`
- **FQN:** `com.cramer.billing.service.TranslationBillingService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/billing/service/TranslationBillingService.java`
- **Responsibility:** Translation billing; monthly `translation_usage` vs. tier limit; over allowance → tier overage (`VOCABULARY_TRANSLATION`), charged after success.
- **Key methods:** `canTranslate(UUID)`, `chargeTranslation(UUID, String)`.
- **Collaborators:** `SubscriptionService`, `TranslationUsageRepository`, `CreditService`.

### MonthlyResetScheduler *(scheduled)*
- **FQN:** `com.cramer.billing.service.MonthlyResetScheduler` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/billing/service/MonthlyResetScheduler.java`
- **Responsibility:** Monthly (1st 00:10 Asia/Ho_Chi_Minh) reset of subscription counters + grant tier `monthly_lua_bonus` (idempotent).
- **Key methods:** `@Scheduled resetMonthlyCounters()`.
- **Collaborators:** `UserSubscriptionRepository`, `SubscriptionTierRepository`, `CreditService`.

### SubscriptionExpiryScheduler *(scheduled)*
- **FQN:** `com.cramer.billing.service.SubscriptionExpiryScheduler` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/billing/service/SubscriptionExpiryScheduler.java`
- **Responsibility:** Daily (00:05 Asia/Ho_Chi_Minh) flip expired `ACTIVE` subscriptions to `EXPIRED`.
- **Key methods:** `@Scheduled expireSubscriptions()`.
- **Collaborators:** `UserSubscriptionRepository`.

### Ports (billing)
`AttemptBillingPort`, `UsageBillingPort`, `SpeakingBillingPort`, `ChatBillingPort`,
`TranslationBillingPort`, `FeatureAccessPort` — all under `…billing/service/`.
*Supporting record:* `CreditResult` (`…billing/service/CreditResult.java`) — `(int balanceAfter, boolean applied, boolean duplicate)` with factories `applied(...)` / `duplicate(...)`.

---

## engagement

Chat, vocabulary/translation, dashboard read-models, activity.

### ChatService
- **FQN:** `com.cramer.engagement.service.ChatService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/engagement/service/ChatService.java`
- **Responsibility:** IELTS assistant chat (Vietnamese system prompt); billed after a successful reply; persists both turns only on success.
- **Key methods:** `chat(UUID, String)` → `ChatResponse`, `history(UUID, int)`, `remaining(UUID)`, `clearHistory(UUID)`.
- **Collaborators:** `ChatMessageRepository`, `platform.DeepSeekClient`, `ChatBillingPort`.

### VocabularyService
- **FQN:** `com.cramer.engagement.service.VocabularyService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/engagement/service/VocabularyService.java`
- **Responsibility:** Vocabulary notebook CRUD with per-user duplicate prevention, mastery toggle, stats, and AI translation billed after success.
- **Key methods:** `list(UUID, page, size, search, filter)`, `get`, `create`, `update`, `delete`, `toggleMastered`, `stats`, `translate(UUID, String word)` → `TranslationView`.
- **Collaborators:** `VocabularyRepository`, `platform.DeepSeekClient`, `TranslationBillingPort`.

### DashboardService
- **FQN:** `com.cramer.engagement.service.DashboardService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/engagement/service/DashboardService.java`
- **Responsibility:** Dashboard read-model assembled read-only via `JdbcTemplate` over `test_attempts`/`user_answers`/`profiles`/`user_activities`; derives R/L band via `BandScale`. No cross-module writes.
- **Key methods:** `summary(UUID, page, size, search)` → `SummaryView`; `courseHistory(UUID, examSource, testNumber, skill)`.
- **Collaborators:** `JdbcTemplate`, `TargetService`.

### TargetService
- **FQN:** `com.cramer.engagement.service.TargetService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/engagement/service/TargetService.java`
- **Responsibility:** IELTS goal management — one `Target` per user, upserted.
- **Key methods:** `current(UUID)` → `Optional<TargetView>`; `upsert(UUID, TargetRequest)`.
- **Collaborators:** `TargetRepository`.

### ActivityService — implements `ActivityPort`
- **FQN:** `com.cramer.engagement.service.ActivityService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/engagement/service/ActivityService.java`
- **Responsibility:** Owner of the activity timeline (`user_activities`); other modules log through the port.
- **Key methods:** `log(UUID, String activityType, String title, String description, JsonNode metadata)`.
- **Collaborators:** `UserActivityRepository`.

### Port (engagement)
- **`ActivityPort`** — `…engagement/service/ActivityPort.java` — `log(UUID, String, String, String, JsonNode)`.

---

## admin

Cross-domain console (users, audit, dashboard, finance). Reads use `JdbcTemplate` projections; writes go through billing services + audit/activity trails.

### AdminUserService
- **FQN:** `com.cramer.admin.service.AdminUserService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/admin/service/AdminUserService.java`
- **Responsibility:** Admin user management; read-only projections across `profiles`/`user_subscriptions`/`subscription_tiers`/`user_credits`; writes via `CreditService` (`ADMIN_ADJUSTMENT`) and `SubscriptionService`, plus audit + activity.
- **Key methods:** `listUsers(...)`, `userDetail(UUID)`, `userStats()`, `setStatus(UUID adminId, UUID userId, String, String)`, `adjustCredits(UUID, UUID, int, String)` → balance, `setSubscription(UUID, UUID, String tierCode, int months)`.
- **Collaborators:** `JdbcTemplate`, `CreditService`, `SubscriptionService`, `AuditPort`, `ActivityPort`.

### AdminFinanceService
- **FQN:** `com.cramer.admin.service.AdminFinanceService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/admin/service/AdminFinanceService.java`
- **Responsibility:** Finance projections; consistent source (`payment_orders.status='PAID'`, `amount_vnd`); whitelisted period clauses.
- **Key methods:** `overview(String period)`, `breakdown(String period)`, `topSpenders(int limit)`, `transactions(status, type, page, size)`.
- **Collaborators:** `JdbcTemplate`.

### AdminDashboardService
- **FQN:** `com.cramer.admin.service.AdminDashboardService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/admin/service/AdminDashboardService.java`
- **Responsibility:** Dashboard projections from live tables; system status reflects DB reachability + integration config.
- **Key methods:** `stats()`, `recentActivities(int)`, `systemStatus()`.
- **Collaborators:** `JdbcTemplate`, `billing.PaymentService`, `platform.OpenRouterClient`.

### AdminActivityService
- **FQN:** `com.cramer.admin.service.AdminActivityService` (`@Service`, `@Transactional(readOnly)`)
- **File:** `backend/src/main/java/com/cramer/admin/service/AdminActivityService.java`
- **Responsibility:** Admin audit reads (`admin_audit_log` repo) + user-activity projections (`user_activities`).
- **Key methods:** `auditLog(page, size)`, `userAudit(UUID, page, size)`, `userActivities(UUID, type, page, size)`, `userActivitiesRecent(UUID, limit)`.
- **Collaborators:** `AdminAuditLogRepository`, `JdbcTemplate`.

### AuditService — implements `AuditPort`
- **FQN:** `com.cramer.admin.service.AuditService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/admin/service/AuditService.java`
- **Responsibility:** Owner of the audit trail (`admin_audit_log`); other modules log admin actions through the port.
- **Key methods:** `record(UUID adminUserId, String action, String targetType, String targetId, String description, JsonNode oldValue, JsonNode newValue)`.
- **Collaborators:** `AdminAuditLogRepository`.

### Port (admin)
- **`AuditPort`** — `…admin/service/AuditPort.java` — `record(UUID, String, String, String, String, JsonNode, JsonNode)`.

---

## abts

AI generation over OpenRouter: model catalog, prompting, generation/refinement pipelines, validation, SSE streaming, draft save.

### abts/service

#### AbtsSaveService
- **FQN:** `com.cramer.abts.service.AbtsSaveService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/service/AbtsSaveService.java`
- **Responsibility:** Translate a `SaveContentRequest` into catalog's `ContentDraftPort` draft contract; ABTS never touches catalog repositories.
- **Key methods:** `save(SaveContentRequest)` → `SaveContentResponse`.
- **Collaborators:** `catalog.ContentDraftPort`.

#### ModelCatalogService
- **FQN:** `com.cramer.abts.service.ModelCatalogService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/service/ModelCatalogService.java`
- **Responsibility:** OpenRouter `/models` catalog: fetch, 5-min cache, capability enrichment, curated fallback; validates default model at `ApplicationReadyEvent` (warn-only).
- **Key methods:** `listModels()` → `ArrayNode`, `capability(String modelId)`, `@EventListener validateDefaultModel()`.
- **Collaborators:** `platform.OpenRouterClient`, `ModelCapabilityRegistry`, `OpenRouterProperties`.

#### ModelCapabilityRegistry
- **FQN:** `com.cramer.abts.service.ModelCapabilityRegistry` (`@Component`)
- **File:** `backend/src/main/java/com/cramer/abts/service/ModelCapabilityRegistry.java`
- **Responsibility:** Map a model slug to its OpenRouter reasoning payload; enrich catalog nodes with capability descriptors; graceful degradation on non-reasoning models.
- **Key methods:** `supportsReasoning(String)`, `reasoningPayload(String, ModelConfig)`, `describe(JsonNode)` → `ObjectNode`.

#### TemplateService
- **FQN:** `com.cramer.abts.service.TemplateService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/service/TemplateService.java`
- **Responsibility:** Curated in-memory generation templates (no template tables in the frozen schema).
- **Key methods:** `categories()` → `ArrayNode`, `templates(String categoryId)` → `ArrayNode`.

### abts/generation

#### GenerationService
- **FQN:** `com.cramer.abts.generation.GenerationService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/generation/GenerationService.java`
- **Responsibility:** Orchestrate generation: resolve per-skill `PartGenerator`, run each part through a ≤3-attempt validate-retry loop (phases cached across attempts), renumber + merge multi-part, aggregate partial success/usage. Speaking → `NOT_IMPLEMENTED`.
- **Key methods:** `generate(Skill, GenerationRequest)`; `generate(Skill, GenerationRequest, StreamEmitter, BooleanSupplier cancelled, boolean streaming)`; `regenerateQuestions(Skill, GenerationRequest)`.
- **Collaborators:** `List<PartGenerator>`, `ContentValidator`, `QuestionRenumberer`, `ModelResolver`, `platform.OpenRouterClient`, `OpenRouterProperties`.

#### StreamingGenerationService
- **FQN:** `com.cramer.abts.generation.StreamingGenerationService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/generation/StreamingGenerationService.java`
- **Responsibility:** SSE streaming of generation/refinement on a bounded executor; emits lifecycle `StreamEvent`s; client disconnect flips a cancellation flag to stop token usage.
- **Key methods:** `stream(Skill, GenerationRequest)` → `SseEmitter`; `streamRefinement(RefinementRequest)` → `SseEmitter`.
- **Collaborators:** `GenerationService`, `RefinementService`, `platform.OpenRouterClient`, `abtsStreamingExecutor` (`ThreadPoolExecutor`), `AbtsProperties`.

#### RefinementService
- **FQN:** `com.cramer.abts.generation.RefinementService` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/generation/RefinementService.java`
- **Responsibility:** Targeted refinement: model proposes JSON patches for selected issues → applied to a copy → surfaced as diff `Hunk`s; accepted hunks applied on `/refine/apply`. Empty selection rejected; round cap via `abts.max-refinement-rounds`.
- **Key methods:** `refine(RefinementRequest, StreamEmitter, BooleanSupplier)` → `List<Hunk>`; `applyAccepted(RefinementApplyRequest)` → `RefinementApplyResponse`.
- **Collaborators:** `platform.OpenRouterClient`, `ModelResolver`, `ContentValidator`, `AbtsProperties`.

#### ModelResolver
- **FQN:** `com.cramer.abts.generation.ModelResolver` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/generation/ModelResolver.java`
- **Responsibility:** Single seam resolving the effective model slug (requested → configured default) and the reasoning payload.
- **Key methods:** `resolve(String requested)`, `reasoningPayload(String model, ModelConfig)`.
- **Collaborators:** `OpenRouterProperties`, `ModelCapabilityRegistry`.

#### Part generators — implement `PartGenerator`
- **`ReadingGenerator`** (`@Component`, `…generation/ReadingGenerator.java`) — two-phase: passage (cached) → questions. `skill()=READING`, `generatePart(int, PartConfig, GenerationContext)`. Uses `prompt.ReadingPromptBuilder`.
- **`ListeningGenerator`** (`@Component`, `…generation/ListeningGenerator.java`) — three-phase: transcript (cached) → stems+layout (cached) → answers (merged by `question_number`). Uses `prompt.ListeningPromptBuilder`.
- **`WritingGenerator`** (`@Component`, `…generation/WritingGenerator.java`) — three-phase: task (cached) → sample (cached) → band breakdown (merged into one section). Uses `prompt.WritingPromptBuilder`.
- **`PartGenerator`** (interface, `…generation/PartGenerator.java`) — `skill()`, `generatePart(int part, PartConfig, GenerationContext)`.

*Per-request collaborator (not a bean):* `GenerationContext` (`…generation/GenerationContext.java`) — phase runner with a phase cache (un-billed retries), token/reasoning accumulators, SSE emitter, cancellation flag. Key methods: `runPhase(...)`, `seedPhase(...)`, `checkCancelled()`, `usage()`, `reasoningText()`.

### abts/validation

#### ContentValidator
- **FQN:** `com.cramer.abts.validation.ContentValidator` (`@Service`)
- **File:** `backend/src/main/java/com/cramer/abts/validation/ContentValidator.java`
- **Responsibility:** Facade dispatching validation to the per-skill validator; Speaking rejected (out of ABTS scope).
- **Key methods:** `validate(Skill, int part, String taskType, JsonNode content)` → `ValidationResult`.
- **Collaborators:** `ReadingValidator`, `ListeningValidator`, `WritingValidator`.

#### Per-skill validators (`@Component`, pure/stateless)
- **`ReadingValidator`** (`…validation/ReadingValidator.java`) — `validate(JsonNode, int part)`: passage present, questions present/in-range/sequential/non-duplicate, allowed `QuestionType`s, ≥2 distinct types; short passage = warning.
- **`ListeningValidator`** (`…validation/ListeningValidator.java`) — `validate(JsonNode, int part)`: transcript/audio_placeholder/section_layout present, exactly 10 in-range questions, allowed interaction types, layout blocks with `question_numbers`.
- **`WritingValidator`** (`…validation/WritingValidator.java`) — `validate(JsonNode, String taskType)`: `task_prompt` + `word_requirement` required; per-type extras (Academic T1 `chart_data`, GT T1 `letter_context`, T2 `essay_metadata`); sample length = warning.

---

## Supporting Records & Value Types

Cross-module DTOs/value types (records) carried over `Port` boundaries (verified via constructor/usage in the services that produce/consume them):

| Record (FQN) | Shape (verified) |
| --- | --- |
| `catalog.service.SectionRef` | `(long sectionId, Long testId, String examSource, Integer testNumber, Skill skill, Integer partNumber)` |
| `catalog.service.GradableQuestion` | `(Long questionId, Integer questionNumber, QuestionType questionType, JsonNode correctAnswer)` |
| `catalog.service.ReviewSection` | `(Long sectionId, Integer partNumber, String passageText, String audioUrl, String displayContentUrl, JsonNode sectionLayout, String imageDescription, List<ReviewQuestion> questions)` |
| `catalog.service.ReviewQuestion` | `(Long questionId, Integer questionNumber, String questionUid, QuestionType questionType, JsonNode questionContent, JsonNode correctAnswer, JsonNode explanation)` |
| `catalog.service.SpeakingQuestionRef` | `(Long questionId, Integer partNumber, String questionUid, JsonNode questionContent)` |
| `assessment.service.AttemptWriteBackPort.AttemptContext` | `(long attemptId, UUID userId, String examSource, String testNumber, String skill, String status)` |
| `billing.service.CreditResult` | `(int balanceAfter, boolean applied, boolean duplicate)` |
| `writing.service.GradingOutcome` | `(BigDecimal overallBand, JsonNode bandScores, JsonNode aiFeedback)` |

> Field types above are inferred from the producing/consuming source (constructor calls + accessor
> usage). The `ContentDraftPort` request/result records (`SaveDraftCommand`, `DraftSection`,
> `DraftQuestion`, `SaveDraftResult`) are defined inline in that interface file.

---

## Per-Module Counts

Counts of service-layer components documented (service classes + workers/schedulers + helper utils),
with cross-module ports/SPIs listed separately.

| Module | Service-layer components | Ports / SPIs |
| --- | --- | --- |
| platform | 6 (`AdminAuthorizationService`, `RateLimiter`, `DeepSeekClient`, `OpenRouterClient`, `SupabaseAdminClient`, `StartupValidator`) | — |
| identity | 2 | — |
| catalog | 9 | 3 (`ContentLookupPort`, `ContentDraftPort`, `TestDependencyGuard`) |
| assessment | 5 | 2 (`AttemptWriteBackPort`, `AttemptCleanupParticipant`) |
| writing | 6 (incl. `WritingPromptBuilder` util) | — |
| speaking | 10 (service ×3 + util ×2 + grading ×5) | 1 (`SpeakingGradingTrigger`) |
| billing | 13 (11 services + 2 schedulers) | 6 (`AttemptBillingPort`, `UsageBillingPort`, `SpeakingBillingPort`, `ChatBillingPort`, `TranslationBillingPort`, `FeatureAccessPort`) |
| engagement | 5 | 1 (`ActivityPort`) |
| admin | 5 | 1 (`AuditPort`) |
| abts | 15 (service ×4 + generation ×4 incl. `ModelResolver` + generators ×3 + validation ×4) | `PartGenerator` (internal strategy interface) |

**Total:** 76 documented service-layer components + 14 port/SPI/seam interfaces (13 cross-module + `SpeakingGradingTrigger` intra-module seam); `PartGenerator` is an internal strategy interface counted separately.

> Counts exclude: `web/` controllers, `web/dto` records, `domain/` entities/enums, `repository/`
> interfaces, `config/` property holders, and pure value records (listed under *Supporting Records*).
