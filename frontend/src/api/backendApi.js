import axios from 'axios';

// Base URL from environment or default to localhost
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
const apiClient = axios.create({
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
  getAll: () => apiClient.get('/courses'),
  getTestsByCourse: (courseName) => apiClient.get(`/courses/${courseName}/tests`),
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
  startAttempt: (source, testNum, skill) => {
    return apiClient.post('/test-attempts/start', null, {
      params: { source, test: testNum, skill },
    });
  },
  submitAttempt: (attemptId, answers) => {
    return apiClient.post(`/test-attempts/${attemptId}/submit`, { answers });
  },
  getTestReview: (attemptId) => {
    return apiClient.get(`/test-attempts/${attemptId}/review`);
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
  getSummary: (userId) => apiClient.get(`/dashboard/summary/${userId}`),
};

// ============================================
// TARGET APIs
// ============================================
export const targetApi = {
  getMyTarget: () => apiClient.get('/targets/me'),
  saveMyTarget: (targetData) => apiClient.put('/targets/me', targetData),
};

export default apiClient;
