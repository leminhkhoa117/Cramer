# SPEC-18 — Platform (Shared Kernel)

> Status: **Authoritative** · Module: `platform` · Depends on: SPEC-04
> The foundation every module builds on. No business domain lives here. SPEC-04 defines the
> contracts; this spec lists the concrete components and the dead-code removal.

---

## 1. Security (`platform.security`)

- **`SupabaseJwtConfig`** builds a `NimbusJwtDecoder` from the Supabase secret (HS256
  `SecretKey`), validating signature + expiry; principal name = `sub` (UUID).
- **`SecurityConfig`** — stateless, CSRF off, CORS on; resource-server JWT; route rules
  (SPEC-04 §1.1); admin gate via `AdminAuthorizationService` for `/api/admin/**`.
- **`AdminAuthorizationService.isAdmin(UUID)`** — checks `profiles.is_admin`.
- **`CurrentUser`** — `requireUserId()` / `userId()` from the security context.
- **Removed:** `JwtAuthFilter`, `JwtUtil` (custom JJWT) → replaced by resource server; drop
  the JJWT dependency once no references remain (SPEC-05 §3).

## 2. Web (`platform.web`)

- **`GlobalExceptionHandler`** → `ApiError` per SPEC-04 §2 (incl. `PayloadTooLargeException`
  → 413). No controller may return `200 {success:false}`.
- **`ApiError`**, **`PageResponse<T>`** — records.
- **`WebConfig`** — CORS (dev origins; allowed headers `Authorization, Content-Type,
  Cache-Control`; **drop `X-User-Id` and `X-Debug-Key`** — no longer used), and the custom
  `ObjectMapper` (JavaTime, `FAIL_ON_UNKNOWN_PROPERTIES=false`).
- **`HealthController`** — `GET /api/health` (liveness) and `GET /api/health/ready`
  (readiness: datasource check). **Replaces** `HelloController`, `DatabaseTestController`,
  `DebugController`.

## 3. Errors (`platform.error`)

`ResourceNotFoundException`, `ResourceAlreadyExistsException`, `OperationNotAllowedException`,
`QuotaExceededException` (+`blockType`), `RateLimitExceededException`,
`PayloadTooLargeException`, `UpstreamServiceException` (→ 503, required-dependency failure).
(All mapped in §2 / SPEC-04 §2.2.)

## 4. Integration clients (`platform.integration`)

Thin, reusable HTTP clients with config + error mapping; **no business logic**:
- **`openrouter.OpenRouterClient`** — chat / JSON-schema / SSE stream / models; error codes
  (SPEC-24 §2). Used by `abts` and `speaking`.
- **`llm.DeepSeekClient`** — OpenAI-compatible `/chat/completions`. Used by `writing`,
  `engagement`.
- **`supabase.SupabaseStorageClient`** — download/upload/exists/publicUrl (bucket ops). Used
  by `speaking`.
- **`supabase.SupabaseAdminClient`** — auth admin (email existence). Used by `identity`.
- **Removed:** the unused anon-key `SupabaseClient`.
- Optional `SUPABASE_INSECURE_TLS` trust-all is **dev-only** and gated by a flag + warning log.

## 5. Rate limiting (`platform.ratelimit`)

- **`RateLimiter`** (Bucket4j, in-memory, keyed `userId:endpointType`): grading 5/min,
  profile/auth 10/min, default 60/min. Applied to writing submit/regrade; available to other
  modules. (In-memory is non-distributed — documented limitation.)

## 6. Config (`platform.config`)

- **`DataSourceConfig`** — conditional Hikari (only when `spring.datasource.url` set;
  `initializationFailTimeout(0)`).
- **`OpenApiConfig`** — OpenAPI metadata + global bearer scheme.
- **`StartupValidator`** — fail fast on missing/short `SUPABASE_JWT_SECRET`; warn on missing
  datasource.

## 7. Common (`platform.common`)

- **`json.Json`** — static serialize/deserialize helper.
- **`ielts.BandScale`** — raw-correct → IELTS band (40→9.0 … <4→0.0).
- **`ielts.Skill`**, **`ielts.QuestionType`** — shared IELTS vocabulary used by catalog,
  assessment, speaking, abts (kept here to avoid cross-module domain coupling, SPEC-01 §3).
- **`ids.Ids`** — UUID parse/validate (invalid → 400 via handler).
- **Removed:** the old `EntityMapper`/`JsonUtil` god-helpers (replaced by per-module mappers +
  `Json`).

## 8. Application entry
- `CramerApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`.

## 9. Dead/dev code removed (consolidated)
`ProfileServiceOld`, `SupabaseClient` (unused), `DebugController`, `DatabaseTestController`,
`HelloController`, `JwtAuthFilter`, `JwtUtil`, `EntityMapper`, `JsonUtil`. JJWT dependency
removed after references are gone.

## 10. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
| 10/06/2026 | Added `UpstreamServiceException` (→ 503) to the error hierarchy (§3). |
| 11/06/2026 | Added shared IELTS vocabulary `Skill` + `QuestionType` to `common.ielts` (§7). |
