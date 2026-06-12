import { get, post, put } from './client';

/* ── subscriptions ─────────────────────────────────────────────────────── */
export const subscriptionApi = {
  tiers: () => get('/subscriptions/tiers'),
  tier: (code) => get(`/subscriptions/tiers/${code}`),
  current: () => get('/subscriptions/current'),
  myStatus: () => get('/subscriptions/my-status'),
  gradingStatus: () => get('/subscriptions/grading-status'),
  gradingsRemaining: () => get('/subscriptions/gradings-remaining'),
  chatLimit: () => get('/subscriptions/chat-limit'),
  setAiGrading: (enabled) => put('/subscriptions/ai-grading', { enabled }),
};

/* ── quotas ────────────────────────────────────────────────────────────── */
export const quotaApi = {
  status: () => get('/quotas'),
  check: () => get('/quotas/check'),
  canAttempt: (skill, ai = false) => get('/quotas/can-attempt', { params: { skill, ai } }),
};

/* ── payments ──────────────────────────────────────────────────────────── */
export const paymentApi = {
  createSubscriptionOrder: (tierId, tierCode) => post('/payments/subscription', { tierId, tierCode }),
  createLuaOrder: (packCode) => post('/payments/lua', { packCode }),
  status: (orderCode) => get(`/payments/status/${orderCode}`),
  history: (params) => get('/payments/history', { params }),
  luaPacks: () => get('/payments/lua-packs'),
  configStatus: () => get('/payments/config-status'),
};

/* ── credits (Lúa) ─────────────────────────────────────────────────────── */
export const creditApi = {
  stats: () => get('/credits/stats'),
  balance: () => get('/credits'),
  check: (amount) => get(`/credits/check/${amount}`),
  transactions: (params) => get('/credits/transactions', { params }),
  history: (params) => get('/credits/history', { params }),
  packages: () => get('/credits/packages'),
  purchase: (packCode) => post('/credits/purchase', { packCode }),
};
