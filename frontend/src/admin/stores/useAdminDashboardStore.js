import { create } from 'zustand';
import adminApi from '../api/adminApi';

// Cache duration: 5 minutes
const CACHE_DURATION_MS = 5 * 60 * 1000;

/**
 * useAdminDashboardStore - Zustand store for Admin Dashboard
 * Quản lý state cho thống kê dashboard, hoạt động gần đây và trạng thái hệ thống
 * 
 * Bao gồm caching để tránh fetch lại mỗi khi navigate
 */
const useAdminDashboardStore = create((set, get) => ({
    // Stats data
    stats: {
        totalUsers: 0,
        activeUsers: 0,
        newUsersThisMonth: 0,
        totalTestAttempts: 0,
        totalQuestions: 0,
        publishedTests: 0,
        totalVocabulary: 0,
        totalRevenue: 0,
    },
    
    // Percentage changes (to be calculated)
    changes: {
        users: { value: 0, type: 'up' },
        revenue: { value: 0, type: 'up' },
        tests: { value: 0, type: 'up' },
        growth: { value: 0, type: 'up' },
    },
    
    // Recent activities
    recentActivities: [],
    
    // System status
    systemStatus: {
        apiServer: 'operational',
        database: 'operational',
        paymentGateway: 'operational',
        aiGrading: 'operational',
    },
    
    // Loading states
    isLoadingStats: false,
    isLoadingActivities: false,
    
    // Error state
    error: null,
    
    // Cache timestamps
    lastStatsFetch: null,
    lastActivitiesFetch: null,
    lastStatusFetch: null,
    
    // Flag to track if data has been fetched at least once
    isInitialized: false,
    
    // Actions
    
    /**
     * Fetch dashboard stats - only if cache expired or forced
     * @param {boolean} force - Force refresh regardless of cache
     */
    fetchDashboardStats: async (force = false) => {
        const { lastStatsFetch, isLoadingStats } = get();
        const now = Date.now();
        
        // Skip if already loading
        if (isLoadingStats) return;
        
        // Skip if cache is still valid and not forced
        if (!force && lastStatsFetch && (now - lastStatsFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        set({ isLoadingStats: true, error: null });
        try {
            const response = await adminApi.dashboard.getStats();
            const data = response.data || response;
            
            set({
                stats: {
                    totalUsers: data.totalUsers || 0,
                    activeUsers: data.activeUsers || 0,
                    newUsersThisMonth: data.newUsersThisMonth || 0,
                    totalTestAttempts: data.totalTestAttempts || 0,
                    totalQuestions: data.totalQuestions || 0,
                    publishedTests: data.publishedTests || 0,
                    totalVocabulary: data.totalVocabulary || 0,
                    totalRevenue: data.totalRevenue || 0,
                },
                changes: data.changes || get().changes,
                isLoadingStats: false,
                lastStatsFetch: Date.now(),
                isInitialized: true,
            });
        } catch (error) {
            console.error('Error fetching dashboard stats:', error);
            set({ 
                error: error.message || 'Không thể tải thống kê',
                isLoadingStats: false 
            });
        }
    },
    
    /**
     * Fetch recent activities - only if cache expired or forced
     * @param {number} limit - Number of activities to fetch
     * @param {boolean} force - Force refresh regardless of cache
     */
    fetchRecentActivities: async (limit = 5, force = false) => {
        const { lastActivitiesFetch, isLoadingActivities } = get();
        const now = Date.now();
        
        // Skip if already loading
        if (isLoadingActivities) return;
        
        // Skip if cache is still valid and not forced
        if (!force && lastActivitiesFetch && (now - lastActivitiesFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        set({ isLoadingActivities: true });
        try {
            const response = await adminApi.dashboard.getRecentActivities(limit);
            const activities = response.data || response || [];
            
            set({
                recentActivities: activities,
                isLoadingActivities: false,
                lastActivitiesFetch: Date.now(),
            });
        } catch (error) {
            console.error('Error fetching recent activities:', error);
            set({ isLoadingActivities: false });
        }
    },
    
    /**
     * Check system status - only if cache expired or forced
     * @param {boolean} force - Force refresh regardless of cache
     */
    checkSystemStatus: async (force = false) => {
        const { lastStatusFetch } = get();
        const now = Date.now();
        
        // Skip if cache is still valid and not forced
        if (!force && lastStatusFetch && (now - lastStatusFetch) < CACHE_DURATION_MS) {
            return;
        }
        
        try {
            const response = await adminApi.dashboard.getSystemStatus();
            const status = response.data || response;
            
            set({
                systemStatus: {
                    apiServer: status.apiServer || 'operational',
                    database: status.database || 'operational',
                    paymentGateway: status.paymentGateway || 'operational',
                    aiGrading: status.aiGrading || 'operational',
                },
                lastStatusFetch: Date.now(),
            });
        } catch (error) {
            console.error('Error checking system status:', error);
            // Nếu lỗi, giả định API đang có vấn đề
            set(state => ({
                systemStatus: {
                    ...state.systemStatus,
                    apiServer: 'degraded',
                },
            }));
        }
    },
    
    /**
     * Force refresh all data
     */
    refreshAll: async () => {
        const { fetchDashboardStats, fetchRecentActivities, checkSystemStatus } = get();
        await Promise.all([
            fetchDashboardStats(true),
            fetchRecentActivities(5, true),
            checkSystemStatus(true),
        ]);
    },
    
    /**
     * Initialize dashboard data (called on mount)
     * Only fetches if data is not initialized or cache expired
     */
    initializeDashboard: async () => {
        const { fetchDashboardStats, fetchRecentActivities, checkSystemStatus } = get();
        // These will check cache internally
        await Promise.all([
            fetchDashboardStats(),
            fetchRecentActivities(),
            checkSystemStatus(),
        ]);
    },
    
    // Reset store
    reset: () => {
        set({
            stats: {
                totalUsers: 0,
                activeUsers: 0,
                newUsersThisMonth: 0,
                totalTestAttempts: 0,
                totalQuestions: 0,
                publishedTests: 0,
                totalVocabulary: 0,
                totalRevenue: 0,
            },
            recentActivities: [],
            error: null,
            lastStatsFetch: null,
            lastActivitiesFetch: null,
            lastStatusFetch: null,
            isInitialized: false,
        });
    },
}));

export default useAdminDashboardStore;
