# 📋 Cramer - Test Cases Report

> **Generated:** January 15, 2026  
> **Total Tests:** 259 tests (116 backend + 143 frontend)  
> **Status:** ✅ All Passing

---

## 🔧 How to Run Tests

### Backend Tests (Java/Spring Boot)
```powershell
cd backend
.\mvnw.cmd test -Dtest="com.cramer.service.unit.**"
```

### Frontend Tests (React/Vitest)
```powershell
cd frontend
npx vitest --run
```

### Frontend Tests with Coverage
```powershell
cd frontend
npx vitest --run --coverage
```

---

## 📊 Backend Test Summary

| Test Class | Tests | Passed | Failed | Status |
|------------|-------|--------|--------|--------|
| CreditServiceImplTest | 16 | 16 | 0 | ✅ |
| LLMGradingServiceTest | 12 | 12 | 0 | ✅ |
| JwtAuthFilterTest | 9 | 9 | 0 | ✅ |
| SubscriptionServiceImplTest | 13 | 13 | 0 | ✅ |
| PaymentServiceImplTest | 22 | 22 | 0 | ✅ |
| ProfileServiceImplTest | 13 | 13 | 0 | ✅ |
| TestAttemptServiceTest | 17 | 17 | 0 | ✅ |
| TestServiceTest | 9 | 9 | 0 | ✅ |
| WritingSubmissionServiceTest | 14 | 14 | 0 | ✅ |
| **Backend Total** | **116** | **116** | **0** | **✅** |

## 📊 Frontend Test Summary

| Test File | Tests | Passed | Failed | Status |
|-----------|-------|--------|--------|--------|
| useAuthStore.test.js | 20 | 20 | 0 | ✅ |
| useTestStore.test.js | 34 | 34 | 0 | ✅ |
| useProfileStore.test.js | 15 | 15 | 0 | ✅ |
| useTestSessionStore.test.js | 18 | 18 | 0 | ✅ |
| useDashboardStore.test.js | 17 | 17 | 0 | ✅ |
| useSubscriptionStore.test.js | 13 | 13 | 0 | ✅ |
| useQuotaStore.test.js | 26 | 26 | 0 | ✅ |
| **Frontend Total** | **143** | **143** | **0** | **✅** |

---

# Backend Test Cases

---

---

## 1. CreditServiceImplTest (16 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/CreditServiceImplTest.java`

Tests the Lúa (credit) system for earning, spending, and balance management.

### 1.1 GetBalanceTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnBalance` | Get balance for existing user | Returns correct balance (150) |
| `shouldReturnZeroWhenUserNotFound` | Get balance for non-existent user | Returns 0 |

### 1.2 HasEnoughCreditsTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnTrueWhenHasEnoughCredits` | Check if user has 150 credits for 100 purchase | Returns `true` |
| `shouldReturnFalseWhenNotEnoughCredits` | Check if user has 150 credits for 200 purchase | Returns `false` |

### 1.3 EarnCreditsTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldEarnCreditsForExistingUser` | Add 50 Lúa to user with 150 balance | Balance becomes 200, lifetimeEarned +50 |
| `shouldCreateNewUserCreditWhenEarning` | Earn credits for new user | Creates UserCredit record |
| `shouldThrowExceptionForNegativeAmount` | Try to earn negative amount | Throws `IllegalArgumentException` |
| `shouldThrowExceptionForNullCategory` | Try to earn with null category | Throws `IllegalArgumentException` |

### 1.4 SpendCreditsTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSpendCreditsSuccessfully` | Spend 50 from 150 balance | Balance becomes 100, lifetimeSpent +50 |
| `shouldCreateUserCreditWhenSpendingForNewUser` | Spend from new user | Creates record with 0 balance |
| `shouldThrowExceptionWhenInsufficientCredits` | Spend 200 from 150 balance | Throws `InsufficientCreditsException` |
| `shouldThrowExceptionForNegativeSpendAmount` | Spend negative amount | Throws `IllegalArgumentException` |

### 1.5 InitializeCreditsTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldInitializeCreditsForNewUser` | Initialize 100 Lúa for new user | Creates record with balance=100 |
| `shouldUpdateExistingUserCredits` | Initialize for existing user | Updates balance |
| `shouldThrowExceptionForNegativeInitialCredits` | Initialize with -50 | Throws `IllegalArgumentException` |
| `shouldHandleNullTierGracefully` | Initialize when tier is null | Uses default 50 Lúa |

---

## 2. LLMGradingServiceTest (12 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/LLMGradingServiceTest.java`

Tests AI essay grading using DeepSeek LLM integration.

### 2.1 GradeEssayTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldGradeEssaySuccessfully` | Grade valid essay with API response | Returns `WritingReviewDTO` with scores |
| `shouldHandleApiFailure` | API returns error status | Throws `RuntimeException` |
| `shouldHandleInvalidJsonResponse` | API returns malformed JSON | Throws exception or handles gracefully |
| `shouldHandleEmptyApiResponse` | API returns empty body | Handles gracefully |

### 2.2 HandleEmptyEssayTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnNullForNullEssay` | Grade null essay | Returns `null` |
| `shouldReturnNullForEmptyEssay` | Grade empty/blank essay | Returns `null` |

### 2.3 BandScoreValidation (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldValidateBandScoresInRange` | Scores 0.0-9.0 | Accepted as valid |
| `shouldRejectInvalidBandScores` | Scores outside range | Throws or returns error |

### 2.4 ModelSelectionTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldUseDefaultModelWhenProfileModelIsNull` | Profile has no model set | Uses `deepseek-chat` |
| `shouldUseCustomModelFromProfile` | Profile has `deepseek-reasoner` | Uses custom model |
| `shouldUseDefaultProviderWhenNotSet` | No provider in profile | Uses `deepseek` |
| `shouldHandleOpenRouterProvider` | Provider is `openrouter` | Uses OpenRouter API URL |

---

## 3. JwtAuthFilterTest (9 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/JwtAuthFilterTest.java`

Tests JWT authentication filter for Spring Security.

### 3.1 Authentication Tests (9 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldAuthenticateValidToken` | Valid JWT in Authorization header | Sets `SecurityContext` with user |
| `shouldRejectMissingToken` | No Authorization header | Returns 401 Unauthorized |
| `shouldRejectInvalidToken` | Malformed JWT token | Returns 401 Unauthorized |
| `shouldRejectExpiredToken` | Expired JWT token | Returns 401 Unauthorized |
| `shouldRejectTokenWithInvalidSignature` | Token signed with wrong key | Returns 401 Unauthorized |
| `shouldHandleMalformedHeader` | Header without "Bearer " prefix | Returns 401 Unauthorized |
| `shouldBypassNonProtectedPath` | Request to `/api/public/**` | Allows without auth |
| `shouldExtractUserIdFromToken` | Valid token with `sub` claim | Extracts UUID correctly |
| `shouldSetRoleUserForValidToken` | Valid authenticated request | Grants `ROLE_USER` |

---

## 4. SubscriptionServiceImplTest (13 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/SubscriptionServiceImplTest.java`

Tests subscription tier management and AI grading quota system.

### 4.1 GetAllTiersTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnAllTiers` | Fetch all active tiers | Returns list with Cramerie, Cramerich |
| `shouldReturnEmptyListWhenNoTiers` | No tiers in database | Returns empty list |

### 4.2 GetUserSubscriptionTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnActiveSubscription` | User has active subscription | Returns `UserSubscriptionDTO` |
| `shouldCreateFreeTierWhenNoSubscription` | New user, no subscription | Creates Cramerie subscription |

### 4.3 CheckAIGradingAllowedTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldAllowGradingWhenHasRemainingQuota` | 2/5 AI gradings used | `allowed=true`, `remaining=3` |
| `shouldDenyGradingWhenQuotaExhaustedButAllowWithLua` | 5/5 used, has 150 Lúa | `allowed=true`, `canUseExtraWithLua=true` |
| `shouldDenyGradingForFreeTierUser` | Free tier, 3/3 exhausted | `allowed=true` if has Lúa |

### 4.4 GetMonthlyGradingsRemainingTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnRemainingCount` | 5 limit, 2 used | Returns 3 |
| `shouldReturnZeroWhenNoSubscription` | No subscription found | Returns 0 |

### 4.5 IncrementAIGradingUsageTests (1 test)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldIncrementUsage` | Increment from 2 to 3 | `attemptAisUsed` becomes 3 |

### 4.6 InitializeNewUserTests (1 test)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldCreateFreeTierSubscription` | New user initialization | Creates Cramerie subscription, status=ACTIVE |

### 4.7 GetTierByCodeTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnTierWhenFound` | Get "cramerich" tier | Returns tier with price 69000 |
| `shouldThrowExceptionWhenNotFound` | Get "invalid_tier" | Throws `ResourceNotFoundException` |

---

## 5. PaymentServiceImplTest (22 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/PaymentServiceImplTest.java`

Tests PayOS payment gateway integration for subscriptions and Lúa purchases.

### 5.1 CreateSubscriptionPaymentTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldCreatePaymentLinkSuccessfully` | Create payment for tier upgrade | Returns PayOS checkout URL |
| `shouldThrowExceptionWhenTierNotFound` | Invalid tier code | Throws `ResourceNotFoundException` |
| `shouldThrowExceptionWhenUserNotFound` | Invalid user ID | Throws `ResourceNotFoundException` |

### 5.2 CreateLuaPackPaymentTests (5 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldCreateLuaPackPayment` | Buy 100 Lúa pack | Returns payment link |
| `shouldRejectInvalidPackSize` | Invalid pack amount | Throws `IllegalArgumentException` |
| `shouldCalculatePriceCorrectly` | 500 Lúa pack | Price = 50000 VND |
| `shouldHandlePayOSApiError` | PayOS returns error | Throws appropriate exception |
| `shouldGenerateUniqueOrderCode` | Multiple requests | Each has unique order code |

### 5.3 HandleWebhookTests (5 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldProcessSuccessfulPayment` | Webhook with PAID status | Grants subscription/credits |
| `shouldIgnoreDuplicateWebhook` | Same orderCode twice | Processes only once |
| `shouldHandleCancelledPayment` | User cancelled | Updates order status to CANCELLED |
| `shouldRejectInvalidSignature` | Tampered webhook | Returns 400 Bad Request |
| `shouldHandleExpiredPayment` | Payment timeout | Updates status to EXPIRED |

### 5.4 VerifyWebhookSignatureTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldVerifyValidSignature` | Correct HMAC-SHA256 | Returns `true` |
| `shouldRejectInvalidSignature` | Wrong signature | Returns `false` |
| `shouldHandleEmptySignature` | No signature header | Returns `false` |

### 5.5 GenerateSignatureTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldGenerateCorrectSignature` | Known payload | Matches expected hash |
| `shouldBeConsistent` | Same input multiple times | Same output |
| `shouldHandleSpecialCharacters` | Unicode in description | Generates valid signature |

### 5.6 UtilityMethodsTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldGetPaymentHistory` | User with 3 payments | Returns list of 3 |
| `shouldGetPaymentStatus` | Check order by code | Returns current status |
| `shouldReturnAvailableLuaPacks` | Get Lúa pack options | Returns pack list with prices |

---

## 6. ProfileServiceImplTest (13 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/ProfileServiceImplTest.java`

Tests user profile CRUD operations.

### 6.1 GetProfileByIdTests (5 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getProfileById_exists_returnsProfile` | Get existing profile | Returns `ProfileDTO` with all fields |
| `getProfileById_notFound_throwsException` | Unknown user ID | Throws `RuntimeException` |
| `getProfileById_nullId_throwsException` | Null user ID | Throws `NullPointerException` |
| `getProfileById_withApiKey_hidesKey` | Profile has LLM API key | `hasLlmApiKey=true`, key not exposed |
| `getProfileById_noApiKey_indicatesNone` | No API key set | `hasLlmApiKey=false` |

### 6.2 UpdateProfileTests (8 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `updateProfile_basicFields_updatesSuccessfully` | Update name, phone, address | All fields updated |
| `updateProfile_avatarUrl_updatesSuccessfully` | Update avatar URL | New URL saved |
| `updateProfile_setLlmApiKey_savesKey` | Set DeepSeek API key | Key saved to profile |
| `updateProfile_clearLlmApiKey_clearsKey` | Clear API key with empty string | Key set to null |
| `updateProfile_llmModel_updatesModel` | Set to `deepseek-reasoner` | Model updated |
| `updateProfile_llmProvider_updatesProvider` | Set to `openrouter` | Provider updated |
| `updateProfile_notFound_throwsException` | Unknown user ID | Throws `RuntimeException` |
| `updateProfile_nullFields_preservesExisting` | Partial update | Unchanged fields preserved |

---

## 7. TestAttemptServiceTest (17 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/TestAttemptServiceTest.java`

Tests test session lifecycle: start, resume, save progress, cancel.

### 7.1 StartOrGetAttemptTests (9 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `startOrGetAttempt_existingInProgress_resumesAttempt` | Existing IN_PROGRESS attempt | Returns existing attempt |
| `startOrGetAttempt_noExisting_createsNew` | No prior attempt | Creates new IN_PROGRESS attempt |
| `startOrGetAttempt_completedNoForce_returnsCompleted` | COMPLETED + forceNew=false | Returns COMPLETED attempt |
| `startOrGetAttempt_completedWithForce_createsNew` | COMPLETED + forceNew=true | Creates new attempt |
| `startOrGetAttempt_forceNewCancelsInProgress_createsNew` | IN_PROGRESS + forceNew=true | Cancels old, creates new |
| `startOrGetAttempt_nullUserId_throwsException` | Null user ID | Throws `RuntimeException` |
| `startOrGetAttempt_emptySource_throwsException` | Empty exam source | Throws `RuntimeException` |
| `startOrGetAttempt_cancelledAttempt_createsNew` | Last was CANCELLED | Creates new attempt |
| `startOrGetAttempt_multipleInProgress_cancelsOldOnes` | Multiple stale IN_PROGRESS | Keeps only latest |

### 7.2 SaveProgressTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `saveProgress_validData_savesSuccessfully` | Save answers + timeLeft | Updates attempt, saves answers |
| `saveProgress_attemptNotFound_throwsException` | Invalid attempt ID | Throws `ResourceNotFoundException` |
| `saveProgress_unauthorized_throwsException` | Different user's attempt | Throws `AccessDeniedException` |
| `saveProgress_emptyAnswers_doesNotSaveAnswers` | Empty answers map | Only updates time, no answer save |

### 7.3 CancelAttemptTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `cancelAttempt_inProgress_cancelsSuccessfully` | Cancel IN_PROGRESS | Deletes attempt and answers |
| `cancelAttempt_completed_returnsSilently` | Cancel COMPLETED (idempotent) | No error, no deletion |
| `cancelAttempt_notFound_returnsSilently` | Cancel non-existent (idempotent) | No error |
| `cancelAttempt_unauthorized_throwsException` | Wrong user | Throws `AccessDeniedException` |

---

## 8. TestServiceTest (9 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/TestServiceTest.java`

Tests fetching test data (sections, questions) for test taking.

### 8.1 GetSafeTestTests (6 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getSafeTest_validParams_returnsSectionsWithoutAnswers` | Valid source/test/skill | Returns sections with questions (no answers) |
| `getSafeTest_noSections_returnsEmpty` | Non-existent test | Returns empty list |
| `getSafeTest_nullSource_throwsException` | Null source | Throws `IllegalArgumentException` |
| `getSafeTest_emptySource_throwsException` | Empty source | Throws `IllegalArgumentException` |
| `getSafeTest_invalidTestNum_throwsException` | TestNum <= 0 | Throws `IllegalArgumentException` |
| `getSafeTest_nullSkill_throwsException` | Null skill | Throws `IllegalArgumentException` |

### 8.2 GetFullTestTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getFullTest_validParams_returnsSectionsWithAnswers` | Admin access | Returns full sections with answers |
| `getFullTest_sectionWithNoQuestions_returnsEmptyQuestions` | Writing section | Section with empty questions list |

### 8.3 InputValidationTests (1 test)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getTest_differentSkills_callsRepositoryCorrectly` | reading/listening/writing | All valid skills work |

---

## 9. WritingSubmissionServiceTest (14 tests)

**File:** `backend/src/test/java/com/cramer/service/unit/WritingSubmissionServiceTest.java`

Tests essay submission, grading workflow, and status tracking.

### 9.1 SaveDraftTests (4 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `saveDraft_newDraft_savedSuccessfully` | Save new essay draft | Creates submission with PENDING status |
| `saveDraft_existingDraft_updatedSuccessfully` | Update existing draft | Updates essay text |
| `saveDraft_attemptNotFound_throwsException` | Invalid attempt ID | Throws `IllegalArgumentException` |
| `saveDraft_unauthorized_throwsException` | Wrong user's attempt | Throws `IllegalArgumentException` |

### 9.2 GetGradingStatusTests (5 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getGradingStatus_allCompleted_returnsCompleted` | Both tasks graded | Status = COMPLETED |
| `getGradingStatus_stillGrading_returnsGrading` | One task still grading | Status = GRADING |
| `getGradingStatus_someFailed_returnsPartialFailure` | One task failed | Status = PARTIAL_FAILURE |
| `getGradingStatus_pending_returnsPending` | No grading started | Status = PENDING |
| `getGradingStatus_unauthorized_throwsException` | Wrong user | Throws `IllegalArgumentException` |

### 9.3 GetWritingReviewTests (2 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getWritingReview_completed_returnsFullReview` | Completed attempt | Returns tasks with scores |
| `getWritingReview_withDuration_calculatesCorrectly` | 45 min test | Duration ≈ 2700 seconds |

### 9.4 SubmitForGradingTests (3 tests)

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `submitForGrading_quotaAllowed_startsGrading` | Has quota | Starts async grading |
| `submitForGrading_quotaExceeded_marksAsFailed` | No quota, no Lúa | Marks as FAILED |
| `submitForGrading_withStaleAttempts_cancelsStale` | Has stale IN_PROGRESS | Cancels stale attempts |

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

## 🚀 Running Tests

### Backend (Java/Spring Boot)
```powershell
# Run all unit tests
cd backend && .\mvnw.cmd test -Dtest="com.cramer.service.unit.**"

# Run specific test class
.\mvnw.cmd test -Dtest=CreditServiceImplTest

# Run with coverage
.\mvnw.cmd test jacoco:report
```

### Frontend (React/Vitest)
```powershell
# Run all tests
cd frontend && npx vitest --run

# Run tests in watch mode
npx vitest

# Run with coverage
npx vitest --run --coverage

# Run specific test file
npx vitest --run useAuthStore.test.js
```

---

## 📁 Test File Locations

```
backend/src/test/
├── java/com/cramer/
│   ├── CramerBackendApplicationTests.java
│   └── service/unit/
│       ├── CreditServiceImplTest.java
│       ├── LLMGradingServiceTest.java
│       ├── JwtAuthFilterTest.java
│       ├── SubscriptionServiceImplTest.java
│       ├── PaymentServiceImplTest.java
│       ├── ProfileServiceImplTest.java
│       ├── TestAttemptServiceTest.java
│       ├── TestServiceTest.java
│       └── WritingSubmissionServiceTest.java
└── resources/
    └── application-test.properties

frontend/src/__tests__/
├── setupTests.js
├── mocks/
│   ├── handlers.js
│   └── server.js
└── stores/
    ├── useAuthStore.test.js
    ├── useTestStore.test.js
    ├── useProfileStore.test.js
    ├── useTestSessionStore.test.js
    ├── useDashboardStore.test.js
    ├── useSubscriptionStore.test.js
    └── useQuotaStore.test.js
```

---

# Frontend Test Cases

---

## 10. useAuthStore.test.js (20 tests)

**File:** `frontend/src/__tests__/stores/useAuthStore.test.js`

Tests authentication state management including sign in, sign out, and session handling.

### 10.1 SignIn Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSignInSuccessfully` | Valid email/password | Sets user and session |
| `shouldSetErrorOnInvalidCredentials` | Wrong password | Sets error message |
| `shouldClearErrorBeforeSignIn` | New sign in clears old error | Error is null |

### 10.2 SignOut Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSignOutSuccessfully` | Sign out logged in user | Clears user/session |
| `shouldClearAuthState` | clearAuth action | All auth state null |

### 10.3 Session Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetUserAndSession` | Set user/session actions | State updated |
| `shouldHandleLoadingState` | Loading during auth | loading = true |

---

## 11. useTestStore.test.js (34 tests)

**File:** `frontend/src/__tests__/stores/useTestStore.test.js`

Tests test-taking UI state: answers, timer, modals, navigation.

### 11.1 Answer Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetSingleAnswer` | setAnswer(1, 'A') | answers[1] = 'A' |
| `shouldSetMultipleAnswers` | setAnswers({1:'A', 2:'B'}) | Both answers set |
| `shouldClearAnswers` | clearAnswers() | answers = {} |

### 11.2 Timer Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetTimeLeft` | setTimeLeft(1800) | timeLeft = 1800 |
| `shouldStartTimer` | startTimer() | timerRunning = true |
| `shouldStopTimer` | stopTimer() | timerRunning = false |

### 11.3 Essay Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetEssay` | setEssay(1, 'text') | essays[1] = 'text' |
| `shouldClearEssays` | clearEssays() | essays = {1:'', 2:''} |

---

## 12. useProfileStore.test.js (15 tests)

**File:** `frontend/src/__tests__/stores/useProfileStore.test.js`

Tests profile loading, caching, creation, and updates.

### 12.1 loadProfile Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldLoadProfileSuccessfully` | Load existing profile | Returns ProfileDTO |
| `shouldSkipLoadingIfAlreadyLoaded` | Same user ID | Uses cached profile |
| `shouldReloadWhenForceReloadIsTrue` | forceReload=true | API called again |
| `shouldCreateProfileOn404` | Profile not found | Creates new profile |

### 12.2 updateProfile Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldUpdateProfileSuccessfully` | Update displayName | Profile updated |
| `shouldThrowErrorIfNoProfileLoaded` | Update without load | Throws error |

---

## 13. useTestSessionStore.test.js (18 tests)

**File:** `frontend/src/__tests__/stores/useTestSessionStore.test.js`

Tests test session API operations: start, load, save, submit, cancel.

### 13.1 startOrResumeAttempt Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldStartNewAttempt` | New test attempt | Returns attemptId |
| `shouldResumeExistingAttempt` | IN_PROGRESS exists | Returns existing |
| `shouldForceNewAttempt` | forceNew=true | Creates new |

### 13.2 loadTestData Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldLoadTestDataSuccessfully` | Load test sections | Returns test data |
| `shouldUseCacheWithinTTL` | Multiple loads | Only 1 API call |

### 13.3 saveProgress Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSaveProgressWithAnswers` | Save answers | Progress saved |
| `shouldSaveEssaysForWriting` | Save essays | Drafts saved |

---

## 14. useDashboardStore.test.js (17 tests)

**File:** `frontend/src/__tests__/stores/useDashboardStore.test.js`

Tests dashboard data fetching, caching, and pagination.

### 14.1 fetchSummary Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchSummarySuccessfully` | Initial fetch | Returns summary |
| `shouldUseCacheWhenFresh` | Cached data | No API call |
| `shouldSkipCacheWithParams` | Pagination params | Fresh fetch |

### 14.2 Pagination Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `setPageShouldUpdateCurrentPage` | setPage(5) | currentPage = 5 |
| `setPageSizeShouldResetToPage0` | setPageSize(10) | page = 0, size = 10 |
| `setDebouncedSearchShouldReset` | setDebounced('test') | page = 0 |

---

## 15. useSubscriptionStore.test.js (13 tests)

**File:** `frontend/src/__tests__/stores/useSubscriptionStore.test.js`

Tests subscription state management and feature access.

### 15.1 fetchSubscriptionStatus Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchFreeTier` | Cramerie tier | isPremium = false |
| `shouldFetchPremiumTier` | Cramerous tier | isPremium = true |
| `shouldHandleFeaturesArray` | features[] format | featuresMap populated |
| `shouldHandleFeaturesMap` | featuresMap format | Features accessible |

### 15.2 hasFeature Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnTrueForExistingFeature` | feature exists | true |
| `shouldReturnFalseForMissingFeature` | feature missing | false |

---

## 16. useQuotaStore.test.js (26 tests)

**File:** `frontend/src/__tests__/stores/useQuotaStore.test.js`

Tests quota status, progress calculations, and pre-check functionality.

### 16.1 fetchQuotaStatus Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchQuotaSuccessfully` | Get quota status | quotaStatus set |
| `shouldSkipIfNoUser` | No authenticated user | No API call |
| `shouldUseCacheWithin30s` | Quick re-fetch | Uses cache |
| `shouldForceFetch` | force=true | Fresh fetch |

### 16.2 Progress Calculation Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getGlobalProgressForPremium` | Premium user | Returns 0 |
| `getGlobalProgressNormal` | 10/20 used | Returns 50 |
| `getGlobalProgressAI` | AI quota | Correct percentage |
| `getSkillProgress` | Skill-specific | Correct percentage |
| `getProgressColor` | <50% / 50-79% / ≥80% | green/yellow/red |

### 16.3 preCheckAttempt Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldPreCheckSuccessfully` | Quota available | allowed = true |
| `shouldHandleBlockedAttempt` | Quota exceeded | allowed = false |

---

## 📝 Notes

1. **Backend Mocking:** All tests use Mockito `@Mock` and `@InjectMocks` for isolation
2. **Frontend Mocking:** Uses `vi.mock()` for API modules
3. **No External Dependencies:** Backend runs with H2 in-memory DB, no Supabase/PayOS calls
4. **Store Testing Pattern:** Frontend tests create isolated store instances per test
5. **Strict Stubbing:** Mockito strict mode enabled to catch unused stubs

---

*Last updated: January 15, 2026*
