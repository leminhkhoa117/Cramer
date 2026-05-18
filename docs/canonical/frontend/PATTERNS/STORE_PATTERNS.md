# Cramer Store Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)
> **Library:** Zustand

This document describes the Zustand store patterns used across all Cramer frontend. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [Store Export Pattern](#1-store-export-pattern)
2. [Middleware Stack](#2-middleware-stack)
3. [State Shape Convention](#3-state-shape-convention)
4. [Async Action Pattern](#4-async-action-pattern)
5. [Error Handling Strategies](#5-error-handling-strategies)
6. [Loading States](#6-loading-states)
7. [Caching Strategy](#7-caching-strategy)
8. [Debouncing / Deduplication](#8-debouncing--deduplication)
9. [Auto-Subscription Pattern](#9-auto-subscription-pattern)
10. [Selector Pattern](#10-selector-pattern)
11. [Outside-React Access](#11-outside-react-access)
12. [Reset Pattern](#12-reset-pattern)
13. [API Layer](#13-api-layer)

---

## 1. Store Export Pattern

All stores are re-exported from a barrel file:

```js
// stores/index.js
export { default as useAuthStore } from './useAuthStore';
export { default as useTestStore } from './useTestStore';
// ... etc
```

Pages import:

```js
import { useAuthStore, useProfileStore } from '../stores';
```

---

## 2. Middleware Stack

| Middleware | Applied To | Purpose |
|-----------|------------|---------|
| `devtools` | ALL stores | Debugging. Every `set()` includes a type string (3rd arg): `'actionName/phase'` |
| `subscribeWithSelector` | auth, subscription, quota, userStats | Selective subscription to state changes |
| `immer` | testStore only | Allows draft mutation syntax in `set()` |
| `persist` | dashboardStore only | Persists to `sessionStorage` with `partialize` whitelist and versioning |

---

## 3. State Shape Convention

```js
{
  // Data
  items: [],           // collections
  item: null,          // single entity
  stats: null,         // aggregated data

  // Loading
  loading: false,      // primary loading
  isSubmitting: false, // secondary loading (submit, save)
  translating: false,  // tertiary loading (specific operation)

  // Error
  error: null,         // string | null

  // Cache
  lastFetchedAt: null, // ISO string or timestamp

  // Pagination
  currentPage: 0,
  pageSize: 10,
  totalPages: 0,
  totalElements: 0,

  // Search/Filter
  searchQuery: '',
  debouncedSearchQuery: '',
  filter: 'all',
}
```

---

## 4. Async Action Pattern

```js
fetchData: async (params) => {
  set({ loading: true, error: null }, false, 'fetchData/start');
  try {
    const response = await api.getData(params);
    set({ data: response, loading: false }, false, 'fetchData/success');
    return { success: true };
  } catch (err) {
    const message = err.response?.data?.message || err.message;
    set({ error: message, loading: false }, false, 'fetchData/error');
    return { success: false, error: message };
  }
},
```

---

## 5. Error Handling Strategies

Two strategies exist:

| Strategy | Behaviour | Used By |
|----------|-----------|---------|
| **Swallow + return** | Store catches error, sets `error` field, returns `{ success: false, error }` | auth, subscription, quota, userStats |
| **Re-throw** | Store catches error, sets `error` field, then re-throws for caller to handle | testSession, vocabulary, course, dashboard |

---

## 6. Loading States

- Boolean `loading` on most stores
- Some use enums: `attemptStatus: 'idle' | 'loading' | 'ready' | 'error'`
- Some use separate bools for specific operations: `isSubmitting`, `translating`, `preCheckLoading`

---

## 7. Caching Strategy

Manual TTL-based:

```js
const CACHE_TTL_MS = 5 * 60 * 1000;
const isStale = () =>
  !lastFetchedAt || Date.now() - new Date(lastFetchedAt).getTime() > CACHE_TTL_MS;
```

- Stores with caching: dashboard, vocabulary, quota, userStats, testSession, course
- Key-value cache objects: testSession (`testDataCache`), course (`courseTests`, `courseDetails`)

---

## 8. Debouncing / Deduplication

- `fetchQuotaStatus` and `fetchUserStats` skip if already loading or fetched within 30s
- Dashboard uses separate `debouncedSearchQuery` field + manual `setTimeout` in component

---

## 9. Auto-Subscription Pattern

Some stores auto-subscribe to auth changes at module level:

```js
useAuthStore.subscribe((state) => {
  if (!state.user) { store.getState().clearData(); }
  else { store.getState().fetchData(); }
});
```

Used by: profileStore, quotaStore, userStatsStore

---

## 10. Selector Pattern

Selectors are exported as named functions:

```js
export const selectUser = (state) => state.user;
export const selectIsAuthenticated = (state) => !!state.user;
export const selectHasFeature = (featureCode) => (state) => state.features?.[featureCode];
```

Usage: `const user = useAuthStore(selectUser)`

---

## 11. Outside-React Access

Some stores export an actions object for use outside React:

```js
export const authActions = {
  signOut: () => useAuthStore.getState().signOut(),
};
```

---

## 12. Reset Pattern

Define `initialState` outside the store, then spread it in reset:

```js
const initialState = { items: [], loading: false, error: null };
const useStore = create((set) => ({
  ...initialState,
  reset: () => set(initialState),
}));
```

---

## 13. API Layer

All stores use `backendApi.js` modules (`xxxApi` objects) — **except** `useAuthStore` which calls Supabase directly.
