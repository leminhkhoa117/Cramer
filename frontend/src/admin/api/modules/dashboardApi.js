import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const dashboardApi = {
    getStats: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/dashboard/stats`,
            { headers }
        );
        return response.data;
    },

    getRecentActivities: async (limit = 5) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/dashboard/activities?limit=${limit}`,
            { headers }
        );
        return response.data;
    },

    getSystemStatus: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/dashboard/status`,
            { headers }
        );
        return response.data;
    },
};

export default dashboardApi;