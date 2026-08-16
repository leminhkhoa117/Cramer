import { del, get, post, put } from '../../../lib/api';

const hashtagsApi = {
    getAll: async () => get('/admin/hashtags'),

    getByCategory: async (category) => get(`/admin/hashtags/category/${category}`),

    search: async (query) => get('/admin/hashtags/search', { params: { q: query } }),

    getPopular: async (limit = 10) => get('/admin/hashtags/popular', { params: { limit } }),

    create: async (data) => post('/admin/hashtags', data),

    update: async (id, data) => put(`/admin/hashtags/${id}`, data),

    delete: async (id) => del(`/admin/hashtags/${id}`),
};

export default hashtagsApi;
