import { del, get, post, put } from '../../../lib/api';

const testSetsApi = {
    getAll: async () => get('/admin/test-sets'),

    getById: async (id) => get(`/admin/test-sets/${id}`),

    getByCode: async (code) => get(`/admin/test-sets/code/${code}`),

    create: async (data) => post('/admin/test-sets', data),

    update: async (id, data) => put(`/admin/test-sets/${id}`, data),

    delete: async (id) => del(`/admin/test-sets/${id}`),

    publish: async (id) => post(`/admin/test-sets/${id}/publish`),

    unpublish: async (id) => post(`/admin/test-sets/${id}/unpublish`),
};

export default testSetsApi;
