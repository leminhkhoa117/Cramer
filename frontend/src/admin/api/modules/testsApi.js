import { del, get, patch, post, put } from '../../../lib/api';

const testsApi = {
    create: async (setId, data) => post(`/admin/test-sets/${setId}/tests`, data),

    getBySetId: async (setId, options = {}) =>
        get(`/admin/test-sets/${setId}/tests`, {
            params: {
                ...(options.sortBy ? { sortBy: options.sortBy } : {}),
                ...(options.sortDir ? { sortDir: options.sortDir } : {}),
            },
        }),

    getById: async (id) => get(`/admin/tests/${id}`),

    getBySetCodeAndNumber: async (setCode, testNumber) =>
        get('/admin/tests/lookup', { params: { setCode, testNumber } }),

    delete: async (id) => del(`/admin/tests/${id}`),

    update: async (id, data) => put(`/admin/tests/${id}`, data),

    publish: async (id) => post(`/admin/tests/${id}/publish`),

    unpublish: async (id) => post(`/admin/tests/${id}/unpublish`),

    updateHashtags: async (id, hashtagCodes) =>
        put(`/admin/tests/${id}/hashtags`, { hashtagCodes: hashtagCodes || [] }),

    duplicate: async (id, newTestNumber) =>
        post(`/admin/tests/${id}/duplicate`, null, { params: { newTestNumber } }),

    getSections: async (id, skill) =>
        get(`/admin/tests/${id}/sections`, { params: { skill } }),
};

export default testsApi;
