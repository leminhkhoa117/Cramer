/**
 * COMPAT SHIM. The real API client now lives in `src/lib/api/`. This file re-exports it so
 * existing `import { xxxApi } from '../api/backendApi'` keeps resolving during the rewrite.
 * New code should import from `../lib/api`. Deleted in the cleanup phase.
 */
export {
  http,
  setupApiClient,
  setUnauthorizedHandler,
  getApiError,
  authApi,
  profileApi,
  courseApi,
  testApi,
  attemptApi,
  writingApi,
  speakingApi,
  subscriptionApi,
  quotaApi,
  paymentApi,
  creditApi,
  chatApi,
  dashboardApi,
  vocabularyApi,
} from '../lib/api';

// Legacy aliases (old names used by not-yet-migrated call sites).
export { attemptApi as testAttemptApi, creditApi as creditsApi } from '../lib/api';
