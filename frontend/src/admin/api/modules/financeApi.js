import { get } from '../../../lib/api';

const financeApi = {
    getOverview: async (timeFilter = '30days') =>
        get('/admin/finance/overview', { params: { period: timeFilter } }),

    getChart: async (timeFilter = '30days') =>
        get('/admin/finance/chart', { params: { period: timeFilter } }),

    getBreakdown: async (timeFilter = '30days') =>
        get('/admin/finance/breakdown', { params: { period: timeFilter } }),

    getTransactions: async (params = {}) =>
        get('/admin/finance/transactions', { params }),

    getTopSpenders: async (limit = 5) =>
        get('/admin/finance/top-spenders', { params: { limit } }),

    getExportData: async (dateFrom, dateTo, status) =>
        get('/admin/finance/export', {
            params: {
                ...(dateFrom ? { dateFrom } : {}),
                ...(dateTo ? { dateTo } : {}),
                ...(status && status !== 'ALL' ? { status } : {}),
            },
        }),

    getReports: async (dateFrom, dateTo, granularity = 'daily') =>
        get('/admin/finance/reports', { params: { dateFrom, dateTo, granularity } }),

    getSubscriptionAnalysis: async (dateFrom, dateTo) =>
        get('/admin/finance/reports/subscriptions', { params: { dateFrom, dateTo } }),

    getLuaEconomy: async (dateFrom, dateTo) =>
        get('/admin/finance/reports/lua-economy', { params: { dateFrom, dateTo } }),

    getAcquisition: async (dateFrom, dateTo) =>
        get('/admin/finance/reports/acquisition', { params: { dateFrom, dateTo } }),
};

export default financeApi;
