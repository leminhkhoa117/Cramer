/**
 * Admin Finance Store - Zustand store cho quản lý tài chính trong Admin CMS
 * 
 * Quản lý state cho thống kê doanh thu, giao dịch, và báo cáo tài chính.
 * Bao gồm caching để tránh fetch lại mỗi khi navigate.
 */
import { create } from 'zustand';
import adminApi from '../api/adminApi';
import { exportFinanceReport } from '../utils/exportUtils';

// Cache duration: 5 minutes
const CACHE_DURATION_MS = 5 * 60 * 1000;

const useAdminFinanceStore = create((set, get) => ({
    // Overview stats
    overview: {
        totalRevenue: 0,
        subscriptionRevenue: 0,
        luaRevenue: 0,
        newSubscriptions: 0,
        luaPacksSold: 0,
        pendingTransactions: 0,
        totalRevenueChange: 0,
        subscriptionChange: 0,
        luaPacksChange: 0,
        growthRate: 0,
        mrr: 0,
    },
    
    // Chart data
    revenueChart: [],
    revenueBreakdown: [],
    
    // Transactions
    transactions: [],
    transactionsPagination: {
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
    },
    
    // Top spenders
    topSpenders: [],
    
    // Filters
    timeFilter: '30days',
    transactionStatusFilter: 'ALL',
    transactionTypeFilter: 'ALL',
    
    // Loading states
    isLoadingOverview: false,
    isLoadingChart: false,
    isLoadingTransactions: false,
    isLoadingTopSpenders: false,
    isExporting: false,
    
    // Error
    error: null,
    
    // Cache timestamps
    lastOverviewFetch: null,
    lastChartFetch: null,
    lastTransactionsFetch: null,
    lastTopSpendersFetch: null,
    isInitialized: false,
    
    // =====================
    // ACTIONS
    // =====================
    
    /**
     * Set time filter
     */
    setTimeFilter: (filter) => {
        set({ 
            timeFilter: filter,
            lastOverviewFetch: null,
            lastChartFetch: null
        });
        const { fetchOverview, fetchChart, fetchBreakdown } = get();
        fetchOverview(true);
        fetchChart(true);
        fetchBreakdown(true);
    },

    /**
     * Set transaction filters
     */
    setTransactionFilters: (filters) => {
        set({
            transactionStatusFilter: filters.status || 'ALL',
            transactionTypeFilter: filters.type || 'ALL',
        });
        get().fetchTransactions(0, true);
    },
    
    /**
     * Fetch finance overview
     */
    fetchOverview: async (force = false) => {
        const { lastOverviewFetch, isLoadingOverview, timeFilter } = get();
        const now = Date.now();
        
        if (isLoadingOverview) return;
        if (!force && lastOverviewFetch && (now - lastOverviewFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        set({ isLoadingOverview: true, error: null });
        
        try {
            const response = await adminApi.finance.getOverview(timeFilter);
            const data = response.data || response;
            
            set({
                overview: {
                    totalRevenue: data.totalRevenue || 0,
                    subscriptionRevenue: data.subscriptionRevenue || 0,
                    luaRevenue: data.luaRevenue || 0,
                    newSubscriptions: data.newSubscriptions || 0,
                    luaPacksSold: data.luaPacksSold || 0,
                    pendingTransactions: data.pendingTransactions || 0,
                    totalRevenueChange: data.totalRevenueChange || 0,
                    subscriptionChange: data.subscriptionChange || 0,
                    luaPacksChange: data.luaPacksChange || 0,
                    growthRate: data.growthRate || 0,
                    mrr: data.mrr || 0,
                },
                isLoadingOverview: false,
                lastOverviewFetch: Date.now(),
                isInitialized: true,
            });
        } catch (error) {
            console.error('Error fetching finance overview:', error);
            set({
                error: 'Không thể tải thống kê tài chính',
                isLoadingOverview: false,
            });
        }
    },
    
    /**
     * Fetch revenue chart data
     */
    fetchChart: async (force = false) => {
        const { lastChartFetch, isLoadingChart, timeFilter } = get();
        const now = Date.now();
        
        if (isLoadingChart) return;
        if (!force && lastChartFetch && (now - lastChartFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        set({ isLoadingChart: true });
        
        try {
            const response = await adminApi.finance.getChart(timeFilter);
            const data = response.data || response || [];
            
            set({
                revenueChart: data,
                isLoadingChart: false,
                lastChartFetch: Date.now(),
            });
        } catch (error) {
            console.error('Error fetching chart data:', error);
            set({ isLoadingChart: false });
        }
    },
    
    /**
     * Fetch revenue breakdown
     */
    fetchBreakdown: async (force = false) => {
        const { timeFilter } = get();
        
        try {
            const response = await adminApi.finance.getBreakdown(timeFilter);
            const data = response.data || response || [];
            
            set({ revenueBreakdown: data });
        } catch (error) {
            console.error('Error fetching breakdown:', error);
        }
    },
    
    /**
     * Fetch transactions with pagination
     */
    fetchTransactions: async (page = 0, force = false) => {
        const { lastTransactionsFetch, isLoadingTransactions, transactionStatusFilter, transactionTypeFilter, transactionsPagination } = get();
        const now = Date.now();
        
        if (isLoadingTransactions) return;
        if (!force && lastTransactionsFetch && (now - lastTransactionsFetch) < CACHE_DURATION_MS && page === transactionsPagination.page) {
            return;
        }
        
        set({ isLoadingTransactions: true });
        
        try {
            const response = await adminApi.finance.getTransactions({
                page,
                size: 20,
                status: transactionStatusFilter !== 'ALL' ? transactionStatusFilter : undefined,
                type: transactionTypeFilter !== 'ALL' ? transactionTypeFilter : undefined,
            });
            const data = response.data || response;
            
            set({
                transactions: data.content || [],
                transactionsPagination: {
                    page: data.page,
                    size: data.size,
                    totalElements: data.totalElements,
                    totalPages: data.totalPages,
                },
                isLoadingTransactions: false,
                lastTransactionsFetch: Date.now(),
            });
        } catch (error) {
            console.error('Error fetching transactions:', error);
            set({ isLoadingTransactions: false });
        }
    },
    
    /**
     * Fetch top spenders
     */
    fetchTopSpenders: async (force = false, limit = 5) => {
        const { lastTopSpendersFetch, isLoadingTopSpenders } = get();
        const now = Date.now();
        
        if (isLoadingTopSpenders) return;
        if (!force && lastTopSpendersFetch && (now - lastTopSpendersFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        set({ isLoadingTopSpenders: true });
        
        try {
            const response = await adminApi.finance.getTopSpenders(limit);
            const data = response.data || response || [];
            
            set({
                topSpenders: data,
                isLoadingTopSpenders: false,
                lastTopSpendersFetch: Date.now(),
            });
        } catch (error) {
            console.error('Error fetching top spenders:', error);
            set({ isLoadingTopSpenders: false });
        }
    },
    
    /**
     * Export transactions to Excel (.xlsx)
     */
    exportToExcel: async (dateFrom, dateTo, status) => {
        set({ isExporting: true });
        
        try {
            const response = await adminApi.finance.getExportData(dateFrom, dateTo, status);
            
            // Handle different response formats
            let data = response;
            if (response && response.data) {
                data = response.data;
            }
            
            // Ensure data is an array
            if (!Array.isArray(data)) {
                data = [];
            }
            
            if (data.length === 0) {
                alert('Không có dữ liệu để xuất. Vui lòng đảm bảo backend đã được restart.');
                set({ isExporting: false });
                return;
            }
            
            // Use utility function to export Excel
            exportFinanceReport(data);
            
            set({ isExporting: false });
        } catch (error) {
            console.error('Error exporting data:', error);
            alert('Lỗi khi xuất báo cáo: ' + (error.message || 'Không thể kết nối đến server.'));
            set({ isExporting: false });
        }
    },
    
    /**
     * Initialize finance page
     */
    initializeFinance: async () => {
        const { fetchOverview, fetchChart, fetchBreakdown, fetchTransactions, fetchTopSpenders } = get();
        await Promise.all([
            fetchOverview(),
            fetchChart(),
            fetchBreakdown(),
            fetchTransactions(),
            fetchTopSpenders(),
        ]);
    },
    
    /**
     * Refresh all data
     */
    refreshAll: async () => {
        const { fetchOverview, fetchChart, fetchBreakdown, fetchTransactions, fetchTopSpenders } = get();
        await Promise.all([
            fetchOverview(true),
            fetchChart(true),
            fetchBreakdown(true),
            fetchTransactions(0, true),
            fetchTopSpenders(true),
        ]);
    },
    
    // Reset store
    reset: () => {
        set({
            overview: {
                totalRevenue: 0,
                subscriptionRevenue: 0,
                luaRevenue: 0,
                newSubscriptions: 0,
                luaPacksSold: 0,
                pendingTransactions: 0,
                totalRevenueChange: 0,
                subscriptionChange: 0,
                luaPacksChange: 0,
                growthRate: 0,
                mrr: 0,
            },
            revenueChart: [],
            revenueBreakdown: [],
            transactions: [],
            topSpenders: [],
            error: null,
            lastOverviewFetch: null,
            lastChartFetch: null,
            lastTransactionsFetch: null,
            lastTopSpendersFetch: null,
            isInitialized: false,
        });
    },
}));

export default useAdminFinanceStore;
