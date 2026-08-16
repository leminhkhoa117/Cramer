import { del, get, patch, post, put } from '../../../lib/api';

const contentApi = {
    getTopics: async (params = {}) => get('/admin/content/topics', { params }),

    getOverview: async () => get('/admin/content/overview'),

    getTestDetails: async (examSource, testNumber) =>
        get(`/admin/content/tests/${examSource}/${testNumber}`),

    getSections: async (examSource, testNumber, skill) =>
        get(`/admin/content/tests/${examSource}/${testNumber}/${skill}/sections`),

    getQuestions: async (sectionId) => get(`/admin/content/sections/${sectionId}/questions`),

    getActivities: async (limit = 10) =>
        get('/admin/content/activities', { params: { limit } }),

    createSection: async (sectionData) => post('/admin/content/sections', sectionData),

    updateSection: async (sectionId, sectionData) =>
        put(`/admin/content/sections/${sectionId}`, sectionData),

    getSectionById: async (sectionId) => get(`/admin/content/sections/${sectionId}`),

    createQuestion: async (sectionId, questionData) =>
        post(`/admin/content/sections/${sectionId}/questions`, questionData),

    updateQuestion: async (questionId, questionData) =>
        put(`/admin/content/questions/${questionId}`, questionData),

    deleteQuestion: async (questionId) => del(`/admin/content/questions/${questionId}`),

    getQuestionById: async (questionId) => get(`/admin/content/questions/${questionId}`),

    updateTestStatus: async (examSource, testNumber, status) =>
        patch(`/admin/content/tests/${examSource}/${testNumber}/status`, { status }),

    createTest: async (testData) => post('/admin/content/tests', testData),

    deleteTest: async (testId) => del(`/admin/content/tests/${testId}`),
};

export default contentApi;
