import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { subscriptionApi, creditsApi, chatApi } from '../api/backendApi';
import { useAuthStore } from './index';

/**
 * Zustand store for user stats (subscription, credits, chat usage).
 * Used by FloatingAssistant widget for displaying balance and tier.
 */
const useUserStatsStore = create(
  devtools(
    subscribeWithSelector((set, get) => ({
      // ===== STATE =====
      // Subscription
      subscription: null,
      tiers: [],

      // Credits (Lúa)
      credits: {
        balance: 0,
        lifetimeEarned: 0,
        lifetimeSpent: 0,
      },

      // Chat usage
      chatUsage: {
        usedToday: 0,
        dailyLimit: 20,
        remainingToday: 20,
      },

      // Grading status
      gradingStatus: {
        canGrade: false,
        monthlyLimit: 0,
        usedThisMonth: 0,
        remaining: 0,
      },

      // Loading states
      loading: false,
      error: null,
      lastFetched: null,

      // ===== SELECTORS =====
      getTierEmoji: () => {
        const { subscription } = get();
        if (!subscription?.tier) return '🌾';
        // Backend returns nameVi (e.g., "Cramerich") or code (e.g., "cramerich")
        const tierCode = subscription.tier.code?.toLowerCase() || '';
        const tierName = subscription.tier.nameVi?.toLowerCase() || subscription.tier.nameEn?.toLowerCase() || '';
        if (tierCode.includes('cramerous') || tierName.includes('cramerous')) return '🌟';
        if (tierCode.includes('cramerich') || tierName.includes('cramerich')) return '🌻';
        return '🌾';
      },

      getTierName: () => {
        const { subscription } = get();
        // Backend returns nameVi for Vietnamese name, fallback to nameEn then code
        return subscription?.tier?.nameVi || subscription?.tier?.nameEn || subscription?.tier?.code || 'Cramerie';
      },

      // ===== ACTIONS =====

      /**
       * Fetch all user stats (subscription, credits, chat usage)
       */
      fetchUserStats: async () => {
        const user = useAuthStore.getState().user;
        if (!user) {
          console.log('⏭️ Skipping fetchUserStats - no user');
          return;
        }

        const { loading, lastFetched } = get();

        // Debounce: don't fetch if already loading or fetched within last 30 seconds
        if (loading) {
          console.log('⏭️ Skipping fetchUserStats - already loading');
          return;
        }

        if (lastFetched && Date.now() - lastFetched < 30000) {
          console.log('⏭️ Skipping fetchUserStats - cached (30s TTL)');
          return;
        }

        set({ loading: true, error: null }, false, 'fetchUserStats/start');

        try {
          // Fetch all data in parallel
          const [subRes, creditsRes, chatRes, tiersRes, gradingRes] = await Promise.allSettled([
            subscriptionApi.getCurrent(),
            creditsApi.getBalance(),
            chatApi.getRemainingQuestions(),
            subscriptionApi.getTiers(),
            subscriptionApi.getGradingStatus(),
          ]);

          // Process subscription
          if (subRes.status === 'fulfilled' && subRes.value?.data) {
            set({ subscription: subRes.value.data }, false, 'fetchUserStats/subscription');
          }

          // Process credits
          if (creditsRes.status === 'fulfilled' && creditsRes.value?.data) {
            set({
              credits: {
                balance: creditsRes.value.data.balance || 0,
                lifetimeEarned: creditsRes.value.data.lifetimeEarned || 0,
                lifetimeSpent: creditsRes.value.data.lifetimeSpent || 0,
              }
            }, false, 'fetchUserStats/credits');
          }

          // Process chat usage - API returns { remaining, unlimited }
          // Now using MONTHLY limits (chatbot_monthly_limit) instead of daily
          if (chatRes.status === 'fulfilled' && chatRes.value?.data) {
            const data = chatRes.value.data;
            const isUnlimited = data.unlimited === true || data.remaining < 0;
            const remainingValue = isUnlimited ? -1 : (data.remaining ?? 50);

            // Get monthly limit from subscription tier if available
            const { subscription } = get();
            const monthlyLimit = isUnlimited ? -1 : (subscription?.tier?.chatbotMonthlyLimit || 50);
            const usedThisMonth = isUnlimited ? 0 : Math.max(0, monthlyLimit - remainingValue);

            set({
              chatUsage: {
                usedThisMonth,
                monthlyLimit,
                remainingThisMonth: remainingValue,
                // Keep legacy field names for backward compatibility
                usedToday: usedThisMonth,
                dailyLimit: monthlyLimit,
                remainingToday: remainingValue,
              }
            }, false, 'fetchUserStats/chatUsage');
          }

          // Process tiers
          if (tiersRes.status === 'fulfilled' && tiersRes.value?.data) {
            set({ tiers: tiersRes.value.data }, false, 'fetchUserStats/tiers');
          }

          // Process grading status
          if (gradingRes.status === 'fulfilled' && gradingRes.value?.data) {
            set({ gradingStatus: gradingRes.value.data }, false, 'fetchUserStats/gradingStatus');
          }

          set({ loading: false, lastFetched: Date.now() }, false, 'fetchUserStats/complete');
          console.log('✅ User stats fetched successfully');

        } catch (error) {
          console.error('❌ Error fetching user stats:', error);
          set({
            loading: false,
            error: error.message || 'Failed to load user stats'
          }, false, 'fetchUserStats/error');
        }
      },

      /**
       * Refresh credits balance only
       */
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
          console.error('❌ Error refreshing credits:', error);
        }
      },

      /**
       * Refresh chat usage only - API returns { remaining, unlimited }
       */
      refreshChatUsage: async () => {
        try {
          const response = await chatApi.getRemainingQuestions();
          if (response?.data) {
            const data = response.data;
            const isUnlimited = data.unlimited === true || data.remaining < 0;
            const remainingValue = isUnlimited ? -1 : (data.remaining ?? 20);

            // Get daily limit from current subscription
            const { subscription } = get();
            const dailyLimit = isUnlimited ? -1 : (subscription?.tier?.dailyChatLimit || 20);
            const usedToday = isUnlimited ? 0 : Math.max(0, dailyLimit - remainingValue);

            set({
              chatUsage: {
                usedToday,
                dailyLimit,
                remainingToday: remainingValue,
              }
            }, false, 'refreshChatUsage');
          }
        } catch (error) {
          console.error('❌ Error refreshing chat usage:', error);
        }
      },

      /**
       * Increment chat usage locally (optimistic update)
       */
      incrementChatUsage: () => {
        const { chatUsage } = get();
        set({
          chatUsage: {
            ...chatUsage,
            usedToday: chatUsage.usedToday + 1,
            remainingToday: Math.max(0, chatUsage.remainingToday - 1),
          }
        }, false, 'incrementChatUsage');
      },

      /**
       * Deduct credits locally (optimistic update)
       */
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

      /**
       * Clear all stats (on logout)
       */
      clearStats: () => {
        set({
          subscription: null,
          tiers: [],
          credits: { balance: 0, lifetimeEarned: 0, lifetimeSpent: 0 },
          chatUsage: { usedToday: 0, dailyLimit: 20, remainingToday: 20 },
          gradingStatus: { canGrade: false, monthlyLimit: 0, usedThisMonth: 0, remaining: 0 },
          loading: false,
          error: null,
          lastFetched: null,
        }, false, 'clearStats');
      },
    })),
    { name: 'UserStatsStore' }
  )
);

// Auto-clear stats when user logs out
useAuthStore.subscribe(
  (state) => state.user,
  (user, prevUser) => {
    if (prevUser && !user) {
      // User logged out
      useUserStatsStore.getState().clearStats();
    } else if (user && !prevUser) {
      // User logged in - fetch stats
      useUserStatsStore.getState().fetchUserStats();
    }
  }
);

export default useUserStatsStore;
