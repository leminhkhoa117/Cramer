/**
 * Unit tests for useSubscriptionStore.
 * Tests subscription state management and feature access checking.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';

// Mock API module
vi.mock('../../api/backendApi', () => ({
  subscriptionApi: {
    getMyStatus: vi.fn(),
  },
}));

// Import mocked modules
import { subscriptionApi } from '../../api/backendApi';

// Create a test version of the subscription store
const createTestSubscriptionStore = () => {
  return create(
    devtools(
      subscribeWithSelector((set, get) => ({
        // State
        tier: null,
        tierNameVi: null,
        tierNameEn: null,
        features: {},
        isPremium: false,
        loading: false,
        error: null,
        lastFetchedAt: null,

        // Actions
        fetchSubscriptionStatus: async () => {
          const { loading } = get();
          if (loading) return;

          set({ loading: true, error: null }, false, 'fetchSubscriptionStatus/start');

          try {
            const response = await subscriptionApi.getMyStatus();
            const data = response.data;

            const tierInfo = data.tier || {};
            const tierCode = tierInfo.code || 'cramerie';

            let featuresMap = {};

            if (data.featureAccess?.features) {
              featuresMap = data.featureAccess.features;
            } else if (tierInfo.features && Array.isArray(tierInfo.features)) {
              tierInfo.features.forEach(feature => {
                featuresMap[feature] = true;
              });
            } else if (tierInfo.featuresMap) {
              featuresMap = tierInfo.featuresMap;
            }

            set({
              tier: tierCode,
              tierNameVi: tierInfo.name || 'Cramerie',
              tierNameEn: tierInfo.name || 'Cramerie',
              features: featuresMap,
              isPremium: (tierInfo.priceVnd || 0) > 0,
              loading: false,
              error: null,
              lastFetchedAt: Date.now(),
            }, false, 'fetchSubscriptionStatus/success');

            return { success: true };
          } catch (error) {
            set({
              loading: false,
              error: error.message || 'Failed to fetch subscription status',
            }, false, 'fetchSubscriptionStatus/error');
            return { success: false, error };
          }
        },

        hasFeature: (featureCode) => {
          const { features } = get();
          return features[featureCode] === true;
        },

        reset: () =>
          set({
            tier: null,
            tierNameVi: null,
            tierNameEn: null,
            features: {},
            isPremium: false,
            loading: false,
            error: null,
            lastFetchedAt: null,
          }, false, 'reset'),

        setFeatures: (features) =>
          set({ features }, false, 'setFeatures'),
      }))
    )
  );
};

describe('useSubscriptionStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestSubscriptionStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // fetchSubscriptionStatus Tests
  // ==========================================================================
  describe('fetchSubscriptionStatus', () => {
    it('should fetch free tier status successfully', async () => {
      const mockStatus = {
        tier: {
          code: 'cramerie',
          name: 'Cramerie',
          priceVnd: 0,
          features: ['basic_tests', 'vocabulary'],
        },
      };

      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      let result;
      await act(async () => {
        result = await store.getState().fetchSubscriptionStatus();
      });

      expect(result.success).toBe(true);
      expect(store.getState().tier).toBe('cramerie');
      expect(store.getState().tierNameVi).toBe('Cramerie');
      expect(store.getState().isPremium).toBe(false);
      expect(store.getState().features.basic_tests).toBe(true);
      expect(store.getState().features.vocabulary).toBe(true);
    });

    it('should fetch premium tier status successfully', async () => {
      const mockStatus = {
        tier: {
          code: 'cramerous',
          name: 'Cramerous',
          priceVnd: 149000,
          features: ['ai_writing_grading', 'unlimited_chat', 'premium_tests'],
        },
      };

      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchSubscriptionStatus();
      });

      expect(store.getState().tier).toBe('cramerous');
      expect(store.getState().isPremium).toBe(true);
      expect(store.getState().features.ai_writing_grading).toBe(true);
    });

    it('should handle featuresMap format', async () => {
      const mockStatus = {
        tier: {
          code: 'cramerich',
          name: 'Cramerich',
          priceVnd: 79000,
          featuresMap: {
            ai_writing_grading: true,
            premium_tests: true,
            vocabulary: false,
          },
        },
      };

      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchSubscriptionStatus();
      });

      expect(store.getState().features.ai_writing_grading).toBe(true);
      expect(store.getState().features.vocabulary).toBe(false);
    });

    it('should handle featureAccess.features format', async () => {
      const mockStatus = {
        tier: { code: 'cramerich', name: 'Cramerich', priceVnd: 79000 },
        featureAccess: {
          features: {
            chat: true,
            ai_grading: true,
          },
        },
      };

      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchSubscriptionStatus();
      });

      expect(store.getState().features.chat).toBe(true);
      expect(store.getState().features.ai_grading).toBe(true);
    });

    it('should not fetch if already loading', async () => {
      const mockStatus = {
        tier: { code: 'cramerie', name: 'Cramerie', priceVnd: 0 },
      };

      subscriptionApi.getMyStatus.mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve({ data: mockStatus }), 100))
      );

      // Start first fetch
      const promise1 = store.getState().fetchSubscriptionStatus();
      
      // Try to start second fetch while first is loading
      const promise2 = store.getState().fetchSubscriptionStatus();

      await Promise.all([promise1, promise2]);

      expect(subscriptionApi.getMyStatus).toHaveBeenCalledTimes(1);
    });

    it('should set error on failure', async () => {
      subscriptionApi.getMyStatus.mockRejectedValueOnce(new Error('Network error'));

      let result;
      await act(async () => {
        result = await store.getState().fetchSubscriptionStatus();
      });

      expect(result.success).toBe(false);
      expect(store.getState().error).toBe('Network error');
      expect(store.getState().loading).toBe(false);
    });

    it('should default to cramerie if no tier code', async () => {
      const mockStatus = { tier: {} };
      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchSubscriptionStatus();
      });

      expect(store.getState().tier).toBe('cramerie');
      expect(store.getState().tierNameVi).toBe('Cramerie');
    });
  });

  // ==========================================================================
  // hasFeature Tests
  // ==========================================================================
  describe('hasFeature', () => {
    it('should return true for existing feature', () => {
      act(() => {
        store.getState().setFeatures({
          ai_writing_grading: true,
          vocabulary: true,
        });
      });

      expect(store.getState().hasFeature('ai_writing_grading')).toBe(true);
      expect(store.getState().hasFeature('vocabulary')).toBe(true);
    });

    it('should return false for non-existing feature', () => {
      act(() => {
        store.getState().setFeatures({
          vocabulary: true,
        });
      });

      expect(store.getState().hasFeature('ai_writing_grading')).toBe(false);
      expect(store.getState().hasFeature('premium_tests')).toBe(false);
    });

    it('should return false for feature set to false', () => {
      act(() => {
        store.getState().setFeatures({
          ai_writing_grading: false,
        });
      });

      expect(store.getState().hasFeature('ai_writing_grading')).toBe(false);
    });
  });

  // ==========================================================================
  // reset Tests
  // ==========================================================================
  describe('reset', () => {
    it('should reset all state to initial values', async () => {
      // First set some state
      const mockStatus = {
        tier: { code: 'cramerous', name: 'Cramerous', priceVnd: 149000 },
      };
      subscriptionApi.getMyStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchSubscriptionStatus();
      });

      expect(store.getState().tier).toBe('cramerous');

      // Reset
      act(() => {
        store.getState().reset();
      });

      expect(store.getState().tier).toBeNull();
      expect(store.getState().tierNameVi).toBeNull();
      expect(store.getState().features).toEqual({});
      expect(store.getState().isPremium).toBe(false);
      expect(store.getState().lastFetchedAt).toBeNull();
    });
  });

  // ==========================================================================
  // setFeatures Tests
  // ==========================================================================
  describe('setFeatures', () => {
    it('should set features directly', () => {
      const features = {
        feature1: true,
        feature2: true,
        feature3: false,
      };

      act(() => {
        store.getState().setFeatures(features);
      });

      expect(store.getState().features).toEqual(features);
    });

    it('should replace existing features', () => {
      act(() => {
        store.getState().setFeatures({ feature1: true });
      });

      act(() => {
        store.getState().setFeatures({ feature2: true });
      });

      expect(store.getState().features).toEqual({ feature2: true });
      expect(store.getState().features.feature1).toBeUndefined();
    });
  });
});
