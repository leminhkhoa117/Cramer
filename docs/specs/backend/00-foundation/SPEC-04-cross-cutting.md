# SPEC-04 — Cross-Cutting Contracts

> Status: **Authoritative** · Depends on: SPEC-01, SPEC-02
> Defines the shared contracts every module relies on: error model, security, response
> conventions, and the cross-module port catalog. Lives in `com.cramer.platform`.

---

## 1. Authentication & authorization

### 1.1 Route categories

| Category | Matcher | Rule |
|----------|---------|------|
| Public | `/api/auth/**`, `/api/health`, `/api/payments/webhook`, `/api/payments/config-status`, `/api/payments/lua-packs`, Swagger/OpenAPI, `/error` | no auth |
| Realtime | `/ws/**` | permitted by HTTP security; **authenticated at WS handshake** (SPEC-14) |
| Admin | `/api/admin/**` | authenticated **and** `profiles.is_admin = true` |
| Authenticated | all other `/api/**` | valid JWT required |

### 1.2 JWT authentication (modernized)

- Use **Spring Security OAuth2 Resource Server** with a `NimbusJwtDecoder` built from the
  Supabase JWT secret (HS256 `SecretKey`). **The custom `JwtAuthFilter` is removed.**
- Validate **signature + expiry**; require a non-blank `sub` (UUID). Reject otherwise (401).
- The authenticated principal name is the Supabase `sub` (user UUID).
- `platform.security.CurrentUser` exposes `UUID requireUserId()` /
  `Optional<UUID> userId()` by reading the security context. **Controllers obtain the user id
  from `CurrentUser`, never from request bodies or the `X-User-Id` header.** The
  `X-User-Id` header is no longer trusted anywhere (removes the spoofable-audit defect).

### 1.3 Admin authorization

- `AdminAuthorizationService.isAdmin(UUID)` checks `profiles.is_admin`.
- Wired into the security chain as an `AuthorizationManager` for `/api/admin/**`, so admin
  identity is always derived from the token + DB, never a header.
- Admin audit attribution uses the authenticated principal (SPEC-17).

## 2. Error model

### 2.1 Standard error body

```jsonc
// ApiError (record)
{ "timestamp": "2026-06-10T12:00:00Z", "status": 404, "error": "Not Found",
  "message": "Test set not found", "path": "/api/admin/test-sets/9" }
```

Validation failures add a `fieldErrors` map and use `error = "Validation Failed"`:

```jsonc
{ "timestamp": "...", "status": 400, "error": "Validation Failed",
  "fieldErrors": { "email": "must be a well-formed email address" }, "path": "..." }
```

Quota errors add `blockType`. 500s add `exceptionType` (never a stack trace).

### 2.2 Exception → HTTP status (GlobalExceptionHandler)

| Exception | Status |
|-----------|--------|
| `ResourceNotFoundException` | 404 |
| `ResourceAlreadyExistsException` | 409 |
| `OperationNotAllowedException` | 403 |
| `IllegalArgumentException`, `MethodArgumentNotValidException`, `MethodArgumentTypeMismatchException`, missing param, unreadable body | 400 |
| `IllegalStateException` | 409 |
| `AccessDeniedException` | 403 |
| `QuotaExceededException` | 402 (+`blockType`) |
| `RateLimitExceededException` | 429 |
| `PayloadTooLargeException` | **413** (was unmapped→500) |
| `UpstreamServiceException` | 503 (a required upstream dependency failed; never fabricate a success) |
| anything else | 500 (+`exceptionType`) |

Spring Security authn/authz failures keep Spring's defaults (401/403).
**Controllers must not swallow exceptions and return `200 {success:false}`** (the old
admin-content anti-pattern). Let exceptions propagate to the handler.

## 3. Response & API conventions

- **Verbs/status**: `GET`→200/404, `POST` create→201, `POST` command→200, `PUT/PATCH`→200,
  `DELETE`→204. Async accepted work may use 202.
- **Paging**: `PageResponse<T>` record `{ content, page, size, totalElements, totalPages }`.
  Page params: `page` (0-based, default 0), `size` (default per endpoint, hard-capped).
- **Answer-key safety (hard rule)**: `question.correct_answer` and `question.explanation`
  are returned **only** by review endpoints (owner) and admin endpoints. Test-delivery and
  generic content reads return answer-free `…View` projections. There is no authenticated
  endpoint that leaks answers (removes the old generic-CRUD leak).
- **Money-affecting writes are transactional and idempotent by reference** (SPEC-15).
- Request bodies are validated records; reject unknown-but-required, accept unknown-extra
  (`FAIL_ON_UNKNOWN_PROPERTIES=false`) for forward-compat.

## 4. Cross-module port catalog

Ports are interfaces published by the **owning** module under `…/service` (or
`…/service/ports`). Consumers inject the port; they never touch foreign repositories.

| Port | Owner | Consumers | Purpose |
|------|-------|-----------|---------|
| `ContentLookupPort` | catalog | speaking, abts, assessment | resolve published sections/questions for a test+skill |
| `AttemptBillingPort` | billing | assessment | pre-check / charge attempt quota+overage |
| `UsageBillingPort` | billing | writing | charge AI grading after success / refund |
| `SpeakingBillingPort` | billing | speaking | check / deduct / refund session Lúa |
| `ChatBillingPort` | billing | engagement | charge chat after success / refund |
| `TranslationBillingPort` | billing | engagement | charge translation after success / refund |
| `ActivityPort` | engagement | billing, assessment, identity | log `user_activities` |
| `AuditPort` | admin | speaking, billing, catalog | write `admin_audit_log` |
| `FeatureAccessPort` | billing | catalog, engagement | gate features by tier (now wired in) |

Rule: a port exposes **records/primitives only** (no JPA entities) across module boundaries.

## 5. Async, events, scheduling

- Domain events (`…Event` records) are published via `ApplicationEventPublisher` and consumed
  with `@TransactionalEventListener(phase = AFTER_COMMIT)` for post-commit async work.
- Module executors are defined in the module's `config` (e.g. `writingGradingExecutor`,
  `speakingGradingExecutor`, `abtsStreamingExecutor`) with bounded pools + queues; queue
  saturation surfaces a clean error/event, never a silent drop.
- Schedulers are `@Scheduled` beans in the owning module, each guarded by a config flag.

## 6. Configuration

- Each module binds its settings to a `…Properties` record via `@ConfigurationProperties`.
- Secrets come from environment (`SUPABASE_JWT_SECRET`, `PAYOS_*`, `OPENROUTER_API_KEY`,
  `DEEPSEEK_API_KEY`). `StartupValidator` fails fast on missing critical secrets.

## 7. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
| 10/06/2026 | Added `UpstreamServiceException` → 503 (used by identity email-check, SPEC-10 §2.1). |
