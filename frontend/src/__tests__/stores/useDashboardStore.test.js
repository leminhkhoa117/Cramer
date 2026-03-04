/**
 * Unit tests for useDashboardStore.
 * Tests dashboard data fetching, caching, and pagination.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Mock the API module
vi.mock('../../api/backendApi', () => ({
  dashboardApi: {
    getSummary: vi.fn(),
  },
}));

// Import mocked modules
import { dashboardApi } from '../../api/backendApi';

const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

// Create a test version of the dashboard store
const createTestDashboardStore = () => {
  return create(
    devtools((set, get) => ({
      // State
      summary: null,
      loading: false,
      error: null,
      lastFetchedAt: null,

      // Pagination State
      currentPage: 0,
      pageSize: 4,
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

        const useCache = page === null && size === null && search === null;
        
        if (useCache && !state.isStale() && state.summary && !state.loading) {
          return state.summary;
        }

        set({ loading: true, error: null });

        try {
          const response = await dashboardApi.getSummary(actualPage, actualSize, actualSearch);
          const data = response.data;
          
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
        set({ pageSize: size, currentPage: 0 });
      },

      setSearchQuery: (query) => {
        set({ searchQuery: query });
      },

      setDebouncedSearchQuery: (query) => {
        set({ debouncedSearchQuery: query, currentPage: 0 });
      },

      resetPagination: () => {
        set({
          currentPage: 0,
          pageSize: 4,
          totalPages: 0,
          searchQuery: '',
          debouncedSearchQuery: '',
        });
      },

      invalidateCache: () => {
        set({ lastFetchedAt: null });
      },

      updateSummary: (updater) => {
        set((state) => ({
          summary: typeof updater === 'function' ? updater(state.summary) : updater,
        }));
      },
    }))
  );
};

describe('useDashboardStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestDashboardStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // fetchSummary Tests
  // ==========================================================================
  describe('fetchSummary', () => {
    it('should fetch summary successfully', async () => {
      const mockSummary = {
        totalTests: 10,
        completedTests: 5,
        averageScore: 7.5,
        courseProgress: {
          totalPages: 3,
          content: [],
        },
      };

      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      let result;
      await act(async () => {
        result = await store.getState().fetchSummary();
      });

      expect(result).toEqual(mockSummary);
      expect(store.getState().summary).toEqual(mockSummary);
      expect(store.getState().totalPages).toBe(3);
      expect(store.getState().loading).toBe(false);
      expect(store.getState().lastFetchedAt).not.toBeNull();
    });

    it('should use cache when data is fresh', async () => {
      const mockSummary = { totalTests: 10 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      // First fetch
      await act(async () => {
        await store.getState().fetchSummary();
      });

      // Second fetch - should use cache
      let result;
      await act(async () => {
        result = await store.getState().fetchSummary();
      });

      expect(dashboardApi.getSummary).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockSummary);
    });

    it('should skip cache when pagination params are provided', async () => {
      const mockSummary1 = { totalTests: 10, page: 0 };
      const mockSummary2 = { totalTests: 10, page: 1 };

      dashboardApi.getSummary
        .mockResolvedValueOnce({ data: mockSummary1 })
        .mockResolvedValueOnce({ data: mockSummary2 });

      // First fetch
      await act(async () => {
        await store.getState().fetchSummary();
      });

      // Second fetch with page param - should skip cache
      await act(async () => {
        await store.getState().fetchSummary(1, null, null);
      });

      expect(dashboardApi.getSummary).toHaveBeenCalledTimes(2);
    });

    it('should pass pagination and search params to API', async () => {
      const mockSummary = { totalTests: 5 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      await act(async () => {
        await store.getState().fetchSummary(2, 10, 'cambridge');
      });

      expect(dashboardApi.getSummary).toHaveBeenCalledWith(2, 10, 'cambridge');
    });

    it('should set error on failure', async () => {
      const error = new Error('Network error');
      dashboardApi.getSummary.mockRejectedValueOnce(error);

      await expect(async () => {
        await store.getState().fetchSummary();
      }).rejects.toThrow('Network error');

      expect(store.getState().error).toBe('Network error');
      expect(store.getState().loading).toBe(false);
    });

    it('should use response error message if available', async () => {
      const error = new Error('Request failed');
      error.response = { data: { message: 'Custom error from server' } };
      dashboardApi.getSummary.mockRejectedValueOnce(error);

      await expect(async () => {
        await store.getState().fetchSummary();
      }).rejects.toThrow();

      expect(store.getState().error).toBe('Custom error from server');
    });
  });

  // ==========================================================================
  // refreshSummary Tests
  // ==========================================================================
  describe('refreshSummary', () => {
    it('should refresh with current pagination state', async () => {
      const mockSummary = { totalTests: 10 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      // Set pagination state - note: setDebouncedSearchQuery resets page to 0, so set search first
      act(() => {
        store.getState().setDebouncedSearchQuery('test');
        store.getState().setPage(2);
      });

      await act(async () => {
        await store.getState().refreshSummary();
      });

      expect(dashboardApi.getSummary).toHaveBeenCalledWith(2, 4, 'test');
    });
  });

  // ==========================================================================
  // isStale Tests
  // ==========================================================================
  describe('isStale', () => {
    it('should return true if never fetched', () => {
      expect(store.getState().isStale()).toBe(true);
    });

    it('should return false immediately after fetch', async () => {
      const mockSummary = { totalTests: 10 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      await act(async () => {
        await store.getState().fetchSummary();
      });

      expect(store.getState().isStale()).toBe(false);
    });

    it('should return true after invalidateCache', async () => {
      const mockSummary = { totalTests: 10 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      await act(async () => {
        await store.getState().fetchSummary();
      });

      act(() => {
        store.getState().invalidateCache();
      });

      expect(store.getState().isStale()).toBe(true);
    });
  });

  // ==========================================================================
  // Pagination Actions Tests
  // ==========================================================================
  describe('pagination actions', () => {
    it('setPage should update currentPage', () => {
      act(() => {
        store.getState().setPage(5);
      });

      expect(store.getState().currentPage).toBe(5);
    });

    it('setPageSize should update pageSize and reset to page 0', () => {
      act(() => {
        store.getState().setPage(3);
        store.getState().setPageSize(10);
      });

      expect(store.getState().pageSize).toBe(10);
      expect(store.getState().currentPage).toBe(0);
    });

    it('setSearchQuery should update searchQuery', () => {
      act(() => {
        store.getState().setSearchQuery('cambridge');
      });

      expect(store.getState().searchQuery).toBe('cambridge');
    });

    it('setDebouncedSearchQuery should update and reset to page 0', () => {
      act(() => {
        store.getState().setPage(5);
        store.getState().setDebouncedSearchQuery('ielts');
      });

      expect(store.getState().debouncedSearchQuery).toBe('ielts');
      expect(store.getState().currentPage).toBe(0);
    });

    it('resetPagination should reset all pagination state', () => {
      act(() => {
        store.getState().setPage(5);
        store.getState().setPageSize(20);
        store.getState().setSearchQuery('test');
        store.getState().setDebouncedSearchQuery('test');
      });

      act(() => {
        store.getState().resetPagination();
      });

      expect(store.getState().currentPage).toBe(0);
      expect(store.getState().pageSize).toBe(4);
      expect(store.getState().searchQuery).toBe('');
      expect(store.getState().debouncedSearchQuery).toBe('');
    });
  });

  // ==========================================================================
  // updateSummary Tests
  // ==========================================================================
  describe('updateSummary', () => {
    it('should update summary with object', async () => {
      const mockSummary = { totalTests: 10, goal: null };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      await act(async () => {
        await store.getState().fetchSummary();
      });

      act(() => {
        store.getState().updateSummary({ totalTests: 10, goal: 'Band 7.5' });
      });

      expect(store.getState().summary.goal).toBe('Band 7.5');
    });

    it('should update summary with function', async () => {
      const mockSummary = { totalTests: 10, completedTests: 5 };
      dashboardApi.getSummary.mockResolvedValueOnce({ data: mockSummary });

      await act(async () => {
        await store.getState().fetchSummary();
      });

      act(() => {
        store.getState().updateSummary((prev) => ({
          ...prev,
          completedTests: prev.completedTests + 1,
        }));
      });

      expect(store.getState().summary.completedTests).toBe(6);
    });
  });
});
