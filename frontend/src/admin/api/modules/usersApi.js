import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const usersApi = {
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

    getById: async (userId) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/users/${userId}`,
            { headers }
        );
        return response.data;
    },

    getStats: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/users/stats`,
            { headers }
        );
        return response.data;
    },

    updateStatus: async (userId, status, reason) => {
        const headers = await getAuthHeaders();
        const response = await axios.patch(
            `${API_BASE_URL}/api/admin/users/${userId}/status`,
            { status, reason },
            { headers }
        );
        return response.data;
    },

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
    },
};

export default usersApi;