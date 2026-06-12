import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { subscriptionApi, getApiError } from '../lib/api';

/** Default Cramerie (free) features when a tier exposes none (SPEC-15 §7). */
const FREE_FEATURES = ['limited_tests', 'normal_grading', 'vocabulary', 'basic_progress'];

function featuresToMap(features) {
  const map = {};
  if (Array.isArray(features)) {
    features.forEach((f) => { map[f] = true; });
  } else if (features && typeof features === 'object') {
    Object.entries(features).forEach(([k, v]) => { map[k] = !!v; });
  }
  return map;
}

/**
 * Subscription / feature-access store (SPEC-F15). Current status (SubscriptionStatusView) gives
 * tierCode + premium; features are read from the matching TierView.features.
 */
const useSubscriptionStore = create(
  devtools(
    subscribeWithSelector((set, get) => ({
      tier: null,
      tierName: null,
      features: {},
      isPremium: false,
      status: null,
      loading: false,
      error: null,
      lastFetchedAt: null,

      fetchSubscriptionStatus: async (force = false) => {
        const { loading, lastFetchedAt } = get();
        if (loading) return { success: true };
        if (!force && lastFetchedAt && Date.now() - lastFetchedAt < 60000) return { success: true };
        set({ loading: true, error: null }, false, 'fetchSubscriptionStatus/start');
        try {
          const [status, tiers] = await Promise.all([
            subscriptionApi.current(),
            subscriptionApi.tiers().catch(() => []),
          ]);
          const tier = (tiers || []).find((t) => t.code === status.tierCode);
          let featuresMap = featuresToMap(tier?.features);
          if (Object.keys(featuresMap).length === 0 && !status.premium) {
            featuresMap = featuresToMap(FREE_FEATURES);
          }
          set({
            tier: status.tierCode,
            tierName: status.tierName,
            features: featuresMap,
            isPremium: !!status.premium,
            status,
            loading: false,
            lastFetchedAt: Date.now(),
          }, false, 'fetchSubscriptionStatus/success');
          return { success: true };
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchSubscriptionStatus/error');
          return { success: false };
        }
      },

      hasFeature: (code) => get().features[code] === true,

      reset: () => set({ tier: null, tierName: null, features: {}, isPremium: false, status: null, lastFetchedAt: null }, false, 'reset'),
    })),
    { name: 'SubscriptionStore' }
  )
);

export const selectHasFeature = (code) => (state) => state.features[code] === true;
export const selectTier = (state) => state.tier;
export const selectTierName = (state) => state.tierName;
export const selectFeatures = (state) => state.features;
export const selectIsPremium = (state) => state.isPremium;
export const selectLoading = (state) => state.loading;
export default useSubscriptionStore;
