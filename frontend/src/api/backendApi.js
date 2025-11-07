import axios from 'axios';
import { supabase } from './supabaseClient';

// Base URL from environment or default to localhost
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
});

// Request interceptor to add JWT token from Supabase
apiClient.interceptors.request.use(
  async (config) => {
    console.log('🌐 API Request:', config.method?.toUpperCase(), config.url);
    
    // Get current session from Supabase
    if (supabase?.auth) {
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (session?.access_token) {
        config.headers.Authorization = `Bearer ${session.access_token}`;
        console.log('🔑 JWT token attached');
      } else {
        console.warn('⚠️ No JWT token available');
      }
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
    console.error('❌ API Error:', error.config?.method?.toUpperCase(), error.config?.url);
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

export default apiClient;
