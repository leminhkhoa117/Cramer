# Cramer Frontend API Client

This document describes the API client architecture used in the Cramer IELTS platform frontend.

---

## Overview

The Cramer frontend uses **Axios** for HTTP communication with the Spring Boot backend. The API client provides:

- **Centralized configuration** with base URL and timeout
- **Automatic JWT token injection** via request interceptor
- **Error handling** with response interceptor
- **Modular API exports** organized by domain

---

## File Structure

```
frontend/src/api/
├── backendApi.js      # Main API client + all API modules
├── supabaseClient.js  # Supabase client + auth helpers
├── config.js          # Legacy API config (deprecated)
└── configApi.js       # API URL constant export
```

---

## Core Configuration

### backendApi.js

**Location:** [backendApi.js](../../../frontend/src/api/backendApi.js)

#### Base URL Configuration
```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
```

**Environment Variable:** `VITE_API_BASE_URL`

#### Axios Instance
```javascript
let apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds (increased for submit operations)
});
```

---

## Authentication Header Handling

### Token Provider Pattern

The API client uses a **token provider function** to get the current JWT without direct coupling to auth state.

```javascript
// Module-scoped token provider
let getAuthToken = () => null;

/**
 * Sets up the token provider function.
 * Called from useAuthStore when session changes.
 * @param {() => string | null} provider - Function returning access token
 */
export const setupApiClient = (provider) => {
  getAuthToken = provider;
};
```

### Request Interceptor

The interceptor attaches the JWT to every outgoing request:

```javascript
apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('❌ Request interceptor error:', error);
    return Promise.reject(error);
  }
);
```

### Integration with Auth Store

In `useAuthStore.js`, the API client is configured when session changes:

```javascript
// Inside initializeAuth()
const unsubscribeApiClient = useAuthStore.subscribe(
  (state) => state.session?.access_token,
  (accessToken) => {
    if (accessToken) {
      console.log('🔑 Setting up API client with new token');
      setupApiClient(() => accessToken);
    }
  },
  { fireImmediately: true }
);
```

---

## Response Interceptor

Handles logging and 401 (Unauthorized) responses:

```javascript
apiClient.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', 
      response.config.method?.toUpperCase(), 
      response.config.url, 
      response.status
    );
    return response;
  },
  async (error) => {
    const method = error.config?.method?.toUpperCase() || 'UNKNOWN_METHOD';
    const url = error.config?.url || 'UNKNOWN_URL';
    console.error(`❌ API Error: ${method} ${url}`);
    console.error('Error details:', {
      message: error.message,
      code: error.code,
      status: error.response?.status,
      data: error.response?.data
    });

    if (error.response?.status === 401) {
      console.error('🔒 Unauthorized. Please log in again.');
      // Could dispatch logout action here
    }

    return Promise.reject(error);
  }
);
```

---

## API Modules

All API modules are exported from `backendApi.js`. Each module is an object with methods for specific endpoints.

### Summary Table

| Module | Endpoints | Description |
|--------|-----------|-------------|
| `authApi` | 1 | Email check |
| `courseApi` | 4 | Course CRUD + tests |
| `profileApi` | 10 | User profiles |
| `sectionApi` | 10 | Test sections |
| `testApi` | 1 | Full test data |
| `testAttemptApi` | 8 | Attempt lifecycle |
| `questionApi` | 12 | Questions CRUD |
| `userAnswerApi` | 13 | User answers |
| `dashboardApi` | 2 | Dashboard summary |
| `writingApi` | 7 | Writing submissions + AI grading |
| `vocabularyApi` | 8 | Vocabulary CRUD + AI translate |
| `subscriptionApi` | 5 | Subscription management |
| `creditsApi` | 3 | Lúa balance |
| `chatApi` | 3 | AI assistant |
| `paymentApi` | 5 | PayOS payments |
| `quotaApi` | 2 | Dual-quota billing |

**Total:** 16 modules, ~90 endpoints

---

## API Module Details

### 1. `authApi` — Authentication

```javascript
export const authApi = {
  checkEmail: (email) => apiClient.post('/auth/check-email', { email }),
};
```

---

### 2. `courseApi` — Courses

```javascript
export const courseApi = {
  getAll: (page = 0, size = 6, search = '') => 
    apiClient.get('/courses', { params: { page, size, search } }),
  
  getAllV2: () => 
    apiClient.get('/courses/v2'), // Full TestSetDTO objects
  
  getTestsByCourse: (courseName) => 
    apiClient.get(`/courses/${courseName}/tests`),
  
  getDetails: (courseCode) => 
    apiClient.get(`/courses/${courseCode}/details`),
};
```

---

### 3. `profileApi` — User Profiles

```javascript
export const profileApi = {
  getAll: () => apiClient.get('/profiles'),
  getById: (id) => apiClient.get(`/profiles/${id}`),
  getByUsername: (username) => apiClient.get(`/profiles/username/${username}`),
  create: (profile) => apiClient.post('/profiles', profile),
  update: (id, profile) => apiClient.put(`/profiles/${id}`, profile),
  delete: (id) => apiClient.delete(`/profiles/${id}`),
  checkUsername: (username) => apiClient.get(`/profiles/check-username/${username}`),
  getCount: () => apiClient.get('/profiles/count'),
  getProfile: () => apiClient.get('/profile'),       // Current user
  updateProfile: (profileData) => apiClient.put('/profile', profileData),
};
```

---

### 4. `sectionApi` — Test Sections

```javascript
export const sectionApi = {
  getAll: () => apiClient.get('/sections'),
  getById: (id) => apiClient.get(`/sections/${id}`),
  getByExam: (examSource) => apiClient.get(`/sections/exam/${examSource}`),
  getByExamAndTest: (examSource, testNumber) =>
    apiClient.get(`/sections/exam/${examSource}/test/${testNumber}`),
  getBySkill: (skill) => apiClient.get(`/sections/skill/${skill}`),
  getSpecific: (params) => apiClient.get('/sections/specific', { params }),
  getSectionsForTest: (examSource, testNumber, skill) =>
    apiClient.get(`/sections/exam/${examSource}/test/${testNumber}/skill/${skill}`),
  create: (section) => apiClient.post('/sections', section),
  update: (id, section) => apiClient.put(`/sections/${id}`, section),
  delete: (id) => apiClient.delete(`/sections/${id}`),
  getCount: () => apiClient.get('/sections/count'),
  getCountByExam: (examSource) => apiClient.get(`/sections/count/exam/${examSource}`),
};
```

---

### 5. `testApi` — Test Data

```javascript
export const testApi = {
  /**
   * Fetches full test data including all passages and questions.
   * @param {string} source - Exam source (e.g., "cam17")
   * @param {number} testNum - Test number (e.g., 1)
   * @param {string} skill - Skill type (e.g., "reading")
   * @returns {Promise<object>} Full test data
   */
  getFullTest: async (source, testNum, skill) => {
    try {
      const response = await apiClient.get('/tests/data', {
        params: { source, test: testNum, skill },
      });
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch test for ${source} T${testNum} ${skill}:`, error);
      throw error;
    }
  },
};
```

---

### 6. `testAttemptApi` — Test Attempt Lifecycle

```javascript
export const testAttemptApi = {
  startAttempt: (source, testNum, skill, forceNew = false) =>
    apiClient.post('/test-attempts/start', null, {
      params: { source, test: testNum, skill, forceNew },
    }),
  
  submitAttempt: (attemptId, answers) =>
    apiClient.post(`/test-attempts/${attemptId}/submit`, { answers }),
  
  saveProgress: (attemptId, { timeLeft, currentPart, answers }) =>
    apiClient.post(`/test-attempts/${attemptId}/progress`, { 
      timeLeft, currentPart, answers 
    }),
  
  getTestReview: (attemptId) =>
    apiClient.get(`/test-attempts/${attemptId}/review`),
  
  cancelAttempt: (attemptId) =>
    apiClient.post(`/test-attempts/${attemptId}/cancel`),
  
  resumeAttempt: (attemptId) =>
    apiClient.post(`/test-attempts/${attemptId}/resume`),
  
  getAttemptAnswers: (attemptId) =>
    apiClient.get(`/test-attempts/${attemptId}/answers`),
  
  deleteAttempt: (attemptId) =>
    apiClient.delete(`/test-attempts/${attemptId}`),
  
  regradeAttempt: (attemptId) =>
    apiClient.post(`/test-attempts/${attemptId}/regrade`),
};
```

**Flow:**
1. `startAttempt()` → Returns existing IN_PROGRESS or creates new
2. `saveProgress()` → Auto-save during test
3. `submitAttempt()` → Final submission + scoring
4. `cancelAttempt()` → Delete attempt and answers

---

### 7. `questionApi` — Questions CRUD

```javascript
export const questionApi = {
  getAll: () => apiClient.get('/questions'),
  getById: (id) => apiClient.get(`/questions/${id}`),
  getBySection: (sectionId) => apiClient.get(`/questions/section/${sectionId}`),
  getByUid: (questionUid) => apiClient.get(`/questions/uid/${questionUid}`),
  getByType: (questionType) => apiClient.get(`/questions/type/${questionType}`),
  getBySectionAndType: (sectionId, questionType) =>
    apiClient.get(`/questions/section/${sectionId}/type/${questionType}`),
  getTypes: () => apiClient.get('/questions/types'),
  create: (question) => apiClient.post('/questions', question),
  update: (id, question) => apiClient.put(`/questions/${id}`, question),
  delete: (id) => apiClient.delete(`/questions/${id}`),
  getCount: () => apiClient.get('/questions/count'),
  getCountBySection: (sectionId) => apiClient.get(`/questions/count/section/${sectionId}`),
};
```

---

### 8. `userAnswerApi` — User Answers

```javascript
export const userAnswerApi = {
  getAll: () => apiClient.get('/user-answers'),
  getById: (id) => apiClient.get(`/user-answers/${id}`),
  getByUser: (userId) => apiClient.get(`/user-answers/user/${userId}`),
  getByQuestion: (questionId) => apiClient.get(`/user-answers/question/${questionId}`),
  getByUserAndQuestion: (userId, questionId) =>
    apiClient.get(`/user-answers/user/${userId}/question/${questionId}`),
  getCorrectAnswers: (userId) => apiClient.get(`/user-answers/user/${userId}/correct`),
  getIncorrectAnswers: (userId) => apiClient.get(`/user-answers/user/${userId}/incorrect`),
  getRecentAnswers: (userId, limit = 10) =>
    apiClient.get(`/user-answers/user/${userId}/recent`, { params: { limit } }),
  getUserStats: (userId) => apiClient.get(`/user-answers/user/${userId}/stats`),
  getUserAccuracy: (userId) => apiClient.get(`/user-answers/user/${userId}/accuracy`),
  submitAnswer: (userAnswer) => apiClient.post('/user-answers', userAnswer),
  update: (id, userAnswer) => apiClient.put(`/user-answers/${id}`, userAnswer),
  delete: (id) => apiClient.delete(`/user-answers/${id}`),
  deleteAllByUser: (userId) => apiClient.delete(`/user-answers/user/${userId}`),
};
```

---

### 9. `dashboardApi` — Dashboard

```javascript
export const dashboardApi = {
  // userId extracted from JWT on backend (security fix)
  getSummary: (page = 0, size = 3, search = '') =>
    apiClient.get('/dashboard/summary', {
      params: { page, size, search }
    }),
  
  saveTarget: (targetData) => 
    apiClient.post('/dashboard/target', targetData),
};
```

---

### 10. `writingApi` — Writing + AI Grading

```javascript
export const writingApi = {
  // Save essay draft during test
  saveDraft: (attemptId, taskNumber, essayText) =>
    apiClient.post(`/writing/draft/${attemptId}?taskNumber=${taskNumber}`, essayText, {
      headers: { 'Content-Type': 'text/plain' }
    }),

  // Submit essays for AI grading
  submitForGrading: (attemptId, essays) =>
    apiClient.post(`/writing/submit/${attemptId}`, { essays }),

  // Get grading status
  getGradingStatus: (attemptId) =>
    apiClient.get(`/writing/status/${attemptId}`),

  // Get full writing review with AI feedback
  getWritingReview: (attemptId) =>
    apiClient.get(`/writing/review/${attemptId}`),

  // Get submissions for an attempt
  getSubmissions: (attemptId) =>
    apiClient.get(`/writing/submissions/${attemptId}`),

  // Validate LLM API key
  validateApiKey: (apiKey) =>
    apiClient.post('/writing/validate-api-key', { apiKey }),

  // Re-grade a completed writing attempt
  regradeAttempt: (attemptId) =>
    apiClient.post(`/writing/regrade/${attemptId}`),
};
```

**Note:** `saveDraft` uses `Content-Type: text/plain` for raw essay text.

---

### 11. `vocabularyApi` — Vocabulary

```javascript
export const vocabularyApi = {
  // Paginated list with filter
  getAll: (page = 0, size = 20, search = '', filter = 'all') =>
    apiClient.get('/vocabulary', { params: { page, size, search, filter } }),

  getById: (id) => apiClient.get(`/vocabulary/${id}`),
  
  create: (data) => apiClient.post('/vocabulary', data),
  
  update: (id, data) => apiClient.put(`/vocabulary/${id}`, data),
  
  delete: (id) => apiClient.delete(`/vocabulary/${id}`),

  // AI translation
  translate: (word, context = null) =>
    apiClient.post('/vocabulary/translate', { word, context }),

  toggleMastered: (id) => apiClient.put(`/vocabulary/${id}/toggle-mastered`),

  getStats: () => apiClient.get('/vocabulary/stats'),
};
```

**Filter Values:** `'all'`, `'mastered'`, `'unmastered'`

---

### 12. `subscriptionApi` — Subscription Management

```javascript
export const subscriptionApi = {
  // Get all available tiers
  getTiers: () => apiClient.get('/subscriptions/tiers'),

  // Get current user's subscription
  getCurrent: () => apiClient.get('/subscriptions/current'),

  // Get AI grading status (remaining this month)
  getGradingStatus: () => apiClient.get('/subscriptions/grading-status'),

  // Comprehensive status (tier, usage, credits, payments)
  getMyStatus: () => apiClient.get('/subscriptions/my-status'),

  // Toggle AI grading preference
  setAiGradingEnabled: (enabled) =>
    apiClient.put('/subscriptions/ai-grading', { enabled }),
};
```

---

### 13. `creditsApi` — Lúa Credits

```javascript
export const creditsApi = {
  // Current balance
  getBalance: () => apiClient.get('/credits'),

  // Lifetime stats
  getStats: () => apiClient.get('/credits/stats'),

  // Transaction history (paginated)
  getTransactions: (page = 0, size = 20) =>
    apiClient.get('/credits/transactions', { params: { page, size } }),
};
```

---

### 14. `chatApi` — AI Assistant

```javascript
export const chatApi = {
  // Send message
  sendMessage: (message) => apiClient.post('/chat', { message }),

  // Get history (paginated)
  getHistory: (page = 0, size = 50) =>
    apiClient.get('/chat/history', { params: { page, size } }),

  // Remaining questions this month
  getRemainingQuestions: () => apiClient.get('/chat/remaining'),
};
```

---

### 15. `paymentApi` — PayOS Integration

```javascript
export const paymentApi = {
  // Create subscription payment link
  createSubscriptionPayment: (tierId, tierCode = null) =>
    apiClient.post('/payments/subscription', {
      type: 'SUBSCRIPTION',
      tierId: tierId,
      tierCode: tierCode
    }),

  // Create Lúa pack payment link
  createLuaPackPayment: (luaAmount, priceVnd) =>
    apiClient.post('/payments/lua', {
      type: 'LUA_PACK',
      luaAmount: luaAmount,
      priceVnd: priceVnd
    }),

  // Check status by order code
  getStatus: (orderCode) => apiClient.get(`/payments/status/${orderCode}`),

  // User payment history
  getHistory: (page = 0, size = 20) =>
    apiClient.get('/payments/history', { params: { page, size } }),

  // Available Lúa packs (public)
  getLuaPacks: () => apiClient.get('/payments/lua-packs'),

  // Check if PayOS configured (public)
  getConfigStatus: () => apiClient.get('/payments/config-status'),
};
```

---

### 16. `quotaApi` — Dual-Quota Billing

```javascript
export const quotaApi = {
  // Get current quota status
  getStatus: () => apiClient.get('/quotas'),

  // Pre-check if attempt allowed
  canAttempt: (skill, isAI = false) =>
    apiClient.get('/quotas/can-attempt', { params: { skill, ai: isAI } }),
};
```

---

## Supabase Client

**Location:** [supabaseClient.js](../../../frontend/src/api/supabaseClient.js)

### Configuration

```javascript
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseAnonKey);
```

**Environment Variables:**
- `VITE_SUPABASE_URL` — Supabase project URL
- `VITE_SUPABASE_ANON_KEY` — Supabase anon (public) key

### Storage Fallback

Handles browsers with blocked localStorage (privacy mode):

```javascript
const supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    storage: isLocalStorageAvailable() ? undefined : {
      getItem: (key) => window.__supabaseMemoryStorage?.[key] || null,
      setItem: (key, value) => {
        if (!window.__supabaseMemoryStorage) {
          window.__supabaseMemoryStorage = {};
        }
        window.__supabaseMemoryStorage[key] = value;
      },
      removeItem: (key) => {
        if (window.__supabaseMemoryStorage) {
          delete window.__supabaseMemoryStorage[key];
        }
      },
    },
    autoRefreshToken: true,
    persistSession: isLocalStorageAvailable(),
    detectSessionInUrl: true,
  },
});
```

### Auth Helpers

Exported `authHelpers` object with all auth operations:

```javascript
export const authHelpers = {
  // Registration & Email Verification
  signUp: async (email, password) => { /* ... */ },
  verifyOtp: async (email, token) => { /* ... */ },
  resendOtp: async (email) => { /* ... */ },

  // Password Reset (OTP-based)
  requestPasswordReset: async (email) => { /* ... */ },
  verifyRecoveryOtp: async (email, token) => { /* ... */ },
  updatePassword: async (newPassword) => { /* ... */ },

  // Session Management
  signIn: async (email, password) => { /* ... */ },
  signOut: async () => { /* ... */ },
  getSession: async () => { /* ... */ },
  getUser: async () => { /* ... */ },

  // State Change Listener
  onAuthStateChange: (callback) => { /* ... */ },
};
```

---

## Error Handling Patterns

### API Response Structure

Backend returns consistent error structure:

```javascript
// Success
{
  data: { /* response body */ }
}

// Error (handled by interceptor)
{
  response: {
    status: 400,
    data: {
      message: "Error message",
      // Additional error details
    }
  }
}
```

### Component Error Handling

```javascript
try {
  const response = await testAttemptApi.submitAttempt(attemptId, answers);
  // response.data contains result
} catch (error) {
  const message = error.response?.data?.message || error.message || 'Unknown error';
  showErrorToast(message);
}
```

### Store Error Handling Pattern

```javascript
// In Zustand store
submitAttempt: async (attemptId, answers) => {
  try {
    const response = await testAttemptApi.submitAttempt(attemptId, answers);
    set({ attemptStatus: 'idle' }, false, 'submitAttempt/fulfilled');
    return response.data;
  } catch (error) {
    console.error('Failed to submit attempt:', error);
    throw error; // Re-throw for component to handle
  }
},
```

---

## Custom Hooks

**Location:** `frontend/src/hooks/`

### `useFeatureAccess`

**File:** [useFeatureAccess.js](../../../frontend/src/hooks/useFeatureAccess.js)

Combines subscription and feature access with auto-fetch:

```javascript
import { useFeatureAccess } from '../hooks/useFeatureAccess';

function MyComponent() {
  const { 
    tier,              // 'cramerie' | 'cramerich' | 'cramerous'
    tierNameVi,        // Vietnamese tier name
    isPremium,         // boolean
    hasFeature,        // (code) => boolean
    hasAllFeatures,    // (codes[]) => boolean
    hasAnyFeature,     // (codes[]) => boolean
    loading,
    error,
    refresh,           // Force refresh
    // Convenience booleans
    isCramerie,
    isCramerich,
    isCramerous,
    canUseAI,
  } = useFeatureAccess();

  if (hasFeature('ai_writing_grading')) {
    // Show AI grading option
  }
}
```

### `useInView`

**File:** [useInView.js](../../../frontend/src/hooks/useInView.js)

IntersectionObserver hook for lazy loading:

```javascript
import { useInView, useSectionInView } from '../hooks/useInView';

function LazyComponent() {
  // Basic usage
  const [ref, isInView] = useInView({ threshold: 0.1, triggerOnce: true });

  // Section with preload buffer
  const [sectionRef, sectionInView] = useSectionInView({ rootMargin: '200px' });

  return (
    <div ref={ref}>
      {isInView && <HeavyContent />}
    </div>
  );
}
```

### `useTextHighlighter`

**File:** [useTextHighlighter.js](../../../frontend/src/hooks/useTextHighlighter.js)

Text selection and highlighting for reading passages:

```javascript
import { useTextHighlighter } from '../hooks/useTextHighlighter';

function ReadingPassage({ containerRef }) {
  const {
    selectedText,
    selectionRange,
    popupPosition,
    applyHighlight,
    removeHighlight,
    hidePopup,
  } = useTextHighlighter(containerRef);

  // Render highlight popup when text selected
}
```

**Features:**
- Tracks text selection within container
- Supports multiple highlight styles
- Persists highlights via `HighlightContext`

---

## Best Practices

### 1. Always Unwrap Axios Response

```javascript
// ✅ Good
const response = await courseApi.getAll();
const courses = response.data.content;

// ❌ Bad (data is nested)
const courses = await courseApi.getAll();
```

### 2. Use Store Actions for API Calls

```javascript
// ✅ Good: Through store (handles caching, state updates)
const { fetchCourses, courses } = useCourseStore();
await fetchCourses(0, 10);

// ⚠️ Direct (only when needed)
const response = await courseApi.getAll(0, 10);
```

### 3. Handle Loading States

```javascript
const { loading, error, fetchSummary } = useDashboardStore();

useEffect(() => {
  fetchSummary();
}, []);

if (loading) return <Spinner />;
if (error) return <ErrorMessage message={error} />;
```

### 4. Cache TTL Awareness

Stores implement caching with TTL:
- **Dashboard:** 5 minutes (sessionStorage persisted)
- **TestSession:** 5 minutes (in-memory)
- **Vocabulary:** 5 minutes (in-memory)
- **UserStats:** 30 seconds (debounce)

Force refresh when needed:
```javascript
useDashboardStore.getState().invalidateCache();
await useDashboardStore.getState().fetchSummary();
```

---

## Environment Variables Summary

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Backend API URL |
| `VITE_SUPABASE_URL` | _(required)_ | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | _(required)_ | Supabase anon key |

---

## Related Documentation

- [STATE_MANAGEMENT.md](STATE_MANAGEMENT.md) — Zustand stores documentation
- [docs/backend/supabase-backend.md](../../backend/supabase-backend.md) — Backend API documentation
- [backend/src/main/resources/application.properties](../../../backend/src/main/resources/application.properties) — Backend configuration
