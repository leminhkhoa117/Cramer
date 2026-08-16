import { get } from '../../../lib/api';

const dashboardApi = {
    getStats: async () => get('/admin/dashboard/stats'),

    getRecentActivities: async (limit = 5) =>
        get('/admin/dashboard/activities', { params: { limit } }),

    getSystemStatus: async () => get('/admin/dashboard/status'),
};

export default dashboardApi;
