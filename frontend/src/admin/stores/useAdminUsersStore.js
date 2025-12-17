/**
 * Admin Users Store - Zustand store cho quản lý users trong Admin CMS
 * 
 * Quản lý state cho danh sách users, filters, pagination, và các actions.
 */
import { create } from 'zustand';
import adminApi from '../api/adminApi';

const useAdminUsersStore = create((set, get) => ({
    // Users data
    users: [],
    selectedUser: null,
    
    // Stats
    stats: {
        totalUsers: 0,
        activeUsers: 0,
        premiumUsers: 0,
        newUsersThisMonth: 0
    },
    
    // Pagination
    currentPage: 0,
    pageSize: 25,
    totalItems: 0,
    totalPages: 0,
    
    // Filters
    searchQuery: '',
    statusFilter: 'ALL',
    subscriptionFilter: 'ALL',
    
    // Sorting
    sortBy: 'createdAt',
    sortOrder: 'desc',
    
    // Loading states
    isLoading: false,
    isLoadingStats: false,
    isLoadingUser: false,
    isExporting: false,
    
    // Error
    error: null,
    
    // =====================
    // ACTIONS
    // =====================
    
    /**
     * Fetch users from API
     * Lấy tất cả users để client-side filtering nhanh hơn
     */
    fetchUsers: async () => {
        set({ isLoading: true, error: null });
        
        try {
            // Fetch all users for client-side filtering
            const response = await adminApi.users.getList({
                page: 0,
                size: 10000, // Lấy tất cả
                sortBy: 'createdAt',
                sortOrder: 'desc'
            });
            
            set({
                users: response.users || [],
                totalItems: response.totalItems || 0,
                isLoading: false
            });
        } catch (error) {
            console.error('Error fetching users:', error);
            set({
                error: 'Không thể tải danh sách người dùng',
                isLoading: false
            });
        }
    },
    
    /**
     * Fetch user stats
     */
    fetchStats: async () => {
        set({ isLoadingStats: true });
        
        try {
            const statsData = await adminApi.users.getStats();
            set({
                stats: statsData,
                isLoadingStats: false
            });
        } catch (error) {
            console.error('Error fetching stats:', error);
            set({ isLoadingStats: false });
        }
    },
    
    /**
     * Fetch single user by ID
     */
    fetchUserById: async (userId) => {
        set({ isLoadingUser: true, selectedUser: null, error: null });
        
        try {
            const user = await adminApi.users.getById(userId);
            set({
                selectedUser: user,
                isLoadingUser: false
            });
            return user;
        } catch (error) {
            console.error('Error fetching user:', error);
            set({
                error: 'Không tìm thấy người dùng',
                isLoadingUser: false
            });
            return null;
        }
    },
    
    /**
     * Set search query and fetch
     */
    setSearchQuery: (query) => {
        set({ searchQuery: query, currentPage: 0 });
        // Debounced fetch will be handled by component
    },
    
    /**
     * Set status filter
     */
    setStatusFilter: (status) => {
        set({ statusFilter: status, currentPage: 0 });
        get().fetchUsers();
    },
    
    /**
     * Set subscription filter
     */
    setSubscriptionFilter: (subscription) => {
        set({ subscriptionFilter: subscription, currentPage: 0 });
        get().fetchUsers();
    },
    
    /**
     * Set sorting
     */
    setSorting: (sortBy, sortOrder) => {
        set({ sortBy, sortOrder });
        get().fetchUsers();
    },
    
    /**
     * Go to page
     */
    goToPage: (page) => {
        set({ currentPage: page });
        get().fetchUsers();
    },
    
    /**
     * Set page size
     */
    setPageSize: (size) => {
        set({ pageSize: size, currentPage: 0 });
        get().fetchUsers();
    },
    
    /**
     * Update user status (ban/unban)
     */
    updateUserStatus: async (userId, status, reason) => {
        try {
            const updatedUser = await adminApi.users.updateStatus(userId, status, reason);
            
            // Update user in list
            set(state => ({
                users: state.users.map(u => u.id === userId ? updatedUser : u),
                selectedUser: state.selectedUser?.id === userId ? updatedUser : state.selectedUser
            }));
            
            return updatedUser;
        } catch (error) {
            console.error('Error updating user status:', error);
            throw error;
        }
    },
    
    /**
     * Update user credits
     */
    updateUserCredits: async (userId, amount, action, reason) => {
        try {
            const updatedUser = await adminApi.users.updateCredits(userId, amount, action, reason);
            
            // Update user in list
            set(state => ({
                users: state.users.map(u => u.id === userId ? updatedUser : u),
                selectedUser: state.selectedUser?.id === userId ? updatedUser : state.selectedUser
            }));
            
            // Refresh stats
            get().fetchStats();
            
            return updatedUser;
        } catch (error) {
            console.error('Error updating credits:', error);
            throw error;
        }
    },
    
    /**
     * Export users to Excel
     */
    exportToExcel: async () => {
        set({ isExporting: true });
        
        try {
            // Fetch all users (no pagination)
            const response = await adminApi.users.getList({
                page: 0,
                size: 10000, // Get all
                status: get().statusFilter !== 'ALL' ? get().statusFilter : undefined,
                subscription: get().subscriptionFilter !== 'ALL' ? get().subscriptionFilter : undefined,
                sortBy: get().sortBy,
                sortOrder: get().sortOrder
            });
            
            set({ isExporting: false });
            return response.users || [];
        } catch (error) {
            console.error('Error exporting:', error);
            set({ isExporting: false });
            throw error;
        }
    },
    
    /**
     * Clear selected user
     */
    clearSelectedUser: () => {
        set({ selectedUser: null });
    },
    
    /**
     * Reset all filters
     */
    resetFilters: () => {
        set({
            searchQuery: '',
            statusFilter: 'ALL',
            subscriptionFilter: 'ALL',
            currentPage: 0,
            sortBy: 'createdAt',
            sortOrder: 'desc'
        });
        get().fetchUsers();
    }
}));

export default useAdminUsersStore;
