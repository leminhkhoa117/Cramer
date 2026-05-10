import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { dashboardApi } from '../api/backendApi';

const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

const useDashboardStore = create(
  devtools(
    persist(
      (set, get) => ({
        // State
        summary: null,
        loading: false,
        error: null,
        lastFetchedAt: null,

        // Pagination State
        currentPage: 0,
        pageSize: 10, // 10 items — compact list layout
        totalPages: 0,
        searchQuery: '',
        debouncedSearchQuery: '',

        // Selectors
        isStale: () => {
          const { lastFetchedAt } = get();
          if (!lastFetchedAt) return true;
          const now = Date.now();
          const fetchedTime = new Date(lastFetchedAt).getTime();
          return now - fetchedTime > CACHE_TTL_MS;
        },

        // Actions
        fetchSummary: async (page = null, size = null, search = null) => {
          const state = get();
          const actualPage = page ?? state.currentPage;
          const actualSize = size ?? state.pageSize;
          const actualSearch = search ?? state.debouncedSearchQuery;

          // Don't use cache when pagination/search params are provided
          const useCache = page === null && size === null && search === null;
          
          // Return cached data if fresh and not currently loading
          if (useCache && !state.isStale() && state.summary && !state.loading) {
            return state.summary;
          }

          set({ loading: true, error: null });

          try {
            const response = await dashboardApi.getSummary(actualPage, actualSize, actualSearch);
            const data = response.data;
            
            // Update pagination info from response
            const totalPages = data.courseProgress?.totalPages ?? 0;
            
            set({
              summary: data,
              loading: false,
              lastFetchedAt: new Date().toISOString(),
              error: null,
              totalPages,
            });
            return data;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch dashboard summary';
            set({
              loading: false,
              error: errorMessage,
            });
            throw err;
          }
        },

        refreshSummary: async () => {
          const { currentPage, pageSize, debouncedSearchQuery } = get();
          set({ loading: true, error: null });

          try {
            const response = await dashboardApi.getSummary(currentPage, pageSize, debouncedSearchQuery);
            const data = response.data;
            
            // Update pagination info from response
            const totalPages = data.courseProgress?.totalPages ?? 0;
            
            set({
              summary: data,
              loading: false,
              lastFetchedAt: new Date().toISOString(),
              error: null,
              totalPages,
            });
            return data;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to refresh dashboard summary';
            set({
              loading: false,
              error: errorMessage,
            });
            throw err;
          }
        },

        setPage: (page) => {
          set({ currentPage: page });
        },

        setPageSize: (size) => {
          set({ pageSize: size, currentPage: 0 }); // Reset to first page when changing size
        },

        setSearchQuery: (query) => {
          set({ searchQuery: query });
        },

        setDebouncedSearchQuery: (query) => {
          set({ debouncedSearchQuery: query, currentPage: 0 }); // Reset to first page when searching
        },

        resetPagination: () => {
          set({
            currentPage: 0,
            pageSize: 10,
            totalPages: 0,
            searchQuery: '',
            debouncedSearchQuery: '',
          });
        },

        invalidateCache: () => {
          set({ lastFetchedAt: null });
        },

        // Update summary locally (for optimistic updates like goal saving)
        updateSummary: (updater) => {
          set((state) => ({
            summary: typeof updater === 'function' ? updater(state.summary) : updater,
          }));
        },
      }),
      {
        name: 'dashboard-storage',
        storage: {
          getItem: (name) => {
            const str = sessionStorage.getItem(name);
            return str ? JSON.parse(str) : null;
          },
          setItem: (name, value) => {
            sessionStorage.setItem(name, JSON.stringify(value));
          },
          removeItem: (name) => {
            sessionStorage.removeItem(name);
          },
        },
        partialize: (state) => ({
          summary: state.summary,
          lastFetchedAt: state.lastFetchedAt,
          currentPage: state.currentPage,
          pageSize: state.pageSize,
          totalPages: state.totalPages,
          searchQuery: state.searchQuery,
          debouncedSearchQuery: state.debouncedSearchQuery,
        }),
      }
    ),
    { name: 'DashboardStore' }
  )
);

export default useDashboardStore;
