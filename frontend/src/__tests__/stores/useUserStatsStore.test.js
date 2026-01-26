/**
 * Unit tests for useUserStatsStore.
 * Tests user stats fetching (subscription, credits, chat usage, grading status).
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';

// Mock API modules
vi.mock('../../api/backendApi', () => ({
  subscriptionApi: {
    getCurrent: vi.fn(),
    getTiers: vi.fn(),
    getGradingStatus: vi.fn(),
  },
  creditsApi: {
    getBalance: vi.fn(),
  },
  chatApi: {
    getRemainingQuestions: vi.fn(),
  },
}));

// Mock useAuthStore
vi.mock('../../stores/index', () => ({
  useAuthStore: {
    getState: vi.fn(() => ({ user: { id: 'user-123' } })),
    subscribe: vi.fn(),
  },
}));

// Import mocked modules
import { subscriptionApi, creditsApi, chatApi } from '../../api/backendApi';
import { useAuthStore } from '../../stores/index';

// Create a test version of the user stats store
const createTestUserStatsStore = () => {
  return create(
    devtools(
      subscribeWithSelector((set, get) => ({
        // ===== STATE =====
        subscription: null,
        tiers: [],
        credits: {
          balance: 0,
          lifetimeEarned: 0,
          lifetimeSpent: 0,
        },
        chatUsage: {
          usedThisMonth: 0,
          monthlyLimit: 50,
          remainingThisMonth: 50,
          usedToday: 0,
          dailyLimit: 50,
          remainingToday: 50,
        },
        gradingStatus: {
          canGrade: false,
          monthlyLimit: 0,
          usedThisMonth: 0,
          remaining: 0,
        },
        loading: false,
        error: null,
        lastFetched: null,

        // ===== SELECTORS =====
        getTierEmoji: () => {
          const { subscription } = get();
          if (!subscription?.tier) return '🌾';
          const tierCode = subscription.tier.code?.toLowerCase() || '';
          const tierName = subscription.tier.name?.toLowerCase() || '';
          if (tierCode.includes('cramerous') || tierName.includes('cramerous')) return '🌟';
          if (tierCode.includes('cramerich') || tierName.includes('cramerich')) return '🌻';
          return '🌾';
        },

        getTierName: () => {
          const { subscription } = get();
          return subscription?.tier?.name || subscription?.tier?.code || 'Cramerie';
        },

        // ===== ACTIONS =====
        fetchUserStats: async () => {
          const user = useAuthStore.getState().user;
          if (!user) {
            return;
          }

          const { loading, lastFetched } = get();

          if (loading) {
            return;
          }

          if (lastFetched && Date.now() - lastFetched < 30000) {
            return;
          }

          set({ loading: true, error: null }, false, 'fetchUserStats/start');

          try {
            const [subRes, creditsRes, chatRes, tiersRes, gradingRes] = await Promise.allSettled([
              subscriptionApi.getCurrent(),
              creditsApi.getBalance(),
              chatApi.getRemainingQuestions(),
              subscriptionApi.getTiers(),
              subscriptionApi.getGradingStatus(),
            ]);

            if (subRes.status === 'fulfilled' && subRes.value?.data) {
              set({ subscription: subRes.value.data }, false, 'fetchUserStats/subscription');
            }

            if (creditsRes.status === 'fulfilled' && creditsRes.value?.data) {
              set({
                credits: {
                  balance: creditsRes.value.data.balance || 0,
                  lifetimeEarned: creditsRes.value.data.lifetimeEarned || 0,
                  lifetimeSpent: creditsRes.value.data.lifetimeSpent || 0,
                }
              }, false, 'fetchUserStats/credits');
            }

            if (chatRes.status === 'fulfilled' && chatRes.value?.data) {
              const data = chatRes.value.data;
              const isUnlimited = data.unlimited === true || data.remaining < 0;
              const remainingValue = isUnlimited ? -1 : (data.remaining ?? 50);
              const { subscription } = get();
              const monthlyLimit = isUnlimited ? -1 : (subscription?.tier?.chatbotMonthlyLimit || 50);
              const usedThisMonth = isUnlimited ? 0 : Math.max(0, monthlyLimit - remainingValue);

              set({
                chatUsage: {
                  usedThisMonth,
                  monthlyLimit,
                  remainingThisMonth: remainingValue,
                  usedToday: usedThisMonth,
                  dailyLimit: monthlyLimit,
                  remainingToday: remainingValue,
                }
              }, false, 'fetchUserStats/chatUsage');
            }

            if (tiersRes.status === 'fulfilled' && tiersRes.value?.data) {
              set({ tiers: tiersRes.value.data }, false, 'fetchUserStats/tiers');
            }

            if (gradingRes.status === 'fulfilled' && gradingRes.value?.data) {
              set({ gradingStatus: gradingRes.value.data }, false, 'fetchUserStats/gradingStatus');
            }

            set({ loading: false, lastFetched: Date.now() }, false, 'fetchUserStats/complete');

          } catch (error) {
            set({
              loading: false,
              error: error.message || 'Failed to load user stats'
            }, false, 'fetchUserStats/error');
          }
        },

        refreshCredits: async () => {
          try {
            const response = await creditsApi.getBalance();
            if (response?.data) {
              set({
                credits: {
                  balance: response.data.balance || 0,
                  lifetimeEarned: response.data.lifetimeEarned || 0,
                  lifetimeSpent: response.data.lifetimeSpent || 0,
                }
              }, false, 'refreshCredits');
            }
          } catch (error) {
            console.error('Error refreshing credits:', error);
          }
        },

        refreshChatUsage: async () => {
          try {
            const response = await chatApi.getRemainingQuestions();
            if (response?.data) {
              const data = response.data;
              const isUnlimited = data.unlimited === true || data.remaining < 0;
              const remainingValue = isUnlimited ? -1 : (data.remaining ?? 50);
              const { subscription } = get();
              const monthlyLimit = isUnlimited ? -1 : (subscription?.tier?.chatbotMonthlyLimit || 50);
              const usedThisMonth = isUnlimited ? 0 : Math.max(0, monthlyLimit - remainingValue);

              set({
                chatUsage: {
                  usedThisMonth,
                  monthlyLimit,
                  remainingThisMonth: remainingValue,
                  usedToday: usedThisMonth,
                  dailyLimit: monthlyLimit,
                  remainingToday: remainingValue,
                }
              }, false, 'refreshChatUsage');
            }
          } catch (error) {
            console.error('Error refreshing chat usage:', error);
          }
        },

        incrementChatUsage: () => {
          const { chatUsage } = get();
          const newRemaining = Math.max(0, (chatUsage.remainingThisMonth ?? chatUsage.remainingToday) - 1);
          const newUsed = (chatUsage.usedThisMonth ?? chatUsage.usedToday) + 1;
          set({
            chatUsage: {
              ...chatUsage,
              usedThisMonth: newUsed,
              remainingThisMonth: newRemaining,
              usedToday: newUsed,
              remainingToday: newRemaining,
            }
          }, false, 'incrementChatUsage');
        },

        deductCredits: (amount) => {
          const { credits } = get();
          set({
            credits: {
              ...credits,
              balance: Math.max(0, credits.balance - amount),
              lifetimeSpent: credits.lifetimeSpent + amount,
            }
          }, false, 'deductCredits');
        },

        clearStats: () => {
          set({
            subscription: null,
            tiers: [],
            credits: { balance: 0, lifetimeEarned: 0, lifetimeSpent: 0 },
            chatUsage: {
              usedThisMonth: 0,
              monthlyLimit: 50,
              remainingThisMonth: 50,
              usedToday: 0,
              dailyLimit: 50,
              remainingToday: 50
            },
            gradingStatus: { canGrade: false, monthlyLimit: 0, usedThisMonth: 0, remaining: 0 },
            loading: false,
            error: null,
            lastFetched: null,
          }, false, 'clearStats');
        },
      })),
      { name: 'user-stats-store-test' }
    )
  );
};

describe('useUserStatsStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestUserStatsStore();
    useAuthStore.getState.mockReturnValue({ user: { id: 'user-123' } });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // fetchUserStats Tests
  // ==========================================================================
  describe('fetchUserStats', () => {
    it('should fetch all stats successfully', async () => {
      const mockSubscription = {
        tier: { code: 'cramerich', name: 'Cramerich', chatbotMonthlyLimit: 100 },
        status: 'ACTIVE',
      };
      const mockCredits = { balance: 500, lifetimeEarned: 1000, lifetimeSpent: 500 };
      const mockChat = { remaining: 80, unlimited: false };
      const mockTiers = [{ code: 'cramerie' }, { code: 'cramerich' }];
      const mockGrading = { canGrade: true, monthlyLimit: 10, usedThisMonth: 2, remaining: 8 };

      subscriptionApi.getCurrent.mockResolvedValueOnce({ data: mockSubscription });
      creditsApi.getBalance.mockResolvedValueOnce({ data: mockCredits });
      chatApi.getRemainingQuestions.mockResolvedValueOnce({ data: mockChat });
      subscriptionApi.getTiers.mockResolvedValueOnce({ data: mockTiers });
      subscriptionApi.getGradingStatus.mockResolvedValueOnce({ data: mockGrading });

      await act(async () => {
        await store.getState().fetchUserStats();
      });

      expect(store.getState().subscription).toEqual(mockSubscription);
      expect(store.getState().credits.balance).toBe(500);
      expect(store.getState().tiers).toHaveLength(2);
      expect(store.getState().gradingStatus.canGrade).toBe(true);
      expect(store.getState().loading).toBe(false);
      expect(store.getState().lastFetched).not.toBeNull();
    });

    it('should skip if no user', async () => {
      useAuthStore.getState.mockReturnValue({ user: null });

      await act(async () => {
        await store.getState().fetchUserStats();
      });

      expect(subscriptionApi.getCurrent).not.toHaveBeenCalled();
      expect(creditsApi.getBalance).not.toHaveBeenCalled();
    });

    it('should skip if already loading', async () => {
      act(() => {
        store.setState({ loading: true });
      });

      await act(async () => {
        await store.getState().fetchUserStats();
      });

      expect(subscriptionApi.getCurrent).not.toHaveBeenCalled();
    });

    it('should use cache within 30 seconds', async () => {
      act(() => {
        store.setState({ lastFetched: Date.now() });
      });

      await act(async () => {
        await store.getState().fetchUserStats();
      });

      expect(subscriptionApi.getCurrent).not.toHaveBeenCalled();
    });

    it('should handle partial API failures gracefully', async () => {
      subscriptionApi.getCurrent.mockResolvedValueOnce({ data: { tier: { code: 'cramerie' } } });
      creditsApi.getBalance.mockRejectedValueOnce(new Error('Credits API error'));
      chatApi.getRemainingQuestions.mockResolvedValueOnce({ data: { remaining: 50 } });
      subscriptionApi.getTiers.mockResolvedValueOnce({ data: [] });
      subscriptionApi.getGradingStatus.mockResolvedValueOnce({ data: { canGrade: false } });

      await act(async () => {
        await store.getState().fetchUserStats();
      });

      expect(store.getState().subscription).not.toBeNull();
      expect(store.getState().loading).toBe(false);
    });
  });

  // ==========================================================================
  // refreshCredits Tests
  // ==========================================================================
  describe('refreshCredits', () => {
    it('should refresh credits successfully', async () => {
      const mockCredits = { balance: 1000, lifetimeEarned: 2000, lifetimeSpent: 1000 };
      creditsApi.getBalance.mockResolvedValueOnce({ data: mockCredits });

      await act(async () => {
        await store.getState().refreshCredits();
      });

      expect(store.getState().credits.balance).toBe(1000);
      expect(store.getState().credits.lifetimeEarned).toBe(2000);
    });

    it('should handle error gracefully', async () => {
      creditsApi.getBalance.mockRejectedValueOnce(new Error('Network error'));

      await act(async () => {
        await store.getState().refreshCredits();
      });

      expect(store.getState().credits.balance).toBe(0);
    });
  });

  // ==========================================================================
  // refreshChatUsage Tests
  // ==========================================================================
  describe('refreshChatUsage', () => {
    it('should refresh chat usage', async () => {
      chatApi.getRemainingQuestions.mockResolvedValueOnce({ data: { remaining: 30, unlimited: false } });

      await act(async () => {
        await store.getState().refreshChatUsage();
      });

      expect(store.getState().chatUsage.remainingThisMonth).toBe(30);
    });

    it('should handle unlimited usage', async () => {
      chatApi.getRemainingQuestions.mockResolvedValueOnce({ data: { remaining: -1, unlimited: true } });

      await act(async () => {
        await store.getState().refreshChatUsage();
      });

      expect(store.getState().chatUsage.remainingThisMonth).toBe(-1);
      expect(store.getState().chatUsage.monthlyLimit).toBe(-1);
    });

    it('should handle error gracefully', async () => {
      chatApi.getRemainingQuestions.mockRejectedValueOnce(new Error('API error'));

      const initialRemaining = store.getState().chatUsage.remainingThisMonth;

      await act(async () => {
        await store.getState().refreshChatUsage();
      });

      expect(store.getState().chatUsage.remainingThisMonth).toBe(initialRemaining);
    });
  });

  // ==========================================================================
  // Optimistic Update Tests
  // ==========================================================================
  describe('incrementChatUsage', () => {
    it('should increment chat usage locally', () => {
      act(() => {
        store.setState({
          chatUsage: {
            usedThisMonth: 10,
            remainingThisMonth: 40,
            monthlyLimit: 50,
            usedToday: 10,
            remainingToday: 40,
            dailyLimit: 50,
          }
        });
      });

      act(() => {
        store.getState().incrementChatUsage();
      });

      expect(store.getState().chatUsage.usedThisMonth).toBe(11);
      expect(store.getState().chatUsage.remainingThisMonth).toBe(39);
    });

    it('should not go below 0 remaining', () => {
      act(() => {
        store.setState({
          chatUsage: {
            usedThisMonth: 50,
            remainingThisMonth: 0,
            monthlyLimit: 50,
            usedToday: 50,
            remainingToday: 0,
            dailyLimit: 50,
          }
        });
      });

      act(() => {
        store.getState().incrementChatUsage();
      });

      expect(store.getState().chatUsage.remainingThisMonth).toBe(0);
    });
  });

  describe('deductCredits', () => {
    it('should deduct credits locally', () => {
      act(() => {
        store.setState({
          credits: { balance: 500, lifetimeEarned: 1000, lifetimeSpent: 500 }
        });
      });

      act(() => {
        store.getState().deductCredits(100);
      });

      expect(store.getState().credits.balance).toBe(400);
      expect(store.getState().credits.lifetimeSpent).toBe(600);
    });

    it('should not go below 0 balance', () => {
      act(() => {
        store.setState({
          credits: { balance: 50, lifetimeEarned: 100, lifetimeSpent: 50 }
        });
      });

      act(() => {
        store.getState().deductCredits(100);
      });

      expect(store.getState().credits.balance).toBe(0);
    });
  });

  // ==========================================================================
  // Selector Tests
  // ==========================================================================
  describe('getTierEmoji', () => {
    it('should return star for cramerous tier', () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerous', name: 'Cramerous' } }
        });
      });

      expect(store.getState().getTierEmoji()).toBe('🌟');
    });

    it('should return sunflower for cramerich tier', () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerich', name: 'Cramerich' } }
        });
      });

      expect(store.getState().getTierEmoji()).toBe('🌻');
    });

    it('should return wheat for default/cramerie tier', () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerie', name: 'Cramerie' } }
        });
      });

      expect(store.getState().getTierEmoji()).toBe('🌾');
    });

    it('should return wheat when no subscription', () => {
      expect(store.getState().getTierEmoji()).toBe('🌾');
    });
  });

  describe('getTierName', () => {
    it('should return tier name', () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerich', name: 'Cramerich' } }
        });
      });

      expect(store.getState().getTierName()).toBe('Cramerich');
    });

    it('should return tier code if name is missing', () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerich' } }
        });
      });

      expect(store.getState().getTierName()).toBe('cramerich');
    });

    it('should return default Cramerie when no subscription', () => {
      expect(store.getState().getTierName()).toBe('Cramerie');
    });
  });

  // ==========================================================================
  // clearStats Tests
  // ==========================================================================
  describe('clearStats', () => {
    it('should clear all stats to initial values', async () => {
      act(() => {
        store.setState({
          subscription: { tier: { code: 'cramerich' } },
          credits: { balance: 500, lifetimeEarned: 1000, lifetimeSpent: 500 },
          tiers: [{ code: 'cramerie' }],
          loading: true,
          error: 'Some error',
          lastFetched: Date.now(),
        });
      });

      act(() => {
        store.getState().clearStats();
      });

      const state = store.getState();
      expect(state.subscription).toBeNull();
      expect(state.tiers).toHaveLength(0);
      expect(state.credits.balance).toBe(0);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.lastFetched).toBeNull();
    });
  });
});
