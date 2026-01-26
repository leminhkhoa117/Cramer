# 📋 Cramer Frontend - Test Cases Report

> **Last Updated:** January 26, 2026  
> **Total Frontend Tests:** ~267 tests (10 User Stores + 2 Admin Stores)  
> **Status:** ✅ All Passing

---

## 🔧 How to Run Tests

### Run All Frontend Tests
```powershell
cd frontend
npx vitest run
```

### Run Tests with Verbose Output
```powershell
npx vitest run --reporter=verbose
```

### Run Tests in Watch Mode
```powershell
npx vitest
```

### Run with Coverage Report
```powershell
npx vitest run --coverage
```

### Run Specific Test File
```powershell
npx vitest run useAuthStore.test.js
```

---

## 📊 User Store Test Summary (10 files)

| Test File | Tests | Status |
|-----------|-------|--------|
| useAuthStore.test.js | 20 | ✅ |
| useTestStore.test.js | 34 | ✅ |
| useProfileStore.test.js | 15 | ✅ |
| useTestSessionStore.test.js | 18 | ✅ |
| useDashboardStore.test.js | 17 | ✅ |
| useSubscriptionStore.test.js | 13 | ✅ |
| useQuotaStore.test.js | 26 | ✅ |
| useCourseStore.test.js | 23 | ✅ |
| useVocabularyStore.test.js | ~30 | ✅ |
| useUserStatsStore.test.js | ~25 | ✅ |
| **User Stores Total** | **~221** | **✅** |

---

## 📊 Admin Store Test Summary (2 files)

| Test File | Tests | Status |
|-----------|-------|--------|
| useABTSStore.test.js | ~32 | ✅ |
| useAdminDashboardStore.test.js | ~14 | ✅ |
| **Admin Stores Total** | **~46** | **✅** |

---

# 📝 Detailed Test Cases

---

## User Store Tests

### 1. useAuthStore.test.js (20 tests)

**File:** `frontend/src/__tests__/stores/useAuthStore.test.js`

Tests authentication state management including sign in, sign out, and session handling.

#### SignIn Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSignInSuccessfully` | Valid email/password | Sets user and session |
| `shouldSetErrorOnInvalidCredentials` | Wrong password | Sets error message |
| `shouldClearErrorBeforeSignIn` | New sign in clears old error | Error is null |

#### SignOut Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSignOutSuccessfully` | Sign out logged in user | Clears user/session |
| `shouldClearAuthState` | clearAuth action | All auth state null |

#### Session Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetUserAndSession` | Set user/session actions | State updated |
| `shouldHandleLoadingState` | Loading during auth | loading = true |

---

### 2. useTestStore.test.js (34 tests)

**File:** `frontend/src/__tests__/stores/useTestStore.test.js`

Tests test-taking UI state: answers, timer, modals, navigation.

#### Answer Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetSingleAnswer` | setAnswer(1, 'A') | answers[1] = 'A' |
| `shouldSetMultipleAnswers` | setAnswers({1:'A', 2:'B'}) | Both answers set |
| `shouldClearAnswers` | clearAnswers() | answers = {} |

#### Timer Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetTimeLeft` | setTimeLeft(1800) | timeLeft = 1800 |
| `shouldStartTimer` | startTimer() | timerRunning = true |
| `shouldStopTimer` | stopTimer() | timerRunning = false |

#### Essay Management

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetEssay` | setEssay(1, 'text') | essays[1] = 'text' |
| `shouldClearEssays` | clearEssays() | essays = {1:'', 2:''} |

---

### 3. useProfileStore.test.js (15 tests)

**File:** `frontend/src/__tests__/stores/useProfileStore.test.js`

Tests profile loading, caching, creation, and updates.

#### loadProfile Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldLoadProfileSuccessfully` | Load existing profile | Returns ProfileDTO |
| `shouldSkipLoadingIfAlreadyLoaded` | Same user ID | Uses cached profile |
| `shouldReloadWhenForceReloadIsTrue` | forceReload=true | API called again |
| `shouldCreateProfileOn404` | Profile not found | Creates new profile |

#### updateProfile Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldUpdateProfileSuccessfully` | Update displayName | Profile updated |
| `shouldThrowErrorIfNoProfileLoaded` | Update without load | Throws error |

---

### 4. useTestSessionStore.test.js (18 tests)

**File:** `frontend/src/__tests__/stores/useTestSessionStore.test.js`

Tests test session API operations: start, load, save, submit, cancel.

#### startOrResumeAttempt Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldStartNewAttempt` | New test attempt | Returns attemptId |
| `shouldResumeExistingAttempt` | IN_PROGRESS exists | Returns existing |
| `shouldForceNewAttempt` | forceNew=true | Creates new |

#### loadTestData Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldLoadTestDataSuccessfully` | Load test sections | Returns test data |
| `shouldUseCacheWithinTTL` | Multiple loads | Only 1 API call |

#### saveProgress Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSaveProgressWithAnswers` | Save answers | Progress saved |
| `shouldSaveEssaysForWriting` | Save essays | Drafts saved |

---

### 5. useDashboardStore.test.js (17 tests)

**File:** `frontend/src/__tests__/stores/useDashboardStore.test.js`

Tests dashboard data fetching, caching, and pagination.

#### fetchSummary Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchSummarySuccessfully` | Initial fetch | Returns summary |
| `shouldUseCacheWhenFresh` | Cached data | No API call |
| `shouldSkipCacheWithParams` | Pagination params | Fresh fetch |

#### Pagination Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `setPageShouldUpdateCurrentPage` | setPage(5) | currentPage = 5 |
| `setPageSizeShouldResetToPage0` | setPageSize(10) | page = 0, size = 10 |
| `setDebouncedSearchShouldReset` | setDebounced('test') | page = 0 |

---

### 6. useSubscriptionStore.test.js (13 tests)

**File:** `frontend/src/__tests__/stores/useSubscriptionStore.test.js`

Tests subscription state management and feature access.

#### fetchSubscriptionStatus Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchFreeTier` | Cramerie tier | isPremium = false |
| `shouldFetchPremiumTier` | Cramerous tier | isPremium = true |
| `shouldHandleFeaturesArray` | features[] format | featuresMap populated |
| `shouldHandleFeaturesMap` | featuresMap format | Features accessible |

#### hasFeature Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldReturnTrueForExistingFeature` | feature exists | true |
| `shouldReturnFalseForMissingFeature` | feature missing | false |

---

### 7. useQuotaStore.test.js (26 tests)

**File:** `frontend/src/__tests__/stores/useQuotaStore.test.js`

Tests quota status, progress calculations, and pre-check functionality.

#### fetchQuotaStatus Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchQuotaSuccessfully` | Get quota status | quotaStatus set |
| `shouldSkipIfNoUser` | No authenticated user | No API call |
| `shouldUseCacheWithin30s` | Quick re-fetch | Uses cache |
| `shouldForceFetch` | force=true | Fresh fetch |

#### Progress Calculation Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `getGlobalProgressForPremium` | Premium user | Returns 0 |
| `getGlobalProgressNormal` | 10/20 used | Returns 50 |
| `getGlobalProgressAI` | AI quota | Correct percentage |
| `getSkillProgress` | Skill-specific | Correct percentage |
| `getProgressColor` | <50% / 50-79% / ≥80% | green/yellow/red |

#### preCheckAttempt Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldPreCheckSuccessfully` | Quota available | allowed = true |
| `shouldHandleBlockedAttempt` | Quota exceeded | allowed = false |

---

### 8. useCourseStore.test.js (23 tests)

**File:** `frontend/src/__tests__/stores/useCourseStore.test.js`

Tests course fetching, caching, pagination, and search functionality.

#### fetchCourses Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetLoadingState` | Loading during fetch | loading = true |
| `shouldFetchCoursesSuccessfully` | Fetch courses | courses populated |
| `shouldHandleFetchError` | API error | error message set |
| `shouldPreservePaginationInfo` | Paginated response | pagination updated |

#### fetchCourseTests Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchTestsForCourse` | Fetch tests | courseTests[code] set |
| `shouldReturnCachedTests` | Cached tests | No API call |

#### Pagination & Search Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSetPage` | setPage(5) | currentPage = 5 |
| `shouldSetPageSize` | setPageSize(25) | pageSize = 25, page = 0 |
| `shouldSetSearchQuery` | setSearchQuery('cam') | searchQuery = 'cam' |

---

### 9. useVocabularyStore.test.js (~30 tests)

**File:** `frontend/src/__tests__/stores/useVocabularyStore.test.js`

Tests vocabulary management: CRUD, search, mastered toggle.

#### Vocabulary CRUD Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchVocabularyList` | Get paginated list | vocabulary populated |
| `shouldAddNewWord` | Add word | Word added to list |
| `shouldUpdateWord` | Update word | Word updated |
| `shouldDeleteWord` | Delete word | Word removed |
| `shouldToggleMastered` | Toggle mastered status | Status toggled |

#### Search & Filter Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldSearchByWord` | Search 'water' | Filtered results |
| `shouldFilterByMastered` | Filter mastered | Only mastered words |

---

### 10. useUserStatsStore.test.js (~25 tests)

**File:** `frontend/src/__tests__/stores/useUserStatsStore.test.js`

Tests user statistics and progress tracking.

#### Fetch Stats Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchUserStats` | Get user stats | stats populated |
| `shouldHandleEmptyStats` | New user | Default values |

#### Progress Calculation Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldCalculateOverallProgress` | Calculate progress | percentage |
| `shouldGetSkillProgress` | Per-skill progress | skill percentages |

---

## Admin Store Tests

### 11. useABTSStore.test.js (~32 tests)

**File:** `frontend/src/__tests__/admin/stores/useABTSStore.test.js`

Tests AI-Based Test Generation System (ABTS) wizard state.

#### Constants Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldHaveCorrectReadingPartTypes` | Reading part types | Correct types per part |
| `shouldHaveCorrectListeningPartTypes` | Listening part types | Correct types per part |
| `shouldHaveCorrectQuestionCounts` | Question counts | 13/13/14 for Reading |

#### Wizard Navigation Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldOpenWizard` | openWizard() | isWizardOpen = true |
| `shouldCloseWizard` | closeWizard() | isWizardOpen = false |
| `shouldGoToStep` | goToStep(n) | currentStep = n |
| `shouldGoToNextStep` | nextStep() | currentStep++ |
| `shouldGoToPrevStep` | prevStep() | currentStep-- |

#### Form Data Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldUpdateFormData` | updateFormData() | formData updated |
| `shouldSetFormField` | setFormField() | Single field updated |
| `shouldResetForm` | resetForm() | Form reset to initial |

#### Part Selection Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldTogglePartSelection` | toggle part | Part added/removed |
| `shouldSetPartConfig` | setPartConfig() | Config saved |
| `shouldRandomizePartConfig` | randomize() | Random types selected |

---

### 12. useAdminDashboardStore.test.js (~14 tests)

**File:** `frontend/src/__tests__/admin/stores/useAdminDashboardStore.test.js`

Tests admin dashboard stats and activities.

#### Fetch Stats Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchStatsSuccessfully` | Fetch stats | stats populated |
| `shouldHandleError` | API error | error set |
| `shouldUseCacheWhenValid` | Cached data | No API call |
| `shouldForceFetch` | force=true | Fresh fetch |

#### Activities Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldFetchRecentActivities` | Get activities | activities list |
| `shouldRespectLimit` | limit parameter | Correct count |

#### System Status Tests

| Test Name | Description | Expected Result |
|-----------|-------------|-----------------|
| `shouldCheckSystemStatus` | Check status | status object |
| `shouldHandleDegradedStatus` | Degraded service | degraded status |

---

## 📁 Test File Locations

```
frontend/src/__tests__/
├── setupTests.js              # Test configuration
├── mocks/                     # Mock handlers
│   ├── handlers.js
│   └── server.js
├── stores/                    # User store tests
│   ├── useAuthStore.test.js
│   ├── useTestStore.test.js
│   ├── useProfileStore.test.js
│   ├── useTestSessionStore.test.js
│   ├── useDashboardStore.test.js
│   ├── useSubscriptionStore.test.js
│   ├── useQuotaStore.test.js
│   ├── useCourseStore.test.js
│   ├── useVocabularyStore.test.js
│   └── useUserStatsStore.test.js
└── admin/                     # Admin store tests
    └── stores/
        ├── useABTSStore.test.js
        └── useAdminDashboardStore.test.js
```

---

## 🧪 Test Configuration

### vitest.config.js

```javascript
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/__tests__/setupTests.js'],
    include: ['src/__tests__/**/*.test.js'],
  },
});
```

### setupTests.js

```javascript
import { beforeEach, vi } from 'vitest';

// Reset all mocks before each test
beforeEach(() => {
  vi.clearAllMocks();
});

// Mock Supabase client
vi.mock('../api/supabaseClient', () => ({
  supabase: {
    auth: {
      signInWithPassword: vi.fn(),
      signOut: vi.fn(),
      getSession: vi.fn(),
    },
  },
}));
```

---

## 📝 Notes

1. **Store Testing Pattern:** Each test creates an isolated store instance
2. **Mocking:** Uses `vi.mock()` for API modules
3. **Act Wrapper:** Uses `act()` from `@testing-library/react` for state updates
4. **Async Testing:** Uses `async/await` with proper `act()` wrapping
5. **State Reset:** Each test resets store to initial state in `beforeEach`

---

## 🚧 TODO: Future Tests

| Store | Priority | Description |
|-------|----------|-------------|
| useWritingStore | Medium | Writing submission state |
| useChatStore | Medium | AI chat state |
| usePaymentStore | Low | Payment flow state |
| Component Tests | Medium | React component testing |
| E2E Tests | Low | Playwright/Cypress integration |

---

*Last updated: January 26, 2026*
