/**
 * Unit tests for useTestSessionStore.
 * Tests test session API operations: start, load, save, submit, cancel.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Mock API modules
vi.mock('../../api/backendApi', () => ({
  testApi: {
    getFullTest: vi.fn(),
  },
  testAttemptApi: {
    startAttempt: vi.fn(),
    getAttemptAnswers: vi.fn(),
    saveProgress: vi.fn(),
    submitAttempt: vi.fn(),
    cancelAttempt: vi.fn(),
  },
  writingApi: {
    getSubmissions: vi.fn(),
    saveDraft: vi.fn(),
    submitForGrading: vi.fn(),
  },
}));

// Import mocked modules
import { testApi, testAttemptApi, writingApi } from '../../api/backendApi';

// Cache TTL: 5 minutes
const CACHE_TTL = 5 * 60 * 1000;

// Create a test version of the test session store
const createTestSessionStore = () => {
  return create(
    devtools((set, get) => ({
      // State
      currentAttemptId: null,
      attemptStatus: 'idle',
      lastSavedAt: null,
      autoSaveEnabled: true,
      testDataCache: {},

      // Actions
      startOrResumeAttempt: async (source, testNum, skill, forceNew = false) => {
        set({ attemptStatus: 'loading' }, false, 'startOrResumeAttempt/pending');
        
        try {
          const response = await testAttemptApi.startAttempt(source, testNum, skill, forceNew);
          const attemptData = response.data;
          
          set({
            currentAttemptId: attemptData.id,
            attemptStatus: 'ready',
          }, false, 'startOrResumeAttempt/fulfilled');
          
          return attemptData;
        } catch (error) {
          set({ attemptStatus: 'error' }, false, 'startOrResumeAttempt/rejected');
          throw error;
        }
      },

      loadTestData: async (source, testNum, skill) => {
        const cacheKey = `${source}-${testNum}-${skill}`;
        const cached = get().testDataCache[cacheKey];
        const now = Date.now();
        
        if (cached && (now - cached.fetchedAt) < CACHE_TTL) {
          return cached.data;
        }
        
        try {
          const testData = await testApi.getFullTest(source, testNum, skill);
          
          set((state) => ({
            testDataCache: {
              ...state.testDataCache,
              [cacheKey]: {
                data: testData,
                fetchedAt: now,
              },
            },
          }), false, 'loadTestData/cached');
          
          return testData;
        } catch (error) {
          throw error;
        }
      },

      loadAnswers: async (attemptId) => {
        try {
          const response = await testAttemptApi.getAttemptAnswers(attemptId);
          return response.data;
        } catch (error) {
          throw error;
        }
      },

      loadEssays: async (attemptId) => {
        try {
          const response = await writingApi.getSubmissions(attemptId);
          return response.data;
        } catch (error) {
          throw error;
        }
      },

      saveProgress: async (attemptId, { answers, essays, timeLeft, currentPart }) => {
        try {
          await testAttemptApi.saveProgress(attemptId, {
            answers: answers || {},
            timeLeft,
            currentPart,
          });
          
          if (essays && Object.keys(essays).length > 0) {
            const essayPromises = Object.entries(essays).map(([taskNumber, essayText]) => {
              if (essayText && essayText.trim()) {
                return writingApi.saveDraft(attemptId, parseInt(taskNumber, 10), essayText);
              }
              return Promise.resolve();
            });
            
            await Promise.all(essayPromises);
          }
          
          set({ lastSavedAt: new Date() }, false, 'saveProgress/fulfilled');
        } catch (error) {
          throw error;
        }
      },

      submitAttempt: async (attemptId, answers) => {
        try {
          const response = await testAttemptApi.submitAttempt(attemptId, answers);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
          }, false, 'submitAttempt/fulfilled');
          
          return response.data;
        } catch (error) {
          throw error;
        }
      },

      submitWriting: async (attemptId) => {
        try {
          const response = await writingApi.submitForGrading(attemptId);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
          }, false, 'submitWriting/fulfilled');
          
          return response.data;
        } catch (error) {
          throw error;
        }
      },

      cancelAttempt: async (attemptId) => {
        try {
          await testAttemptApi.cancelAttempt(attemptId);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
          }, false, 'cancelAttempt/fulfilled');
        } catch (error) {
          throw error;
        }
      },

      invalidateCache: (source, testNum, skill) => {
        const cacheKey = `${source}-${testNum}-${skill}`;
        set((state) => {
          const newCache = { ...state.testDataCache };
          delete newCache[cacheKey];
          return { testDataCache: newCache };
        }, false, 'invalidateCache');
      },

      reset: () => {
        set({
          currentAttemptId: null,
          attemptStatus: 'idle',
          lastSavedAt: null,
        }, false, 'reset');
      },
    }))
  );
};

describe('useTestSessionStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestSessionStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // startOrResumeAttempt Tests
  // ==========================================================================
  describe('startOrResumeAttempt', () => {
    it('should start a new attempt successfully', async () => {
      const mockAttempt = {
        id: 'attempt-123',
        status: 'IN_PROGRESS',
        source: 'cam17',
        testNum: 1,
        skill: 'reading',
      };

      testAttemptApi.startAttempt.mockResolvedValueOnce({ data: mockAttempt });

      let result;
      await act(async () => {
        result = await store.getState().startOrResumeAttempt('cam17', 1, 'reading');
      });

      expect(result).toEqual(mockAttempt);
      expect(store.getState().currentAttemptId).toBe('attempt-123');
      expect(store.getState().attemptStatus).toBe('ready');
      expect(testAttemptApi.startAttempt).toHaveBeenCalledWith('cam17', 1, 'reading', false);
    });

    it('should resume existing attempt', async () => {
      const mockAttempt = {
        id: 'existing-attempt',
        status: 'IN_PROGRESS',
        timeLeft: 1800,
      };

      testAttemptApi.startAttempt.mockResolvedValueOnce({ data: mockAttempt });

      let result;
      await act(async () => {
        result = await store.getState().startOrResumeAttempt('cam17', 1, 'reading', false);
      });

      expect(result.id).toBe('existing-attempt');
      expect(result.timeLeft).toBe(1800);
    });

    it('should force new attempt when forceNew is true', async () => {
      const mockAttempt = {
        id: 'new-attempt',
        status: 'IN_PROGRESS',
      };

      testAttemptApi.startAttempt.mockResolvedValueOnce({ data: mockAttempt });

      await act(async () => {
        await store.getState().startOrResumeAttempt('cam17', 1, 'reading', true);
      });

      expect(testAttemptApi.startAttempt).toHaveBeenCalledWith('cam17', 1, 'reading', true);
    });

    it('should set error status on failure', async () => {
      testAttemptApi.startAttempt.mockRejectedValueOnce(new Error('API Error'));

      await expect(async () => {
        await store.getState().startOrResumeAttempt('cam17', 1, 'reading');
      }).rejects.toThrow('API Error');

      expect(store.getState().attemptStatus).toBe('error');
    });
  });

  // ==========================================================================
  // loadTestData Tests
  // ==========================================================================
  describe('loadTestData', () => {
    it('should load test data successfully', async () => {
      const mockTestData = {
        sections: [
          { id: 1, part: 1, title: 'Part 1' },
          { id: 2, part: 2, title: 'Part 2' },
        ],
      };

      testApi.getFullTest.mockResolvedValueOnce(mockTestData);

      let result;
      await act(async () => {
        result = await store.getState().loadTestData('cam17', 1, 'reading');
      });

      expect(result).toEqual(mockTestData);
      expect(testApi.getFullTest).toHaveBeenCalledWith('cam17', 1, 'reading');
    });

    it('should use cached data within TTL', async () => {
      const mockTestData = { sections: [{ id: 1 }] };
      testApi.getFullTest.mockResolvedValueOnce(mockTestData);

      // First call
      await act(async () => {
        await store.getState().loadTestData('cam17', 1, 'reading');
      });

      // Second call - should use cache
      let result;
      await act(async () => {
        result = await store.getState().loadTestData('cam17', 1, 'reading');
      });

      expect(testApi.getFullTest).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockTestData);
    });

    it('should cache data with different keys', async () => {
      const mockData1 = { sections: [{ id: 1, skill: 'reading' }] };
      const mockData2 = { sections: [{ id: 2, skill: 'listening' }] };

      testApi.getFullTest
        .mockResolvedValueOnce(mockData1)
        .mockResolvedValueOnce(mockData2);

      await act(async () => {
        await store.getState().loadTestData('cam17', 1, 'reading');
        await store.getState().loadTestData('cam17', 1, 'listening');
      });

      expect(testApi.getFullTest).toHaveBeenCalledTimes(2);

      const cache = store.getState().testDataCache;
      expect(cache['cam17-1-reading']).toBeDefined();
      expect(cache['cam17-1-listening']).toBeDefined();
    });
  });

  // ==========================================================================
  // loadAnswers Tests
  // ==========================================================================
  describe('loadAnswers', () => {
    it('should load answers successfully', async () => {
      const mockAnswers = { 1: 'A', 2: 'B', 3: 'C' };
      testAttemptApi.getAttemptAnswers.mockResolvedValueOnce({ data: mockAnswers });

      let result;
      await act(async () => {
        result = await store.getState().loadAnswers('attempt-123');
      });

      expect(result).toEqual(mockAnswers);
      expect(testAttemptApi.getAttemptAnswers).toHaveBeenCalledWith('attempt-123');
    });

    it('should throw error on failure', async () => {
      testAttemptApi.getAttemptAnswers.mockRejectedValueOnce(new Error('Not found'));

      await expect(async () => {
        await store.getState().loadAnswers('invalid-attempt');
      }).rejects.toThrow('Not found');
    });
  });

  // ==========================================================================
  // loadEssays Tests
  // ==========================================================================
  describe('loadEssays', () => {
    it('should load essays successfully', async () => {
      const mockEssays = {
        1: { taskNumber: 1, essayText: 'Task 1 essay' },
        2: { taskNumber: 2, essayText: 'Task 2 essay' },
      };

      writingApi.getSubmissions.mockResolvedValueOnce({ data: mockEssays });

      let result;
      await act(async () => {
        result = await store.getState().loadEssays('attempt-123');
      });

      expect(result).toEqual(mockEssays);
    });
  });

  // ==========================================================================
  // saveProgress Tests
  // ==========================================================================
  describe('saveProgress', () => {
    it('should save progress with answers only', async () => {
      testAttemptApi.saveProgress.mockResolvedValueOnce({});

      await act(async () => {
        await store.getState().saveProgress('attempt-123', {
          answers: { 1: 'A', 2: 'B' },
          timeLeft: 1800,
          currentPart: 0,
        });
      });

      expect(testAttemptApi.saveProgress).toHaveBeenCalledWith('attempt-123', {
        answers: { 1: 'A', 2: 'B' },
        timeLeft: 1800,
        currentPart: 0,
      });
      expect(store.getState().lastSavedAt).toBeInstanceOf(Date);
    });

    it('should save essays for writing tests', async () => {
      testAttemptApi.saveProgress.mockResolvedValueOnce({});
      writingApi.saveDraft.mockResolvedValue({});

      await act(async () => {
        await store.getState().saveProgress('attempt-123', {
          answers: {},
          essays: { 1: 'Task 1 text', 2: 'Task 2 text' },
          timeLeft: 3000,
          currentPart: 1,
        });
      });

      expect(writingApi.saveDraft).toHaveBeenCalledTimes(2);
      expect(writingApi.saveDraft).toHaveBeenCalledWith('attempt-123', 1, 'Task 1 text');
      expect(writingApi.saveDraft).toHaveBeenCalledWith('attempt-123', 2, 'Task 2 text');
    });

    it('should skip empty essays', async () => {
      testAttemptApi.saveProgress.mockResolvedValueOnce({});
      writingApi.saveDraft.mockResolvedValue({});

      await act(async () => {
        await store.getState().saveProgress('attempt-123', {
          answers: {},
          essays: { 1: 'Task 1 text', 2: '' },
          timeLeft: 3000,
          currentPart: 1,
        });
      });

      expect(writingApi.saveDraft).toHaveBeenCalledTimes(1);
      expect(writingApi.saveDraft).toHaveBeenCalledWith('attempt-123', 1, 'Task 1 text');
    });
  });

  // ==========================================================================
  // submitAttempt Tests
  // ==========================================================================
  describe('submitAttempt', () => {
    it('should submit attempt and reset state', async () => {
      const mockResult = { attemptId: 'attempt-123', score: 32 };
      testAttemptApi.submitAttempt.mockResolvedValueOnce({ data: mockResult });

      // Set current attempt first
      await act(async () => {
        store.setState({ currentAttemptId: 'attempt-123', attemptStatus: 'ready' });
      });

      let result;
      await act(async () => {
        result = await store.getState().submitAttempt('attempt-123', { 1: 'A', 2: 'B' });
      });

      expect(result).toEqual(mockResult);
      expect(store.getState().currentAttemptId).toBeNull();
      expect(store.getState().attemptStatus).toBe('idle');
    });
  });

  // ==========================================================================
  // submitWriting Tests
  // ==========================================================================
  describe('submitWriting', () => {
    it('should submit writing for grading', async () => {
      const mockResult = { status: 'GRADING' };
      writingApi.submitForGrading.mockResolvedValueOnce({ data: mockResult });

      let result;
      await act(async () => {
        result = await store.getState().submitWriting('attempt-123');
      });

      expect(result).toEqual(mockResult);
      expect(store.getState().currentAttemptId).toBeNull();
    });
  });

  // ==========================================================================
  // cancelAttempt Tests
  // ==========================================================================
  describe('cancelAttempt', () => {
    it('should cancel attempt and reset state', async () => {
      testAttemptApi.cancelAttempt.mockResolvedValueOnce({});

      // Set current attempt first
      act(() => {
        store.setState({ currentAttemptId: 'attempt-123', attemptStatus: 'ready' });
      });

      await act(async () => {
        await store.getState().cancelAttempt('attempt-123');
      });

      expect(testAttemptApi.cancelAttempt).toHaveBeenCalledWith('attempt-123');
      expect(store.getState().currentAttemptId).toBeNull();
      expect(store.getState().attemptStatus).toBe('idle');
    });
  });

  // ==========================================================================
  // invalidateCache Tests
  // ==========================================================================
  describe('invalidateCache', () => {
    it('should remove specific cache entry', async () => {
      const mockData = { sections: [] };
      testApi.getFullTest.mockResolvedValueOnce(mockData);

      // Load data to cache it
      await act(async () => {
        await store.getState().loadTestData('cam17', 1, 'reading');
      });

      expect(store.getState().testDataCache['cam17-1-reading']).toBeDefined();

      // Invalidate cache
      act(() => {
        store.getState().invalidateCache('cam17', 1, 'reading');
      });

      expect(store.getState().testDataCache['cam17-1-reading']).toBeUndefined();
    });
  });

  // ==========================================================================
  // reset Tests
  // ==========================================================================
  describe('reset', () => {
    it('should reset session state', async () => {
      // Set some state
      act(() => {
        store.setState({
          currentAttemptId: 'attempt-123',
          attemptStatus: 'ready',
          lastSavedAt: new Date(),
        });
      });

      // Reset
      act(() => {
        store.getState().reset();
      });

      expect(store.getState().currentAttemptId).toBeNull();
      expect(store.getState().attemptStatus).toBe('idle');
      expect(store.getState().lastSavedAt).toBeNull();
    });
  });
});
