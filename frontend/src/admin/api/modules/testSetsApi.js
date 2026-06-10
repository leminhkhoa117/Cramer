import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const testSetsApi = {
    getAll: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/test-sets`,
            { headers }
        );
        return response.data;
    },

    getById: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/test-sets/${id}`,
            { headers }
        );
        return response.data;
    },

    getByCode: async (code) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/test-sets/code/${code}`,
            { headers }
        );
        return response.data;
    },

    create: async (data) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/test-sets`,
            data,
            { headers }
        );
        return response.data;
    },

    update: async (id, data) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/test-sets/${id}`,
            data,
            { headers }
        );
        return response.data;
    },

    delete: async (id) => {
        const headers = await getAuthHeaders();
        await axios.delete(
            `${API_BASE_URL}/api/admin/test-sets/${id}`,
            { headers }
        );
    },

    publish: async (id) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/test-sets/${id}/publish`,
            null,
            { headers }
        );
        return response.data;
    },

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

export default testSetsApi;