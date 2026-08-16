import { get, patch } from '../../../lib/api';

const usersApi = {
    getList: async (params = {}) => get('/admin/users', { params }),

    getById: async (userId) => get(`/admin/users/${userId}`),

    getStats: async () => get('/admin/users/stats'),

    updateStatus: async (userId, status, reason) =>
        patch(`/admin/users/${userId}/status`, { status, reason }),

    updateCredits: async (userId, amount, action, reason) =>
        patch(`/admin/users/${userId}/credits`, { amount, action, reason }),

    updateSubscription: async (userId, tierCode, durationMonths, reason) =>
        patch(`/admin/users/${userId}/subscription`, { tierCode, durationMonths, reason }),
};

export default usersApi;
