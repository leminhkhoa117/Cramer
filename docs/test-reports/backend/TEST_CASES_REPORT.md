# 📋 Cramer Backend - Test Cases Report

> **Last Updated:** January 26, 2026  
> **Total Backend Tests:** ~420 tests (16 Controller + 16 Service test files)  
> **Status:** ✅ All Passing

---

## 🔧 How to Run Tests

### Run All Backend Tests
```powershell
cd backend
.\mvnw.cmd test
```

### Run Only Unit Tests (Service Layer)
```powershell
.\mvnw.cmd test -Dtest="com.cramer.service.unit.**"
```

### Run Only Controller Tests
```powershell
.\mvnw.cmd test -Dtest="com.cramer.controller.**"
```

### Run Specific Test Class
```powershell
.\mvnw.cmd test -Dtest=CreditServiceImplTest
```

### Run with Coverage Report
```powershell
.\mvnw.cmd test jacoco:report
```

---

## 📊 Controller Test Summary (16 files)

| Test Class | Est. Tests | Status |
|------------|------------|--------|
| AuthControllerTest | ~8 | ✅ |
| ChatControllerTest | ~18 | ✅ |
| CourseControllerTest | ~16 | ✅ |
| CreditControllerTest | ~20 | ✅ |
| DashboardControllerTest | ~11 | ✅ |
| PaymentControllerTest | ~22 | ✅ |
| ProfileControllerTest | ~12 | ✅ |
| QuestionControllerTest | ~14 | ✅ |
| QuotaControllerTest | ~8 | ✅ |
| SectionControllerTest | ~14 | ✅ |
| SubscriptionControllerTest | ~17 | ✅ |
| TestAttemptControllerTest | ~22 | ✅ |
| TestControllerTest | ~10 | ✅ |
| VocabularyControllerTest | ~18 | ✅ |
| WritingControllerTest | ~22 | ✅ |
| SpeakingControllerTest | 4 | ✅ |
| **Controller Total** | **~224** | **✅** |

---

## 📊 Service Test Summary (16 files)

| Test Class | Tests | Status |
|------------|-------|--------|
| AsyncGradingServiceTest | ~18 | ✅ |
| ChatServiceTest | 9 | ✅ |
| CourseServiceTest | 7 | ✅ |
| CreditServiceImplTest | 16 | ✅ |
| DashboardServiceTest | 13 | ✅ |
| LLMGradingServiceTest | 12 | ✅ |
| PaymentServiceImplTest | 22 | ✅ |
| ProfileServiceImplTest | 13 | ✅ |
| QuestionServiceTest | ~18 | ✅ |
| QuotaServiceTest | 17 | ✅ |
| SectionServiceTest | ~18 | ✅ |
| SubscriptionServiceImplTest | 13 | ✅ |
| TestAttemptServiceTest | 17 | ✅ |
| TestServiceTest | 9 | ✅ |
| VocabularyServiceTest | 20 | ✅ |
| WritingSubmissionServiceTest | 14 | ✅ |
| SpeakingContentServiceImplTest | 4 | ✅ |
| SpeakingSessionServiceImplTest | 7 | ✅ |
| **Service Total** | **~211** | **✅** |

---

# 📝 Detailed Test Cases

---

## Controller Tests

### 1. AuthControllerTest (~8 tests)

**File:** `backend/src/test/java/com/cramer/controller/AuthControllerTest.java`

Tests authentication endpoints.

| Test Group | Description |
|------------|-------------|
| Login | POST /api/auth/login - success, invalid credentials |
| Register | POST /api/auth/register - validation |
| Session | Session management tests |

---

### 2. ChatControllerTest (18 tests)

**File:** `backend/src/test/java/com/cramer/controller/ChatControllerTest.java`

Tests AI chat endpoints.

| Test Group | Tests | Description |
|------------|-------|-------------|
| Send Message | 5 | POST /api/chat/send - success, rate limit, validation, auth |
| Get History | 4 | GET /api/chat/history - with/without messages, limit |
| Get Remaining | 3 | GET /api/chat/remaining - quota remaining |
| Clear History | 4 | DELETE /api/chat/history - success, no auth |
| Edge Cases | 2 | Empty message, very long message |

---

### 3. CourseControllerTest (16 tests)

**File:** `backend/src/test/java/com/cramer/controller/CourseControllerTest.java`

Tests course management endpoints.

| Test Group | Tests | Description |
|------------|-------|-------------|
| Get Courses | 6 | GET /api/courses - pagination, search, empty |
| Get Tests | 5 | GET /api/courses/{name}/tests - list, empty |
| Auth Tests | 3 | Unauthorized access tests |
| Validation | 2 | Invalid parameters |

---

### 4. CreditControllerTest (~20 tests)

**File:** `backend/src/test/java/com/cramer/controller/CreditControllerTest.java`

Tests Lúa (credit) system endpoints.

| Test Group | Description |
|------------|-------------|
| Get Balance | GET /api/credits/balance |
| Earn Credits | POST /api/credits/earn |
| Spend Credits | POST /api/credits/spend |
| Transaction History | GET /api/credits/history |

---

### 5. DashboardControllerTest (11 tests)

**File:** `backend/src/test/java/com/cramer/controller/DashboardControllerTest.java`

Tests user dashboard endpoints.

| Test Group | Tests | Description |
|------------|-------|-------------|
| Get Summary | 4 | GET /api/dashboard/summary - success, empty, auth |
| Update Target | 4 | PUT /api/dashboard/target - success, validation |
| Get Stats | 3 | GET /api/dashboard/stats - various stats |

---

### 6. PaymentControllerTest (~22 tests)

**File:** `backend/src/test/java/com/cramer/controller/PaymentControllerTest.java`

Tests PayOS payment integration.

| Test Group | Description |
|------------|-------------|
| Create Payment | POST /api/payments/create - subscription, Lúa packs |
| Webhook | POST /api/payments/webhook - PayOS callbacks |
| Get History | GET /api/payments/history |
| Verify Signature | Webhook signature validation |

---

### 7. ProfileControllerTest (~12 tests)

**File:** `backend/src/test/java/com/cramer/controller/ProfileControllerTest.java`

Tests user profile CRUD.

| Test Group | Description |
|------------|-------------|
| Get Profile | GET /api/profile - success, not found |
| Update Profile | PUT /api/profile - update fields |
| LLM Settings | API key, model, provider settings |

---

### 8. QuestionControllerTest (~14 tests)

**File:** `backend/src/test/java/com/cramer/controller/QuestionControllerTest.java`

Tests question management endpoints.

| Test Group | Description |
|------------|-------------|
| Get Questions | GET /api/questions - by section |
| Create Question | POST /api/questions |
| Update Question | PUT /api/questions/{id} |
| Delete Question | DELETE /api/questions/{id} |

---

### 9. QuotaControllerTest (~8 tests)

**File:** `backend/src/test/java/com/cramer/controller/QuotaControllerTest.java`

Tests quota management endpoints.

| Test Group | Description |
|------------|-------------|
| Get Status | GET /api/quota/status |
| Pre-check | POST /api/quota/precheck |
| Cap Status | GET /api/quota/caps |

---

### 10. SectionControllerTest (~14 tests)

**File:** `backend/src/test/java/com/cramer/controller/SectionControllerTest.java`

Tests section management endpoints.

| Test Group | Description |
|------------|-------------|
| Get Sections | GET /api/sections - by test |
| Create Section | POST /api/sections |
| Update Section | PUT /api/sections/{id} |
| Delete Section | DELETE /api/sections/{id} |

---

### 11. SubscriptionControllerTest (~17 tests)

**File:** `backend/src/test/java/com/cramer/controller/SubscriptionControllerTest.java`

Tests subscription tier management.

| Test Group | Description |
|------------|-------------|
| Get Tiers | GET /api/subscriptions/tiers |
| Get User Sub | GET /api/subscriptions/current |
| Upgrade | POST /api/subscriptions/upgrade |
| AI Grading Quota | Quota checking endpoints |

---

### 12. TestAttemptControllerTest (~22 tests)

**File:** `backend/src/test/java/com/cramer/controller/TestAttemptControllerTest.java`

Tests test session lifecycle.

| Test Group | Description |
|------------|-------------|
| Start/Resume | POST /api/attempts/start |
| Save Progress | PUT /api/attempts/{id}/progress |
| Submit | POST /api/attempts/{id}/submit |
| Cancel | DELETE /api/attempts/{id} |
| Get History | GET /api/attempts/history |

---

### 13. TestControllerTest (~10 tests)

**File:** `backend/src/test/java/com/cramer/controller/TestControllerTest.java`

Tests test data endpoints.

| Test Group | Description |
|------------|-------------|
| Get Safe Test | GET /api/tests - without answers |
| Get Full Test | GET /api/tests/full - with answers (admin) |

---

### 14. VocabularyControllerTest (~18 tests)

**File:** `backend/src/test/java/com/cramer/controller/VocabularyControllerTest.java`

Tests vocabulary management.

| Test Group | Description |
|------------|-------------|
| Get List | GET /api/vocabulary - paginated |
| Create | POST /api/vocabulary |
| Update | PUT /api/vocabulary/{id} |
| Delete | DELETE /api/vocabulary/{id} |
| Toggle Mastered | PATCH /api/vocabulary/{id}/toggle |

---

### 15. WritingControllerTest (~22 tests)

**File:** `backend/src/test/java/com/cramer/controller/WritingControllerTest.java`

Tests writing submission and grading.

| Test Group | Description |
|------------|-------------|
| Save Draft | POST /api/writing/draft |
| Submit | POST /api/writing/submit |
| Get Status | GET /api/writing/{id}/status |
| Get Review | GET /api/writing/{id}/review |

---

## Service Tests

### 1. CreditServiceImplTest (16 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/CreditServiceImplTest.java`

Tests Lúa (credit) system.

| Test Group | Tests | Description |
|------------|-------|-------------|
| GetBalanceTests | 2 | Get balance for existing/new user |
| HasEnoughCreditsTests | 2 | Check if user has enough credits |
| EarnCreditsTests | 4 | Add credits, handle errors |
| SpendCreditsTests | 4 | Spend credits, handle insufficient |
| InitializeCreditsTests | 4 | Initialize credits for new users |

---

### 2. LLMGradingServiceTest (12 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/LLMGradingServiceTest.java`

Tests AI essay grading.

| Test Group | Tests | Description |
|------------|-------|-------------|
| GradeEssayTests | 4 | Grade valid essay, handle errors |
| HandleEmptyEssayTests | 2 | Null/empty essay handling |
| BandScoreValidation | 2 | Validate score range 0-9 |
| ModelSelectionTests | 4 | Default/custom model selection |

---

### 3. SubscriptionServiceImplTest (13 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/SubscriptionServiceImplTest.java`

Tests subscription tier management.

| Test Group | Tests | Description |
|------------|-------|-------------|
| GetAllTiersTests | 2 | Fetch all tiers |
| GetUserSubscriptionTests | 2 | Get/create user subscription |
| CheckAIGradingAllowedTests | 3 | AI grading quota checks |
| GetMonthlyGradingsRemainingTests | 2 | Remaining quota count |
| IncrementAIGradingUsageTests | 1 | Increment usage |
| InitializeNewUserTests | 1 | Create free tier subscription |
| GetTierByCodeTests | 2 | Get tier by code |

---

### 4. PaymentServiceImplTest (22 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/PaymentServiceImplTest.java`

Tests PayOS payment integration.

| Test Group | Tests | Description |
|------------|-------|-------------|
| CreateSubscriptionPaymentTests | 3 | Create payment links |
| CreateLuaPackPaymentTests | 5 | Buy Lúa packs |
| HandleWebhookTests | 5 | Process webhooks |
| VerifyWebhookSignatureTests | 3 | Signature verification |
| GenerateSignatureTests | 3 | Signature generation |
| UtilityMethodsTests | 3 | Payment history, status |

---

### 5. ProfileServiceImplTest (13 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/ProfileServiceImplTest.java`

Tests user profile CRUD.

| Test Group | Tests | Description |
|------------|-------|-------------|
| GetProfileByIdTests | 5 | Get profile, null handling |
| UpdateProfileTests | 8 | Update fields, validation |

---

### 6. TestAttemptServiceTest (17 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/TestAttemptServiceTest.java`

Tests test session lifecycle.

| Test Group | Tests | Description |
|------------|-------|-------------|
| StartOrGetAttemptTests | 9 | Start/resume attempts |
| SaveProgressTests | 4 | Save answers, time |
| CancelAttemptTests | 4 | Cancel attempts |

---

### 7. TestServiceTest (9 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/TestServiceTest.java`

Tests test data fetching.

| Test Group | Tests | Description |
|------------|-------|-------------|
| GetSafeTestTests | 6 | Get test without answers |
| GetFullTestTests | 2 | Get test with answers |
| InputValidationTests | 1 | Validate input params |

---

### 8. WritingSubmissionServiceTest (14 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/WritingSubmissionServiceTest.java`

Tests essay submission and grading.

| Test Group | Tests | Description |
|------------|-------|-------------|
| SaveDraftTests | 4 | Save essay drafts |
| GetGradingStatusTests | 5 | Check grading status |
| GetWritingReviewTests | 2 | Get completed review |
| SubmitForGradingTests | 3 | Submit for AI grading |

---

### 9. VocabularyServiceTest (20 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/VocabularyServiceTest.java`

Tests vocabulary management.

| Test Group | Tests | Description |
|------------|-------|-------------|
| Get User Vocabulary | 3 | Paginated vocabulary list |
| Create Vocabulary | 3 | Add new words, duplicates |
| Update Vocabulary | 4 | Update fields, not found |
| Delete Vocabulary | 2 | Delete word, auth |
| Toggle Mastered | 3 | Toggle mastered status |
| Search/Export | 5 | Search by word, export |

---

### 10. QuotaServiceTest (17 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/QuotaServiceTest.java`

Tests quota management.

| Test Group | Tests | Description |
|------------|-------|-------------|
| Can Attempt | 5 | Check quota caps |
| Is Global Cap Hit | 4 | Global cap checking |
| Is Local Cap Hit | 4 | Per-skill cap checking |
| Increment Attempt | 3 | Increment usage counters |
| Get Quota Status | 1 | Return quota DTO |

---

### 11. Additional Service Tests

| Test Class | Tests | Description |
|------------|-------|-------------|
| AsyncGradingServiceTest | ~18 | Async grading workflow |
| ChatServiceTest | 9 | AI chat functionality |
| CourseServiceTest | 7 | Course management |
| DashboardServiceTest | 13 | Dashboard stats |
| QuestionServiceTest | ~18 | Question CRUD |
| SectionServiceTest | ~18 | Section CRUD |

---

## 📁 Test File Locations

```
backend/src/test/
├── java/com/cramer/
│   ├── CramerBackendApplicationTests.java
│   ├── controller/                    # Controller tests
│   │   ├── BaseControllerTest.java    # Base class with shared setup
│   │   ├── AuthControllerTest.java
│   │   ├── ChatControllerTest.java
│   │   ├── CourseControllerTest.java
│   │   ├── CreditControllerTest.java
│   │   ├── DashboardControllerTest.java
│   │   ├── PaymentControllerTest.java
│   │   ├── ProfileControllerTest.java
│   │   ├── QuestionControllerTest.java
│   │   ├── QuotaControllerTest.java
│   │   ├── SectionControllerTest.java
│   │   ├── SubscriptionControllerTest.java
│   │   ├── TestAttemptControllerTest.java
│   │   ├── TestControllerTest.java
│   │   ├── VocabularyControllerTest.java
│   │   └── WritingControllerTest.java
│   └── service/unit/                  # Service tests
│       ├── AsyncGradingServiceTest.java
│       ├── ChatServiceTest.java
│       ├── CourseServiceTest.java
│       ├── CreditServiceImplTest.java
│       ├── DashboardServiceTest.java
│       ├── LLMGradingServiceTest.java
│       ├── PaymentServiceImplTest.java
│       ├── ProfileServiceImplTest.java
│       ├── QuestionServiceTest.java
│       ├── QuotaServiceTest.java
│       ├── SectionServiceTest.java
│       ├── SubscriptionServiceImplTest.java
│       ├── TestAttemptServiceTest.java
│       ├── TestServiceTest.java
│       ├── VocabularyServiceTest.java
│       └── WritingSubmissionServiceTest.java
└── resources/
    └── application-test.properties
```

---

## 🧪 Test Configuration

### application-test.properties

```properties
# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# Mock Supabase JWT
supabase.jwt.secret=test-jwt-secret-key-that-is-at-least-32-characters-long

# Mock PayOS
payos.checksum-key=test-checksum-key

# Mock DeepSeek
deepseek.api.key=test-api-key
```

---

## 📝 Notes

1. **Mocking:** All tests use Mockito `@Mock` and `@InjectMocks` for isolation
2. **No External Dependencies:** Tests run with H2 in-memory DB, no Supabase/PayOS calls
3. **Strict Stubbing:** Mockito strict mode enabled to catch unused stubs
4. **Controller Tests:** Use `@WebMvcTest` with MockMvc for HTTP request/response testing
5. **Base Test Class:** `BaseControllerTest.java` provides shared setup for all controller tests

---

## 🚧 TODO: Future Tests

| Area | Priority | Description |
|------|----------|-------------|
| ABTSControllerTest | Low | ABTS (AI Test Generation) admin endpoints |
| Integration Tests | Medium | Full flow tests (Start → Answer → Submit → Review) |
| Performance Tests | Low | Load testing for concurrent users |

---

*Last updated: January 26, 2026*
