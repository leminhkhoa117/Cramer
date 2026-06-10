import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const testsApi = {
    create: async (setId, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/test-sets/${setId}/tests`,
            data,
            { headers }
        );
        return response.data;
    },

    getBySetId: async (setId, options = {}) => {
        const headers = await getAuthHeaders();
        const queryParams = new URLSearchParams();

        if (options.sortBy) queryParams.append('sortBy', options.sortBy);
        if (options.sortDir) queryParams.append('sortDir', options.sortDir);

        const queryString = queryParams.toString();
        const url = queryString
            ? `${API_BASE_URL}/api/admin/test-sets/${setId}/tests?${queryString}`
            : `${API_BASE_URL}/api/admin/test-sets/${setId}/tests`;
        const response = await axios.get(url, { headers });
        return response.data;
    },

    getById: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/tests/${id}`,
            { headers }
        );
        return response.data;
    },

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

    delete: async (id) => {
        const headers = await getAuthHeaders();
        await axios.delete(
            `${API_BASE_URL}/api/admin/tests/${id}`,
            { headers }
        );
    },

    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/tests/${id}`,
            data,
            { headers }
        );
        return response.data;
    },

    publish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/tests/${id}/publish`,
            null,
            { headers }
        );
        return response.data;
    },

    unpublish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/tests/${id}/unpublish`,
            null,
            { headers }
        );
        return response.data;
    },

    updateHashtags: async (id, hashtagCodes) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/tests/${id}/hashtags`,
            { hashtagCodes: hashtagCodes || [] },
            { headers }
        );
        return response.data;
    },

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

export default testsApi;