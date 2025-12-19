/**
 * Admin API Client - API calls cho Admin CMS
 * 
 * Sử dụng axios để gọi các endpoints admin.
 */
import axios from 'axios';
import { supabase } from '../../api/supabaseClient';

// API base URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Get authorization headers with JWT token
 */
const getAuthHeaders = async () => {
    const { data: { session } } = await supabase.auth.getSession();
    if (!session?.access_token) {
        throw new Error('No auth session found');
    }
    return {
        'Authorization': `Bearer ${session.access_token}`,
        'X-User-Id': session.user.id,
        'Content-Type': 'application/json'
    };
};

/**
 * Admin API instance
 */
const adminApi = {
    /**
     * USER MANAGEMENT APIs
     */
    users: {
        /**
         * Get list of users with pagination and filters
         * @param {Object} params - Query parameters
         * @param {number} params.page - Page number (0-indexed)
         * @param {number} params.size - Page size
         * @param {string} params.search - Search query
         * @param {string} params.status - Status filter
         * @param {string} params.subscription - Subscription filter
         * @param {string} params.sortBy - Sort field
         * @param {string} params.sortOrder - Sort order (asc/desc)
         */
        getList: async (params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);
            if (params.search) queryParams.append('search', params.search);
            if (params.status && params.status !== 'ALL') queryParams.append('status', params.status);
            if (params.subscription && params.subscription !== 'ALL') queryParams.append('subscription', params.subscription);
            if (params.sortBy) queryParams.append('sortBy', params.sortBy);
            if (params.sortOrder) queryParams.append('sortOrder', params.sortOrder);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/users?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get user by ID
         * @param {string} userId - User UUID
         */
        getById: async (userId) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/users/${userId}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get user statistics
         */
        getStats: async () => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/users/stats`,
                { headers }
            );
            return response.data;
        },

        /**
         * Update user status (ban/unban)
         * @param {string} userId - User UUID
         * @param {string} status - New status (ACTIVE, BANNED, etc.)
         * @param {string} reason - Reason for status change
         */
        updateStatus: async (userId, status, reason) => {
            const headers = await getAuthHeaders();
            const response = await axios.patch(
                `${API_BASE_URL}/api/admin/users/${userId}/status`,
                { status, reason },
                { headers }
            );
            return response.data;
        },

        /**
         * Update user credits (Lúa)
         * @param {string} userId - User UUID
         * @param {number} amount - Amount of credits
         * @param {string} action - ADD or SUBTRACT
         * @param {string} reason - Reason for credit change
         */
        updateCredits: async (userId, amount, action, reason) => {
            const headers = await getAuthHeaders();
            const response = await axios.patch(
                `${API_BASE_URL}/api/admin/users/${userId}/credits`,
                { amount, action, reason },
                { headers }
            );
            return response.data;
        }
    },

    /**
     * FINANCE APIs
     */
    finance: {
        /**
         * Get finance overview/dashboard data
         */
        getOverview: async (timeFilter = '30days') => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/overview?period=${timeFilter}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get revenue chart data
         */
        getChart: async (timeFilter = '30days') => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/chart?period=${timeFilter}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get revenue breakdown by type/tier
         */
        getBreakdown: async (timeFilter = '30days') => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/breakdown?period=${timeFilter}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get transaction list
         * @param {Object} params - Query parameters
         */
        getTransactions: async (params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);
            if (params.status) queryParams.append('status', params.status);
            if (params.type) queryParams.append('type', params.type);
            if (params.dateFrom) queryParams.append('dateFrom', params.dateFrom);
            if (params.dateTo) queryParams.append('dateTo', params.dateTo);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/transactions?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get top spenders
         */
        getTopSpenders: async (limit = 5) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/top-spenders?limit=${limit}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get export data
         */
        getExportData: async (dateFrom, dateTo, status) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (dateFrom) queryParams.append('dateFrom', dateFrom);
            if (dateTo) queryParams.append('dateTo', dateTo);
            if (status && status !== 'ALL') queryParams.append('status', status);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/export?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        }
    },

    /**
     * CONTENT MANAGEMENT APIs
     */
    content: {
        /**
         * Get list of topics with their tests
         * @param {Object} params - Query parameters
         * @param {string} params.search - Search query
         * @param {string} params.status - Status filter (DRAFT, PUBLISHED, ARCHIVED)
         */
        getTopics: async (params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.search) queryParams.append('search', params.search);
            if (params.status) queryParams.append('status', params.status);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/topics?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get content overview/stats
         */
        getOverview: async () => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/overview`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get test details
         * @param {string} examSource - Exam source (e.g., cam17, cam18)
         * @param {number} testNumber - Test number
         */
        getTestDetails: async (examSource, testNumber) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get sections for a specific skill
         * @param {string} examSource - Exam source
         * @param {number} testNumber - Test number
         * @param {string} skill - Skill (reading, listening, writing, speaking)
         */
        getSections: async (examSource, testNumber, skill) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}/${skill}/sections`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get questions for a section
         * @param {number} sectionId - Section ID
         */
        getQuestions: async (sectionId) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/sections/${sectionId}/questions`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get recent content activities
         * @param {number} limit - Number of activities to fetch
         */
        getActivities: async (limit = 10) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/activities?limit=${limit}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Create a new section
         * @param {Object} sectionData - Section data
         */
        createSection: async (sectionData) => {
            const headers = await getAuthHeaders();
            const response = await axios.post(
                `${API_BASE_URL}/api/admin/content/sections`,
                sectionData,
                { headers }
            );
            return response.data;
        },

        /**
         * Update a section
         * @param {number} sectionId - Section ID
         * @param {Object} sectionData - Section data to update
         */
        updateSection: async (sectionId, sectionData) => {
            const headers = await getAuthHeaders();
            const response = await axios.put(
                `${API_BASE_URL}/api/admin/content/sections/${sectionId}`,
                sectionData,
                { headers }
            );
            return response.data;
        },

        /**
         * Get section by ID
         * @param {number} sectionId - Section ID
         */
        getSectionById: async (sectionId) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/sections/${sectionId}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Create a new question
         * @param {number} sectionId - Section ID
         * @param {Object} questionData - Question data
         */
        createQuestion: async (sectionId, questionData) => {
            const headers = await getAuthHeaders();
            const response = await axios.post(
                `${API_BASE_URL}/api/admin/content/sections/${sectionId}/questions`,
                questionData,
                { headers }
            );
            return response.data;
        },

        /**
         * Update a question
         * @param {number} questionId - Question ID
         * @param {Object} questionData - Question data to update
         */
        updateQuestion: async (questionId, questionData) => {
            const headers = await getAuthHeaders();
            const response = await axios.put(
                `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
                questionData,
                { headers }
            );
            return response.data;
        },

        /**
         * Delete a question
         * @param {number} questionId - Question ID
         */
        deleteQuestion: async (questionId) => {
            const headers = await getAuthHeaders();
            const response = await axios.delete(
                `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get question by ID
         * @param {number} questionId - Question ID
         */
        getQuestionById: async (questionId) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Update test status (publish/draft/archive)
         * @param {string} examSource - Exam source
         * @param {number} testNumber - Test number
         * @param {string} status - New status (DRAFT, PUBLISHED, ARCHIVED)
         */
        updateTestStatus: async (examSource, testNumber, status) => {
            const headers = await getAuthHeaders();
            const response = await axios.patch(
                `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}/status`,
                { status },
                { headers }
            );
            return response.data;
        }
    },
    /**
     * ACTIVITY MANAGEMENT APIs
     */
    activity: {
        /**
         * Get user activities
         * @param {string} userId - User UUID
         * @param {Object} params - Query parameters (page, size)
         */
        getUserActivities: async (userId, params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/activities/users/${userId}?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get recent activities for a user
         * @param {string} userId - User UUID
         * @param {number} limit - Number of activities to fetch
         */
        getRecentActivities: async (userId, limit = 10) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/activities/users/${userId}/recent?limit=${limit}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get audit logs for a user
         * @param {string} userId - User UUID
         * @param {Object} params - Query parameters (page, size)
         */
        getAuditLogs: async (userId, params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/activities/audit/users/${userId}?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get all audit logs
         * @param {Object} params - Query parameters
         */
        getAllAuditLogs: async (params = {}) => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();
            
            if (params.page !== undefined) queryParams.append('page', params.page);
            if (params.size !== undefined) queryParams.append('size', params.size);
            
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/activities/audit?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        }
    },

    /**
     * DASHBOARD APIs
     */
    dashboard: {
        /**
         * Get dashboard statistics
         */
        getStats: async () => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/dashboard/stats`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get recent activities for dashboard
         * @param {number} limit - Number of activities to fetch
         */
        getRecentActivities: async (limit = 5) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/dashboard/activities?limit=${limit}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get system status
         */
        getSystemStatus: async () => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/dashboard/status`,
                { headers }
            );
            return response.data;
        }
    },
};

export default adminApi;

