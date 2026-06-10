import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const hashtagsApi = {
    getAll: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags`,
            { headers }
        );
        return response.data;
    },

    getByCategory: async (category) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/hashtags/category/${category}`,
            { headers }
        );
        return response.data;
    },

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

    create: async (data) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/hashtags`,
            data,
            { headers }
        );
        return response.data;
    },

    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/hashtags/${id}`,
            data,
            { headers }
        );
        return response.data;
    },

    delete: async (id) => {
        const headers = await getAuthHeaders();
        await axios.delete(
            `${API_BASE_URL}/api/admin/hashtags/${id}`,
            { headers }
        );
    },
};

export default hashtagsApi;