# Cramer Frontend State Management

This document describes the Zustand-based state management architecture used in the Cramer IELTS platform frontend.

---

## Overview

The Cramer frontend uses **Zustand** for all global state management, replacing the previous React Context approach. Zustand provides:

- **Minimal re-renders** via granular subscriptions
- **Built-in DevTools** for debugging
- **Middleware support** (persist, immer, devtools, subscribeWithSelector)
- **Cross-component state access** without props drilling

### Key Benefits
- Data caching prevents refetch on navigation
- Reduced re-renders via selector patterns
- DevTools integration in development mode
- ~200 lines of code consolidated from duplicated logic

---

## Store Architecture

All stores are located in `frontend/src/stores/` and re-exported from `index.js` for clean imports.

### Import Pattern
```javascript
import { useAuthStore, useProfileStore, useTestStore } from '../stores';
```

### Selector Pattern (Recommended)
```javascript
// Good: Only re-renders when `user` changes
const user = useAuthStore(state => state.user);

// Bad: Re-renders on ANY state change
const { user, session, loading } = useAuthStore();
```

---

## Stores Reference

### 1. `useAuthStore` — Authentication State

**Location:** [useAuthStore.js](../../../frontend/src/stores/useAuthStore.js)

**Purpose:** Manages user authentication, session, and OAuth flows.

**State Shape:**
```typescript
{
  user: User | null;           // Current Supabase user
  session: Session | null;     // Supabase session with JWT
  loading: boolean;            // Auth operation in progress
  error: string | null;        // Last error message
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `setUser(user)` | Set current user |
| `setSession(session)` | Set current session |
| `setLoading(loading)` | Set loading state |
| `setError(error)` | Set error message |
| `clearAuth()` | Clear all auth state (on sign out) |
| `signUp(email, password, username)` | Register new user (stores pending username for OTP flow) |
| `signIn(email, password)` | Sign in with credentials |
| `signOut()` | Sign out and clear state |
| `verifyOtp(email, otpCode)` | Verify email OTP code |
| `resendOtp(email)` | Resend OTP email |
| `signInWithGoogle()` | OAuth with Google |
| `signInWithFacebook()` | OAuth with Facebook |
| `initializeAuth()` | Initialize auth listener, sets up API client token |

**Exported Selectors:**
```javascript
export const selectUser = (state) => state.user;
export const selectSession = (state) => state.session;
export const selectLoading = (state) => state.loading;
export const selectError = (state) => state.error;
export const selectIsAuthenticated = (state) => !!state.user && !!state.session;
```

**External Actions:**
```javascript
import { authActions } from '../stores/useAuthStore';
// For use outside React components
authActions.signIn(email, password);
```

**Middleware:** `devtools`, `subscribeWithSelector`

**Notes:**
- Uses `subscribeWithSelector` to enable watching specific state slices
- Handles `TOKEN_REFRESHED` events without triggering full state updates
- Auto-configures API client with `setupApiClient()` when session changes

---

### 2. `useProfileStore` — User Profile Management

**Location:** [useProfileStore.js](../../../frontend/src/stores/useProfileStore.js)

**Purpose:** Manages user profile data with auto-sync to auth state.

**State Shape:**
```typescript
{
  profile: Profile | null;        // User profile data
  loading: boolean;               // Loading state
  lastLoadedUserId: string | null; // ID of last loaded profile (cache key)
  error: string | null;           // Error message
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `loadProfile(userId, forceReload)` | Load profile by user ID (cached unless force) |
| `createProfileForUser(userId, username)` | Create new profile (handles username conflicts) |
| `updateProfile(newData)` | Update profile (merges with existing) |
| `clearProfile()` | Clear profile state (on sign out) |

**Middleware:** `devtools`

**Auto-Sync:**
The store automatically subscribes to `useAuthStore.user` changes:
- When user logs in → loads their profile
- When user logs out → clears profile

```javascript
// This runs automatically on module load
useAuthStore.subscribe(
  (state) => state.user,
  (user, previousUser) => { /* auto-load/clear */ }
);
```

---

### 3. `useTestStore` — Test-Taking UI State

**Location:** [useTestStore.js](../../../frontend/src/stores/useTestStore.js)

**Purpose:** Single source of truth for ALL test-taking UI state. Eliminates 24+ props drilling.

**State Shape:**
```typescript
{
  // Core Test State
  testStatus: 'idle' | 'loading' | 'running' | 'submitted' | 'error';
  testData: Part[];              // Array of test parts/sections
  attempt: Attempt | null;       // Current test attempt
  answers: Record<string, any>;  // { [questionId]: answer }
  essays: { 1: string, 2: string }; // Writing Task 1 & 2
  loading: boolean;
  error: string | null;
  isSubmitting: boolean;

  // UI State
  displayPartIndex: number;      // Current part index
  activeTask: 1 | 2;             // Active writing task

  // Timer State
  timeLeft: number;              // Seconds remaining
  timerRunning: boolean;

  // Audio State (Listening)
  isAutoplay: boolean;
  activeAudioIndex: number;

  // Modal State
  isConfirmModalOpen: boolean;
  isResumeModalOpen: boolean;
  isExitModalOpen: boolean;
  inProgressAttempt: Attempt | null;
  isStartingNew: boolean;
  isSavingProgress: boolean;
}
```

**Actions (Core):**
| Action | Description |
|--------|-------------|
| `setTestStatus(status)` | Set test lifecycle status |
| `setTestData(data)` | Set test parts/sections |
| `setAttempt(attempt)` | Set current attempt |
| `setAnswer(questionId, value)` | Set single answer |
| `setAnswers(answers)` | Bulk set answers (resume) |
| `setEssay(taskNumber, text)` | Set single essay |
| `setEssays(essays)` | Bulk set essays |
| `setError(error)` | Set error (also sets status to 'error') |

**Actions (Timer):**
| Action | Description |
|--------|-------------|
| `setTimeLeft(time)` | Set remaining time |
| `decrementTime()` | Decrement time by 1 second |
| `startTimer()` | Start timer |
| `stopTimer()` | Stop timer |

**Actions (Audio - Listening):**
| Action | Description |
|--------|-------------|
| `setIsAutoplay(bool)` | Toggle autoplay |
| `setActiveAudioIndex(index)` | Set current audio track |

**Actions (Modal):**
| Action | Description |
|--------|-------------|
| `openConfirmModal()` / `closeConfirmModal()` | Submit confirmation |
| `openResumeModal(attempt)` / `closeResumeModal()` | Resume/new choice |
| `openExitModal()` / `closeExitModal()` | Exit confirmation |

**Computed/Selectors:**
| Selector | Returns |
|----------|---------|
| `getWordCount(taskNumber)` | Word count for essay |
| `getTotalQuestions()` | Total questions in test |
| `getAnsweredCount()` | Count of answered questions |
| `isAllAnswered()` | Boolean if all answered |
| `getProgressPercentage()` | 0-100 completion |
| `getCurrentPart()` | Current part data |
| `getFormattedTime()` | "MM:SS" string |
| `isTimeLow()` | Under 5 minutes |
| `isTimeCritical()` | Under 1 minute |

**Exported Standalone Selectors:**
```javascript
export const selectTestStatus = (state) => state.testStatus;
export const selectAnswers = (state) => state.answers;
export const selectTimeLeft = (state) => state.timeLeft;
export const selectModalState = (state) => ({
  isConfirmModalOpen: state.isConfirmModalOpen,
  isResumeModalOpen: state.isResumeModalOpen,
  // ...
});
```

**Middleware:** `devtools`, `immer`

**Notes:**
- Uses `immer` for immutable updates with mutable syntax
- `resetTestState()` restores all values to `initialState`

---

### 4. `useTestSessionStore` — Test API Operations

**Location:** [useTestSessionStore.js](../../../frontend/src/stores/useTestSessionStore.js)

**Purpose:** Handles test attempt API operations and data caching.

**State Shape:**
```typescript
{
  currentAttemptId: string | null;
  attemptStatus: 'idle' | 'loading' | 'ready' | 'error';
  lastSavedAt: Date | null;
  autoSaveEnabled: boolean;
  testDataCache: Record<string, { data: any, fetchedAt: number }>;
}
```

**Cache Configuration:**
```javascript
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes
```

**Actions:**
| Action | Description |
|--------|-------------|
| `startOrResumeAttempt(source, testNum, skill, forceNew)` | Start/resume test attempt |
| `loadTestData(source, testNum, skill)` | Load test data (cached) |
| `loadAnswers(attemptId)` | Fetch saved answers |
| `loadEssays(attemptId)` | Fetch saved essays |
| `saveProgress(attemptId, { answers, essays, timeLeft, currentPart })` | Save progress |
| `submitAttempt(attemptId, answers)` | Submit reading/listening test |
| `submitWriting(attemptId, essays)` | Submit writing for AI grading |
| `cancelAttempt(attemptId)` | Cancel/delete attempt |
| `clearCache()` | Clear all cached test data |
| `setAutoSave(enabled)` | Toggle auto-save |
| `reset()` | Reset to initial state (keeps cache) |

**Middleware:** `devtools`

---

### 5. `useDashboardStore` — Dashboard Data

**Location:** [useDashboardStore.js](../../../frontend/src/stores/useDashboardStore.js)

**Purpose:** Dashboard summary with sessionStorage persistence.

**State Shape:**
```typescript
{
  summary: DashboardSummary | null;
  loading: boolean;
  error: string | null;
  lastFetchedAt: string | null;  // ISO timestamp

  // Pagination
  currentPage: number;
  pageSize: number;              // Default: 4 (2x2 grid)
  totalPages: number;
  searchQuery: string;
  debouncedSearchQuery: string;
}
```

**Cache Configuration:**
```javascript
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
```

**Persistence Configuration:**
```javascript
persist(
  (set, get) => ({ /* store */ }),
  {
    name: 'dashboard-storage',
    storage: {
      getItem: (name) => JSON.parse(sessionStorage.getItem(name)),
      setItem: (name, value) => sessionStorage.setItem(name, JSON.stringify(value)),
      removeItem: (name) => sessionStorage.removeItem(name),
    },
    partialize: (state) => ({
      summary: state.summary,
      lastFetchedAt: state.lastFetchedAt,
      currentPage: state.currentPage,
      // ...pagination fields
    }),
  }
)
```

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchSummary(page, size, search)` | Fetch dashboard data (uses cache) |
| `refreshSummary()` | Force refresh |
| `setPage(page)` | Set current page |
| `setPageSize(size)` | Set page size (resets to page 0) |
| `setSearchQuery(query)` | Set search (immediate) |
| `setDebouncedSearchQuery(query)` | Set debounced search (resets page) |
| `resetPagination()` | Reset all pagination |
| `invalidateCache()` | Mark cache as stale |
| `updateSummary(updater)` | Optimistic update |

**Selectors:**
| Selector | Returns |
|----------|---------|
| `isStale()` | Boolean if cache expired |

**Middleware:** `devtools`, `persist` (sessionStorage)

---

### 6. `useCourseStore` — Course Data

**Location:** [useCourseStore.js](../../../frontend/src/stores/useCourseStore.js)

**Purpose:** Course list and test mappings with caching.

**State Shape:**
```typescript
{
  courses: Course[];
  courseTests: Record<string, Test[]>;    // { [courseName]: tests }
  courseDetails: Record<string, Details>; // { [courseCode]: details }
  loading: boolean;
  error: string | null;
  lastFetchedAt: Date | null;

  // Pagination
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  searchQuery: string;
  debouncedSearchQuery: string;
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchCourses(page, size, search)` | Paginated course list |
| `fetchCoursesV2()` | Full TestSetDTO objects |
| `fetchCourseTests(courseName)` | Tests for course (cached) |
| `fetchCourseDetails(courseCode)` | Course details (cached) |
| `getCachedDetails(courseCode)` | Get cached details only |
| `setPage(page)` | Set current page |
| `setSearchQuery(query)` | Set search query |
| `clearCache()` | Clear all cached data |

**Middleware:** `devtools`

---

### 7. `useVocabularyStore` — Vocabulary Management

**Location:** [useVocabularyStore.js](../../../frontend/src/stores/useVocabularyStore.js)

**Purpose:** Vocabulary CRUD with filtering and translation.

**State Shape:**
```typescript
{
  vocabulary: VocabularyEntry[];
  stats: VocabStats | null;
  loading: boolean;
  error: string | null;
  lastFetchedAt: string | null;

  // Pagination
  currentPage: number;
  pageSize: number;           // Default: 20
  totalPages: number;
  totalElements: number;
  searchQuery: string;
  debouncedSearchQuery: string;
  
  // Filter
  filter: 'all' | 'mastered' | 'unmastered';

  // Translation
  translating: boolean;
  translationError: string | null;
}
```

**Cache:** 5 minutes TTL

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchVocabulary(page, size, search, filter)` | Paginated list |
| `fetchStats()` | Get vocabulary statistics |
| `addWord(wordData)` | Add new word |
| `updateWord(id, wordData)` | Update word |
| `deleteWord(id)` | Delete word |
| `translateWord(word, context)` | AI translation |
| `toggleMastered(id)` | Toggle mastered status |
| `setPage(page)` | Change page (auto-fetches) |
| `setSearchQuery(query)` | Set immediate search |
| `setDebouncedSearchQuery(query)` | Set debounced search |
| `setFilter(filter)` | Set filter (server-side) |
| `resetStore()` | Reset all state |

**Middleware:** `devtools`

---

### 8. `useUserStatsStore` — User Stats (Subscription, Credits, Chat)

**Location:** [useUserStatsStore.js](../../../frontend/src/stores/useUserStatsStore.js)

**Purpose:** Aggregated user stats for FloatingAssistant and other widgets.

**State Shape:**
```typescript
{
  // Subscription
  subscription: UserSubscription | null;
  tiers: SubscriptionTier[];

  // Credits (Lúa)
  credits: {
    balance: number;
    lifetimeEarned: number;
    lifetimeSpent: number;
  };

  // Chat Usage (MONTHLY limits)
  chatUsage: {
    usedThisMonth: number;
    monthlyLimit: number;
    remainingThisMonth: number;
    // Legacy fields for backward compatibility
    usedToday: number;
    dailyLimit: number;
    remainingToday: number;
  };

  // Grading Status
  gradingStatus: {
    canGrade: boolean;
    monthlyLimit: number;
    usedThisMonth: number;
    remaining: number;
  };

  loading: boolean;
  error: string | null;
  lastFetched: number | null;  // Timestamp
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchUserStats()` | Fetch all stats in parallel (30s debounce) |
| `refreshCredits()` | Refresh credits only |
| `refreshChatUsage()` | Refresh chat usage only |
| `incrementChatUsage()` | Optimistic update |
| `deductCredits(amount)` | Optimistic deduction |

**Selectors:**
| Selector | Returns |
|----------|---------|
| `getTierEmoji()` | 🌾, 🌻, or 🌟 |
| `getTierName()` | Vietnamese tier name |

**Middleware:** `devtools`, `subscribeWithSelector`

---

### 9. `useSubscriptionStore` — Feature Access Control

**Location:** [useSubscriptionStore.js](../../../frontend/src/stores/useSubscriptionStore.js)

**Purpose:** Subscription tier and feature gating.

**State Shape:**
```typescript
{
  tier: string | null;           // 'cramerie' | 'cramerich' | 'cramerous'
  tierNameVi: string | null;
  tierNameEn: string | null;
  features: Record<string, boolean>; // { [featureCode]: true/false }
  isPremium: boolean;
  loading: boolean;
  error: string | null;
  lastFetchedAt: number | null;
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchSubscriptionStatus()` | Fetch tier and features |
| `hasFeature(featureCode)` | Check single feature access |
| `reset()` | Reset on logout |
| `setFeatures(features)` | Direct set (testing/optimistic) |

**Exported Selectors:**
```javascript
export const selectTier = (state) => state.tier;
export const selectFeatures = (state) => state.features;
export const selectIsPremium = (state) => state.isPremium;
export const selectHasFeature = (featureCode) => (state) =>
  state.features[featureCode] === true;
```

**Middleware:** `devtools`, `subscribeWithSelector`

---

### 10. `useQuotaStore` — Dual-Quota Billing

**Location:** [useQuotaStore.js](../../../frontend/src/stores/useQuotaStore.js)

**Purpose:** Tracks global and per-skill monthly usage limits.

**State Shape:**
```typescript
{
  quotaStatus: QuotaStatus | null;
  loading: boolean;
  error: string | null;
  lastFetched: number | null;

  // Pre-check for current attempt
  preCheckResult: PreCheckResult | null;
  preCheckLoading: boolean;
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `fetchQuotaStatus(force)` | Fetch quota (30s debounce) |
| `preCheckAttempt(skill, isAI)` | Check if attempt allowed |
| `clearPreCheck()` | Clear pre-check result |

**Selectors:**
| Selector | Returns |
|----------|---------|
| `isPremium()` | Boolean for unlimited quota |
| `getGlobalProgress(isAI)` | 0-100 progress |
| `getSkillProgress(skill, isAI)` | 0-100 progress |
| `getProgressColor(percent)` | 'green' | 'yellow' | 'red' |

**Middleware:** `devtools`, `subscribeWithSelector`

---

### 11. `useABTSStore` — AI-Based Test Generation (Admin)

**Location:** [src/admin/stores/useABTSStore.js](../../../frontend/src/admin/stores/useABTSStore.js) (~1,549 lines)

**Purpose:** Central state management for the AI Generation Studio in the admin panel.

**State Shape:**
```typescript
{
  // Wizard State
  currentStep: number;           // 1-4 wizard steps
  isWizardOpen: boolean;

  // Form Data
  formData: {
    skill: 'READING' | 'LISTENING' | 'WRITING' | null;
    scope: 'MULTI_PART';         // Always multi-part mode (v7.0+)
    partNumber: number;
    topic: string;
    hashtags: string[];
    facts: string[];
    difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
    model: string;               // OpenRouter model ID
    temperature: number;
    enableReasoning: boolean;    // Chain-of-thought tokens
    selectedParts: number[];     // [1, 2, 3] for Reading
    partConfigs: Record<number, PartConfig>;
    refinementModel: string | null;
    enableRefinementCaching: boolean;
    enableRefinementReasoning: boolean;
  };

  // Generation State
  isGenerating: boolean;
  generationResult: GenerationResult | null;
  generationStream: StreamEvent[];
  validationIssues: ValidationIssue[];
  abortController: AbortController | null;

  // Save State
  isSaving: boolean;
  saveError: string | null;
  selectedSetId: string | null;
  selectedTestId: string | null;

  // Refinement State (Agent 2)
  selectedIssues: string[];      // IDs of issues for refinement
  isRefining: boolean;
  refinementResult: RefinementResult | null;
  refinementStream: StreamEvent[];
  abortRefinement: () => void | null;

  // Audio URLs (Listening)
  audioUrls: Record<number, string>;
}
```

**Key Actions (Wizard):**
| Action | Description |
|--------|-------------|
| `openWizard()` | Open generation wizard |
| `closeWizard()` | Close and reset wizard |
| `goToStep(step)` | Navigate to specific step |
| `nextStep()` / `prevStep()` | Step navigation |
| `isStepValid(step)` | Validate step completion |

**Key Actions (Form):**
| Action | Description |
|--------|-------------|
| `updateFormData(updates)` | Bulk update form fields |
| `setFormField(field, value)` | Set single field |
| `resetForm()` | Reset to initial state |
| `setAudioUrl(part, url)` | Set Listening audio URL |

**Key Actions (Multi-Part):**
| Action | Description |
|--------|-------------|
| `togglePartSelection(part)` | Toggle part for generation |
| `setPartConfig(part, config)` | Set part-specific config |
| `applyGlobalConfigToAllParts()` | Copy global to all parts |
| `randomizePartConfig(part)` | Randomize question types |
| `randomizeAllParts()` | Randomize all selected parts |

**Key Actions (Generation):**
| Action | Description |
|--------|-------------|
| `startGeneration()` | Begin AI generation |
| `cancelGeneration()` | Abort in-progress generation |
| `clearGenerationResult()` | Clear results |
| `setValidationIssues(issues)` | Set validation issues |

**Key Actions (Refinement):**
| Action | Description |
|--------|-------------|
| `toggleIssueSelection(id)` | Toggle issue for refinement |
| `selectAllIssues()` | Select all issues |
| `clearSelectedIssues()` | Clear selection |
| `startRefinement()` | Begin AI refinement |
| `cancelRefinement()` | Abort refinement |
| `applyRefinementResult()` | Apply refined content |

**Key Actions (Save):**
| Action | Description |
|--------|-------------|
| `saveToDatabase(setId, testId)` | Save generated content |
| `setSelectedSet(id, code)` | Set target set |
| `setSelectedTest(id)` | Set target test |

**Question Type Constants:**
```javascript
export const READING_PART_TYPES = {
  1: ['TRUE_FALSE_NOT_GIVEN', 'FILL_IN_BLANK', 'MATCHING_HEADINGS', ...],
  2: ['MATCHING_INFORMATION', 'MATCHING_FEATURES', ...],
  3: ['MULTIPLE_CHOICE', 'YES_NO_NOT_GIVEN', ...]
};

export const LISTENING_PART_TYPES = {
  1: ['FILL_IN_BLANK', 'MULTIPLE_CHOICE', 'MATCHING'],
  2: ['FILL_IN_BLANK', 'MATCHING', 'MULTIPLE_CHOICE'],
  3: ['MULTIPLE_CHOICE', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', ...],
  4: ['FILL_IN_BLANK', 'MULTIPLE_CHOICE', ...]
};

export const QUESTION_COUNTS = {
  READING: { 1: 13, 2: 13, 3: 14 },
  LISTENING: { 1: 10, 2: 10, 3: 10, 4: 10 }
};
```

**Middleware:** `devtools`

---

## Store Naming Conventions

| Convention | Example |
|------------|---------|
| Hook prefix | `useAuthStore`, `useTestStore` |
| Action verbs | `fetchX`, `setX`, `clearX`, `updateX`, `resetX` |
| Boolean actions | `openModal()` / `closeModal()`, `startTimer()` / `stopTimer()` |
| Async actions | Return `Promise`, use try/catch |
| Selectors (exported) | `selectUser`, `selectLoading`, `selectIsAuthenticated` |
| Selector factories | `selectHasFeature(code)` returns selector function |

---

## Middleware Summary

| Store | `devtools` | `persist` | `immer` | `subscribeWithSelector` |
|-------|------------|-----------|---------|------------------------|
| useAuthStore | ✅ | ❌ | ❌ | ✅ |
| useProfileStore | ✅ | ❌ | ❌ | ❌ |
| useTestStore | ✅ | ❌ | ✅ | ❌ |
| useTestSessionStore | ✅ | ❌ | ❌ | ❌ |
| useDashboardStore | ✅ | ✅ (sessionStorage) | ❌ | ❌ |
| useCourseStore | ✅ | ❌ | ❌ | ❌ |
| useVocabularyStore | ✅ | ❌ | ❌ | ❌ |
| useUserStatsStore | ✅ | ❌ | ❌ | ✅ |
| useSubscriptionStore | ✅ | ❌ | ❌ | ✅ |
| useQuotaStore | ✅ | ❌ | ❌ | ✅ |
| useABTSStore | ✅ | ❌ | ❌ | ❌ |

---

## Best Practices

### 1. Use Selectors for Performance
```javascript
// ✅ Good: Granular subscription
const user = useAuthStore(state => state.user);
const loading = useAuthStore(selectLoading);

// ❌ Bad: Subscribes to entire store
const store = useAuthStore();
```

### 2. Use Action References Outside React
```javascript
// Inside component
const signOut = useAuthStore(state => state.signOut);

// Outside component (e.g., API interceptor)
import { authActions } from '../stores/useAuthStore';
authActions.signOut();
```

### 3. Combine Related Selectors
```javascript
// Create compound selector
export const selectModalState = (state) => ({
  isConfirmModalOpen: state.isConfirmModalOpen,
  isResumeModalOpen: state.isResumeModalOpen,
});

// Use in component
const { isConfirmModalOpen, isResumeModalOpen } = useTestStore(selectModalState);
```

### 4. Handle Cache Invalidation
```javascript
// Force refresh after mutation
await updateProfile(newData);
useDashboardStore.getState().invalidateCache();
```

---

## DevTools

All stores have DevTools enabled in development mode:

```javascript
devtools(
  (set, get) => ({ /* store */ }),
  {
    name: 'auth-store',
    enabled: import.meta.env.DEV,
  }
)
```

Install [Redux DevTools Extension](https://github.com/zalmoxisus/redux-devtools-extension) to inspect state changes, time-travel, and debug actions.

---

## Migration Notes

The stores replaced the deprecated `AuthContext.jsx` which was removed from the codebase in January 2026.
