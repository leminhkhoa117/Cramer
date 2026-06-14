# Cramer Backend API Reference

> **Last Updated:** 14/06/2026
> **Version:** 2.0.0 (regenerated from vertical-slice code)
> **Base URL:** `http://localhost:8080`

> **Supersedes the legacy v1.2.0 reference (17/05/2026).** That document described a
> pre-rewrite architecture (global `com.cramer.controller` package, custom `JwtAuthFilter`,
> `X-User-Id` header trust) that **no longer exists**. This version is regenerated directly from
> the June 2026 vertical-slice source tree under `backend/src/main/java/com/cramer/**/web/`.
> Every path, HTTP method, controller class, and DTO below was verified against the actual
> `.java` source.

---

## Table of Contents

- [Overview](#overview)
- [Authentication & Authorization](#authentication--authorization)
- [Conventions](#conventions)
- [Endpoint Index](#endpoint-index)
- [Modules](#modules)
  - [platform](#module-platform)
  - [identity](#module-identity)
  - [catalog](#module-catalog)
  - [assessment](#module-assessment)
  - [writing](#module-writing)
  - [speaking](#module-speaking)
  - [billing](#module-billing)
  - [engagement](#module-engagement)
  - [admin](#module-admin)
  - [abts](#module-abts)
- [Verification Notes & Caveats](#verification-notes--caveats)

---

## Overview

The Cramer backend is a Spring Boot REST API organised into **10 vertical-slice modules**. Each
module owns its own `web/` package (controllers + `web/dto` records), `service/`, `domain/`, and
`repository/`. There is no global controller/service/entity layer.

**Tech stack (verified in `backend/pom.xml`):**

| Item | Value |
| --- | --- |
| Framework | Spring Boot **4.0.0** |
| Language | Java **25** |
| API docs | springdoc-openapi-starter-webmvc-ui **3.0.0** |
| Security | Spring Security OAuth2 Resource Server (JWT) |
| JSON | Jackson 2 pinned for controller (de)serialization (`platform/web/WebConfig`) |
| Rate limiting | bucket4j-core 8.7.0 |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
(Both verified in `application.properties` and permitted in `SecurityConfig`.)

Controllers map their full paths (each `@RequestMapping`/method mapping already includes the
`/api/...` prefix). There is **no** global servlet context-path; `server.port` defaults to `8080`.

---

## Authentication & Authorization

Source of truth: `com.cramer.platform.security` (`SecurityConfig`, `SupabaseJwtConfig`,
`AdminAuthorizationService`, `CurrentUser`).

### Token verification

- **Mechanism:** Spring Security **OAuth2 resource server** with a `NimbusJwtDecoder` built from
  the Supabase **HS256** secret (`SupabaseJwtConfig.jwtDecoder`). Signature **and** expiry are
  validated. The legacy custom `JwtAuthFilter` is gone.
- **Secret:** `SUPABASE_JWT_SECRET` (must be ≥ 32 bytes for HS256, else startup fails).
- **Header:** `Authorization: Bearer <supabase_jwt_token>`
- **Principal:** the authenticated user id is the Supabase `sub` claim (a UUID). Controllers obtain
  it via `CurrentUser.requireUserId()` — **never** from a request body or an `X-User-Id` header.
- **Session policy:** stateless; CSRF disabled; CORS enabled (`WebConfig`).

### Authorization tiers

Each endpoint below is tagged with one of:

| Tag | Meaning |
| --- | --- |
| **public** | Listed in the `permitAll()` matcher — no token required. |
| **authenticated** | Requires a valid Bearer token (`anyRequest().authenticated()`). |
| **admin** | Path under `/api/admin/**`; additionally requires `profiles.is_admin = true`, checked by `AdminAuthorizationService` against the verified principal UUID. |

### Public paths (verified `SecurityConfig` `permitAll()` list)

```
/api/auth/**
/api/health, /api/health/**
/api/payments/webhook, /api/payments/config-status, /api/payments/lua-packs
/swagger-ui/**, /swagger-ui.html, /v3/api-docs, /v3/api-docs/**
/error
/ws/**            (permit rule only; see Verification Notes — no WS handler is registered)
```

> **Important nuance:** Several controllers whose Javadoc calls them "public" (e.g.
> `CourseController`, subscription **tiers**) are **not** in the `permitAll()` list. At the HTTP
> layer they require a valid Bearer token. "Public" there means *public catalog content visible to
> any authenticated user* — not anonymous access. They are tagged **authenticated** below.

### Error model

Filter-chain auth failures return a JSON `ApiError` body (`platform/web/ApiError`):

```
{ timestamp, status, error, message, path, fieldErrors?, blockType?, exceptionType? }
```

- `401 Unauthorized` — missing/invalid token.
- `403 Forbidden` — authenticated but not authorized (e.g. non-admin hitting `/api/admin/**`, or
  an IDOR owner-check failure).

---

## Conventions

- **Pagination wrapper** (`platform/web/PageResponse<T>`): `{ content[], page, size, totalElements, totalPages }`.
  Some list endpoints return a raw `List<T>` instead (noted per endpoint).
- **`JsonNode`** in a DTO field = an opaque JSON tree (JSONB column or AI payload); its internal
  shape is not a fixed record and is not enumerated here.
- **`Map<...>`** responses are ad-hoc JSON objects built inline in the controller; the documented
  keys are taken verbatim from the source.
- Request DTOs are Java `record`s in each module's `web/dto`. Validation annotations
  (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Email`, `@Pattern`, `@DecimalMin/Max`) are noted
  where present; violations produce a `400 Validation Failed` `ApiError`.
- HTTP status defaults to `200 OK` unless the controller declares `@ResponseStatus` (noted as
  `201` / `204`).

---

## Endpoint Index

| Module | Controller | Base path | # Endpoints | Auth |
| --- | --- | --- | --- | --- |
| platform | `HealthController` | `/api/health` | 2 | public |
| identity | `AuthController` | `/api/auth` | 1 | public |
| identity | `ProfileController` | `/api/profiles` | 2 | authenticated |
| catalog | `TestDeliveryController` | `/api/tests` | 1 | authenticated |
| catalog | `CourseController` | `/api/courses` | 4 | authenticated |
| catalog | `AdminTestSetController` | `/api/admin/test-sets` | 9 | admin |
| catalog | `AdminTestController` | `/api/admin` | 11 | admin |
| catalog | `AdminSectionController` | `/api/admin/sections` | 4 | admin |
| catalog | `AdminQuestionController` | `/api/admin/questions` | 5 | admin |
| catalog | `AdminHashtagController` | `/api/admin/hashtags` | 8 | admin |
| assessment | `AttemptController` | `/api/test-attempts` | 9 | authenticated |
| writing | `WritingController` | `/api/writing` | 6 | authenticated |
| speaking | `SpeakingController` | `/api/speaking` | 8 | authenticated |
| speaking | `AdminSpeakingController` | `/api/admin/speaking` | 1 | admin |
| billing | `SubscriptionController` | `/api/subscriptions` | 8 | authenticated |
| billing | `QuotaController` | `/api/quotas` | 3 | authenticated |
| billing | `PaymentController` | `/api/payments` | 7 | mixed (3 public / 4 auth) |
| billing | `CreditController` | `/api/credits` | 7 | authenticated |
| engagement | `ChatController` | `/api/chat` | 4 | authenticated |
| engagement | `VocabularyController` | `/api/vocabulary` | 8 | authenticated |
| engagement | `DashboardController` | `/api/dashboard` | 4 | authenticated |
| admin | `AdminDashboardController` | `/api/admin/dashboard` | 3 | admin |
| admin | `AdminActivityController` | `/api/admin/activities` | 4 | admin |
| admin | `AdminFinanceController` | `/api/admin/finance` | 4 | admin |
| admin | `AdminUserController` | `/api/admin/users` | 6 | admin |
| abts | `AbtsController` | `/api/admin/abts` | 12 | admin |

**Total: 141 endpoints across 26 controllers in 10 modules.**

---

## Modules

---

### Module: platform

Shared kernel: liveness/readiness, security, web config, error model. The only platform
controller is the health probe; everything else (security, CORS, JSON, `PageResponse`, `ApiError`)
is infrastructure consumed by other modules.

#### `HealthController` · `/api/health` · **public**

Source: `com.cramer.platform.web.HealthController`

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/health` | Liveness | — | `Map` → `{ status: "UP" }` |
| GET | `/api/health/ready` | Readiness (DB `SELECT 1`) | — | `Map` → `{ status, db }`; `503` `{status:"DOWN", db:"DOWN"}` on DB failure |

---

### Module: identity

Supabase-JWT auth helpers + self-service profile.

#### `AuthController` · `/api/auth` · **public**

Source: `com.cramer.identity.web.AuthController`. Auth itself is performed by Supabase on the
client; this only answers "does an account exist for this email".

| Method | Path | Purpose | Request body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/check-email` | Email existence check | `CheckEmailRequest { email @NotBlank @Email }` | `CheckEmailResponse { exists: boolean }` |

#### `ProfileController` · `/api/profiles` · **authenticated**

Source: `com.cramer.identity.web.ProfileController`. IDOR guard: the authenticated user must equal
the path `{id}`, else `403`.

| Method | Path | Purpose | Path/Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/profiles/{id}` | Get own profile | `id: UUID` | `ProfileResponse` |
| PUT | `/api/profiles/{id}` | Update own profile | `id: UUID`; body `UpdateProfileRequest` | `ProfileResponse` |

**`UpdateProfileRequest`** (all nullable; only these fields are mutable):
`fullName, phoneNumber, address, avatarUrl, heroBackgroundUrl, pageBackgroundUrl, llmApiKey,
llmModel, llmProvider`. `llmApiKey` semantics: `""` clears the stored key; non-empty stores it;
`null`/absent leaves unchanged.

**`ProfileResponse`**: `id (UUID), username, fullName, phoneNumber, address, avatarUrl,
heroBackgroundUrl, pageBackgroundUrl, hasLlmApiKey (boolean — the raw key is never returned),
llmModel, llmProvider, isAdmin (Boolean), accountStatus, createdAt (OffsetDateTime)`.

---

### Module: catalog

Test content hierarchy (`test_sets` → `tests` → `sections` → `questions` + hashtags), answer-free
test delivery, public course browsing, and admin CRUD. Admin sub-controllers live in
`catalog/web/admin`.

#### `TestDeliveryController` · `/api/tests` · **authenticated**

Source: `com.cramer.catalog.web.TestDeliveryController`. Returns **answer-free** section/question
views for the test-taking UI.

| Method | Path | Purpose | Query params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/tests/data` | Sections + questions for a test (no answer key) | `source` (String), `test` (int), `skill` (String → `Skill.from`) | `List<TestSectionView>` |

**`TestSectionView`**: `id (long), testId, skill, partNumber, passageText, audioUrl,
sectionLayout (JsonNode), displayContentUrl, imageDescription, questions: List<TestQuestionView>`.
**`TestQuestionView`**: `id (long), questionNumber, questionUid, questionType,
questionContent (JsonNode), imageUrl, wordLimit` — deliberately omits `correct_answer`/`explanation`.

#### `CourseController` · `/api/courses` · **authenticated**

Source: `com.cramer.catalog.web.CourseController`. Only published content is visible. (Javadoc says
"public" in the content sense; the HTTP layer still requires a Bearer token — see auth nuance.)

| Method | Path | Purpose | Params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/courses` | Paged published course codes | `page` (≥0, def 0), `size` (1–100, def 6), `search?` | `PageResponse<String>` |
| GET | `/api/courses/v2` | Published test sets | — | `List<TestSetView>` |
| GET | `/api/courses/{course}/tests` | Test numbers in a course | `course` (String) | `List<Integer>` |
| GET | `/api/courses/{code}/details` | Set details by code | `code` (String) | `TestSetView` |

**`TestSetView`**: `id, code, name, description, coverImageUrl, sourceType, isPublished,
displayOrder, testCount (Long, nullable), createdAt`.

#### `AdminTestSetController` · `/api/admin/test-sets` · **admin**

Source: `com.cramer.catalog.web.admin.AdminTestSetController`.

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/test-sets` | List all sets | — | `List<TestSetView>` |
| GET | `/api/admin/test-sets/{id}` | Get by id | `id: Long` | `TestSetView` |
| GET | `/api/admin/test-sets/code/{code}` | Get by code | `code: String` | `TestSetView` |
| POST | `/api/admin/test-sets` | Create | `CreateTestSetRequest` | `TestSetView` · **201** |
| PUT | `/api/admin/test-sets/{id}` | Update | `id`; `CreateTestSetRequest` | `TestSetView` |
| DELETE | `/api/admin/test-sets/{id}` | Delete | `id` | **204** |
| POST | `/api/admin/test-sets/{id}/publish` | Publish | `id` | `TestSetView` |
| POST | `/api/admin/test-sets/{id}/unpublish` | Unpublish | `id` | `TestSetView` |
| POST | `/api/admin/test-sets/reorder` | Reorder by id list | `ReorderRequest` | `List<TestSetView>` |

**`CreateTestSetRequest`**: `code @NotBlank @Size(max=100), name @NotBlank @Size(max=255),
description?, coverImageUrl?, sourceType?, isPublished?, displayOrder?`.
**`ReorderRequest`**: `orderedIds @NotEmpty List<Long>`.

#### `AdminTestController` · `/api/admin` · **admin**

Source: `com.cramer.catalog.web.admin.AdminTestController`. (Base path is `/api/admin`; method
mappings carry the `test-sets`/`tests` segments.)

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/test-sets/{setId}/tests` | List tests in a set | `setId: Long` | `List<TestView>` |
| POST | `/api/admin/test-sets/{setId}/tests` | Create test in set | `setId`; `CreateTestRequest` | `TestView` · **201** |
| GET | `/api/admin/tests/lookup` | Lookup by set code + number | `setCode` (String), `testNumber` (int) | `TestView` |
| GET | `/api/admin/tests/{id}` | Get test | `id: Long` | `TestView` |
| PUT | `/api/admin/tests/{id}` | Update test | `id`; `UpdateTestRequest` | `TestView` |
| DELETE | `/api/admin/tests/{id}` | Delete (optional cascade) | `id`; `force` (bool, def false) | **204** |
| POST | `/api/admin/tests/{id}/publish` | Publish (cascade) | `id` | `TestView` |
| POST | `/api/admin/tests/{id}/unpublish` | Unpublish | `id` | `TestView` |
| POST | `/api/admin/tests/{id}/duplicate` | Duplicate test | `id`; `includeSections` (bool, def false) | `TestView` · **201** |
| PUT | `/api/admin/tests/{id}/hashtags` | Replace hashtag set | `id`; `UpdateTestHashtagsRequest` | `TestView` |
| GET | `/api/admin/tests/{id}/sections` | Sections for a test | `id`; `skill?` | `List<SectionAdminView>` |

**`CreateTestRequest`**: `testNumber? @Min(1), name? @Size(max=255), description?, difficulty?,
estimatedTimeMinutes? @Min(1), isPublished?`.
**`UpdateTestRequest`**: same fields, all optional (null = unchanged).
**`UpdateTestHashtagsRequest`**: `codes @NotNull List<String>` (each `^[a-z0-9_-]+$`, max 20).
**`TestView`**: `id, setId, testNumber, name, description, difficulty, estimatedTimeMinutes,
isPublished, isAiGenerated, hashtagCodes (List<String>, nullable), createdAt`.

#### `AdminSectionController` · `/api/admin/sections` · **admin**

Source: `com.cramer.catalog.web.admin.AdminSectionController`. Admin views may expose all authored
fields (unlike delivery).

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/sections/{id}` | Get section | `id: Long` | `SectionAdminView` |
| POST | `/api/admin/sections` | Create | `SectionRequest` | `SectionAdminView` · **201** |
| PUT | `/api/admin/sections/{id}` | Update | `id`; `SectionRequest` | `SectionAdminView` |
| DELETE | `/api/admin/sections/{id}` | Delete | `id` | **204** |

**`SectionRequest`**: `testId?, examSource?, testNumber?, skill @NotBlank, partNumber?,
passageText?, audioUrl?, sectionLayout? (JsonNode), imageDescription?, displayContentUrl?, status?`.
**`SectionAdminView`**: `id, testId, examSource, testNumber, skill, partNumber, passageText,
audioUrl, sectionLayout (JsonNode), imageDescription, displayContentUrl, status`.

#### `AdminQuestionController` · `/api/admin/questions` · **admin**

Source: `com.cramer.catalog.web.admin.AdminQuestionController`. Returns the answer key +
explanation (admin surface only).

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/questions` | List by section | `sectionId` (Long, query) | `List<QuestionAdminView>` |
| GET | `/api/admin/questions/{id}` | Get question | `id: Long` | `QuestionAdminView` |
| POST | `/api/admin/questions` | Create | `QuestionRequest` | `QuestionAdminView` · **201** |
| PUT | `/api/admin/questions/{id}` | Update | `id`; `QuestionRequest` | `QuestionAdminView` |
| DELETE | `/api/admin/questions/{id}` | Delete | `id` | **204** |

**`QuestionRequest`**: `sectionId @NotNull, questionNumber?, questionUid?, questionType @NotBlank,
questionContent? (JsonNode), correctAnswer? (JsonNode), explanation? (JsonNode), imageUrl?, wordLimit?`.
**`QuestionAdminView`**: `id, sectionId, questionNumber, questionUid, questionType,
questionContent, correctAnswer, explanation, imageUrl, wordLimit` (JSON fields are `JsonNode`).

#### `AdminHashtagController` · `/api/admin/hashtags` · **admin**

Source: `com.cramer.catalog.web.admin.AdminHashtagController`. Soft delete; lists return active only.

| Method | Path | Purpose | Request | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/hashtags` | List active | — | `List<HashtagView>` |
| GET | `/api/admin/hashtags/category/{category}` | By category | `category: String` | `List<HashtagView>` |
| GET | `/api/admin/hashtags/search` | Search | `q` (String) | `List<HashtagView>` |
| GET | `/api/admin/hashtags/popular` | Most-used | `limit` (int, def 10) | `List<HashtagView>` |
| GET | `/api/admin/hashtags/categories` | Distinct categories | — | `List<String>` |
| POST | `/api/admin/hashtags` | Create | `HashtagRequest` | `HashtagView` · **201** |
| PUT | `/api/admin/hashtags/{id}` | Update | `id`; `HashtagRequest` | `HashtagView` |
| DELETE | `/api/admin/hashtags/{id}` | Soft delete | `id` | **204** |

**`HashtagRequest`**: `code @NotBlank @Pattern(^[a-z0-9_-]+$), name?, category @NotBlank, icon?, color?`.
**`HashtagView`**: `id, code, name, category, icon, color, useCount, isActive`.

---

### Module: assessment

Test-attempt lifecycle, scoring, and owner-only review. User id from `CurrentUser`; all mutating
ops are owner-checked in the services.

#### `AttemptController` · `/api/test-attempts` · **authenticated**

Source: `com.cramer.assessment.web.AttemptController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/test-attempts/start` | Start (or reuse) an attempt | `source`, `test`, `skill` (all String, query); `forceNew` (bool, def false) | `AttemptView` |
| POST | `/api/test-attempts/{id}/progress` | Save in-progress state | `id`; `SaveProgressRequest` | `AttemptView` |
| POST | `/api/test-attempts/{id}/submit` | Submit for grading | `id`; `SubmitAnswersRequest` | `AttemptResultResponse` |
| POST | `/api/test-attempts/{id}/cancel` | Cancel | `id` | **204** |
| POST | `/api/test-attempts/{id}/resume` | Resume | `id` | `AttemptView` |
| POST | `/api/test-attempts/{id}/regrade` | Regrade | `id` | `AttemptResultResponse` |
| GET | `/api/test-attempts/{id}/answers` | Saved answers | `id` | `List<AnswerView>` |
| GET | `/api/test-attempts/{id}/review` | Full review (answer key) | `id` | `AttemptReviewView` |
| DELETE | `/api/test-attempts/{id}` | Delete | `id` | **204** |

**`SaveProgressRequest`**: `timeLeft?, currentPart?, answers? List<AnswerInput>`.
**`SubmitAnswersRequest`**: `answers @NotNull List<AnswerInput>`.
**`AnswerInput`**: `questionId @NotNull Long, value? String`.
**`AttemptView`**: `id, examSource, testNumber, skill, status, score, currentPart, timeLeft,
startedAt, completedAt`.
**`AttemptResultResponse`**: `attemptId, status, score (int), totalQuestions (int), bandScore
(Double — set for Reading/Listening, null for Writing), completedAt`.
**`AnswerView`**: `questionId, answerContent (JsonNode), userAnswer, isCorrect`.
**`AttemptReviewView`**: `attemptId, examSource, testNumber, skill, status, score, totalQuestions,
bandScore, startedAt, completedAt, durationSeconds, questions: List<QuestionReviewView>,
sections: List<SectionReviewView>`.
**`QuestionReviewView`**: `questionId, questionNumber, questionUid, questionType, questionContent
(JsonNode), userAnswer, correctAnswer (JsonNode), isCorrect, explanation (JsonNode)`.
**`SectionReviewView`**: `sectionId, partNumber, passageText, audioUrl, displayContentUrl,
sectionLayout (JsonNode), imageDescription, questions: List<QuestionReviewView>`.

---

### Module: writing

Writing submissions + async DeepSeek grading. Owner-checked via the assessment attempt. Submit
and regrade are rate-limited (`429` on exceed).

#### `WritingController` · `/api/writing` · **authenticated**

Source: `com.cramer.writing.web.WritingController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/writing/draft/{attemptId}` | Save a task draft | `attemptId`; `taskNumber` (int, def 1); `SaveDraftRequest` | **204** |
| POST | `/api/writing/submit/{attemptId}` | Submit 1–2 essays | `attemptId`; `SubmitEssayRequest` | `WritingStatusResponse` |
| GET | `/api/writing/status/{attemptId}` | Grading status | `attemptId` | `WritingStatusResponse` |
| GET | `/api/writing/review/{attemptId}` | Full review | `attemptId` | `WritingReviewView` |
| GET | `/api/writing/submissions/{attemptId}` | Raw per-task submissions | `attemptId` | `List<WritingTaskReview>` |
| POST | `/api/writing/regrade/{attemptId}` | Regrade | `attemptId` | `WritingStatusResponse` |

**`SaveDraftRequest`**: `essayText @NotBlank`.
**`SubmitEssayRequest`**: `essays @NotEmpty @Size(min=1,max=2) Map<Integer,String>` (task number → text).
**`WritingStatusResponse`**: `attemptId, overall (COMPLETED|PARTIAL_FAILURE|GRADING|PENDING),
total, completed, failed, tasks: List<TaskStatus{ taskNumber, status }>`.
**`WritingReviewView`**: `attemptId, weightedOverallBand (Double), averageBandScores (JsonNode),
tasks: List<WritingTaskReview>`.
**`WritingTaskReview`**: `taskNumber, status, essayText, wordCount, overallBand (BigDecimal),
bandScores (JsonNode), aiFeedback (JsonNode), taskPrompt, taskImageUrl, gradedAt`.

---

### Module: speaking

Speaking sessions, frozen blueprint, transcript turns, async grading worker, and admin regrade.

> **Realtime audio:** the controller Javadoc references a realtime examiner WS endpoint, but the
> realtime/audio dependencies were removed for a CVE (see `pom.xml` note and
> `SpeakingGradingWorker.java:25`); grading runs in `text_only`. No WebSocket handler is currently
> registered in the codebase — see [Verification Notes](#verification-notes--caveats).

#### `SpeakingController` · `/api/speaking` · **authenticated**

Source: `com.cramer.speaking.web.SpeakingController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/speaking/sessions` | Create session | `CreateSessionRequest` | `SpeakingSessionView` · **201** |
| GET | `/api/speaking/sessions/{id}` | Get session | `id: Long` | `SpeakingSessionView` |
| POST | `/api/speaking/sessions/{id}/transcripts` | Upsert a transcript turn | `id`; `SaveTranscriptRequest` | **204** |
| POST | `/api/speaking/sessions/{id}/complete` | Complete session | `id`; `durationSeconds?` (Integer) | `SpeakingSessionView` |
| POST | `/api/speaking/sessions/{id}/abandon` | Abandon (no charge) | `id` | `SpeakingSessionView` |
| GET | `/api/speaking/sessions/{id}/grading-status` | Grading status | `id` | `Map` → `{ status }` |
| GET | `/api/speaking/sessions/{id}/results` | Grading results | `id` | `JsonNode` (opaque) |
| GET | `/api/speaking/history` | Paged session history | `page` (def 0), `size` (def 20), `status?` | `PageResponse<SpeakingSessionView>` |

**`CreateSessionRequest`**: `testId @NotNull Long, sessionMode @NotBlank
(FULL|PART_1|PART_2|PART_3|PART_2_AND_3), accent? (british|american|australian|neutral),
speed? (0.85|1.00|1.15)`.
**`SaveTranscriptRequest`**: `turnIndex @NotNull, partNumber @NotNull, sourceQuestionId?,
questionSnapshot @NotNull (JsonNode), audioStoragePath?, audioDurationSeconds?, transcriptText?,
transcriptConfidence?`.
**`SpeakingSessionView`**: `id, testId, sessionMode, status, accent, speed, blueprint (JsonNode —
`_internal` banks stripped), luaCost, luaDeducted, startedAt, completedAt`.

#### `AdminSpeakingController` · `/api/admin/speaking` · **admin**

Source: `com.cramer.speaking.web.AdminSpeakingController`. Resets the session to `completed` +
audits, then dispatches grading post-commit (async).

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/admin/speaking/sessions/{id}/regrade` | Admin regrade | `id` (long); `mode?`, `force` (bool, def false); body `Map<String,String>` optional (`{ reason? }`) | `Map` → `{ success, sessionId, status:"completed" }` |

---

### Module: billing

Subscriptions, Lúa credits, monthly quota, and PayOS payments.

#### `SubscriptionController` · `/api/subscriptions` · **authenticated**

Source: `com.cramer.billing.web.SubscriptionController`. Tiers are catalog data; status/actions are
scoped to the authenticated user. (Tier reads still require a Bearer token.)

| Method | Path | Purpose | Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/subscriptions/tiers` | List tiers | — | `List<TierView>` |
| GET | `/api/subscriptions/tiers/{code}` | Tier by code | `code: String` | `TierView` |
| GET | `/api/subscriptions/current` | Current status | — | `SubscriptionStatusView` |
| GET | `/api/subscriptions/my-status` | Current status (alias) | — | `SubscriptionStatusView` |
| GET | `/api/subscriptions/grading-status` | AI-grading flags | — | `Map` → `{ aiGradingEnabled, gradingsRemaining }` |
| GET | `/api/subscriptions/gradings-remaining` | Gradings left | — | `Map` → `{ remaining }` |
| GET | `/api/subscriptions/chat-limit` | Chat monthly limit | — | `Map` → `{ limit }` |
| PUT | `/api/subscriptions/ai-grading` | Toggle AI grading | `BillingRequests.SetAiGrading { enabled }` | `SubscriptionStatusView` |

**`TierView`**: `id, code, name, priceVnd, premium, monthlyAttemptLimit, monthlyAttemptAiLimit,
perSkillAttemptLimit, includedAiGradings, chatbotMonthlyLimit, monthlyTranslationLimit, initialLua,
monthlyLuaBonus, features (JsonNode)`.
**`SubscriptionStatusView`**: `tierCode, tierName, premium, status, expiresAt, autoRenew,
attemptsUsed, attemptAisUsed, chatbotUsed, aiGradingEnabled, gradingsRemaining, chatMonthlyLimit`.

#### `QuotaController` · `/api/quotas` · **authenticated**

Source: `com.cramer.billing.web.QuotaController`.

| Method | Path | Purpose | Params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/quotas` | Tier-aware quota status | — | `QuotaStatusView` |
| GET | `/api/quotas/check` | Quota status (alias) | — | `QuotaStatusView` |
| GET | `/api/quotas/can-attempt` | Attempt pre-check | `skill` (String), `ai` (bool, def false) | `CanAttemptView` |

**`QuotaStatusView`**: `premium, tierCode, globalLimit, globalUsed, globalAiLimit, globalAiUsed,
unlimited` (`limit < 0` = unlimited).
**`CanAttemptView`**: `allowed, premium, requiresLua, luaCost, balance, reason`.

#### `PaymentController` · `/api/payments` · **mixed**

Source: `com.cramer.billing.web.PaymentController`. `webhook`, `lua-packs`, and `config-status`
are **public**; order creation, status, and history are **authenticated**.

| Method | Path | Auth | Purpose | Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/payments/subscription` | auth | Create subscription order | `BillingRequests.CreateSubscriptionOrder { tierId?, tierCode? }` | `CreateOrderResponse` |
| POST | `/api/payments/lua` | auth | Create Lúa-pack order | `BillingRequests.CreateLuaOrder { packCode }` | `CreateOrderResponse` |
| POST | `/api/payments/webhook` | public | PayOS webhook (idempotent grant) | `JsonNode` | `Map` → `{ code:"00", desc:"success" }` (always 200) |
| GET | `/api/payments/status/{orderCode}` | auth | Order status | `orderCode: long` | `PaymentOrderView` |
| GET | `/api/payments/history` | auth | Order history | `page` (def 0), `size` (def 20, max 100) | `List<PaymentOrderView>` |
| GET | `/api/payments/lua-packs` | public | Active Lúa packs | — | `List<LuaPackView>` |
| GET | `/api/payments/config-status` | public | PayOS configured? | — | `Map` → `{ configured }` |

**`CreateOrderResponse`**: `orderCode (long), checkoutUrl, amountVnd, status, mock (boolean — true
when PayOS unconfigured → mock checkout URL)`.
**`PaymentOrderView`**: `orderCode, type, tierCode, luaAmount, amountVnd, status, checkoutUrl,
createdAt, paidAt, expiresAt`.
**`LuaPackView`**: `id, code, name, emoji, luaAmount, priceVnd, discountPercent, bonusLua,
totalLua, descriptionVi, descriptionEn, displayOrder`.

#### `CreditController` · `/api/credits` · **authenticated**

Source: `com.cramer.billing.web.CreditController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/credits` | Balance/stats | — | `CreditStatsView` |
| GET | `/api/credits/stats` | Balance/stats (alias) | — | `CreditStatsView` |
| GET | `/api/credits/check/{amount}` | Sufficient-balance check | `amount: int` | `Map` → `{ sufficient, balance, required }` |
| GET | `/api/credits/transactions` | Transaction history | `page` (def 0), `size` (def 20, max 100) | `List<TransactionView>` |
| GET | `/api/credits/history` | History (alias) | `page`, `size` | `List<TransactionView>` |
| GET | `/api/credits/packages` | Active Lúa packs | — | `List<LuaPackView>` |
| POST | `/api/credits/purchase` | Buy a Lúa pack | `BillingRequests.CreateLuaOrder { packCode }` | `CreateOrderResponse` |

**`CreditStatsView`**: `balance, lifetimeEarned, lifetimeSpent` (all int).
**`TransactionView`**: `id, amount, balanceAfter, type, category, description, referenceId, createdAt`.

---

### Module: engagement

AI chat assistant, vocabulary notebook + translation, and dashboard read-models / IELTS goal.

#### `ChatController` · `/api/chat` · **authenticated**

Source: `com.cramer.engagement.web.ChatController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| POST | `/api/chat` | Send a message | `ChatRequest { message @NotBlank }` | `ChatResponse { reply, remaining }` |
| GET | `/api/chat/history` | History | `limit` (int, def 50) | `List<ChatMessageView>` |
| GET | `/api/chat/remaining` | Remaining allowance | — | `Map` → `{ remaining }` |
| DELETE | `/api/chat/history` | Clear history | — | **204** |

**`ChatResponse`**: `reply, remaining (int; −1 = unlimited)`.
**`ChatMessageView`**: `role, content, createdAt`.

#### `VocabularyController` · `/api/vocabulary` · **authenticated**

Source: `com.cramer.engagement.web.VocabularyController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/vocabulary` | Paged list | `page` (def 0), `size` (def 20), `search?`, `filter?` | `PageResponse<VocabularyView>` |
| GET | `/api/vocabulary/stats` | Stats | — | `VocabularyStats` |
| POST | `/api/vocabulary/translate` | Translate a word | body `Map<String,String>` (`{ word }`) | `TranslationView` |
| GET | `/api/vocabulary/{id}` | Get entry | `id: Long` | `VocabularyView` |
| POST | `/api/vocabulary` | Create entry | `VocabularyRequest` | `VocabularyView` · **201** |
| PUT | `/api/vocabulary/{id}` | Update entry | `id`; `VocabularyRequest` | `VocabularyView` |
| DELETE | `/api/vocabulary/{id}` | Delete entry | `id` | **204** |
| PUT | `/api/vocabulary/{id}/toggle-mastered` | Toggle mastered | `id` | `VocabularyView` |

**`VocabularyRequest`**: `word @NotBlank, translation?, phonetic?, partOfSpeech?, definition?,
exampleSentence?, sourceContext?, sourceTestId?, sourceSectionId?, notes?`.
**`VocabularyView`**: `id, word, translation, phonetic, partOfSpeech, definition, exampleSentence,
sourceContext, notes, isMastered, reviewCount, lastReviewedAt, createdAt`.
**`VocabularyStats`**: `total, mastered, learning (long), masteredPercent (int)`.
**`TranslationView`**: `translation, phonetic, partOfSpeech, definition, exampleSentence`.

#### `DashboardController` · `/api/dashboard` · **authenticated**

Source: `com.cramer.engagement.web.DashboardController`. Read-only projections + IELTS goal upsert.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/dashboard/summary` | Aggregate summary | `page` (def 0), `size` (def 20), `search?` | `DashboardDtos.SummaryView` |
| GET | `/api/dashboard/course-history` | Per-attempt course history | `examSource?`, `testNumber?`, `skill?` | `List<DashboardDtos.CourseHistoryItem>` |
| GET | `/api/dashboard/target` | Get IELTS goal | — | `Map` → `{ target: TargetView | null }` |
| POST | `/api/dashboard/target` | Upsert IELTS goal | `TargetRequest` | `TargetView` |

**`TargetRequest`**: `examName @NotBlank, examDate? (LocalDate), listening?/reading?/writing?/speaking?
(Double @DecimalMin "0" @DecimalMax "9")`.
**`TargetView`**: `examName, examDate, listening, reading, writing, speaking`.
**`DashboardDtos.SummaryView`**: `profile (ProfileBrief), target (TargetView), stats (UserStats),
perSkillAccuracy (List<SkillAccuracy>), courseProgress (List<CourseProgressItem>),
recentActivity (List<ActivityBrief>)` — see `engagement/web/dto/DashboardDtos.java` for nested shapes.
**`DashboardDtos.CourseHistoryItem`**: `attemptId, skill, status, score, answered, correct,
startedAt, completedAt`.

---

### Module: admin

Cross-domain admin console: users, audit/activity, dashboard, finance. All paths under
`/api/admin/**` and admin-gated. Admin id is the authenticated principal (never a header).

#### `AdminDashboardController` · `/api/admin/dashboard` · **admin**

Source: `com.cramer.admin.web.AdminDashboardController`.

| Method | Path | Purpose | Params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/dashboard/stats` | Top-level counts | — | `AdminDtos.DashboardStatsView` |
| GET | `/api/admin/dashboard/activities` | Recent activity | `limit` (def 20) | `List<AdminDtos.AuditLogView>` |
| GET | `/api/admin/dashboard/status` | System status | — | `AdminDtos.SystemStatusView` |

**`DashboardStatsView`**: `totalUsers, activeSubscriptions, premiumSubscriptions, testsTaken,
writingSubmissions, paidOrders, revenueVnd` (all long).
**`SystemStatusView`**: `db, payment, ai`.

#### `AdminActivityController` · `/api/admin/activities` · **admin**

Source: `com.cramer.admin.web.AdminActivityController`.

| Method | Path | Purpose | Params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/activities/audit` | Admin audit trail | `page` (def 0), `size` (def 20) | `List<AdminDtos.AuditLogView>` |
| GET | `/api/admin/activities/audit/users/{userId}` | Per-user audit | `userId: UUID`; `page`, `size` | `List<AdminDtos.AuditLogView>` |
| GET | `/api/admin/activities/users/{userId}` | Per-user activity timeline | `userId: UUID`; `type?`, `page`, `size` | `List<AdminDtos.UserActivityView>` |
| GET | `/api/admin/activities/users/{userId}/recent` | Recent user activity | `userId: UUID`; `limit` (def 10) | `List<AdminDtos.UserActivityView>` |

**`AuditLogView`**: `id, adminUserId, action, targetType, targetId, description, createdAt`.
**`UserActivityView`**: `id, activityType, title, description, createdAt`.

#### `AdminFinanceController` · `/api/admin/finance` · **admin**

Source: `com.cramer.admin.web.AdminFinanceController`. Figures derive from
`payment_orders.status='PAID'` + `amount_vnd` (read-only).

| Method | Path | Purpose | Params | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/finance/overview` | Revenue overview | `period` (def `30d`) | `AdminDtos.FinanceOverviewView` |
| GET | `/api/admin/finance/breakdown` | Revenue by type | `period` (def `30d`) | `AdminDtos.RevenueBreakdownView` |
| GET | `/api/admin/finance/top-spenders` | Top spenders | `limit` (def 10) | `List<AdminDtos.TopSpenderView>` |
| GET | `/api/admin/finance/transactions` | Transactions | `status?`, `type?`, `page` (def 0), `size` (def 20) | `List<AdminDtos.AuditLogView>` |

**`FinanceOverviewView`**: `totalRevenueVnd, paidOrders, subscriptionRevenueVnd, luaRevenueVnd`.
**`RevenueBreakdownView`**: `slices: List<RevenueSlice{ type, revenueVnd, orders }>`.
**`TopSpenderView`**: `userId, username, totalSpentVnd, orders`.

#### `AdminUserController` · `/api/admin/users` · **admin**

Source: `com.cramer.admin.web.AdminUserController`.

| Method | Path | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/users` | List/search users | `search?`, `status?`, `sortBy` (def `created_at`), `sortOrder` (def `desc`), `page` (def 0), `size` (def 20) | `List<AdminDtos.AdminUserView>` |
| GET | `/api/admin/users/stats` | User stats | — | `AdminDtos.UserStatsView` |
| GET | `/api/admin/users/{id}` | User detail | `id: UUID` | `AdminDtos.AdminUserDetailView` |
| PATCH | `/api/admin/users/{id}/status` | Set account status | `id`; `StatusChangeRequest { status, reason }` | `Map` → `{ success }` |
| PATCH | `/api/admin/users/{id}/credits` | Adjust Lúa | `id`; `CreditAdjustRequest { amount, reason }` | `Map` → `{ success, balance }` |
| PATCH | `/api/admin/users/{id}/subscription` | Set subscription | `id`; `SubscriptionChangeRequest { tierCode, months? }` (months def 1) | `Map` → `{ success }` |

**`AdminUserView`**: `id, username, fullName, accountStatus, isAdmin, tierCode, luaBalance, createdAt`.
**`AdminUserDetailView`**: `id, username, fullName, accountStatus, statusReason, isAdmin, tierCode,
subscriptionStatus, subscriptionExpiresAt, luaBalance, lifetimeEarned, lifetimeSpent, createdAt`.
**`UserStatsView`**: `totalUsers, activeUsers, premiumUsers, newThisMonth`.

---

### Module: abts

AI-Based Test Generation System (OpenRouter). All routes under `/api/admin/abts` and admin-gated.
Errors use the global `ApiError` model (never a `200 {success:false}` wrapper). Includes two
**SSE streaming** endpoints.

#### `AbtsController` · `/api/admin/abts` · **admin**

Source: `com.cramer.abts.web.AbtsController`. `{skill}` ∈ `reading | listening | writing` (parsed
via `Skill.valueOf`; unknown → `400`).

| Method | Path | Type | Purpose | Params / Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/admin/abts/generate/{skill}` | sync | Generate content for a skill | `skill`; `GenerationRequest` | `GenerationResult` |
| POST | `/api/admin/abts/generate/{skill}/stream` | **SSE** | Streamed generation (`text/event-stream`) | `skill`; `GenerationRequest` | `SseEmitter` |
| POST | `/api/admin/abts/generate/questions` | sync | Regenerate questions only | `skill` (query); `GenerationRequest` | `GenerationResult` |
| POST | `/api/admin/abts/validate` | sync | Validate content | `skill`, `part` (def 1), `taskType?` (query); body `JsonNode` | `ValidationView` |
| POST | `/api/admin/abts/refine/stream` | **SSE** | Streamed refinement diff hunks | `RefinementRequest` | `SseEmitter` |
| POST | `/api/admin/abts/refine/apply` | sync | Apply accepted hunks | `RefinementApplyRequest` | `RefinementApplyResponse` |
| POST | `/api/admin/abts/save` | sync | Save generated content as draft | `SaveContentRequest` | `SaveContentResponse` |
| GET | `/api/admin/abts/models` | sync | List models | — | `ArrayNode` (JSON array) |
| GET | `/api/admin/abts/models/capabilities/{id}` | sync | Model capability | `id: String` | `JsonNode` |
| GET | `/api/admin/abts/templates` | sync | Template categories | — | `ArrayNode` |
| GET | `/api/admin/abts/templates/{categoryId}` | sync | Templates in a category | `categoryId: String` | `ArrayNode` |
| GET | `/api/admin/abts/status` | sync | Service status | — | `StatusResponse` |

**`GenerationRequest`**: `partsToGenerate (List<Integer>), parts (Map<String,PartConfig> keyed by
part number), model (ModelConfig), explanationLanguage?, customInstructions?, existingPassageText?`.
**`PartConfig`**: `topic?, factsMode? (AUTO|STRICT), facts? (List<String>), questionTypes?
(List<String>), questionTypeCounts? (Map<String,Integer>), totalQuestions?, passageLength?,
difficulty?, taskType? (ACADEMIC_TASK_1|GENERAL_TASK_1|TASK_2)`.
**`ModelConfig`**: `model?, temperature? (def 0.7), maxTokens?, enableReasoning?, reasoningEffort?
(high|medium|low), reasoningBudget?, contextCache?`.
**`RefinementRequest`**: `originalJson (JsonNode), issueIds (List<String>, non-empty), skill, part?,
taskType?, model (ModelConfig), round? (0-based), validation (JsonNode)`.
**`RefinementApplyRequest`**: `originalJson (JsonNode), acceptedHunks (List<Hunk>), skill, part?,
taskType?`.
**`RefinementApplyResponse`**: `content (JsonNode), skipped (List<String>), validation (ValidationView)`.
**`SaveContentRequest`**: `setCode?, setId?, testNumber?, testId?, testName?, difficulty?, hashtags?
(List<String>), generationMetadata? (JsonNode), sections (List<SaveSectionInput{ skill, partNumber,
passageText, audioUrl, sectionLayout (JsonNode), imageDescription, questions (JsonNode) }])`.
**`SaveContentResponse`**: `success, setId, setCode, testId, testNumber, sectionIds (List<Long>),
questionCount (int), message`.
**`StatusResponse`**: `keyConfigured, defaultModel, emitterTimeoutMs, partTimeoutMs,
maxRefinementRounds, version`.
**`GenerationResult`** (ABTS domain record, `abts/domain/GenerationResult.java`): `status
(SUCCESS|PARTIAL_SUCCESS|FAILED|NOT_IMPLEMENTED), skill, content (JsonNode), validation
(ValidationView), partErrors (Map<Integer,String>), reasoning, usage (TokenUsage), model, attempts
(int), errorCode, retryable (Boolean)`.
**`ValidationView`** (`abts/validation/ValidationView.java`): `valid (boolean), issues
(List<ValidationIssue>), errors (List<String>), warnings (List<String>), errorCount, warningCount`.

---

## Verification Notes & Caveats

All endpoints above were verified by reading the controller `.java` source under
`backend/src/main/java/com/cramer/**/web/`. The following points warrant explicit notes:

1. **WebSocket / `/ws/**` — not implemented.** `SecurityConfig` permits `/ws/**`, and
   `spring-boot-starter-websocket` is on the classpath, **but no WebSocket handler, `@ServerEndpoint`,
   or `registerWebSocketHandlers` configuration exists** anywhere in `com.cramer` (verified by grep).
   The speaking realtime/audio dependencies (`java-websocket`, `google-cloud-speech`,
   `grpc-netty-shaded`, `google-cloud-vertexai`) were removed for **CVE-2025-55163** (see `pom.xml`
   lines 192–195) and grading runs `text_only` (`SpeakingGradingWorker.java:25`). The `/ws/**`
   permit rule and the "Realtime examiner audio uses the WS endpoint" Javadoc are **vestigial** —
   no such backend endpoint is currently served.

2. **"Public" controllers that require auth.** `CourseController` and the subscription **tier**
   endpoints are described as "public catalog" in their Javadoc, but they are **not** in the
   `SecurityConfig` `permitAll()` list, so the HTTP layer requires a valid Bearer token. They are
   tagged **authenticated** here. The genuinely anonymous endpoints are only those in the
   `permitAll()` list (health, `/api/auth/**`, the three payment public routes, swagger/api-docs,
   `/error`, `/ws/**`).

3. **`Map<...>` and `JsonNode` responses.** Endpoints returning `Map` (e.g. several billing/chat
   status checks, admin mutation acknowledgements) build ad-hoc JSON inline; documented keys are
   taken verbatim from source. `JsonNode`/`ArrayNode` responses (speaking `results`, ABTS
   `models`/`templates`/`capabilities`) are opaque trees whose internal shape is not a fixed record
   and is intentionally **not** enumerated to avoid fabrication.

4. **Nested DTO shapes.** For grouped DTO holders (`DashboardDtos`, `AdminDtos`, `BillingRequests`),
   nested record fields are summarised; consult the cited source files for the exhaustive field
   lists. No request/response example bodies were invented — every field name above comes directly
   from a record declaration.

5. **`GenerationResult` / `ValidationView`** are returned by ABTS endpoints but live in `abts/domain`
   and `abts/validation` (not `web/dto`); they are documented from their actual record declarations.

6. **Not independently verified in this pass:** rate-limit thresholds and HTTP `429` triggers
   (mentioned in writing/speaking Javadoc but enforced in `platform/ratelimit` + services, not the
   controllers); the exact JSON schema of all `JsonNode` payloads; and `402` quota-block bodies
   (`ApiError.blockType`) which are produced by services/handlers rather than declared in
   controllers.
