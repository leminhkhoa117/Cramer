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
        }
    }
};

export default adminApi;
