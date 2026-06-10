import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const activityApi = {
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

    getRecentActivities: async (userId, limit = 10) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/activities/users/${userId}/recent?limit=${limit}`,
            { headers }
        );
        return response.data;
    },

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
    },
};

export default activityApi;