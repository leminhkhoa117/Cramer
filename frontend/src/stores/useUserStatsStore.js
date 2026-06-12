import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { subscriptionApi, creditApi, chatApi, getApiError } from '../lib/api';
import { useAuthStore } from './index';

const EMOJI = { cramerous: '🌟', cramerich: '🌻', cramerie: '🌾' };

/**
 * User stats store (SPEC-F15) for the FloatingAssistant + subscription widgets: subscription
 * status (flat SubscriptionStatusView), Lúa balance (CreditStatsView), chat remaining, tiers,
 * grading status. 30s cache.
 */
const useUserStatsStore = create(
  devtools(
    subscribeWithSelector((set, get) => ({
      subscription: null,   // SubscriptionStatusView
      tiers: [],
      credits: { balance: 0, lifetime: 0, spent: 0 },
      chat: { remaining: 50, limit: 50, unlimited: false },
      grading: { aiGradingEnabled: false, gradingsRemaining: 0 },
      loading: false,
      error: null,
      lastFetched: null,

      getTierEmoji: () => EMOJI[get().subscription?.tierCode] || '🌾',
      getTierName: () => get().subscription?.tierName || 'Cramerie',
      isPremium: () => get().subscription?.premium === true,

      fetchUserStats: async (force = false) => {
        const user = useAuthStore.getState().user;
        if (!user) return;
        const { loading, lastFetched } = get();
        if (loading) return;
        if (!force && lastFetched && Date.now() - lastFetched < 30000) return;
        set({ loading: true, error: null }, false, 'fetchUserStats/start');
        try {
          const [sub, credits, chat, tiers, grading] = await Promise.allSettled([
            subscriptionApi.current(),
            creditApi.stats(),
            chatApi.remaining(),
            subscriptionApi.tiers(),
            subscriptionApi.gradingStatus(),
          ]);
          const patch = { loading: false, lastFetched: Date.now() };
          if (sub.status === 'fulfilled') patch.subscription = sub.value;
          if (credits.status === 'fulfilled') {
            patch.credits = {
              balance: credits.value.balance ?? 0,
              lifetime: credits.value.lifetime ?? 0,
              spent: credits.value.spent ?? 0,
            };
          }
          if (chat.status === 'fulfilled') {
            const remaining = chat.value.remaining ?? 50;
            const unlimited = remaining < 0;
            const limit = sub.status === 'fulfilled' ? (sub.value.chatMonthlyLimit ?? 50) : 50;
            patch.chat = { remaining, unlimited, limit };
          }
          if (tiers.status === 'fulfilled') patch.tiers = tiers.value;
          if (grading.status === 'fulfilled') patch.grading = grading.value;
          set(patch, false, 'fetchUserStats/complete');
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchUserStats/error');
        }
      },

      refreshCredits: async () => {
        try {
          const c = await creditApi.stats();
          set({ credits: { balance: c.balance ?? 0, lifetime: c.lifetime ?? 0, spent: c.spent ?? 0 } }, false, 'refreshCredits');
        } catch { /* ignore */ }
      },

      refreshChat: async () => {
        try {
          const { remaining = 50 } = await chatApi.remaining();
          set((s) => ({ chat: { ...s.chat, remaining, unlimited: remaining < 0 } }), false, 'refreshChat');
        } catch { /* ignore */ }
      },

      reset: () => set({
        subscription: null, tiers: [], credits: { balance: 0, lifetime: 0, spent: 0 },
        chat: { remaining: 50, limit: 50, unlimited: false }, grading: { aiGradingEnabled: false, gradingsRemaining: 0 },
        lastFetched: null,
      }, false, 'reset'),
    })),
    { name: 'UserStatsStore' }
  )
);

export default useUserStatsStore;
