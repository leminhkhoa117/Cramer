# SPEC-02 — Naming Conventions

> Status: **Authoritative** · Depends on: SPEC-01
> Goal: names that are **navigable** and **role-explicit**. The old tree used short,
> ambiguous names (`TestService`, `TestSetService`, `TestManagementService`). The new tree
> encodes **domain + role** so any file's purpose is obvious from its name and package.

---

## 1. Packages

- Lowercase, single word per segment: `com.cramer.<module>.<layer>[.<sublayer>]`.
- Module names are nouns for bounded contexts: `catalog`, `assessment`, `writing`,
  `speaking`, `billing`, `engagement`, `identity`, `admin`, `abts`, `platform`.
- Layer segments are fixed: `web`, `web.dto`, `service`, `domain`, `repository`, `config`.

## 2. Class name = `[Qualifier]Role` (role is a suffix)

Every non-domain type ends in a **role suffix**. The qualifier disambiguates within a module.

| Role | Suffix | Example |
|------|--------|---------|
| REST controller | `Controller` | `AttemptController`, `AdminTestSetController` |
| Application service | `Service` | `ScoringService`, `WritingGradingService` |
| Published cross-module port | `Port` | `UsageBillingPort`, `ContentLookupPort` |
| Scheduled job | `Scheduler` | `SubscriptionExpiryScheduler` |
| Background worker | `Worker` | `SpeakingGradingWorker` |
| Spring Data repository | `Repository` | `QuestionRepository` |
| Inbound DTO | `Request` | `StartAttemptRequest`, `SubmitEssayRequest` |
| Outbound DTO | `Response` | `AttemptResultResponse` |
| Read-model / projection | `View` | `AttemptReviewView`, `CourseProgressView` |
| Mapper (entity↔dto) | `Mapper` | `QuestionMapper` |
| Config properties | `Properties` | `SpeakingSessionProperties` |
| Spring `@Configuration` | `Config` | `SpeakingAsyncConfig` |
| Custom exception | `Exception` | `QuotaExceededException` |
| Domain event | `Event` | `WritingSubmittedEvent` |
| Integration client | `Client` | `OpenRouterClient`, `DeepSeekClient` |
| Prompt builder | `PromptBuilder` | `ReadingPromptBuilder` |
| Validator | `Validator` | `ListeningContentValidator` |
| Enum | (no suffix) | `AttemptStatus`, `SpeakingSessionStatus` |
| JPA entity / aggregate | (no suffix) | `Attempt`, `Question`, `Subscription` |

**Entities and enums carry no role suffix** — their `domain` package already signals role,
and a clean name reads naturally in business logic (`attempt.status()`). This is the only
exception to "role suffix everywhere".

## 3. Qualifiers resolve the old ambiguity

Concrete renames (old → new) to eliminate vague/overlapping names:

| Old | New | Why |
|-----|-----|-----|
| `TestService` | `catalog.service.TestDeliveryService` | builds safe test-taking payloads |
| `TestSetService` | `catalog.service.TestSetService` | unchanged (already clear) |
| `TestManagementService` | `catalog.service.TestAdminService` | admin CRUD over tests |
| `QuestionService` (generic CRUD) | `catalog.service.QuestionService` | scoped to catalog |
| `TestAttemptService` | `assessment.service.AttemptService` | attempt lifecycle |
| (scoring inside attempt) | `assessment.service.ScoringService` | extracted scoring concern |
| `WritingSubmissionService` | `writing.service.WritingSubmissionService` | unchanged |
| `LLMGradingService` | `writing.service.WritingGradingService` | role explicit |
| `AsyncGradingService` | `writing.service.WritingGradingDispatcher` | async dispatch |
| `AdminContentService(Impl)` + `AdminContentOperations` | removed; folded into `catalog.service.*Admin*` | one typed API (SPEC-11) |
| `SubscriptionServiceImpl` | `billing.service.SubscriptionService` | drop reflexive Impl |
| `QuotaBillingServiceImpl` | `billing.service.AttemptBillingService` | role explicit |
| `ChatBillingServiceImpl` | `billing.service.ChatBillingService` | |
| `ProfileServiceOld` | deleted | dead code |
| `SupabaseClient` (unused) | deleted | dead code |
| `DebugController`, `DatabaseTestController`, `HelloController` | replaced by `platform.web.HealthController` | |

## 4. DTO naming

- Inbound: `<Verb><Noun>Request` — `StartAttemptRequest`, `CreateTestSetRequest`.
- Outbound: `<Noun>Response` — `PaymentResponse`. Use `Response` when it mirrors a command
  result; use `View` when it is a read projection of an entity/aggregate.
- Avoid the old flat `dto/` dumping ground and the `…V2DTO` explosion. Group DTOs under the
  owning module's `web/dto`. The Speaking grading result is a **schema-driven** type, not 24
  classes (SPEC-14).
- Never suffix DTOs with `DTO`. The `Request`/`Response`/`View` suffix is the contract marker.

## 5. Method naming

- Queries: `find…` (Optional/empty ok), `get…` (throws if absent), `list…` (collections),
  `count…`, `exists…`.
- Commands: imperative verbs — `start`, `submit`, `cancel`, `grade`, `charge`, `refund`,
  `publish`.
- Booleans: `is…`, `has…`, `can…`.

## 6. Test naming (mirrors module tree)

- Path: `src/test/java/com/cramer/<module>/…` mirroring main.
- Class: `<TypeUnderTest>Test` (unit), `<Controller>WebTest` (`@WebMvcTest`),
  `<Repository>DataTest` (`@DataJpaTest`), `<Port>ContractTest`.
- Method: `methodName_condition_expectedResult` or `@DisplayName` sentences.

## 7. Configuration property keys

- Kebab-case under a module root namespace: `speaking.session.lua-cost`,
  `billing.payos.checksum-key`, `abts.streaming.emitter-timeout`.
- Bound to a `…Properties` record with `@ConfigurationProperties(prefix = "…")`.

## 8. Resource files

- Prompt/schema resources under `src/main/resources/<module>/…`
  (e.g. `speaking/grading-schema.json`, `abts/prompt/reading-system.md`).

## 9. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
