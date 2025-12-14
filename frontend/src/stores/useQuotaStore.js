import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { quotaApi } from '../api/backendApi';
import { useAuthStore } from './index';

/**
 * Zustand store for quota status (dual-quota billing system).
 * Tracks global and per-skill monthly usage limits.
 */
const useQuotaStore = create(
    devtools(
        subscribeWithSelector((set, get) => ({
            // ===== STATE =====
            quotaStatus: null,
            loading: false,
            error: null,
            lastFetched: null,

            // Pre-check result (for current attempt)
            preCheckResult: null,
            preCheckLoading: false,

            // ===== SELECTORS =====

            /**
             * Check if user is on premium tier (unlimited quota).
             */
            isPremium: () => {
                const { quotaStatus } = get();
                return quotaStatus?.isPremium === true;
            },

            /**
             * Get global quota progress (0-100).
             */
            getGlobalProgress: (isAI = false) => {
                const { quotaStatus } = get();
                if (!quotaStatus || quotaStatus.isPremium) return 0;

                if (isAI) {
                    const cap = quotaStatus.globalAttemptAICap;
                    return cap > 0 ? Math.min(100, (quotaStatus.globalAttemptAI / cap) * 100) : 0;
                }
                const cap = quotaStatus.globalAttemptCap;
                return cap > 0 ? Math.min(100, (quotaStatus.globalAttempt / cap) * 100) : 0;
            },

            /**
             * Get skill quota progress (0-100).
             */
            getSkillProgress: (skill, isAI = false) => {
                const { quotaStatus } = get();
                if (!quotaStatus || quotaStatus.isPremium) return 0;

                const skillInfo = quotaStatus.skills?.[skill];
                if (!skillInfo) return 0;

                if (isAI) {
                    const cap = skillInfo.attemptAICap;
                    return cap > 0 ? Math.min(100, (skillInfo.attemptAI / cap) * 100) : 0;
                }
                const cap = skillInfo.attemptCap;
                return cap > 0 ? Math.min(100, (skillInfo.attempt / cap) * 100) : 0;
            },

            /**
             * Get color for progress bar based on percentage.
             */
            getProgressColor: (percent) => {
                if (percent < 50) return 'green';
                if (percent < 80) return 'yellow';
                return 'red';
            },

            // ===== ACTIONS =====

            /**
             * Fetch quota status from API.
             */
            fetchQuotaStatus: async (force = false) => {
                const user = useAuthStore.getState().user;
                if (!user) {
                    console.log('⏭️ Skipping fetchQuotaStatus - no user');
                    return;
                }

                const { loading, lastFetched } = get();

                // Debounce: don't fetch if already loading or fetched within last 30 seconds
                if (loading) {
                    console.log('⏭️ Skipping fetchQuotaStatus - already loading');
                    return;
                }

                if (!force && lastFetched && Date.now() - lastFetched < 30000) {
                    console.log('⏭️ Skipping fetchQuotaStatus - cached (30s TTL)');
                    return;
                }

                set({ loading: true, error: null }, false, 'fetchQuotaStatus/start');

                try {
                    const response = await quotaApi.getStatus();
                    if (response?.data) {
                        set({
                            quotaStatus: response.data,
                            loading: false,
                            lastFetched: Date.now()
                        }, false, 'fetchQuotaStatus/success');
                        console.log('✅ Quota status fetched successfully');
                    }
                } catch (error) {
                    console.error('❌ Error fetching quota status:', error);
                    set({
                        loading: false,
                        error: error.message || 'Failed to load quota status'
                    }, false, 'fetchQuotaStatus/error');
                }
            },

            /**
             * Pre-check if an attempt is allowed.
             */
            preCheckAttempt: async (skill, isAI = false) => {
                const user = useAuthStore.getState().user;
                if (!user) return null;

                set({ preCheckLoading: true }, false, 'preCheckAttempt/start');

                try {
                    const response = await quotaApi.canAttempt(skill, isAI);
                    const result = response?.data;
                    set({
                        preCheckResult: result,
                        preCheckLoading: false
                    }, false, 'preCheckAttempt/success');
                    return result;
                } catch (error) {
                    console.error('❌ Error pre-checking attempt:', error);
                    set({ preCheckLoading: false }, false, 'preCheckAttempt/error');
                    return null;
                }
            },

            /**
             * Clear pre-check result.
             */
            clearPreCheck: () => {
                set({ preCheckResult: null }, false, 'clearPreCheck');
            },

            /**
             * Invalidate cache (force refresh on next fetch).
             */
            invalidateCache: () => {
                set({ lastFetched: null }, false, 'invalidateCache');
            },

            /**
             * Clear all quota data (on logout).
             */
            clearQuota: () => {
                set({
                    quotaStatus: null,
                    loading: false,
                    error: null,
                    lastFetched: null,
                    preCheckResult: null,
                    preCheckLoading: false,
                }, false, 'clearQuota');
            },
        })),
        { name: 'QuotaStore' }
    )
);

// Auto-clear quota when user logs out
useAuthStore.subscribe(
    (state) => state.user,
    (user, prevUser) => {
        if (prevUser && !user) {
            // User logged out
            useQuotaStore.getState().clearQuota();
        } else if (user && !prevUser) {
            // User logged in - fetch quota status
            useQuotaStore.getState().fetchQuotaStatus();
        }
    }
);

export default useQuotaStore;
