import { useEffect } from 'react';
import useSubscriptionStore, {
    selectTier,
    selectFeatures,
    selectIsPremium,
    selectLoading,
    selectError,
    selectHasFeature,
} from '../stores/useSubscriptionStore';
import useAuthStore, { selectIsAuthenticated } from '../stores/useAuthStore';

/**
 * Custom hook for accessing subscription and feature information.
 * Automatically fetches subscription status when user is authenticated.
 * 
 * @returns {Object} Feature access utilities
 * 
 * @example
 * const { hasFeature, isPremium, tier } = useFeatureAccess();
 * 
 * if (hasFeature('ai_writing_grading')) {
 *   // Show AI grading option
 * }
 */
export function useFeatureAccess() {
    const isAuthenticated = useAuthStore(selectIsAuthenticated);
    const tier = useSubscriptionStore(selectTier);
    const tierNameVi = useSubscriptionStore((state) => state.tierNameVi);
    const tierNameEn = useSubscriptionStore((state) => state.tierNameEn);
    const features = useSubscriptionStore(selectFeatures);
    const isPremium = useSubscriptionStore(selectIsPremium);
    const loading = useSubscriptionStore(selectLoading);
    const error = useSubscriptionStore(selectError);
    const lastFetchedAt = useSubscriptionStore((state) => state.lastFetchedAt);
    const fetchSubscriptionStatus = useSubscriptionStore((state) => state.fetchSubscriptionStatus);

    // Auto-fetch subscription status when authenticated and not already fetched
    useEffect(() => {
        if (isAuthenticated && !loading && !lastFetchedAt) {
            fetchSubscriptionStatus();
        }
    }, [isAuthenticated, loading, lastFetchedAt, fetchSubscriptionStatus]);

    /**
     * Check if user has access to a specific feature.
     * @param {string} featureCode - Feature code to check
     * @returns {boolean}
     */
    const hasFeature = (featureCode) => {
        return features[featureCode] === true;
    };

    /**
     * Check multiple features at once.
     * @param {string[]} featureCodes - Array of feature codes
     * @returns {boolean} True if user has ALL features
     */
    const hasAllFeatures = (featureCodes) => {
        return featureCodes.every((code) => features[code] === true);
    };

    /**
     * Check if user has any of the specified features.
     * @param {string[]} featureCodes - Array of feature codes
     * @returns {boolean} True if user has ANY of the features
     */
    const hasAnyFeature = (featureCodes) => {
        return featureCodes.some((code) => features[code] === true);
    };

    /**
     * Refresh subscription status from backend.
     */
    const refresh = () => {
        return fetchSubscriptionStatus();
    };

    return {
        // State
        tier,
        tierNameVi,
        tierNameEn,
        features,
        isPremium,
        loading,
        error,

        // Methods
        hasFeature,
        hasAllFeatures,
        hasAnyFeature,
        refresh,

        // Convenience booleans for common checks
        isCramerie: tier === 'cramerie',
        isCramerich: tier === 'cramerich',
        isCramerous: tier === 'cramerous',
        canUseAI: hasAnyFeature(['ai_writing_grading', 'ai_reading_grading', 'ai_speaking_grading', 'vocab_ai', 'chatbot']),
    };
}

/**
 * Hook for checking a single feature.
 * Optimized version that only subscribes to the specific feature state.
 * 
 * @param {string} featureCode - Feature code to check
 * @returns {boolean} Whether user has access
 * 
 * @example
 * const canGradeWriting = useHasFeature('ai_writing_grading');
 */
export function useHasFeature(featureCode) {
    return useSubscriptionStore(selectHasFeature(featureCode));
}

/**
 * Hook for checking premium status only.
 * 
 * @returns {boolean} Whether user is on a premium tier
 */
export function useIsPremium() {
    return useSubscriptionStore(selectIsPremium);
}

export default useFeatureAccess;
