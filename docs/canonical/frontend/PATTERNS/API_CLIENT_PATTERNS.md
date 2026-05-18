# Cramer API Client Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)
> **Library:** Axios

This document describes the API client patterns used in the Cramer frontend. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [Axios Instance Setup](#1-axios-instance-setup)
2. [JWT Token Injection](#2-jwt-token-injection)
3. [API Module Organization](#3-api-module-organization)
4. [Error Handling at API Layer](#4-error-handling-at-api-layer)
5. [Existing API Modules](#5-existing-api-modules)
6. [Adding a New API Module](#6-adding-a-new-api-module)

---

## 1. Axios Instance Setup

- Single `apiClient` created with `axios.create()`
- Base URL from `import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'`
- Timeout: 30 seconds

---

## 2. JWT Token Injection

```js
let getAuthToken = () => null;

export const setupApiClient = (provider) => {
  getAuthToken = provider;
};

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

- `setupApiClient` is called from the auth store when session changes
- The provider function reads the current token from Supabase session

---

## 3. API Module Organization

Each domain is a named export object:

```js
export const domainApi = {
  list: (params) => apiClient.get('/domain', { params }),
  get: (id) => apiClient.get(`/domain/${id}`),
  create: (data) => apiClient.post('/domain', data),
  update: (id, data) => apiClient.put(`/domain/${id}`, data),
  delete: (id) => apiClient.delete(`/domain/${id}`),
};
```

---

## 4. Error Handling at API Layer

- Request interceptor: logs errors, re-throws
- Response interceptor: logs method + URL + status on success
- On error: logs full details (message, code, status, data)
- 401 handling: logs "Unauthorized" — currently no auto-redirect
- Errors are re-thrown via `Promise.reject(error)` — consumers handle with try/catch

---

## 5. Existing API Modules

| Module | Base Path | Key Methods |
|--------|-----------|-------------|
| `authApi` | `/auth` | `checkEmail` |
| `courseApi` | `/courses` | `getAll`, `getAllV2`, `getTestsByCourse`, `getDetails` |
| `profileApi` | `/profiles`, `/profile` | `getAll`, `getById`, `create`, `update`, `delete` |
| `sectionApi` | `/sections` | `getAll`, `getById`, `getByExam`, `getBySkill`, `create`, `update`, `delete` |
| `testApi` | `/tests` | `getFullTest` |
| `testAttemptApi` | `/test-attempts` | `startAttempt`, `submitAttempt`, `saveProgress`, `getTestReview` |
| `questionApi` | `/questions` | `getAll`, `getById`, `getBySection`, `create`, `update`, `delete` |
| `userAnswerApi` | `/user-answers` | `submitAnswer`, `getUserStats`, `getUserAccuracy` |
| `dashboardApi` | `/dashboard` | `getSummary`, `saveTarget`, `getCourseHistory` |
| `writingApi` | `/writing` | `saveDraft`, `submitForGrading`, `getGradingStatus`, `getWritingReview` |
| `vocabularyApi` | `/vocabulary` | `getAll`, `create`, `update`, `delete`, `translate`, `toggleMastered`, `getStats` |
| `subscriptionApi` | `/subscriptions` | `getTiers`, `getCurrent`, `getMyStatus` |
| `creditsApi` | `/credits` | `getBalance`, `getStats`, `getTransactions` |
| `chatApi` | `/chat` | `sendMessage`, `getHistory`, `getRemainingQuestions` |
| `paymentApi` | `/payments` | `createSubscriptionPayment`, `createLuaPackPayment`, `getStatus`, `getHistory` |
| `quotaApi` | `/quotas` | `getStatus`, `canAttempt` |

---

## 6. Adding a New API Module

1. Create a new export object in `backendApi.js`
2. Base path should match the Spring Boot controller's `@RequestMapping`
3. Each method returns `apiClient.{method}(path, ...)`
4. Export the object as a named export
5. Import in stores: `import { newApi } from './backendApi'`
