import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const contentApi = {
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

    getOverview: async () => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/overview`,
            { headers }
        );
        return response.data;
    },

    getTestDetails: async (examSource, testNumber) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}`,
            { headers }
        );
        return response.data;
    },

    getSections: async (examSource, testNumber, skill) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}/${skill}/sections`,
            { headers }
        );
        return response.data;
    },

    getQuestions: async (sectionId) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/sections/${sectionId}/questions`,
            { headers }
        );
        return response.data;
    },

    getActivities: async (limit = 10) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/activities?limit=${limit}`,
            { headers }
        );
        return response.data;
    },

    createSection: async (sectionData) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/sections`,
            sectionData,
            { headers }
        );
        return response.data;
    },

    updateSection: async (sectionId, sectionData) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/content/sections/${sectionId}`,
            sectionData,
            { headers }
        );
        return response.data;
    },

    getSectionById: async (sectionId) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/sections/${sectionId}`,
            { headers }
        );
        return response.data;
    },

    createQuestion: async (sectionId, questionData) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/sections/${sectionId}/questions`,
            questionData,
            { headers }
        );
        return response.data;
    },

    updateQuestion: async (questionId, questionData) => {
        const headers = await getAuthHeaders();
        const response = await axios.put(
            `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
            questionData,
            { headers }
        );
        return response.data;
    },

    deleteQuestion: async (questionId) => {
        const headers = await getAuthHeaders();
        const response = await axios.delete(
            `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
            { headers }
        );
        return response.data;
    },

    getQuestionById: async (questionId) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/content/questions/${questionId}`,
            { headers }
        );
        return response.data;
    },

    updateTestStatus: async (examSource, testNumber, status) => {
        const headers = await getAuthHeaders();
        const response = await axios.patch(
            `${API_BASE_URL}/api/admin/content/tests/${examSource}/${testNumber}/status`,
            { status },
            { headers }
        );
        return response.data;
    },

    createTest: async (testData) => {
        const headers = await getAuthHeaders();
        const response = await axios.post(
            `${API_BASE_URL}/api/admin/content/tests`,
            testData,
            { headers }
        );
        return response.data;
    },

    deleteTest: async (testId) => {
        const headers = await getAuthHeaders();
        const response = await axios.delete(
            `${API_BASE_URL}/api/admin/content/tests/${testId}`,
            { headers }
        );
        return response.data;
    },
};

export default contentApi;