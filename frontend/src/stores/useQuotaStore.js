import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { quotaApi, getApiError } from '../lib/api';
import { useAuthStore } from './index';

/**
 * Quota store (SPEC-F15). Wraps the tier-aware QuotaStatusView {premium, tierCode, globalLimit,
 * globalUsed, globalAiLimit, globalAiUsed, unlimited} and the can-attempt pre-check
 * (CanAttemptView {canAttempt, reason, blockType}). The backend quota model is global (no
 * per-skill breakdown), so progress helpers report global usage.
 */
const useQuotaStore = create(
  devtools(
    subscribeWithSelector((set, get) => ({
      quotaStatus: null,
      loading: false,
      error: null,
      lastFetched: null,
      preCheckResult: null,
      preCheckLoading: false,

      isPremium: () => get().quotaStatus?.premium === true || get().quotaStatus?.unlimited === true,

      getGlobalProgress: (isAI = false) => {
        const q = get().quotaStatus;
        if (!q || q.unlimited) return 0;
        const cap = isAI ? q.globalAiLimit : q.globalLimit;
        const used = isAI ? q.globalAiUsed : q.globalUsed;
        return cap > 0 ? Math.min(100, (used / cap) * 100) : 0;
      },

      getProgressColor: (percent) => (percent < 50 ? 'green' : percent < 80 ? 'yellow' : 'red'),

      fetchQuotaStatus: async (force = false) => {
        const user = useAuthStore.getState().user;
        if (!user) return;
        const { loading, lastFetched } = get();
        if (loading) return;
        if (!force && lastFetched && Date.now() - lastFetched < 30000) return;
        set({ loading: true, error: null }, false, 'fetchQuotaStatus/start');
        try {
          const quotaStatus = await quotaApi.status();
          set({ quotaStatus, loading: false, lastFetched: Date.now() }, false, 'fetchQuotaStatus/success');
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchQuotaStatus/error');
        }
      },

      preCheckAttempt: async (skill, isAI = false) => {
        const user = useAuthStore.getState().user;
        if (!user) return null;
        set({ preCheckLoading: true }, false, 'preCheckAttempt/start');
        try {
          const result = await quotaApi.canAttempt(skill, isAI);
          set({ preCheckResult: result, preCheckLoading: false }, false, 'preCheckAttempt/success');
          return result;
        } catch (error) {
          set({ preCheckLoading: false }, false, 'preCheckAttempt/error');
          return null;
        }
      },
    })),
    { name: 'QuotaStore' }
  )
);

export default useQuotaStore;
