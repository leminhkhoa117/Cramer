/**
 * Tests for useAdminDashboardStore
 * 
 * Tests admin dashboard stats, activities, system status, and caching.
 * 
 * @author Cramer Test Team
 * @since 2026-01-26
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';

// Mock the adminApi
vi.mock('../../../admin/api/adminApi', () => ({
    default: {
        dashboard: {
            getStats: vi.fn(),
            getRecentActivities: vi.fn(),
            getSystemStatus: vi.fn(),
        }
    }
}));

import useAdminDashboardStore from '../../../admin/stores/useAdminDashboardStore';
import adminApi from '../../../admin/api/adminApi';

describe('useAdminDashboardStore', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        
        // Reset store to initial state
        act(() => {
            useAdminDashboardStore.getState().reset();
        });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // =========================================================================
    // INITIAL STATE TESTS
    // =========================================================================
    describe('Initial State', () => {
        it('should have correct initial stats values', () => {
            const state = useAdminDashboardStore.getState();
            
            expect(state.stats.totalUsers).toBe(0);
            expect(state.stats.activeUsers).toBe(0);
            expect(state.stats.totalTestAttempts).toBe(0);
            expect(state.stats.publishedTests).toBe(0);
        });

        it('should have initial system status as operational', () => {
            const state = useAdminDashboardStore.getState();
            
            expect(state.systemStatus.apiServer).toBe('operational');
            expect(state.systemStatus.database).toBe('operational');
            expect(state.systemStatus.paymentGateway).toBe('operational');
            expect(state.systemStatus.aiGrading).toBe('operational');
        });

        it('should not be initialized initially', () => {
            const state = useAdminDashboardStore.getState();
            expect(state.isInitialized).toBe(false);
        });
    });

    // =========================================================================
    // FETCH DASHBOARD STATS TESTS
    // =========================================================================
    describe('fetchDashboardStats()', () => {
        it('should fetch stats successfully', async () => {
            const mockStats = {
                totalUsers: 1500,
                activeUsers: 350,
                newUsersThisMonth: 45,
                totalTestAttempts: 8500,
                totalQuestions: 12000,
                publishedTests: 48,
                totalVocabulary: 25000,
                totalRevenue: 15000000,
            };

            adminApi.dashboard.getStats.mockResolvedValueOnce({ data: mockStats });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats();
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.stats.totalUsers).toBe(1500);
            expect(newState.stats.activeUsers).toBe(350);
            expect(newState.stats.totalRevenue).toBe(15000000);
            expect(newState.isLoadingStats).toBe(false);
            expect(newState.isInitialized).toBe(true);
        });

        it('should set loading state when fetching', async () => {
            adminApi.dashboard.getStats.mockImplementation(() => 
                new Promise(resolve => setTimeout(() => resolve({ data: {} }), 100))
            );

            const state = useAdminDashboardStore.getState();
            const fetchPromise = state.fetchDashboardStats(true);
            
            expect(useAdminDashboardStore.getState().isLoadingStats).toBe(true);
            
            vi.advanceTimersByTime(100);
            await fetchPromise;
        });

        it('should handle fetch error gracefully', async () => {
            adminApi.dashboard.getStats.mockRejectedValueOnce(new Error('Server error'));

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats(true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.isLoadingStats).toBe(false);
            expect(newState.error).toBeTruthy();
        });

        it('should skip fetch if cache is still valid', async () => {
            // First fetch to populate cache
            adminApi.dashboard.getStats.mockResolvedValueOnce({ data: { totalUsers: 100 } });
            
            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats(true);
            });

            vi.clearAllMocks();

            // Second fetch without force - should skip
            await act(async () => {
                await state.fetchDashboardStats(false);
            });

            expect(adminApi.dashboard.getStats).not.toHaveBeenCalled();
        });

        it('should fetch if cache expired', async () => {
            adminApi.dashboard.getStats.mockResolvedValue({ data: { totalUsers: 100 } });
            
            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats(true);
            });

            vi.clearAllMocks();

            // Advance time past cache duration (5 minutes)
            vi.advanceTimersByTime(6 * 60 * 1000);

            await act(async () => {
                await state.fetchDashboardStats(false);
            });

            expect(adminApi.dashboard.getStats).toHaveBeenCalled();
        });

        it('should always fetch when force is true', async () => {
            adminApi.dashboard.getStats.mockResolvedValue({ data: { totalUsers: 100 } });
            
            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats(true);
            });

            vi.clearAllMocks();

            // Force fetch even with valid cache
            await act(async () => {
                await state.fetchDashboardStats(true);
            });

            expect(adminApi.dashboard.getStats).toHaveBeenCalled();
        });
    });

    // =========================================================================
    // FETCH RECENT ACTIVITIES TESTS
    // =========================================================================
    describe('fetchRecentActivities()', () => {
        it('should fetch recent activities successfully', async () => {
            const mockActivities = [
                { id: 1, type: 'USER_REGISTERED', description: 'New user registered' },
                { id: 2, type: 'TEST_COMPLETED', description: 'User completed test' },
                { id: 3, type: 'PAYMENT_RECEIVED', description: 'Payment received' },
            ];

            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ data: mockActivities });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchRecentActivities(5, true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.recentActivities).toHaveLength(3);
            expect(newState.recentActivities[0].type).toBe('USER_REGISTERED');
            expect(newState.isLoadingActivities).toBe(false);
        });

        it('should respect limit parameter', async () => {
            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ data: [] });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchRecentActivities(10, true);
            });

            expect(adminApi.dashboard.getRecentActivities).toHaveBeenCalledWith(10);
        });

        it('should handle error silently', async () => {
            adminApi.dashboard.getRecentActivities.mockRejectedValueOnce(new Error('API error'));

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchRecentActivities(5, true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.isLoadingActivities).toBe(false);
        });

        it('should use cache when available', async () => {
            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ data: [{ id: 1 }] });
            
            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchRecentActivities(5, true);
            });

            vi.clearAllMocks();

            // Second call - should use cache
            await act(async () => {
                await state.fetchRecentActivities(5, false);
            });

            expect(adminApi.dashboard.getRecentActivities).not.toHaveBeenCalled();
        });
    });

    // =========================================================================
    // CHECK SYSTEM STATUS TESTS
    // =========================================================================
    describe('checkSystemStatus()', () => {
        it('should check system status successfully', async () => {
            const mockStatus = {
                apiServer: 'operational',
                database: 'operational',
                paymentGateway: 'operational',
                aiGrading: 'operational',
            };

            adminApi.dashboard.getSystemStatus.mockResolvedValueOnce({ data: mockStatus });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.checkSystemStatus(true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.systemStatus.apiServer).toBe('operational');
            expect(newState.systemStatus.database).toBe('operational');
        });

        it('should handle degraded status', async () => {
            const mockStatus = {
                apiServer: 'operational',
                database: 'degraded',
                paymentGateway: 'operational',
                aiGrading: 'maintenance',
            };

            adminApi.dashboard.getSystemStatus.mockResolvedValueOnce({ data: mockStatus });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.checkSystemStatus(true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.systemStatus.database).toBe('degraded');
            expect(newState.systemStatus.aiGrading).toBe('maintenance');
        });

        it('should set apiServer to degraded on error', async () => {
            adminApi.dashboard.getSystemStatus.mockRejectedValueOnce(new Error('Timeout'));

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.checkSystemStatus(true);
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.systemStatus.apiServer).toBe('degraded');
        });
    });

    // =========================================================================
    // REFRESH ALL TESTS
    // =========================================================================
    describe('refreshAll()', () => {
        it('should refresh all data with force flag', async () => {
            adminApi.dashboard.getStats.mockResolvedValueOnce({ data: {} });
            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ data: [] });
            adminApi.dashboard.getSystemStatus.mockResolvedValueOnce({ data: {} });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.refreshAll();
            });

            expect(adminApi.dashboard.getStats).toHaveBeenCalled();
            expect(adminApi.dashboard.getRecentActivities).toHaveBeenCalled();
            expect(adminApi.dashboard.getSystemStatus).toHaveBeenCalled();
        });
    });

    // =========================================================================
    // INITIALIZE DASHBOARD TESTS
    // =========================================================================
    describe('initializeDashboard()', () => {
        it('should initialize all data', async () => {
            adminApi.dashboard.getStats.mockResolvedValueOnce({ data: { totalUsers: 100 } });
            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ data: [] });
            adminApi.dashboard.getSystemStatus.mockResolvedValueOnce({ data: {} });

            const state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.initializeDashboard();
            });

            expect(adminApi.dashboard.getStats).toHaveBeenCalled();
            expect(useAdminDashboardStore.getState().isInitialized).toBe(true);
        });
    });

    // =========================================================================
    // RESET TESTS
    // =========================================================================
    describe('reset()', () => {
        it('should reset all state to initial values', async () => {
            // Populate state first
            adminApi.dashboard.getStats.mockResolvedValueOnce({ 
                data: { totalUsers: 1000, activeUsers: 500 } 
            });
            adminApi.dashboard.getRecentActivities.mockResolvedValueOnce({ 
                data: [{ id: 1 }] 
            });

            let state = useAdminDashboardStore.getState();
            await act(async () => {
                await state.fetchDashboardStats(true);
                await state.fetchRecentActivities(5, true);
            });

            // Verify data was populated
            expect(useAdminDashboardStore.getState().stats.totalUsers).toBe(1000);
            expect(useAdminDashboardStore.getState().recentActivities).toHaveLength(1);

            // Reset
            act(() => {
                useAdminDashboardStore.getState().reset();
            });

            const newState = useAdminDashboardStore.getState();
            expect(newState.stats.totalUsers).toBe(0);
            expect(newState.stats.activeUsers).toBe(0);
            expect(newState.recentActivities).toHaveLength(0);
            expect(newState.isInitialized).toBe(false);
            expect(newState.lastStatsFetch).toBeNull();
        });
    });
});
