# SPEC-01 — Architecture

> Status: **Authoritative** · Depends on: SPEC-00

---

## 1. Style: feature-based vertical slices

The backend is organized into **modules**, one per bounded context. Each module owns its
web layer, application services, domain types, and persistence. Modules are cohesive and
loosely coupled. There is **no global `controller/`, `service/`, `repository/`, `dto/`
folder** — those are the old layered layout we are leaving behind.

```
com.cramer
├── platform        # shared kernel (no business domain)
├── identity        # auth, profile, target
├── catalog         # test sets/tests/sections/questions/hashtags, course browsing
├── assessment      # attempts, answers, scoring, review (reading/listening runtime)
├── writing         # writing submissions + AI grading
├── speaking        # sessions, realtime, grading, transcripts
├── billing         # subscription, credit (Lúa), quota, billing, payment, gating
├── engagement      # chat, vocabulary+translation, dashboard, activity
├── admin           # cross-domain admin console (users, audit, finance, dashboard)
└── abts            # AI test generation (admin-only)
```

The full file tree is in `SPEC-03-package-structure.md`.

## 2. Slice internals (every module has the same shape)

```
<module>
├── web
│   ├── XxxController.java         # REST endpoints, thin; no business logic
│   └── dto
│       ├── XxxRequest.java        # inbound payloads (records, validated)
│       ├── XxxResponse.java       # outbound payloads (records)
│       └── XxxView.java           # read-model projections (records)
├── service
│   └── XxxService.java            # application/business logic (use cases)
├── domain
│   ├── Xxx.java                   # JPA entity / aggregate
│   ├── XxxStatus.java             # enums
│   └── XxxId.java / value objects # optional
├── repository
│   └── XxxRepository.java         # Spring Data JPA
└── config                         # module-local @ConfigurationProperties / beans (optional)
    └── XxxProperties.java
```

Rules:
- **Controllers are thin**: validate input, call a service, map to response. No persistence,
  no cross-aggregate orchestration in controllers.
- **Services hold behavior**. Default to a single concrete `@Service` class. Introduce an
  interface **only** when there are genuinely ≥2 implementations or a real seam for testing
  that a mock can't cover (e.g. Speaking selection planner: heuristic vs LLM). No reflexive
  `XxxService` + `XxxServiceImpl` pairs.
- **Domain types map to existing Supabase tables** (schema is frozen, SPEC-00 §5).
- **DTOs are Java records**, immutable, with Bean Validation annotations on requests.

## 3. Dependency rules

1. **`platform` depends on nothing** in `com.cramer.*` business modules. Everyone may
   depend on `platform`.
2. **Business modules do not depend on each other's `web`, `domain`, or `repository`.**
   Cross-module needs go through a **published service interface** exposed by the owning
   module under `<module>/service` (a "port"), consumed via Spring injection.
3. **No cyclic dependencies** between modules. If A and B both need shared concepts, the
   concept belongs in `platform` or one side owns it and the other consumes its port.
4. **`admin` and `abts` are allowed to depend on domain modules' published services** (they
   are orchestrators over existing capabilities). They must not reach into another module's
   repositories directly.
5. **Aggregation/read-models** (e.g. dashboard, admin finance) may run read-only queries via
   their own repositories/projections rather than fanning out to many services, to avoid
   chatty cross-module calls — but never write through another module's tables.

### Cross-module contract example

`writing` needs to charge AI-grading usage. It depends on a `billing` port:

```java
// billing/service/UsageBillingPort.java  (published contract)
public interface UsageBillingPort {
    BillingDecision chargeAiGrading(UUID userId, String reference);
    void refund(UUID userId, String reference);
}
```

`writing` injects `UsageBillingPort`; it never touches `billing` entities/repositories.

## 4. Cross-cutting concerns live in `platform`

- **Security**: Supabase JWT filter, security config, authenticated-user accessor.
- **Web**: global exception handler, error response model, CORS + Jackson config, paging DTO.
- **Errors**: the shared exception hierarchy.
- **Integration clients**: OpenRouter, DeepSeek/LLM, Supabase storage/admin — thin, reusable
  HTTP clients with no business logic. Business modules wrap these with their own prompts/logic.
- **Config**: app startup validation, conditional datasource, OpenAPI.
- **Rate limiting**, **common utilities** (JSON, IELTS band conversion, ids).

See `SPEC-04-cross-cutting.md` for contracts.

## 5. Async, scheduled, and realtime patterns

- **Async work** (writing grading, speaking grading) uses module-local
  `ThreadPoolTaskExecutor` beans defined in the module's `config`. Dispatch happens
  **after transaction commit** (`TransactionSynchronization` / event listener), never inside
  the committing transaction.
- **Scheduled jobs** live in the owning module under `service` (e.g.
  `SubscriptionExpiryScheduler` in `billing`, `SpeakingGradingWatchdog` in `speaking`),
  annotated `@Scheduled`, guarded by a config flag.
- **Realtime** (Speaking ↔ Gemini Live) lives in `speaking/web/ws` (handler + interceptor)
  and `speaking/service` (upstream client). WebSocket auth is enforced at handshake even
  though Spring Security permits `/ws/**`.
- **Streaming (SSE)** for ABTS lives in `abts/web` with a bounded executor in `abts/config`.

## 6. Persistence conventions

- Spring Data JPA repositories per aggregate; custom queries via `@Query`.
- JSONB columns use Hypersistence Utils `@Type(JsonType.class)` or
  `@JdbcTypeCode(SqlTypes.JSON)`, mapping to typed records where practical, `JsonNode` where
  the payload is genuinely dynamic (e.g. AI-generated `question_content`).
- Mutating bulk queries use `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
- Money/quota mutations use atomic repository operations or row locks (SPEC-15), not
  read-modify-write on detached entities.

## 7. Testing strategy

- **Unit tests** for services (mock ports/repositories).
- **Web slice tests** (`@WebMvcTest`) for controllers + validation + authz mapping.
- **Persistence tests** (`@DataJpaTest`) for non-trivial queries.
- **Contract tests** for cross-module ports.
- Test packages mirror the module tree (SPEC-02 §6).

## 8. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
