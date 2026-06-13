import { get, post, put, patch, del } from './client';

/* ── identity ──────────────────────────────────────────────────────────── */
export const authApi = {
  checkEmail: (email) => post('/auth/check-email', { email }),
};

export const profileApi = {
  get: (id) => get(`/profiles/${id}`),
  update: (id, body) => put(`/profiles/${id}`, body),
};

/* ── catalog ───────────────────────────────────────────────────────────── */
export const courseApi = {
  list: (params) => get('/courses', { params }),       // PageResponse<String>
  listV2: () => get('/courses/v2'),                    // List<TestSetView>
  tests: (course) => get(`/courses/${encodeURIComponent(course)}/tests`),
  details: (code) => get(`/courses/${encodeURIComponent(code)}/details`),
};

export const testApi = {
  data: (source, test, skill) => get('/tests/data', { params: { source, test, skill } }),
};

/* ── assessment (attempts) ─────────────────────────────────────────────── */
export const attemptApi = {
  start: (source, test, skill, forceNew = false) =>
    post('/test-attempts/start', null, { params: { source, test, skill, forceNew } }),
  saveProgress: (id, body) => post(`/test-attempts/${id}/progress`, body),
  submit: (id, body) => post(`/test-attempts/${id}/submit`, body),
  cancel: (id) => post(`/test-attempts/${id}/cancel`),
  resume: (id) => post(`/test-attempts/${id}/resume`),
  regrade: (id) => post(`/test-attempts/${id}/regrade`),
  answers: (id) => get(`/test-attempts/${id}/answers`),
  review: (id) => get(`/test-attempts/${id}/review`),
  remove: (id) => del(`/test-attempts/${id}`),
};

/* ── writing ───────────────────────────────────────────────────────────── */
export const writingApi = {
  saveDraft: (attemptId, essayText, taskNumber = 1) =>
    post(`/writing/draft/${attemptId}`, { essayText }, { params: { taskNumber } }),
  submit: (attemptId, essays) => post(`/writing/submit/${attemptId}`, { essays }),
  status: (attemptId) => get(`/writing/status/${attemptId}`),
  review: (attemptId) => get(`/writing/review/${attemptId}`),
  submissions: (attemptId) => get(`/writing/submissions/${attemptId}`),
  regrade: (attemptId) => post(`/writing/regrade/${attemptId}`),
};

/* ── speaking ──────────────────────────────────────────────────────────── */
export const speakingApi = {
  create: (body) => post('/speaking/sessions', body),
  get: (id) => get(`/speaking/sessions/${id}`),
  saveTranscript: (id, body) => post(`/speaking/sessions/${id}/transcripts`, body),
  complete: (id, durationSeconds) => post(`/speaking/sessions/${id}/complete`, null, { params: { durationSeconds } }),
  abandon: (id) => post(`/speaking/sessions/${id}/abandon`),
  gradingStatus: (id) => get(`/speaking/sessions/${id}/grading-status`),
  results: (id) => get(`/speaking/sessions/${id}/results`),
  history: (params) => get('/speaking/history', { params }),
};
