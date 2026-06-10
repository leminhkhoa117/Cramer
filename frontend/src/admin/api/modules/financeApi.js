import axios from 'axios';
import { API_BASE_URL, getAuthHeaders } from './core';

const financeApi = {
    getOverview: async (timeFilter = '30days') => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/overview?period=${timeFilter}`,
            { headers }
        );
        return response.data;
    },

    getChart: async (timeFilter = '30days') => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/chart?period=${timeFilter}`,
            { headers }
        );
        return response.data;
    },

    getBreakdown: async (timeFilter = '30days') => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/breakdown?period=${timeFilter}`,
            { headers }
        );
        return response.data;
    },

    getTransactions: async (params = {}) => {
        const headers = await getAuthHeaders();
        const queryParams = new URLSearchParams();

        if (params.page !== undefined) queryParams.append('page', params.page);
        if (params.size !== undefined) queryParams.append('size', params.size);
        if (params.status) queryParams.append('status', params.status);
        if (params.type) queryParams.append('type', params.type);
        if (params.dateFrom) queryParams.append('dateFrom', params.dateFrom);
        if (params.dateTo) queryParams.append('dateTo', params.dateTo);

        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/transactions?${queryParams.toString()}`,
            { headers }
        );
        return response.data;
    },

    getTopSpenders: async (limit = 5) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/top-spenders?limit=${limit}`,
            { headers }
        );
        return response.data;
    },

    getExportData: async (dateFrom, dateTo, status) => {
        const headers = await getAuthHeaders();
        const queryParams = new URLSearchParams();

        if (dateFrom) queryParams.append('dateFrom', dateFrom);
        if (dateTo) queryParams.append('dateTo', dateTo);
        if (status && status !== 'ALL') queryParams.append('status', status);

        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/export?${queryParams.toString()}`,
            { headers }
        );
        return response.data;
    },

    getReports: async (dateFrom, dateTo, granularity = 'daily') => {
        const headers = await getAuthHeaders();
        const queryParams = new URLSearchParams();

        queryParams.append('dateFrom', dateFrom);
        queryParams.append('dateTo', dateTo);
        queryParams.append('granularity', granularity);

        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/reports?${queryParams.toString()}`,
            { headers }
        );
        return response.data;
    },

    getSubscriptionAnalysis: async (dateFrom, dateTo) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/reports/subscriptions?dateFrom=${dateFrom}&dateTo=${dateTo}`,
            { headers }
        );
        return response.data;
    },

    getLuaEconomy: async (dateFrom, dateTo) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/reports/lua-economy?dateFrom=${dateFrom}&dateTo=${dateTo}`,
            { headers }
        );
        return response.data;
    },

    getAcquisition: async (dateFrom, dateTo) => {
        const headers = await getAuthHeaders();
        const response = await axios.get(
            `${API_BASE_URL}/api/admin/finance/reports/acquisition?dateFrom=${dateFrom}&dateTo=${dateTo}`,
            { headers }
        );
        return response.data;
    },
};

export default financeApi;