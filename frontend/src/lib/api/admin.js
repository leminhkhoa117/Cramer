import { get, post, patch } from './client';

/* Admin console (SPEC-17). All under /api/admin, admin-gated. JWT only (no X-User-Id). */

export const adminUserApi = {
  list: (params) => get('/admin/users', { params }),
  stats: () => get('/admin/users/stats'),
  detail: (id) => get(`/admin/users/${id}`),
  setStatus: (id, status, reason) => patch(`/admin/users/${id}/status`, { status, reason }),
  adjustCredits: (id, amount, reason) => patch(`/admin/users/${id}/credits`, { amount, reason }),
  setSubscription: (id, tierCode, months) => patch(`/admin/users/${id}/subscription`, { tierCode, months }),
};

export const adminDashboardApi = {
  stats: () => get('/admin/dashboard/stats'),
  activities: (limit = 20) => get('/admin/dashboard/activities', { params: { limit } }),
  status: () => get('/admin/dashboard/status'),
};

export const adminActivityApi = {
  audit: (params) => get('/admin/activities/audit', { params }),
  userAudit: (userId, params) => get(`/admin/activities/audit/users/${userId}`, { params }),
  userActivities: (userId, params) => get(`/admin/activities/users/${userId}`, { params }),
  userActivitiesRecent: (userId, limit = 10) => get(`/admin/activities/users/${userId}/recent`, { params: { limit } }),
};

export const adminFinanceApi = {
  overview: (period = '30d') => get('/admin/finance/overview', { params: { period } }),
  breakdown: (period = '30d') => get('/admin/finance/breakdown', { params: { period } }),
  topSpenders: (limit = 10) => get('/admin/finance/top-spenders', { params: { limit } }),
  transactions: (params) => get('/admin/finance/transactions', { params }),
};

export const adminSpeakingApi = {
  regrade: (id, reason, { mode, force } = {}) =>
    post(`/admin/speaking/sessions/${id}/regrade`, { reason }, { params: { mode, force } }),
};
