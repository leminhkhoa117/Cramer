# 📋 Cramer - Controller Test Cases Plan

> **Created:** January 19, 2026  
> **Author:** Cramer Test Team  
> **Status:** 📝 Planning Phase  
> **Estimated Tests:** ~180 controller tests

---

## 📚 Mục lục

1. [Tổng quan](#-tổng-quan)
2. [Cấu trúc & Convention](#-cấu-trúc--convention)
3. [Test Plan chi tiết](#-test-plan-chi-tiết)
   - [AuthControllerTest](#1-authcontrollertest-4-tests)
   - [ProfileControllerTest](#2-profilecontrollertest-10-tests)
   - [TestControllerTest](#3-testcontrollertest-8-tests)
   - [TestAttemptControllerTest](#4-testattemptcontrollertest-28-tests)
   - [WritingControllerTest](#5-writingcontrollertest-22-tests)
   - [PaymentControllerTest](#6-paymentcontrollertest-24-tests)
   - [CreditControllerTest](#7-creditcontrollertest-18-tests)
   - [SubscriptionControllerTest](#8-subscriptioncontrollertest-16-tests)
   - [DashboardControllerTest](#9-dashboardcontrollertest-8-tests)
   - [QuotaControllerTest](#10-quotacontrollertest-8-tests)
   - [CourseControllerTest](#11-coursecontrollertest-8-tests)
   - [ChatControllerTest](#12-chatcontrollertest-14-tests)
   - [QuestionControllerTest](#13-questioncontrollertest-12-tests)
   - [SectionControllerTest](#14-sectioncontrollertest-12-tests)
   - [VocabularyControllerTest](#15-vocabularycontrollertest-16-tests)
4. [Admin Controllers](#-admin-controllers)
5. [Tài liệu tham khảo](#-tài-liệu-tham-khảo)

---

## 🎯 Tổng quan

### Mục tiêu
- Test tất cả REST endpoints trong layer Controller
- Đảm bảo routing, request mapping, response status codes đúng
- Kiểm tra input validation và error handling
- Kiểm tra authorization và IDOR protection
- Mock service layer để isolated testing

### Công nghệ sử dụng
- **Spring Boot Test** - `@WebMvcTest` cho lightweight controller testing
- **MockMvc** - Simulate HTTP requests
- **Mockito** - Mock service dependencies
- **JUnit 5** - Test framework
- **AssertJ** - Fluent assertions
- **@WithMockUser** - Mock Spring Security context

### Test Coverage Goals
| Loại Test | Số lượng | Mức độ ưu tiên |
|-----------|----------|----------------|
| User Controllers | ~130 tests | 🔴 High |
| Admin Controllers | ~50 tests | 🟡 Medium |
| **Tổng cộng** | **~180 tests** | - |

---

## 🏗️ Cấu trúc & Convention

### Thư mục
```
backend/src/test/java/com/cramer/controller/
├── AuthControllerTest.java
├── ProfileControllerTest.java
├── TestControllerTest.java
├── TestAttemptControllerTest.java
├── WritingControllerTest.java
├── PaymentControllerTest.java
├── CreditControllerTest.java
├── SubscriptionControllerTest.java
├── DashboardControllerTest.java
├── QuotaControllerTest.java
├── CourseControllerTest.java
├── ChatControllerTest.java
├── QuestionControllerTest.java
├── SectionControllerTest.java
├── VocabularyControllerTest.java
└── admin/
    ├── ABTSControllerTest.java
    ├── AdminDashboardControllerTest.java
    ├── AdminUserControllerTest.java
    └── TestHierarchyControllerTest.java
```

### Naming Convention
```java
@Test
@DisplayName("Should return 200 when profile exists")
void shouldReturn200WhenProfileExists() { ... }

// Pattern: should[Expected]When[Condition]
```

### Base Test Template
```java
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, TestSecurityConfig.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }
}
```

### Security Test Config
```java
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", UUID.randomUUID().toString())
            .build();
    }
}
```

---

## 📝 Test Plan chi tiết

---

## 1. AuthControllerTest (4 tests)

**File:** `backend/src/test/java/com/cramer/controller/AuthControllerTest.java`  
**Controller:** `/api/auth`  
**Requires Auth:** ❌ No (public endpoint)

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/check-email` | Check if email exists |

### Test Cases

| # | Test Name | Description | HTTP Status | Assertions |
|---|-----------|-------------|-------------|------------|
| 1 | `checkEmail_validEmail_returnsExists` | Email exists in system | 200 | `{"exists": true}` |
| 2 | `checkEmail_unknownEmail_returnsNotExists` | Email not registered | 200 | `{"exists": false}` |
| 3 | `checkEmail_nullRequest_returnsBadRequest` | Request body is null | 400 | No body |
| 4 | `checkEmail_blankEmail_returnsBadRequest` | Email is blank/empty | 400 | No body |

### Key Assertions
- Verify service method `checkEmailExists()` is called with correct email
- Verify JSON response structure `{"exists": boolean}`

---

## 2. ProfileControllerTest (10 tests)

**File:** `backend/src/test/java/com/cramer/controller/ProfileControllerTest.java`  
**Controller:** `/api/profiles`  
**Requires Auth:** ✅ Yes (JWT Bearer token)

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/profiles/{id}` | Get profile by UUID |
| PUT | `/api/profiles/{id}` | Update profile |

### Test Cases

#### GET /api/profiles/{id}

| # | Test Name | Description | HTTP Status | Assertions |
|---|-----------|-------------|-------------|------------|
| 1 | `getProfile_exists_returns200` | Profile exists and user owns it | 200 | Returns ProfileDTO |
| 2 | `getProfile_notFound_returns404` | Profile doesn't exist | 404 | Error message |
| 3 | `getProfile_unauthorized_returns401` | No JWT token | 401 | Unauthorized |
| 4 | `getProfile_idorViolation_returns403` | User tries to access another's profile | 403 | AccessDenied |
| 5 | `getProfile_invalidUuid_returns400` | Invalid UUID format | 400 | Bad request |

#### PUT /api/profiles/{id}

| # | Test Name | Description | HTTP Status | Assertions |
|---|-----------|-------------|-------------|------------|
| 6 | `updateProfile_valid_returns200` | Valid update request | 200 | Updated ProfileDTO |
| 7 | `updateProfile_unauthorized_returns401` | No JWT token | 401 | Unauthorized |
| 8 | `updateProfile_idorViolation_returns403` | User tries to update another's profile | 403 | AccessDenied |
| 9 | `updateProfile_invalidData_returns400` | Invalid field values | 400 | Validation errors |
| 10 | `updateProfile_partialUpdate_preservesFields` | Only update some fields | 200 | Other fields unchanged |

### Mock Data Types
```java
ProfileDTO mockProfile = ProfileDTO.builder()
    .id(testUserId)
    .username("testuser")
    .fullName("Test User")
    .hasLlmApiKey(false)
    .build();
```

---

## 3. TestControllerTest (8 tests)

**File:** `backend/src/test/java/com/cramer/controller/TestControllerTest.java`  
**Controller:** `/api/tests`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tests/data` | Get test sections (SAFE mode, no answers) |

### Test Cases

| # | Test Name | Description | HTTP Status | Assertions |
|---|-----------|-------------|-------------|------------|
| 1 | `getTestData_valid_returns200` | Valid source/test/skill params | 200 | List<TestSectionDTO> |
| 2 | `getTestData_noData_returns404` | Test doesn't exist | 404 | Not found |
| 3 | `getTestData_unauthorized_returns401` | No JWT | 401 | Unauthorized |
| 4 | `getTestData_missingSource_returns400` | Missing source param | 400 | Bad request |
| 5 | `getTestData_missingTest_returns400` | Missing test param | 400 | Bad request |
| 6 | `getTestData_missingSkill_returns400` | Missing skill param | 400 | Bad request |
| 7 | `getTestData_invalidTestNumber_returns400` | test=abc (non-integer) | 400 | Bad request |
| 8 | `getTestData_verifySafeMode_noAnswers` | Response contains no answer fields | 200 | Answers null/hidden |

### Request Parameters
```
GET /api/tests/data?source=cambridge&test=1&skill=reading
```

---

## 4. TestAttemptControllerTest (28 tests)

**File:** `backend/src/test/java/com/cramer/controller/TestAttemptControllerTest.java`  
**Controller:** `/api/test-attempts`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/test-attempts/start` | Start or resume test attempt |
| POST | `/api/test-attempts/{id}/submit` | Submit test for grading |
| POST | `/api/test-attempts/{id}/progress` | Save progress |
| POST | `/api/test-attempts/{id}/cancel` | Cancel attempt |
| POST | `/api/test-attempts/{id}/resume` | Mark for resume |
| GET | `/api/test-attempts/{id}/answers` | Get attempt answers |
| GET | `/api/test-attempts/{id}/review` | Get test review |
| DELETE | `/api/test-attempts/{id}` | Delete attempt |
| POST | `/api/test-attempts/{id}/regrade` | Re-grade attempt |

### Test Cases

#### POST /start

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `start_valid_returns200` | Valid request creates/resumes attempt | 200 |
| 2 | `start_unauthorized_returns401` | No JWT | 401 |
| 3 | `start_missingSource_returns400` | Missing source param | 400 |
| 4 | `start_forceNew_createsNew` | forceNew=true creates new | 200 |

#### POST /{id}/submit

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 5 | `submit_valid_returns200` | Submit with answers | 200 |
| 6 | `submit_unauthorized_returns401` | No JWT | 401 |
| 7 | `submit_nullAnswers_returns400` | Null answers | 400 |
| 8 | `submit_attemptNotFound_returns404` | Invalid attempt ID | 404 |

#### POST /{id}/progress

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 9 | `saveProgress_valid_returns200` | Valid progress save | 200 |
| 10 | `saveProgress_unauthorized_returns401` | No JWT | 401 |
| 11 | `saveProgress_attemptNotFound_returns404` | Invalid ID | 404 |

#### POST /{id}/cancel

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 12 | `cancel_inProgress_returns200` | Cancel IN_PROGRESS | 200 |
| 13 | `cancel_unauthorized_returns401` | No JWT | 401 |
| 14 | `cancel_idorViolation_returns403` | Wrong user | 403 |
| 15 | `cancel_alreadyCompleted_returns200` | Idempotent | 200 |

#### POST /{id}/resume

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 16 | `resume_valid_returns200` | Resume attempt | 200 |
| 17 | `resume_unauthorized_returns401` | No JWT | 401 |

#### GET /{id}/answers

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 18 | `getAnswers_valid_returns200` | Get answers list | 200 |
| 19 | `getAnswers_unauthorized_returns401` | No JWT | 401 |
| 20 | `getAnswers_idorViolation_returns403` | Wrong user | 403 |

#### GET /{id}/review

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 21 | `getReview_valid_returns200` | Get review DTO | 200 |
| 22 | `getReview_unauthorized_returns401` | No JWT | 401 |
| 23 | `getReview_notCompleted_returns400` | Attempt still in progress | 400 |

#### DELETE /{id}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 24 | `delete_valid_returns204` | Delete attempt | 204 |
| 25 | `delete_unauthorized_returns401` | No JWT | 401 |
| 26 | `delete_idorViolation_returns403` | Wrong user | 403 |

#### POST /{id}/regrade

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 27 | `regrade_valid_returns200` | Re-grade attempt | 200 |
| 28 | `regrade_unauthorized_returns401` | No JWT | 401 |

### Request/Response DTOs
```java
// Request
AnswerSubmissionDTO submission = new AnswerSubmissionDTO();
submission.setAnswers(Map.of("Q1", "A", "Q2", "B"));

// Response
TestResultDTO result = TestResultDTO.builder()
    .attemptId(1L)
    .score(35)
    .totalQuestions(40)
    .build();
```

---

## 5. WritingControllerTest (22 tests)

**File:** `backend/src/test/java/com/cramer/controller/WritingControllerTest.java`  
**Controller:** `/api/writing`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/writing/draft/{attemptId}` | Save essay draft |
| POST | `/api/writing/submit/{attemptId}` | Submit for AI grading |
| GET | `/api/writing/status/{attemptId}` | Get grading status |
| GET | `/api/writing/review/{attemptId}` | Get full review |
| GET | `/api/writing/submissions/{attemptId}` | Get submissions |
| POST | `/api/writing/validate-api-key` | Validate LLM API key |
| POST | `/api/writing/regrade/{attemptId}` | Re-grade attempt |

### Test Cases

#### POST /draft/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `saveDraft_valid_returns200` | Save draft successfully | 200 |
| 2 | `saveDraft_unauthorized_returns401` | No JWT | 401 |
| 3 | `saveDraft_invalidTask_returns400` | taskNumber invalid | 400 |
| 4 | `saveDraft_attemptNotFound_returns404` | Invalid attemptId | 404 |

#### POST /submit/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 5 | `submit_valid_returns200` | Submit essays for grading | 200 |
| 6 | `submit_unauthorized_returns401` | No JWT | 401 |
| 7 | `submit_emptyEssays_returns400` | No essays | 400 |
| 8 | `submit_rateLimitExceeded_returns429` | Too many requests | 429 |

#### GET /status/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 9 | `getStatus_grading_returns200` | Status is GRADING | 200 |
| 10 | `getStatus_completed_returns200` | Status is COMPLETED | 200 |
| 11 | `getStatus_unauthorized_returns401` | No JWT | 401 |

#### GET /review/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 12 | `getReview_valid_returns200` | Get writing review | 200 |
| 13 | `getReview_unauthorized_returns401` | No JWT | 401 |
| 14 | `getReview_notCompleted_returns400` | Still grading | 400 |

#### GET /submissions/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 15 | `getSubmissions_valid_returns200` | Get submissions list | 200 |
| 16 | `getSubmissions_unauthorized_returns401` | No JWT | 401 |

#### POST /validate-api-key

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 17 | `validateApiKey_valid_returns200` | Valid API key | 200 |
| 18 | `validateApiKey_invalid_returns200` | Invalid key, valid=false | 200 |
| 19 | `validateApiKey_emptyKey_returns400` | No key provided | 400 |

#### POST /regrade/{attemptId}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 20 | `regrade_valid_returns200` | Re-grade successfully | 200 |
| 21 | `regrade_unauthorized_returns401` | No JWT | 401 |
| 22 | `regrade_rateLimitExceeded_returns429` | Rate limit | 429 |

---

## 6. PaymentControllerTest (24 tests)

**File:** `backend/src/test/java/com/cramer/controller/PaymentControllerTest.java`  
**Controller:** `/api/payments`  
**Requires Auth:** ✅ Yes (except webhook)

### Endpoints Covered
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/subscription` | ✅ | Create subscription payment |
| POST | `/lua` | ✅ | Create Lúa pack payment |
| POST | `/webhook` | ❌ | PayOS webhook |
| GET | `/status/{orderCode}` | ✅ | Get payment status |
| GET | `/history` | ✅ | Get payment history |
| GET | `/lua-packs` | ❌ | Get Lúa pack options |
| GET | `/config-status` | ❌ | Check PayOS config |

### Test Cases

#### POST /subscription

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `createSubscription_valid_returns200` | Valid tier code | 200 |
| 2 | `createSubscription_invalidType_returns400` | Wrong type | 400 |
| 3 | `createSubscription_unauthorized_returns401` | No JWT | 401 |
| 4 | `createSubscription_tierNotFound_returns404` | Invalid tier | 404 |

#### POST /lua

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 5 | `createLuaPack_valid_returns200` | Valid lua pack | 200 |
| 6 | `createLuaPack_invalidType_returns400` | Wrong type | 400 |
| 7 | `createLuaPack_missingAmount_returns400` | No luaAmount | 400 |
| 8 | `createLuaPack_unauthorized_returns401` | No JWT | 401 |

#### POST /webhook (Public)

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 9 | `webhook_success_returns200` | Valid webhook | 200 |
| 10 | `webhook_invalidSignature_returns403` | Bad signature | 403 |
| 11 | `webhook_cancelled_returns200` | Cancelled payment | 200 |
| 12 | `webhook_duplicate_returns200` | Duplicate webhook | 200 |

#### GET /status/{orderCode}

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 13 | `getStatus_valid_returns200` | Get order status | 200 |
| 14 | `getStatus_notFound_returns404` | Invalid order | 404 |
| 15 | `getStatus_unauthorized_returns401` | No JWT | 401 |
| 16 | `getStatus_idorViolation_returns403` | Wrong user | 403 |

#### GET /history

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 17 | `getHistory_valid_returns200` | Get history page | 200 |
| 18 | `getHistory_unauthorized_returns401` | No JWT | 401 |
| 19 | `getHistory_pagination_works` | page=1, size=10 | 200 |
| 20 | `getHistory_sizeLimit_capped` | size=100 -> capped to 50 | 200 |

#### GET /lua-packs (Public)

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 21 | `getLuaPacks_returns200` | Returns pack list | 200 |
| 22 | `getLuaPacks_verifyStructure` | Correct JSON structure | 200 |

#### GET /config-status (Public)

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 23 | `getConfigStatus_configured_returns200` | PayOS configured | 200 |
| 24 | `getConfigStatus_notConfigured_returns200` | PayOS not set | 200 |

---

## 7. CreditControllerTest (18 tests)

**File:** `backend/src/test/java/com/cramer/controller/CreditControllerTest.java`  
**Controller:** `/api/credits`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get balance |
| GET | `/check/{amount}` | Check if has enough |
| GET | `/transactions` | Transaction history |
| GET | `/stats` | User stats |
| GET | `/packages` | Available packages |
| POST | `/purchase` | Purchase package |
| GET | `/history` | History with filter |

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getBalance_valid_returns200` | Get Lúa balance | 200 |
| 2 | `getBalance_unauthorized_returns401` | No JWT | 401 |
| 3 | `hasEnoughCredits_true_returns200` | Has enough | 200 |
| 4 | `hasEnoughCredits_false_returns200` | Not enough | 200 |
| 5 | `hasEnoughCredits_invalidAmount_returns400` | Negative amount | 400 |
| 6 | `getTransactions_valid_returns200` | Paginated transactions | 200 |
| 7 | `getTransactions_pagination_works` | page/size params | 200 |
| 8 | `getStats_valid_returns200` | Full stats | 200 |
| 9 | `getStats_unauthorized_returns401` | No JWT | 401 |
| 10 | `getPackages_returns200` | Package list | 200 |
| 11 | `purchasePackage_valid_returns200` | Valid package code | 200 |
| 12 | `purchasePackage_invalidCode_returns400` | Invalid package | 400 |
| 13 | `purchasePackage_unauthorized_returns401` | No JWT | 401 |
| 14 | `getHistory_all_returns200` | type=all | 200 |
| 15 | `getHistory_earn_returns200` | type=earn | 200 |
| 16 | `getHistory_spend_returns200` | type=spend | 200 |
| 17 | `getHistory_pagination_works` | Pagination | 200 |
| 18 | `getHistory_sizeLimit_capped` | size capped to 100 | 200 |

---

## 8. SubscriptionControllerTest (16 tests)

**File:** `backend/src/test/java/com/cramer/controller/SubscriptionControllerTest.java`  
**Controller:** `/api/subscriptions`  
**Requires Auth:** ✅ Yes (except /tiers)

### Endpoints Covered
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/tiers` | ❌ | All subscription tiers |
| GET | `/tiers/{code}` | ❌ | Tier by code |
| GET | `/current` | ✅ | User's subscription |
| GET | `/grading-status` | ✅ | AI grading allowed |
| GET | `/gradings-remaining` | ✅ | Remaining count |
| GET | `/chat-limit` | ✅ | Daily chat limit |
| GET | `/my-status` | ✅ | Full status |
| PUT | `/ai-grading` | ✅ | Toggle AI grading |

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getAllTiers_returns200` | List all tiers | 200 |
| 2 | `getTierByCode_valid_returns200` | Get cramerie tier | 200 |
| 3 | `getTierByCode_notFound_returns404` | Invalid code | 404 |
| 4 | `getCurrentSubscription_returns200` | User's sub | 200 |
| 5 | `getCurrentSubscription_unauthorized_returns401` | No JWT | 401 |
| 6 | `getGradingStatus_allowed_returns200` | Has quota | 200 |
| 7 | `getGradingStatus_exhausted_returns200` | No quota left | 200 |
| 8 | `getGradingsRemaining_returns200` | Remaining count | 200 |
| 9 | `getChatLimit_returns200` | Chat limit | 200 |
| 10 | `getMyStatus_returns200` | Full status DTO | 200 |
| 11 | `getMyStatus_verifyStructure` | Correct nested structure | 200 |
| 12 | `toggleAiGrading_enable_returns200` | Enable AI | 200 |
| 13 | `toggleAiGrading_disable_returns200` | Disable AI | 200 |
| 14 | `toggleAiGrading_cramerieUser_returns403` | Free user can't enable | 403 |
| 15 | `toggleAiGrading_missingField_returns400` | No enabled field | 400 |
| 16 | `toggleAiGrading_unauthorized_returns401` | No JWT | 401 |

---

## 9. DashboardControllerTest (8 tests)

**File:** `backend/src/test/java/com/cramer/controller/DashboardControllerTest.java`  
**Controller:** `/api/dashboard`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/summary` | Dashboard summary |
| POST | `/target` | Save target |

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getSummary_valid_returns200` | Get summary | 200 |
| 2 | `getSummary_unauthorized_returns401` | No JWT | 401 |
| 3 | `getSummary_withSearch_returns200` | Search param | 200 |
| 4 | `getSummary_pagination_works` | page/size | 200 |
| 5 | `saveTarget_valid_returns200` | Save target | 200 |
| 6 | `saveTarget_unauthorized_returns401` | No JWT | 401 |
| 7 | `saveTarget_invalidData_returns400` | Invalid target | 400 |
| 8 | `saveTarget_nullBody_returns400` | No body | 400 |

---

## 10. QuotaControllerTest (8 tests)

**File:** `backend/src/test/java/com/cramer/controller/QuotaControllerTest.java`  
**Controller:** `/api/quotas`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Quota status |
| GET | `/can-attempt` | Pre-check attempt |
| GET | `/check` | Alias for can-attempt |

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getQuotaStatus_returns200` | Get status | 200 |
| 2 | `getQuotaStatus_unauthorized_returns401` | No JWT | 401 |
| 3 | `canAttempt_allowed_returns200` | Quota available | 200 |
| 4 | `canAttempt_blocked_returns200` | Quota exceeded | 200 |
| 5 | `canAttempt_aiFlag_works` | ai=true param | 200 |
| 6 | `canAttempt_missingSkill_returns400` | No skill param | 400 |
| 7 | `checkAttempt_aliasWorks` | Same as canAttempt | 200 |
| 8 | `checkAttempt_isAIParam_works` | isAI param name | 200 |

---

## 11. CourseControllerTest (8 tests)

**File:** `backend/src/test/java/com/cramer/controller/CourseControllerTest.java`  
**Controller:** `/api/courses`  
**Requires Auth:** ❌ No (public endpoints)

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getAllCourses_returns200` | List courses | 200 |
| 2 | `getAllCourses_pagination_works` | page/size | 200 |
| 3 | `getAllCourses_search_works` | search param | 200 |
| 4 | `getAllCoursesV2_returns200` | V2 endpoint | 200 |
| 5 | `getAllCoursesV2_onlyPublished` | Filter unpublished | 200 |
| 6 | `getTestsByCourse_valid_returns200` | Get tests | 200 |
| 7 | `getTestsByCourse_notFound_returns200` | Empty list | 200 |
| 8 | `getCourseDetails_valid_returns200` | Course details | 200 |

---

## 12. ChatControllerTest (14 tests)

**File:** `backend/src/test/java/com/cramer/controller/ChatControllerTest.java`  
**Controller:** `/api/chat`  
**Requires Auth:** ✅ Yes

### Endpoints Covered
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Send message |
| GET | `/history` | Chat history |
| GET | `/remaining` | Remaining questions |
| DELETE | `/history` | Clear history |

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `sendMessage_valid_returns200` | Send message | 200 |
| 2 | `sendMessage_unauthorized_returns401` | No JWT | 401 |
| 3 | `sendMessage_emptyMessage_returns400` | Empty message | 400 |
| 4 | `sendMessage_rateLimitExceeded_returns429` | Rate limit | 429 |
| 5 | `sendMessage_serviceError_returns500` | AI error | 500 |
| 6 | `getHistory_valid_returns200` | Get history | 200 |
| 7 | `getHistory_unauthorized_returns401` | No JWT | 401 |
| 8 | `getHistory_limitParam_works` | limit=25 | 200 |
| 9 | `getHistory_limitCapped_returns100` | limit=500 -> 100 | 200 |
| 10 | `getRemaining_returns200` | Remaining count | 200 |
| 11 | `getRemaining_unlimited_returns200` | Premium user | 200 |
| 12 | `clearHistory_returns200` | Clear history | 200 |
| 13 | `clearHistory_unauthorized_returns401` | No JWT | 401 |
| 14 | `clearHistory_emptyHistory_returns200` | No messages | 200 |

---

## 13. QuestionControllerTest (12 tests)

**File:** `backend/src/test/java/com/cramer/controller/QuestionControllerTest.java`  
**Controller:** `/api/questions`  
**Requires Auth:** ❌ No (public read) / ✅ Yes (write)

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getAllQuestions_returns200` | List questions | 200 |
| 2 | `getQuestionById_valid_returns200` | Get by ID | 200 |
| 3 | `getQuestionById_notFound_returns404` | Invalid ID | 404 |
| 4 | `getQuestionsBySection_returns200` | By section | 200 |
| 5 | `getQuestionByUid_returns200` | By UID | 200 |
| 6 | `getQuestionsByType_returns200` | By type | 200 |
| 7 | `getAllQuestionTypes_returns200` | Types list | 200 |
| 8 | `createQuestion_valid_returns201` | Create question | 201 |
| 9 | `updateQuestion_valid_returns200` | Update question | 200 |
| 10 | `deleteQuestion_valid_returns204` | Delete question | 204 |
| 11 | `countBySection_returns200` | Count | 200 |
| 12 | `getQuestionCount_returns200` | Total count | 200 |

---

## 14. SectionControllerTest (12 tests)

**File:** `backend/src/test/java/com/cramer/controller/SectionControllerTest.java`  
**Controller:** `/api/sections`  
**Requires Auth:** ❌ No (public read)

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `getAllSections_returns200` | List sections | 200 |
| 2 | `getFullSectionById_returns200` | Full section | 200 |
| 3 | `getSectionById_valid_returns200` | By ID | 200 |
| 4 | `getSectionById_notFound_returns404` | Invalid ID | 404 |
| 5 | `getSectionsByExamSource_returns200` | By source | 200 |
| 6 | `getSectionsByTest_returns200` | By test | 200 |
| 7 | `getSectionsBySkill_returns200` | By skill | 200 |
| 8 | `getSpecificSection_returns200` | Specific section | 200 |
| 9 | `getSpecificSection_notFound_returns404` | Not found | 404 |
| 10 | `getSectionsForTest_returns200` | Ordered sections | 200 |
| 11 | `createSection_valid_returns201` | Create section | 201 |
| 12 | `deleteSection_valid_returns204` | Delete section | 204 |

---

## 15. VocabularyControllerTest (16 tests)

**File:** `backend/src/test/java/com/cramer/controller/VocabularyControllerTest.java`  
**Controller:** `/api/vocabulary`  
**Requires Auth:** ✅ Yes

### Test Cases

| # | Test Name | Description | HTTP Status |
|---|-----------|-------------|-------------|
| 1 | `listVocabulary_returns200` | List vocab | 200 |
| 2 | `listVocabulary_unauthorized_returns401` | No JWT | 401 |
| 3 | `listVocabulary_pagination_works` | page/size | 200 |
| 4 | `listVocabulary_search_works` | search param | 200 |
| 5 | `listVocabulary_filterMastered_works` | filter=mastered | 200 |
| 6 | `listVocabulary_filterUnmastered_works` | filter=unmastered | 200 |
| 7 | `getVocabularyById_returns200` | Get by ID | 200 |
| 8 | `getVocabularyById_idorViolation_returns403` | Wrong user | 403 |
| 9 | `createVocabulary_returns201` | Create vocab | 201 |
| 10 | `createVocabulary_duplicate_returns409` | Duplicate word | 409 |
| 11 | `updateVocabulary_returns200` | Update vocab | 200 |
| 12 | `deleteVocabulary_returns204` | Delete vocab | 204 |
| 13 | `toggleMastered_returns200` | Toggle status | 200 |
| 14 | `translateWord_returns200` | AI translate | 200 |
| 15 | `translateWord_rateLimited_returns429` | Rate limit | 429 |
| 16 | `getVocabularyStats_returns200` | Stats | 200 |

---

## 🔐 Admin Controllers

### Priority: 🟡 Medium

Admin controllers đòi hỏi `@RequestHeader("X-User-Id")` và được bảo vệ bởi `AdminAuthFilter`. 
Tests cần mock header này.

### Danh sách Admin Controllers cần test

| Controller | Endpoints | Est. Tests |
|------------|-----------|------------|
| ABTSController | 10+ | ~20 |
| AdminDashboardController | 3 | ~8 |
| AdminUserController | 5 | ~12 |
| TestHierarchyController | 12+ | ~20 |
| AdminContentController | TBD | ~10 |
| AdminFinanceController | TBD | ~8 |
| AdminActivityController | TBD | ~6 |

**Note:** Admin controller tests sẽ được implement ở phase tiếp theo.

---

## 📚 Tài liệu tham khảo

### Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Run Command
```powershell
cd backend
.\mvnw.cmd test -Dtest="com.cramer.controller.**"
```

### Related Documentation
- [TEST_CASES_REPORT.md](./TEST_CASES_REPORT.md) - Service layer tests
- [BUILD_INSTRUCTIONS.md](../../backend/BUILD_INSTRUCTIONS.md) - Build guide
- [copilot-instructions.md](../../.github/copilot-instructions.md) - Project conventions

---

## ✅ Checklist triển khai

- [ ] Tạo `TestSecurityConfig.java` - Mock JWT decoder
- [ ] Tạo `BaseControllerTest.java` - Abstract base class
- [ ] Implement AuthControllerTest (4 tests)
- [ ] Implement ProfileControllerTest (10 tests)
- [ ] Implement TestControllerTest (8 tests)
- [ ] Implement TestAttemptControllerTest (28 tests)
- [ ] Implement WritingControllerTest (22 tests)
- [ ] Implement PaymentControllerTest (24 tests)
- [ ] Implement CreditControllerTest (18 tests)
- [ ] Implement SubscriptionControllerTest (16 tests)
- [ ] Implement DashboardControllerTest (8 tests)
- [ ] Implement QuotaControllerTest (8 tests)
- [ ] Implement CourseControllerTest (8 tests)
- [ ] Implement ChatControllerTest (14 tests)
- [ ] Implement QuestionControllerTest (12 tests)
- [ ] Implement SectionControllerTest (12 tests)
- [ ] Implement VocabularyControllerTest (16 tests)
- [ ] Admin Controllers (Phase 2)
- [ ] Cập nhật TEST_CASES_REPORT.md

---

## 📋 Appendix A: DTO Data Types Reference

Để tránh lỗi kiểu dữ liệu khi implement tests, tham khảo bảng sau:

### Request DTOs

| DTO Class | Field | Type | Validation |
|-----------|-------|------|------------|
| **CheckEmailRequest** | email | `String` | - |
| **ProfileDTO** | id | `UUID` | - |
| | username | `String` | - |
| | fullName | `String` | - |
| | llmApiKey | `String` | - |
| | llmModel | `String` | - |
| | llmProvider | `String` | - |
| **AnswerSubmissionDTO** | answers | `Map<Long, String>` | Key=questionId |
| **SaveProgressDTO** | timeLeft | `Integer` | - |
| | currentPart | `Integer` | - |
| | answers | `Map<Long, String>` | Key=questionId |
| **WritingSubmitDTO** | essays | `Map<Integer, String>` | Key=taskNumber (1 or 2) |
| **PaymentCreateDTO** | type | `PaymentOrder.Type` | SUBSCRIPTION / LUA_PACK |
| | tierId | `Integer` | for SUBSCRIPTION |
| | tierCode | `String` | for SUBSCRIPTION |
| | luaAmount | `Integer` | for LUA_PACK |
| | priceVnd | `Integer` | for LUA_PACK |
| **ChatRequestDTO** | message | `String` | @NotBlank, max 2000 |
| **TargetDTO** | examName | `String` | @NotBlank |
| | examDate | `LocalDate` | nullable |
| | listening/reading/writing/speaking | `Double` | 0-9 |
| **LuaPurchaseDTO** | packageCode | `String` | @NotBlank |

### Response DTOs

| DTO Class | Key Fields | Types |
|-----------|------------|-------|
| **TestResultDTO** | attemptId | `Long` |
| | score | `Integer` |
| | totalQuestions | `Integer` |
| **TestReviewDTO** | attemptId | `Long` |
| | sections | `List<SectionReviewDTO>` |
| **UserAnswerDTO** | questionId | `Long` |
| | userAnswer | `String` |
| **PaymentResponseDTO** | orderCode | `Long` |
| | checkoutUrl | `String` |
| **PaymentOrderDTO** | userId | `UUID` |
| | orderCode | `Long` |
| | amount | `Integer` |
| | status | `PaymentOrder.Status` |
| **UserCreditDTO** | balance | `Integer` |
| | lifetimeEarned | `Integer` |
| **GradingStatusDTO** | allowed | `boolean` |
| | remaining | `Integer` |
| | canUseExtraWithLua | `boolean` |
| **QuotaStatusDTO** | globalUsed | `Integer` |
| | globalLimit | `Integer` |
| **BillingResultDTO** | allowed | `boolean` |
| | reason | `String` |

### Entity Enums

```java
// PaymentOrder.Type
public enum Type { SUBSCRIPTION, LUA_PACK }

// PaymentOrder.Status  
public enum Status { PENDING, PAID, CANCELLED, EXPIRED }

// TestAttempt.Status
public enum Status { IN_PROGRESS, COMPLETED, CANCELLED }
```

### Mock Security Setup

```java
// Option 1: @WithMockUser annotation
@Test
@WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
void testMethod() { ... }

// Option 2: SecurityMockMvcRequestPostProcessors
mockMvc.perform(get("/api/profiles/{id}", userId)
    .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
    .andExpect(status().isOk());

// Option 3: Custom JWT in header
mockMvc.perform(get("/api/profiles/{id}", userId)
    .header("Authorization", "Bearer " + validJwtToken))
    .andExpect(status().isOk());
```

### JSON Request Examples

```java
// AnswerSubmissionDTO
String json = """
{
    "answers": {
        "1": "A",
        "2": "B", 
        "3": "True"
    }
}
""";

// SaveProgressDTO
String json = """
{
    "timeLeft": 1800,
    "currentPart": 2,
    "answers": {"1": "A", "2": "B"}
}
""";

// PaymentCreateDTO (Subscription)
String json = """
{
    "type": "SUBSCRIPTION",
    "tierCode": "cramerich"
}
""";

// PaymentCreateDTO (Lúa Pack)
String json = """
{
    "type": "LUA_PACK",
    "luaAmount": 100,
    "priceVnd": 10000
}
""";

// WritingSubmitDTO
String json = """
{
    "essays": {
        "1": "Task 1 essay text...",
        "2": "Task 2 essay text..."
    }
}
""";
```

---

*Created: January 19, 2026*  
*Last Updated: January 19, 2026*
