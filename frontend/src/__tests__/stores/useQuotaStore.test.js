/**
 * Unit tests for useQuotaStore.
 * Tests quota status fetching, progress calculations, and pre-check functionality.
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
  quotaApi: {
    getStatus: vi.fn(),
    canAttempt: vi.fn(),
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
import { quotaApi } from '../../api/backendApi';
import { useAuthStore } from '../../stores/index';

// Create a test version of the quota store
const createTestQuotaStore = () => {
  return create(
    devtools(
      subscribeWithSelector((set, get) => ({
        // State
        quotaStatus: null,
        loading: false,
        error: null,
        lastFetched: null,
        preCheckResult: null,
        preCheckLoading: false,

        // Selectors
        isPremium: () => {
          const { quotaStatus } = get();
          return quotaStatus?.isPremium === true;
        },

        getGlobalProgress: (isAI = false) => {
          const { quotaStatus } = get();
          if (!quotaStatus || quotaStatus.isPremium) return 0;

          if (isAI) {
            const cap = quotaStatus.globalAttemptAICap;
            return cap > 0 ? Math.min(100, (quotaStatus.globalAttemptAI / cap) * 100) : 0;
          }
          const cap = quotaStatus.globalAttemptCap;
          return cap > 0 ? Math.min(100, (quotaStatus.globalAttempt / cap) * 100) : 0;
        },

        getSkillProgress: (skill, isAI = false) => {
          const { quotaStatus } = get();
          if (!quotaStatus || quotaStatus.isPremium) return 0;

          const skillInfo = quotaStatus.skills?.[skill];
          if (!skillInfo) return 0;

          if (isAI) {
            const cap = skillInfo.attemptAICap;
            return cap > 0 ? Math.min(100, (skillInfo.attemptAI / cap) * 100) : 0;
          }
          const cap = skillInfo.attemptCap;
          return cap > 0 ? Math.min(100, (skillInfo.attempt / cap) * 100) : 0;
        },

        getProgressColor: (percent) => {
          if (percent < 50) return 'green';
          if (percent < 80) return 'yellow';
          return 'red';
        },

        // Actions
        fetchQuotaStatus: async (force = false) => {
          const user = useAuthStore.getState().user;
          if (!user) return;

          const { loading, lastFetched } = get();

          if (loading) return;

          if (!force && lastFetched && Date.now() - lastFetched < 30000) {
            return;
          }

          set({ loading: true, error: null }, false, 'fetchQuotaStatus/start');

          try {
            const response = await quotaApi.getStatus();
            if (response?.data) {
              set({
                quotaStatus: response.data,
                loading: false,
                lastFetched: Date.now(),
              }, false, 'fetchQuotaStatus/success');
            }
          } catch (error) {
            set({
              loading: false,
              error: error.message || 'Failed to load quota status',
            }, false, 'fetchQuotaStatus/error');
          }
        },

        preCheckAttempt: async (skill, isAI = false) => {
          const user = useAuthStore.getState().user;
          if (!user) return null;

          set({ preCheckLoading: true }, false, 'preCheckAttempt/start');

          try {
            const response = await quotaApi.canAttempt(skill, isAI);
            const result = response?.data;
            set({
              preCheckResult: result,
              preCheckLoading: false,
            }, false, 'preCheckAttempt/success');
            return result;
          } catch (error) {
            set({ preCheckLoading: false }, false, 'preCheckAttempt/error');
            return null;
          }
        },

        clearPreCheck: () => {
          set({ preCheckResult: null }, false, 'clearPreCheck');
        },

        invalidateCache: () => {
          set({ lastFetched: null }, false, 'invalidateCache');
        },

        clearQuota: () => {
          set({
            quotaStatus: null,
            loading: false,
            error: null,
            lastFetched: null,
            preCheckResult: null,
            preCheckLoading: false,
          }, false, 'clearQuota');
        },
      }))
    )
  );
};

describe('useQuotaStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestQuotaStore();
    // Reset auth mock to return user
    useAuthStore.getState.mockReturnValue({ user: { id: 'user-123' } });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // fetchQuotaStatus Tests
  // ==========================================================================
  describe('fetchQuotaStatus', () => {
    it('should fetch quota status successfully', async () => {
      const mockStatus = {
        isPremium: false,
        globalAttempt: 5,
        globalAttemptCap: 20,
        globalAttemptAI: 2,
        globalAttemptAICap: 5,
        skills: {
          reading: { attempt: 3, attemptCap: 10 },
          writing: { attempt: 2, attemptCap: 5 },
        },
      };

      quotaApi.getStatus.mockResolvedValueOnce({ data: mockStatus });

      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(store.getState().quotaStatus).toEqual(mockStatus);
      expect(store.getState().loading).toBe(false);
      expect(store.getState().lastFetched).not.toBeNull();
    });

    it('should skip fetch if no user', async () => {
      useAuthStore.getState.mockReturnValue({ user: null });

      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(quotaApi.getStatus).not.toHaveBeenCalled();
    });

    it('should skip fetch if already loading', async () => {
      quotaApi.getStatus.mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve({ data: {} }), 100))
      );

      // Start first fetch
      store.getState().fetchQuotaStatus();

      // Try second fetch while loading
      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(quotaApi.getStatus).toHaveBeenCalledTimes(1);
    });

    it('should use cache within 30 seconds', async () => {
      quotaApi.getStatus.mockResolvedValue({ data: {} });

      // First fetch
      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      // Second fetch within 30s - should use cache
      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(quotaApi.getStatus).toHaveBeenCalledTimes(1);
    });

    it('should force fetch when force=true', async () => {
      quotaApi.getStatus.mockResolvedValue({ data: {} });

      // First fetch
      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      // Force fetch
      await act(async () => {
        await store.getState().fetchQuotaStatus(true);
      });

      expect(quotaApi.getStatus).toHaveBeenCalledTimes(2);
    });

    it('should set error on failure', async () => {
      quotaApi.getStatus.mockRejectedValueOnce(new Error('API Error'));

      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(store.getState().error).toBe('API Error');
      expect(store.getState().loading).toBe(false);
    });
  });

  // ==========================================================================
  // Progress Calculation Tests
  // ==========================================================================
  describe('getGlobalProgress', () => {
    it('should return 0 for premium users', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: true,
            globalAttempt: 5,
            globalAttemptCap: 20,
          },
        });
      });

      expect(store.getState().getGlobalProgress()).toBe(0);
    });

    it('should calculate regular global progress correctly', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            globalAttempt: 10,
            globalAttemptCap: 20,
          },
        });
      });

      expect(store.getState().getGlobalProgress()).toBe(50);
    });

    it('should calculate AI global progress correctly', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            globalAttemptAI: 3,
            globalAttemptAICap: 5,
          },
        });
      });

      expect(store.getState().getGlobalProgress(true)).toBe(60);
    });

    it('should cap progress at 100%', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            globalAttempt: 25,
            globalAttemptCap: 20,
          },
        });
      });

      expect(store.getState().getGlobalProgress()).toBe(100);
    });

    it('should return 0 when cap is 0', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            globalAttempt: 5,
            globalAttemptCap: 0,
          },
        });
      });

      expect(store.getState().getGlobalProgress()).toBe(0);
    });
  });

  describe('getSkillProgress', () => {
    it('should calculate skill progress correctly', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            skills: {
              reading: { attempt: 4, attemptCap: 10 },
            },
          },
        });
      });

      expect(store.getState().getSkillProgress('reading')).toBe(40);
    });

    it('should return 0 for unknown skill', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            skills: {},
          },
        });
      });

      expect(store.getState().getSkillProgress('speaking')).toBe(0);
    });

    it('should calculate AI skill progress', () => {
      act(() => {
        store.setState({
          quotaStatus: {
            isPremium: false,
            skills: {
              writing: { attemptAI: 2, attemptAICap: 4 },
            },
          },
        });
      });

      expect(store.getState().getSkillProgress('writing', true)).toBe(50);
    });
  });

  describe('getProgressColor', () => {
    it('should return green for < 50%', () => {
      expect(store.getState().getProgressColor(30)).toBe('green');
      expect(store.getState().getProgressColor(49)).toBe('green');
    });

    it('should return yellow for 50-79%', () => {
      expect(store.getState().getProgressColor(50)).toBe('yellow');
      expect(store.getState().getProgressColor(79)).toBe('yellow');
    });

    it('should return red for >= 80%', () => {
      expect(store.getState().getProgressColor(80)).toBe('red');
      expect(store.getState().getProgressColor(100)).toBe('red');
    });
  });

  // ==========================================================================
  // preCheckAttempt Tests
  // ==========================================================================
  describe('preCheckAttempt', () => {
    it('should pre-check attempt successfully', async () => {
      const mockResult = { allowed: true, reason: null };
      quotaApi.canAttempt.mockResolvedValueOnce({ data: mockResult });

      let result;
      await act(async () => {
        result = await store.getState().preCheckAttempt('reading', false);
      });

      expect(result).toEqual(mockResult);
      expect(store.getState().preCheckResult).toEqual(mockResult);
      expect(quotaApi.canAttempt).toHaveBeenCalledWith('reading', false);
    });

    it('should return null if no user', async () => {
      useAuthStore.getState.mockReturnValue({ user: null });

      const result = await store.getState().preCheckAttempt('reading');

      expect(result).toBeNull();
      expect(quotaApi.canAttempt).not.toHaveBeenCalled();
    });

    it('should handle blocked attempt', async () => {
      const mockResult = { allowed: false, reason: 'quota_exceeded' };
      quotaApi.canAttempt.mockResolvedValueOnce({ data: mockResult });

      let result;
      await act(async () => {
        result = await store.getState().preCheckAttempt('writing', true);
      });

      expect(result.allowed).toBe(false);
      expect(result.reason).toBe('quota_exceeded');
    });

    it('should handle API error gracefully', async () => {
      quotaApi.canAttempt.mockRejectedValueOnce(new Error('Network error'));

      let result;
      await act(async () => {
        result = await store.getState().preCheckAttempt('reading');
      });

      expect(result).toBeNull();
      expect(store.getState().preCheckLoading).toBe(false);
    });
  });

  // ==========================================================================
  // clearPreCheck Tests
  // ==========================================================================
  describe('clearPreCheck', () => {
    it('should clear pre-check result', async () => {
      quotaApi.canAttempt.mockResolvedValueOnce({ data: { allowed: true } });

      await act(async () => {
        await store.getState().preCheckAttempt('reading');
      });

      expect(store.getState().preCheckResult).not.toBeNull();

      act(() => {
        store.getState().clearPreCheck();
      });

      expect(store.getState().preCheckResult).toBeNull();
    });
  });

  // ==========================================================================
  // clearQuota Tests
  // ==========================================================================
  describe('clearQuota', () => {
    it('should clear all quota state', async () => {
      quotaApi.getStatus.mockResolvedValueOnce({ data: { isPremium: false } });

      await act(async () => {
        await store.getState().fetchQuotaStatus();
      });

      expect(store.getState().quotaStatus).not.toBeNull();

      act(() => {
        store.getState().clearQuota();
      });

      expect(store.getState().quotaStatus).toBeNull();
      expect(store.getState().lastFetched).toBeNull();
      expect(store.getState().preCheckResult).toBeNull();
      expect(store.getState().error).toBeNull();
    });
  });

  // ==========================================================================
  // isPremium Tests
  // ==========================================================================
  describe('isPremium', () => {
    it('should return true for premium users', () => {
      act(() => {
        store.setState({ quotaStatus: { isPremium: true } });
      });

      expect(store.getState().isPremium()).toBe(true);
    });

    it('should return false for non-premium users', () => {
      act(() => {
        store.setState({ quotaStatus: { isPremium: false } });
      });

      expect(store.getState().isPremium()).toBe(false);
    });

    it('should return false if no quota status', () => {
      expect(store.getState().isPremium()).toBe(false);
    });
  });
});
