import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { subscriptionApi } from '../api/backendApi';

/**
 * Zustand store for subscription and feature access state management.
 * Used to gate premium features and show upgrade prompts.
 */
const useSubscriptionStore = create(
    devtools(
        subscribeWithSelector((set, get) => ({
            // ===== STATE =====
            tier: null, // Current tier code (cramerie, cramerich, cramerous)
            tierNameVi: null,
            tierNameEn: null,
            features: {}, // Map of feature codes to boolean access
            isPremium: false,
            loading: false,
            error: null,
            lastFetchedAt: null,

            // ===== ACTIONS =====

            /**
             * Fetch subscription status from backend.
             * This populates tier, features, and isPremium state.
             */
            fetchSubscriptionStatus: async () => {
                const { loading } = get();
                if (loading) return; // Prevent duplicate fetches

                set({ loading: true, error: null }, false, 'fetchSubscriptionStatus/start');

                try {
                    const response = await subscriptionApi.getMyStatus();
                    const data = response.data;

                    // Extract tier info
                    const tierInfo = data.tier || {};
                    const tierCode = tierInfo.code || 'cramerie';

                    // Parse features from tier info
                    // The backend returns features as part of the tier or as a separate field
                    let featuresMap = {};

                    // Option 1: Features are in a separate featureAccess field
                    if (data.featureAccess?.features) {
                        featuresMap = data.featureAccess.features;
                    }
                    // Option 2: Features are in tier.features as an array
                    else if (tierInfo.features && Array.isArray(tierInfo.features)) {
                        tierInfo.features.forEach(feature => {
                            featuresMap[feature] = true;
                        });
                    }
                    // Option 3: Features are in tier.featuresMap
                    else if (tierInfo.featuresMap) {
                        featuresMap = tierInfo.featuresMap;
                    }

                    set(
                        {
                            tier: tierCode,
                            tierNameVi: tierInfo.name || 'Cramerie',
                            tierNameEn: tierInfo.name || 'Cramerie',
                            features: featuresMap,
                            isPremium: (tierInfo.priceVnd || 0) > 0,
                            loading: false,
                            error: null,
                            lastFetchedAt: Date.now(),
                        },
                        false,
                        'fetchSubscriptionStatus/success'
                    );

                    return { success: true };
                } catch (error) {
                    console.error('Failed to fetch subscription status:', error);
                    set(
                        {
                            loading: false,
                            error: error.message || 'Failed to fetch subscription status',
                        },
                        false,
                        'fetchSubscriptionStatus/error'
                    );
                    return { success: false, error };
                }
            },

            /**
             * Check if user has access to a specific feature.
             * @param {string} featureCode - The feature code to check
             * @returns {boolean} Whether user has access
             */
            hasFeature: (featureCode) => {
                const { features } = get();
                return features[featureCode] === true;
            },

            /**
             * Reset subscription state (on logout).
             */
            reset: () =>
                set(
                    {
                        tier: null,
                        tierNameVi: null,
                        tierNameEn: null,
                        features: {},
                        isPremium: false,
                        loading: false,
                        error: null,
                        lastFetchedAt: null,
                    },
                    false,
                    'reset'
                ),

            /**
             * Set feature access directly (for testing or optimistic updates).
             */
            setFeatures: (features) =>
                set({ features }, false, 'setFeatures'),
        })),
        {
            name: 'subscription-store',
            enabled: import.meta.env.DEV,
        }
    )
);

// ===== SELECTORS =====
export const selectTier = (state) => state.tier;
export const selectTierNameVi = (state) => state.tierNameVi;
export const selectFeatures = (state) => state.features;
export const selectIsPremium = (state) => state.isPremium;
export const selectLoading = (state) => state.loading;
export const selectError = (state) => state.error;

/**
 * Selector factory for checking specific feature access.
 * Usage: useSubscriptionStore(selectHasFeature('ai_writing_grading'))
 */
export const selectHasFeature = (featureCode) => (state) =>
    state.features[featureCode] === true;

// ===== ACTIONS (for use outside React components) =====
export const subscriptionActions = {
    fetchSubscriptionStatus: useSubscriptionStore.getState().fetchSubscriptionStatus,
    hasFeature: useSubscriptionStore.getState().hasFeature,
    reset: useSubscriptionStore.getState().reset,
};

export default useSubscriptionStore;
