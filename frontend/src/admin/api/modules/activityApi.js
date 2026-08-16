import { get } from '../../../lib/api';

const activityApi = {
    getUserActivities: async (userId, params = {}) =>
        get(`/admin/activities/users/${userId}`, { params }),

    getRecentActivities: async (userId, limit = 10) =>
        get(`/admin/activities/users/${userId}/recent`, { params: { limit } }),

    getAuditLogs: async (userId, params = {}) =>
        get(`/admin/activities/audit/users/${userId}`, { params }),

    getAllAuditLogs: async (params = {}) =>
        get('/admin/activities/audit', { params }),
};

export default activityApi;
