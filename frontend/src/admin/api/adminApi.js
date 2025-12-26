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
         * @param {string} userId - User UUID
         * @param {string} tierCode - New tier code (cramerie, cramerich)
         * @param {string} reason - Reason for change (optional)
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
        },
        updateSubscription: async (userId, tierCode, durationMonths, reason) => {
            const headers = await getAuthHeaders();
            const response = await axios.patch(
                `${API_BASE_URL}/api/admin/users/${userId}/subscription`,
                { tierCode, durationMonths, reason },
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
        },

        /**
         * Get reports data with date range and granularity
         * @param {string} dateFrom - Start date (YYYY-MM-DD)
         * @param {string} dateTo - End date (YYYY-MM-DD)
         * @param {string} granularity - daily, weekly, monthly
         */
        getReports: async (dateFrom, dateTo, granularity = 'daily') => {
            const headers = await getAuthHeaders();
            const queryParams = new URLSearchParams();

            queryParams.append('dateFrom', dateFrom);
            queryParams.append('dateTo', dateTo);
            queryParams.append('granularity', granularity);

            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/reports?${queryParams.toString()}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get subscription analysis data
         */
        getSubscriptionAnalysis: async (dateFrom, dateTo) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/reports/subscriptions?dateFrom=${dateFrom}&dateTo=${dateTo}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get Lua economy data
         */
        getLuaEconomy: async (dateFrom, dateTo) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/reports/lua-economy?dateFrom=${dateFrom}&dateTo=${dateTo}`,
                { headers }
            );
            return response.data;
        },

        /**
         * Get user acquisition data
         */
        getAcquisition: async (dateFrom, dateTo) => {
            const headers = await getAuthHeaders();
            const response = await axios.get(
                `${API_BASE_URL}/api/admin/finance/reports/acquisition?dateFrom=${dateFrom}&dateTo=${dateTo}`,
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
        },

        /**
         * Create a new test
         * @param {Object} testData - Test data (examSource, testNumber)
         */
        createTest: async (testData) => {
            const headers = await getAuthHeaders();
            const response = await axios.post(
                `${API_BASE_URL}/api/admin/content/tests`,
                testData,
                { headers }
            );
            return response.data;
        },

        /**
         * Delete a test
         * @param {string} testId - Test ID (examSource-testNumber)
         */
        deleteTest: async (testId) => {
            const headers = await getAuthHeaders();
            const response = await axios.delete(
                `${API_BASE_URL}/api/admin/content/tests/${testId}`,
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

    // Secondary APIs exposed through adminApi
    testSetsApi: null,
    testsApi: null,
    hashtagsApi: null
};

// Assign secondary APIs after they are defined (moved to end of file)

// ============ TEST SETS API ============
export const testSetsApi = {
    /**
     * List all test sets
     * @returns {Promise<Array>} List of test sets
     */
    getAll: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/test-sets`,
            { headers }
        );
        return response.data;
    },

    /**
     * Get test set by ID (includes tests)
     * @param {number} id - Test set ID
     * @returns {Promise<Object>} Test set with tests
     */
    getById: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/test-sets/${id}`,
            { headers }
        );
        return response.data;
    },

    /**
     * Get test set by code
     * @param {string} code - Test set code (e.g., "cam17")
     * @returns {Promise<Object>} Test set
     */
    getByCode: async (code) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/test-sets/code/${code}`,
            { headers }
        );
        return response.data;
    },

    /**
     * Create test set
     * @param {Object} data - Test set data
     * @returns {Promise<Object>} Created test set
     */
    create: async (data) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/test-sets`,
            data,
            { headers }
        );
        return response.data;
    },

    /**
     * Update test set
     * @param {number} id - Test set ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated test set
     */
    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/content/test-sets/${id}`,
            data,
            { headers }
        );
        return response.data;
    },

    /**
     * Delete test set
     * @param {number} id - Test set ID
     */
    delete: async (id) => {
        const headers = await getAuthHeaders();
        await axios.delete(
            `${API_BASE_URL}/api/admin/content/test-sets/${id}`,
            { headers }
        );
    },

    /**
     * Publish test set
     * @param {number} id - Test set ID
     * @returns {Promise<Object>} Updated test set
     */
    publish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/test-sets/${id}/publish`,
            null,
            { headers }
        );
        return response.data;
    },

    /**
     * Unpublish test set
     * @param {number} id - Test set ID
     * @returns {Promise<Object>} Updated test set
     */
    unpublish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/test-sets/${id}/unpublish`,
            null,
            { headers }
        );
        return response.data;
    },
};

// ============ TESTS API ============
export const testsApi = {
    /**
     * List tests in a set
     * @param {number} setId - Test set ID
     * @returns {Promise<Array>} List of tests
     */
    getBySetId: async (setId) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/test-sets/${setId}/tests`,
            { headers }
        );
        return response.data;
    },

    /**
     * Get test by ID (full details)
     * @param {number} id - Test ID
     * @returns {Promise<Object>} Test details
     */
    getById: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/tests/${id}`,
            { headers }
        );
        return response.data;
    },

    /**
     * Get test by set code and test number
     * @param {string} setCode - Test set code (e.g., "cam17")
     * @param {number} testNumber - Test number
     * @returns {Promise<Object>} Test details
     */
    getBySetCodeAndNumber: async (setCode, testNumber) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/tests/lookup`,
            {
                headers,
                params: { setCode, testNumber }
            }
        );
        return response.data;
    },




    /**
     * Update test
     * @param {number} id - Test ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated test
     */
    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/content/tests/${id}`,
            data,
            { headers }
        );
        return response.data;
    },



    /**
     * Publish test
     * @param {number} id - Test ID
     * @returns {Promise<Object>} Updated test
     */
    publish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/tests/${id}/publish`,
            null,
            { headers }
        );
        return response.data;
    },

    /**
     * Unpublish test
     * @param {number} id - Test ID
     * @returns {Promise<Object>} Updated test
     */
    unpublish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/tests/${id}/unpublish`,
            null,
            { headers }
        );
        return response.data;
    },

    /**
     * Update hashtags for a test
     * @param {number} id - Test ID
     * @param {Array<number>} hashtagIds - List of hashtag IDs
     * @param {number|null} primaryHashtagId - Primary hashtag ID (optional)
     * @returns {Promise<Object>} Updated test
     */
    updateHashtags: async (id, hashtagIds, primaryHashtagId = null) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/content/tests/${id}/hashtags`,
            { hashtagIds, primaryHashtagId },
            { headers }
        );
        return response.data;
    },

    /**
     * Duplicate test
     * @param {number} id - Test ID to duplicate
     * @param {number} newTestNumber - Test number for the duplicate
     * @returns {Promise<Object>} Created duplicate test
     */
    duplicate: async (id, newTestNumber) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/tests/${id}/duplicate`,
            null,
            {
                headers,
                params: { newTestNumber }
            }
        );
        return response.data;
    },

    /**
     * Get sections for a test
     * @param {number} id - Test ID
     * @param {string} skill - Skill (reading, listening, writing, speaking)
     * @returns {Promise<Array>} List of sections
     */
    getSections: async (id, skill) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/tests/${id}/sections`,
            {
                headers,
                params: { skill }
            }
        );
        return response.data;
    },
};

// ============ HASHTAGS API ============
export const hashtagsApi = {
    /**
     * List all hashtags
     * @returns {Promise<Array>} List of hashtags
     */
    getAll: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags`,
            { headers }
        );
        return response.data;
    },

    /**
     * Get hashtags by category
     * @param {string} category - Hashtag category (e.g., "topic", "theme", "difficulty")
     * @returns {Promise<Array>} List of hashtags in category
     */
    getByCategory: async (category) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags/category/${category}`,
            { headers }
        );
        return response.data;
    },

    /**
     * Search hashtags
     * @param {string} query - Search query
     * @returns {Promise<Array>} Matching hashtags
     */
    search: async (query) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags/search`,
            {
                headers,
                params: { q: query }
            }
        );
        return response.data;
    },

    /**
     * Get popular hashtags
     * @param {number} limit - Maximum number of hashtags to return
     * @returns {Promise<Array>} Popular hashtags
     */
    getPopular: async (limit = 10) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags/popular`,
            {
                headers,
                params: { limit }
            }
        );
        return response.data;
    },

    /**
     * Create hashtag
     * @param {Object} data - Hashtag data (name, category, code, displayName)
     * @returns {Promise<Object>} Created hashtag
     */
    create: async (data) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/hashtags`,
            data,
            { headers }
        );
        return response.data;
    },

    /**
     * Update hashtag
     * @param {number} id - Hashtag ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated hashtag
     */
    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/hashtags/${id}`,
            data,
            { headers }
        );
        return response.data;
    },

    /**
     * Delete hashtag
     * @param {number} id - Hashtag ID
     */
    delete: async (id) => {
        const headers = await getAuthHeaders();
        await axios.delete(
            `${API_BASE_URL}/api/admin/hashtags/${id}`,
            { headers }
        );
    },
};
// Assign secondary APIs to adminApi object
adminApi.testSetsApi = testSetsApi;
adminApi.testsApi = testsApi;
adminApi.hashtagsApi = hashtagsApi;

export default adminApi;
