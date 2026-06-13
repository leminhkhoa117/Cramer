import { get, post, put, del } from './client';

/* ── chat ──────────────────────────────────────────────────────────────── */
export const chatApi = {
  send: (message) => post('/chat', { message }),
  history: (limit = 50) => get('/chat/history', { params: { limit } }),
  remaining: () => get('/chat/remaining'),
  clear: () => del('/chat/history'),
};

/* ── dashboard ─────────────────────────────────────────────────────────── */
export const dashboardApi = {
  summary: (params) => get('/dashboard/summary', { params }),
  courseHistory: (params) => get('/dashboard/course-history', { params }),
  getTarget: () => get('/dashboard/target'),
  setTarget: (body) => post('/dashboard/target', body),
};

/* ── vocabulary ────────────────────────────────────────────────────────── */
export const vocabularyApi = {
  list: (params) => get('/vocabulary', { params }),     // PageResponse<VocabularyView>
  stats: () => get('/vocabulary/stats'),
  translate: (word) => post('/vocabulary/translate', { word }),
  getOne: (id) => get(`/vocabulary/${id}`),
  create: (body) => post('/vocabulary', body),
  update: (id, body) => put(`/vocabulary/${id}`, body),
  remove: (id) => del(`/vocabulary/${id}`),
  toggleMastered: (id) => put(`/vocabulary/${id}/toggle-mastered`),
};
