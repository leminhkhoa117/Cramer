# SPEC-05 — Tech Stack & Build

> Status: **Authoritative** · Depends on: SPEC-00
> Fixed for this rewrite pass. Versions reflect the current `backend/pom.xml`.

---

## 1. Platform

| Item | Value |
|------|-------|
| Language | **Java 25** (`maven.compiler.release=25`) |
| Framework | **Spring Boot 4.0.0** |
| Build | Maven (`./mvnw`) |
| Packaging | executable jar (`spring-boot-maven-plugin`) |
| DB | PostgreSQL (Supabase), schema **frozen** |

## 2. Runtime dependencies (keep)

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`, `-websocket`,
  `-mail`, `-jackson`.
- `spring-boot-starter-oauth2-resource-server` — **now the primary auth mechanism**
  (NimbusJwtDecoder, HS256 + Supabase secret). See SPEC-04 §1.
- `hypersistence-utils-hibernate-70:3.15.2` — JSONB mapping.
- `springdoc-openapi-starter-webmvc-ui:3.0.0` — Swagger UI.
- `bucket4j-core:8.7.0` — in-memory rate limiting (`platform.ratelimit`).
- `postgresql` (runtime), `jackson-databind`, `jackson-datatype-jsr310`.
- `sendgrid-java:4.10.2` + `spring-boot-starter-mail` — email. **Verify active usage during
  `identity`/notification work; drop if unused.**
- `totp:1.7.1` — TOTP 2FA. **Verify active usage in `identity`; drop if unused.**

## 3. Dependencies to remove

- `jjwt-api/impl/jackson:0.13.0` — superseded by resource-server `NimbusJwtDecoder`. Remove
  once the custom `JwtAuthFilter`/`JwtUtil` are deleted (SPEC-18 migration).
- Confirm no remaining references before removing (build must stay green).

## 4. Lombok policy

- Lombok `1.18.46` stays (annotation processor configured; excluded from the boot jar).
- **Use Lombok on JPA entities only** (`@Getter/@Setter/@Builder/@NoArgsConstructor/
  @AllArgsConstructor`). DTOs are **records** (no Lombok). Services use constructor injection
  (`@RequiredArgsConstructor` allowed on services to reduce boilerplate).

## 5. Persistence specifics

- JSONB: `@Type(JsonType.class)` (Hypersistence) or `@JdbcTypeCode(SqlTypes.JSON)`. Prefer
  typed records for stable payloads; `JsonNode` for genuinely dynamic AI content.
- Custom `ObjectMapper` (in `platform.web.WebConfig`) registers JavaTime and sets
  `FAIL_ON_UNKNOWN_PROPERTIES=false` (carry-over compatibility for tolerant deserialization).

## 6. Realtime

- Speaking ↔ Gemini Live uses **Spring WebSocket client** (`StandardWebSocketClient`);
  the old `java-websocket`/google-cloud libs remain removed (CVE + unused). Re-add only with
  patched versions if ever needed.

## 7. Testing stack

| Lib | Use |
|-----|-----|
| `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) | unit + integration |
| `spring-boot-starter-webmvc-test` | `@WebMvcTest` slice tests (Boot 4 split-out) |
| `spring-security-test` | auth in web slice tests |
| `h2` | `@DataJpaTest` (where Postgres-specific SQL isn't required) |
| `wiremock-standalone:3.13.2` | stub OpenRouter/DeepSeek/PayOS/Supabase HTTP |
| `awaitility:4.3.0` | async grading/dispatch assertions |

> H2 caveat: JSONB + raw Postgres SQL paths need either Testcontainers-Postgres or focused
> mocking. Module specs note where Postgres fidelity is required.

## 8. Build & run commands

```bash
# Build (skip tests)
cd backend && ./mvnw clean package -DskipTests
# Run tests
cd backend && ./mvnw test
# Run app (loads root .env via run scripts)
cd backend && ./run-app.ps1     # Windows
cd backend && ./run-app.sh      # Linux/macOS
```

Required env: `SPRING_DATASOURCE_*`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`,
`OPENROUTER_API_KEY`, `DEEPSEEK_API_KEY`, `PAYOS_*`.

## 9. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
