import axios from 'axios';
import {
  showErrorToast,
  showSuccessToast
} from '../utils/toast.js';

// Base URL from environment or default to localhost
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
let apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 second timeout (increased for submit operations)
});

// A function that will hold the latest token provider function.
// It's defined in the module scope.
let getAuthToken = () => null;

/**
 * Sets up the token provider function. This should be called from the AuthContext
 * whenever the session changes.
 * @param {() => string | null} provider A function that returns the access token.
 */
export const setupApiClient = (provider) => {
  getAuthToken = provider;
};

// Add the request interceptor ONCE.
// It will use the `getAuthToken` function to get the latest token.
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

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', response.config.method?.toUpperCase(), response.config.url, response.status);
    return response;
  },
  async (error) => {
    const method = (error.config?.method || 'UNKNOWN_METHOD').toUpperCase();
    const url = error.config?.url || 'UNKNOWN_URL';
    console.error(`❌ API Error: ${method} ${url}`);
    console.error('Error details:', {
      message: error.message,
      code: error.code,
      status: error.response?.status,
      data: error.response?.data
    });

    if (error.response?.status === 401) {
      // Token expired or invalid, redirect to login
      console.error('🔒 Unauthorized. Please log in again.');
      // You can dispatch a logout action here
    }

    return Promise.reject(error);
  }
);

// ============================================
// AUTH APIs
// ============================================
export const authApi = {
  checkEmail: (email) => apiClient.post('/auth/check-email', { email }),
};

// ============================================
// COURSE APIs
// ============================================
export const courseApi = {
  getAll: (page = 0, size = 6, search = '') => apiClient.get('/courses', { params: { page, size, search } }),
  getAllV2: () => apiClient.get('/courses/v2'), // Returns full TestSetDTO objects
  getTestsByCourse: (courseName) => apiClient.get(`/courses/${courseName}/tests`),
  getDetails: (courseCode) => apiClient.get(`/courses/${courseCode}/details`),
};

// ============================================
// PROFILE APIs
// ============================================
export const profileApi = {
  getAll: () => apiClient.get('/profiles'),
  getById: (id) => apiClient.get(`/profiles/${id}`),
  getByUsername: (username) => apiClient.get(`/profiles/username/${username}`),
  create: (profile) => apiClient.post('/profiles', profile),
  update: (id, profile) => apiClient.put(`/profiles/${id}`, profile),
  delete: (id) => apiClient.delete(`/profiles/${id}`),
  checkUsername: (username) => apiClient.get(`/profiles/check-username/${username}`),
  getCount: () => apiClient.get('/profiles/count'),
  getProfile: () => apiClient.get('/profile'),
  updateProfile: (profileData) => apiClient.put('/profile', profileData),
};

// ============================================
// SECTION APIs
// ============================================
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

// ============================================
// TEST APIs
// ============================================
export const testApi = {
  /**
   * Fetches the full data for a test, including all passages and questions.
   * @param {string} source The exam source (e.g., "cam17")
   * @param {number} testNum The test number (e.g., 1)
   * @param {string} skill The skill (e.g., "reading")
   * @returns {Promise<object>} The full test data.
   */
  getFullTest: async (source, testNum, skill) => {
    try {
      const response = await apiClient.get('/tests/data', {
        params: { source, test: testNum, skill },
      });
      return response.data;
    } catch (error) {
      console.error(`Failed to fetch full test for ${source} T${testNum} ${skill}:`, error);
      throw error;
    }
  },
};

// ============================================
// TEST ATTEMPT APIs
// ============================================
export const testAttemptApi = {
  startAttempt: (source, testNum, skill, forceNew = false) => {
    return apiClient.post('/test-attempts/start', null, {
      params: { source, test: testNum, skill, forceNew },
    });
  },
  submitAttempt: (attemptId, answers) => {
    return apiClient.post(`/test-attempts/${attemptId}/submit`, { answers });
  },
  saveProgress: (attemptId, { timeLeft, currentPart, answers }) => {
    return apiClient.post(`/test-attempts/${attemptId}/progress`, { timeLeft, currentPart, answers });
  },
  getTestReview: (attemptId) => {
    return apiClient.get(`/test-attempts/${attemptId}/review`);
  },
  cancelAttempt: (attemptId) => {
    return apiClient.post(`/test-attempts/${attemptId}/cancel`);
  },
  resumeAttempt: (attemptId) => {
    return apiClient.post(`/test-attempts/${attemptId}/resume`);
  },
  getAttemptAnswers: (attemptId) => {
    return apiClient.get(`/test-attempts/${attemptId}/answers`);
  },
  deleteAttempt: (attemptId) => {
    return apiClient.delete(`/test-attempts/${attemptId}`);
  },
  regradeAttempt: (attemptId) => {
    return apiClient.post(`/test-attempts/${attemptId}/regrade`);
  },
};


// ============================================
// QUESTION APIs
// ============================================
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

// ============================================
// USER ANSWER APIs
// ============================================
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

// ============================================
// DASHBOARD APIs
// ============================================
export const dashboardApi = {
  // Note: userId is now extracted from JWT on the backend (security fix)
  getSummary: (page = 0, size = 3, search = '') => {
    return apiClient.get('/dashboard/summary', {
      params: {
        page,
        size,
        search
      }
    });
  },
  saveTarget: (targetData) => apiClient.post('/dashboard/target', targetData),
  getCourseHistory: (examSource, testNumber, skill) =>
    apiClient.get('/dashboard/course-history', { params: { examSource, testNumber, skill } }),
};

// ============================================
// WRITING APIs
// ============================================
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

  // Validate Gemini API key
  validateApiKey: (apiKey) =>
    apiClient.post('/writing/validate-api-key', { apiKey }),

  // Re-grade a completed writing attempt
  regradeAttempt: (attemptId) =>
    apiClient.post(`/writing/regrade/${attemptId}`),
};

// ============================================
// VOCABULARY APIs
// ============================================
export const vocabularyApi = {
  // Get paginated vocabulary list with optional filter
  getAll: (page = 0, size = 20, search = '', filter = 'all') =>
    apiClient.get('/vocabulary', { params: { page, size, search, filter } }),

  // Get single vocabulary entry by ID
  getById: (id) =>
    apiClient.get(`/vocabulary/${id}`),

  // Create new vocabulary entry
  create: (data) =>
    apiClient.post('/vocabulary', data),

  // Update vocabulary entry
  update: (id, data) =>
    apiClient.put(`/vocabulary/${id}`, data),

  // Delete vocabulary entry
  delete: (id) =>
    apiClient.delete(`/vocabulary/${id}`),

  // Translate word using AI
  translate: (word, context = null) =>
    apiClient.post('/vocabulary/translate', { word, context }),

  // Toggle mastered status
  toggleMastered: (id) =>
    apiClient.put(`/vocabulary/${id}/toggle-mastered`),

  // Get vocabulary statistics
  getStats: () =>
    apiClient.get('/vocabulary/stats'),
};

// ============================================
// SUBSCRIPTION APIs
// ============================================
export const subscriptionApi = {
  // Get all available subscription tiers
  getTiers: () =>
    apiClient.get('/subscriptions/tiers'),

  // Get current user's subscription
  getCurrent: () =>
    apiClient.get('/subscriptions/current'),

  // Get AI grading status (remaining gradings this month)
  getGradingStatus: () =>
    apiClient.get('/subscriptions/grading-status'),

  // Get comprehensive subscription status (tier, usage, credits, payments)
  getMyStatus: () =>
    apiClient.get('/subscriptions/my-status'),

  // Toggle AI grading preference (enabled/disabled)
  setAiGradingEnabled: (enabled) =>
    apiClient.put('/subscriptions/ai-grading', { enabled }),
};

// ============================================
// CREDITS (LÚA) APIs
// ============================================
export const creditsApi = {
  // Get user's current credit balance
  getBalance: () =>
    apiClient.get('/credits'),

  // Get credit statistics (lifetime earned/spent)
  getStats: () =>
    apiClient.get('/credits/stats'),

  // Get transaction history (paginated)
  getTransactions: (page = 0, size = 20) =>
    apiClient.get('/credits/transactions', { params: { page, size } }),
};

// ============================================
// CHAT (AI ASSISTANT) APIs
// ============================================
export const chatApi = {
  // Send a message to the AI assistant
  sendMessage: (message) =>
    apiClient.post('/chat', { message }),

  // Get chat history
  getHistory: (page = 0, size = 50) =>
    apiClient.get('/chat/history', { params: { page, size } }),

  // Get remaining questions for today
  getRemainingQuestions: () =>
    apiClient.get('/chat/remaining'),
};

// ============================================
// PAYMENT APIs (PayOS Integration)
// ============================================
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

  // Get payment status by order code
  getStatus: (orderCode) =>
    apiClient.get(`/payments/status/${orderCode}`),

  // Get user's payment history
  getHistory: (page = 0, size = 20) =>
    apiClient.get('/payments/history', { params: { page, size } }),

  // Get available Lúa packs (public endpoint)
  getLuaPacks: () =>
    apiClient.get('/payments/lua-packs'),

  // Check if PayOS is configured (public endpoint)
  getConfigStatus: () =>
    apiClient.get('/payments/config-status'),
};

// ============================================
// QUOTA (Dual-Quota Billing System) APIs
// ============================================
export const quotaApi = {
  // Get current quota status (global and per-skill usage)
  getStatus: () =>
    apiClient.get('/quotas'),

  // Pre-check if an attempt is allowed
  canAttempt: (skill, isAI = false) =>
    apiClient.get('/quotas/can-attempt', { params: { skill, ai: isAI } }),
};
