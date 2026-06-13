import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { dashboardApi, getApiError } from '../lib/api';

/**
 * Dashboard read-model store (SPEC-F16). Wraps GET /dashboard/summary (SummaryView) and
 * /dashboard/course-history + target upsert. 5-minute cache.
 */
const useDashboardStore = create(
  devtools(
    (set, get) => ({
      summary: null,
      loading: false,
      error: null,
      lastFetchedAt: null,
      page: 0,
      size: 20,
      search: '',

      fetchSummary: async (force = false) => {
        const { loading, lastFetchedAt, page, size, search } = get();
        if (loading) return;
        if (!force && lastFetchedAt && Date.now() - lastFetchedAt < 5 * 60 * 1000) return;
        set({ loading: true, error: null }, false, 'fetchSummary/start');
        try {
          const summary = await dashboardApi.summary({ page, size, search: search || undefined });
          set({ summary, loading: false, lastFetchedAt: Date.now() }, false, 'fetchSummary/success');
          return summary;
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchSummary/error');
          return null;
        }
      },

      refreshSummary: () => get().fetchSummary(true),

      setSearch: (search) => set({ search }, false, 'setSearch'),
      setPage: (page) => { set({ page }, false, 'setPage'); return get().fetchSummary(true); },

      courseHistory: async (params) => {
        try {
          return await dashboardApi.courseHistory(params);
        } catch (error) {
          set({ error: getApiError(error).message }, false, 'courseHistory/error');
          return [];
        }
      },

      saveTarget: async (payload) => {
        try {
          const target = await dashboardApi.setTarget(payload);
          set((s) => ({ summary: s.summary ? { ...s.summary, target } : s.summary }), false, 'saveTarget');
          return target;
        } catch (error) {
          set({ error: getApiError(error).message }, false, 'saveTarget/error');
          throw error;
        }
      },

      reset: () => set({ summary: null, loading: false, error: null, lastFetchedAt: null, page: 0, search: '' }, false, 'reset'),
    }),
    { name: 'DashboardStore' }
  )
);

export default useDashboardStore;
