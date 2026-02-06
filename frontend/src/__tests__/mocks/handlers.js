/**
 * MSW (Mock Service Worker) Request Handlers
 * 
 * These handlers intercept API requests during testing and return mock responses.
 * Use these for integration testing without hitting real backend/Supabase.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */

import { http, HttpResponse } from 'msw';

// Base URLs
const API_URL = 'http://localhost:8080/api';
const SUPABASE_URL = 'https://mock-supabase.supabase.co';

// ============================================================================
// MOCK DATA
// ============================================================================

const mockUser = {
  id: 'user-uuid-123',
  email: 'test@example.com',
  app_metadata: {},
  user_metadata: { username: 'testuser' },
  aud: 'authenticated',
  role: 'authenticated',
};

const mockSession = {
  access_token: 'mock-access-token',
  refresh_token: 'mock-refresh-token',
  expires_in: 3600,
  token_type: 'bearer',
  user: mockUser,
};

const mockProfile = {
  id: mockUser.id,
  username: 'testuser',
  email: 'test@example.com',
  avatarUrl: null,
  bio: 'Test user bio',
  targetBand: 7.0,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-11T00:00:00Z',
};

const mockSubscriptionTiers = [
  {
    id: 1,
    code: 'cramerie',
    name: 'Cramerie',
    priceVnd: 0,
    monthlyAttemptAiLimit: 3,
    dailyChatLimit: 20,
    initialLua: 50,
    features: ['3 AI gradings/month', '20 chat messages/day'],
  },
  {
    id: 2,
    code: 'cramerich',
    name: 'Cramerich',
    priceVnd: 79000,
    monthlyAttemptAiLimit: 20,
    dailyChatLimit: 100,
    initialLua: 100,
    features: ['20 AI gradings/month', '100 chat messages/day'],
  },
];

const mockUserCredits = {
  userId: mockUser.id,
  balance: 150,
  lifetimeEarned: 200,
  lifetimeSpent: 50,
};

const mockTestAttempt = {
  id: 'attempt-uuid-123',
  userId: mockUser.id,
  testId: 1,
  skill: 'READING',
  status: 'IN_PROGRESS',
  timeLeft: 3600,
  currentPart: 0,
  answers: {},
  createdAt: '2026-01-11T10:00:00Z',
};

// ============================================================================
// HANDLERS
// ============================================================================

export const handlers = [
  // ========== AUTH HANDLERS ==========
  
  // Get current session
  http.get(`${SUPABASE_URL}/auth/v1/session`, () => {
    return HttpResponse.json({ session: mockSession });
  }),

  // Sign in
  http.post(`${SUPABASE_URL}/auth/v1/token`, async ({ request }) => {
    const body = await request.json();
    
    if (body.email === 'test@example.com' && body.password === 'password123') {
      return HttpResponse.json({
        access_token: mockSession.access_token,
        refresh_token: mockSession.refresh_token,
        user: mockUser,
      });
    }
    
    return new HttpResponse(
      JSON.stringify({ error: 'Invalid credentials' }),
      { status: 401 }
    );
  }),

  // Sign out
  http.post(`${SUPABASE_URL}/auth/v1/logout`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // ========== PROFILE HANDLERS ==========
  
  // Get profile
  http.get(`${API_URL}/profile`, () => {
    return HttpResponse.json(mockProfile);
  }),

  // Update profile
  http.put(`${API_URL}/profile`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ ...mockProfile, ...body });
  }),

  // ========== SUBSCRIPTION HANDLERS ==========
  
  // Get all tiers
  http.get(`${API_URL}/subscriptions/tiers`, () => {
    return HttpResponse.json(mockSubscriptionTiers);
  }),

  // Get current subscription
  http.get(`${API_URL}/subscriptions/current`, () => {
    return HttpResponse.json({
      userId: mockUser.id,
      tier: mockSubscriptionTiers[0],
      status: 'ACTIVE',
      attemptAisUsed: 1,
      expiresAt: '2026-02-11T00:00:00Z',
    });
  }),

  // Check grading status
  http.get(`${API_URL}/subscriptions/grading-status`, () => {
    return HttpResponse.json({
      allowed: true,
      monthlyUsed: 1,
      monthlyLimit: 3,
      remaining: 2,
      canUseLua: true,
      luaBalance: 150,
      tierCode: 'cramerie',
    });
  }),

  // ========== CREDITS HANDLERS ==========
  
  // Get balance
  http.get(`${API_URL}/credits`, () => {
    return HttpResponse.json(mockUserCredits);
  }),

  // Get stats
  http.get(`${API_URL}/credits/stats`, () => {
    return HttpResponse.json({
      userId: mockUser.id,
      currentTier: mockSubscriptionTiers[0],
      attemptAisRemaining: 2,
      dailyChatRemaining: 15,
      isSubscriptionActive: true,
      luaBalance: 150,
      lifetimeEarned: 200,
      lifetimeSpent: 50,
      currentStreak: 0,
      longestStreak: 0,
      totalVocabulary: 50,
      masteredVocabulary: 10,
    });
  }),

  // Get transaction history
  http.get(`${API_URL}/credits/transactions`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 'tx-1',
          amount: 50,
          balanceAfter: 150,
          type: 'EARN',
          category: 'DAILY_LOGIN',
          description: 'Daily login bonus',
          createdAt: '2026-01-11T09:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 10,
    });
  }),

  // ========== PAYMENT HANDLERS ==========
  
  // Create subscription payment
  http.post(`${API_URL}/payments/subscription`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      orderCode: 1234567890123,
      checkoutUrl: 'https://pay.payos.vn/mock/1234567890123',
      status: 'PENDING',
      tierCode: body.tierCode,
      amountVnd: 79000,
    });
  }),

  // Create Lua pack payment
  http.post(`${API_URL}/payments/lua`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      orderCode: 9876543210123,
      checkoutUrl: 'https://pay.payos.vn/mock/9876543210123',
      status: 'PENDING',
      luaAmount: body.luaAmount,
      amountVnd: body.priceVnd,
    });
  }),

  // Get Lua packs
  http.get(`${API_URL}/payments/lua-packs`, () => {
    return HttpResponse.json([
      { amount: 100, priceVnd: 10000, name: 'Túi Lúa' },
      { amount: 500, priceVnd: 45000, name: 'Bao Lúa' },
      { amount: 2000, priceVnd: 150000, name: 'Xe Lúa' },
    ]);
  }),

  // ========== TEST ATTEMPT HANDLERS ==========
  
  // Start test attempt
  http.post(`${API_URL}/test-attempts/start`, () => {
    return HttpResponse.json(mockTestAttempt);
  }),

  // Get attempt
  http.get(`${API_URL}/test-attempts/:id`, ({ params }) => {
    return HttpResponse.json({
      ...mockTestAttempt,
      id: params.id,
    });
  }),

  // Save progress
  http.post(`${API_URL}/test-attempts/:id/progress`, async ({ params, request }) => {
    const body = await request.json();
    return HttpResponse.json({
      ...mockTestAttempt,
      id: params.id,
      ...body,
    });
  }),

  // Submit test
  http.post(`${API_URL}/test-attempts/:id/submit`, ({ params }) => {
    return HttpResponse.json({
      ...mockTestAttempt,
      id: params.id,
      status: 'COMPLETED',
      score: 7.5,
      correctCount: 30,
      totalQuestions: 40,
    });
  }),

  // Cancel test
  http.post(`${API_URL}/test-attempts/:id/cancel`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // ========== TEST DATA HANDLERS ==========
  
  // Get tests
  http.get(`${API_URL}/tests`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          name: 'Cambridge 17 Test 1',
          skill: 'READING',
          testSetId: 1,
        },
        {
          id: 2,
          name: 'Cambridge 17 Test 2',
          skill: 'LISTENING',
          testSetId: 1,
        },
      ],
      totalElements: 2,
      totalPages: 1,
    });
  }),

  // Get test sections
  http.get(`${API_URL}/tests/:testId/sections`, ({ params }) => {
    return HttpResponse.json([
      {
        id: 1,
        testId: params.testId,
        partNumber: 1,
        passageTitle: 'Test Passage',
        passageContent: '<p>Test content...</p>',
        questions: [
          {
            id: 'q1',
            questionNumber: 1,
            questionType: 'MULTIPLE_CHOICE',
            questionText: 'What is the main idea?',
            options: ['A', 'B', 'C', 'D'],
            correctAnswer: 'A',
          },
        ],
      },
    ]);
  }),

  // ========== DASHBOARD HANDLERS ==========
  
  // Get dashboard data
  http.get(`${API_URL}/dashboard`, () => {
    return HttpResponse.json({
      recentAttempts: [],
      skillProgress: {
        reading: { attempts: 5, averageBand: 6.5 },
        listening: { attempts: 3, averageBand: 7.0 },
        writing: { attempts: 2, averageBand: 6.0 },
        speaking: { attempts: 0, averageBand: null },
      },
      totalTestsTaken: 10,
      currentStreak: 5,
    });
  }),
];

// ============================================================================
// EXPORTS
// ============================================================================

export { mockUser, mockSession, mockProfile, mockSubscriptionTiers, mockUserCredits };
