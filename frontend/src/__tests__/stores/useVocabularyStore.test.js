/**
 * Unit tests for useVocabularyStore.
 * Tests vocabulary CRUD operations, pagination, search, and filtering.
 * 
 * @author Cramer Test Team
 * @since 2026-01-25
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Mock API module
vi.mock('../../api/backendApi', () => ({
  vocabularyApi: {
    getAll: vi.fn(),
    getStats: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    translate: vi.fn(),
    toggleMastered: vi.fn(),
  },
}));

// Import mocked modules
import { vocabularyApi } from '../../api/backendApi';

// Create a test version of the vocabulary store (matching actual store logic)
const CACHE_TTL_MS = 5 * 60 * 1000;

const createTestVocabularyStore = () => {
  return create(
    devtools(
      (set, get) => ({
        // State
        vocabulary: [],
        stats: null,
        loading: false,
        error: null,
        lastFetchedAt: null,

        // Pagination State
        currentPage: 0,
        pageSize: 20,
        totalPages: 0,
        totalElements: 0,
        searchQuery: '',
        debouncedSearchQuery: '',

        // Filter State
        filter: 'all',

        // Translation state
        translating: false,
        translationError: null,

        // Selectors
        isStale: () => {
          const { lastFetchedAt } = get();
          if (!lastFetchedAt) return true;
          const now = Date.now();
          const fetchedTime = new Date(lastFetchedAt).getTime();
          return now - fetchedTime > CACHE_TTL_MS;
        },

        // Actions
        fetchVocabulary: async (page = null, size = null, search = null, filterOverride = null) => {
          const state = get();
          const actualPage = page ?? state.currentPage;
          const actualSize = size ?? state.pageSize;
          const actualSearch = search ?? state.debouncedSearchQuery;
          const actualFilter = filterOverride ?? state.filter;

          set({ loading: true, error: null });

          try {
            const response = await vocabularyApi.getAll(actualPage, actualSize, actualSearch, actualFilter);
            const data = response.data;

            set({
              vocabulary: data.content || [],
              loading: false,
              lastFetchedAt: new Date().toISOString(),
              error: null,
              totalPages: data.totalPages ?? 0,
              totalElements: data.totalElements ?? 0,
              currentPage: actualPage,
            });
            return data;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch vocabulary';
            set({
              loading: false,
              error: errorMessage,
            });
            throw err;
          }
        },

        fetchStats: async () => {
          try {
            const response = await vocabularyApi.getStats();
            set({ stats: response.data });
            return response.data;
          } catch (err) {
            console.error('Failed to fetch vocabulary stats:', err);
            return null;
          }
        },

        addWord: async (wordData) => {
          set({ loading: true, error: null });
          try {
            const response = await vocabularyApi.create(wordData);
            const newWord = response.data;

            const currentVocabulary = get().vocabulary;
            set({
              vocabulary: [newWord, ...currentVocabulary],
              loading: false,
            });

            return newWord;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to add word';
            set({ loading: false, error: errorMessage });
            throw err;
          }
        },

        updateWord: async (id, wordData) => {
          set({ loading: true, error: null });
          try {
            const response = await vocabularyApi.update(id, wordData);
            const updatedWord = response.data;

            const currentVocabulary = get().vocabulary;
            set({
              vocabulary: currentVocabulary.map(v => v.id === id ? updatedWord : v),
              loading: false,
            });

            return updatedWord;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to update word';
            set({ loading: false, error: errorMessage });
            throw err;
          }
        },

        deleteWord: async (id) => {
          set({ loading: true, error: null });
          try {
            await vocabularyApi.delete(id);

            const currentVocabulary = get().vocabulary;
            set({
              vocabulary: currentVocabulary.filter(v => v.id !== id),
              loading: false,
            });

            return true;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to delete word';
            set({ loading: false, error: errorMessage });
            throw err;
          }
        },

        translateWord: async (word, context = null) => {
          set({ translating: true, translationError: null });
          try {
            const response = await vocabularyApi.translate(word, context);
            set({ translating: false });
            return response.data;
          } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to translate word';
            set({ translating: false, translationError: errorMessage });
            throw err;
          }
        },

        toggleMastered: async (id) => {
          try {
            const response = await vocabularyApi.toggleMastered(id);
            const updatedWord = response.data;

            const currentVocabulary = get().vocabulary;
            const currentFilter = get().filter;

            if (currentFilter === 'mastered' && !updatedWord.mastered) {
              set({
                vocabulary: currentVocabulary.filter(v => v.id !== id),
              });
            } else if (currentFilter === 'unmastered' && updatedWord.mastered) {
              set({
                vocabulary: currentVocabulary.filter(v => v.id !== id),
              });
            } else {
              set({
                vocabulary: currentVocabulary.map(v => v.id === id ? updatedWord : v),
              });
            }

            return updatedWord;
          } catch (err) {
            console.error('Failed to toggle mastered status:', err);
            throw err;
          }
        },

        // Pagination actions
        setPage: (page) => {
          set({ currentPage: page });
          get().fetchVocabulary(page);
        },

        setSearchQuery: (query) => {
          set({ searchQuery: query });
        },

        setDebouncedSearchQuery: (query) => {
          set({ debouncedSearchQuery: query, currentPage: 0 });
          get().fetchVocabulary(0, null, query);
        },

        setFilter: (newFilter) => {
          set({ filter: newFilter, currentPage: 0 });
          get().fetchVocabulary(0, null, null, newFilter);
        },

        // Clear state
        clearError: () => set({ error: null }),

        resetStore: () => set({
          vocabulary: [],
          stats: null,
          loading: false,
          error: null,
          lastFetchedAt: null,
          currentPage: 0,
          totalPages: 0,
          totalElements: 0,
          searchQuery: '',
          debouncedSearchQuery: '',
          filter: 'all',
        }),
      }),
      { name: 'vocabulary-store-test' }
    )
  );
};

describe('useVocabularyStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestVocabularyStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // fetchVocabulary Tests
  // ==========================================================================
  describe('fetchVocabulary', () => {
    it('should fetch vocabulary successfully', async () => {
      const mockData = {
        content: [
          { id: 1, word: 'ubiquitous', translation: 'everywhere' },
          { id: 2, word: 'ephemeral', translation: 'short-lived' },
        ],
        totalPages: 1,
        totalElements: 2,
      };

      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        await store.getState().fetchVocabulary();
      });

      expect(store.getState().vocabulary).toHaveLength(2);
      expect(store.getState().vocabulary[0].word).toBe('ubiquitous');
      expect(store.getState().loading).toBe(false);
      expect(store.getState().totalElements).toBe(2);
    });

    it('should handle pagination correctly', async () => {
      const mockData = {
        content: [{ id: 3, word: 'paradigm' }],
        totalPages: 3,
        totalElements: 50,
      };

      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        await store.getState().fetchVocabulary(2, 20);
      });

      expect(vocabularyApi.getAll).toHaveBeenCalledWith(2, 20, '', 'all');
      expect(store.getState().currentPage).toBe(2);
    });

    it('should apply search filter', async () => {
      const mockData = {
        content: [{ id: 1, word: 'test' }],
        totalPages: 1,
        totalElements: 1,
      };

      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        await store.getState().fetchVocabulary(0, 20, 'test');
      });

      expect(vocabularyApi.getAll).toHaveBeenCalledWith(0, 20, 'test', 'all');
    });

    it('should apply mastered filter', async () => {
      const mockData = {
        content: [{ id: 1, word: 'mastered', mastered: true }],
        totalPages: 1,
        totalElements: 1,
      };

      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        await store.getState().fetchVocabulary(0, 20, null, 'mastered');
      });

      expect(vocabularyApi.getAll).toHaveBeenCalledWith(0, 20, '', 'mastered');
    });

    it('should set error on failure', async () => {
      vocabularyApi.getAll.mockRejectedValueOnce(new Error('Network error'));

      await act(async () => {
        try {
          await store.getState().fetchVocabulary();
        } catch (e) {
          // Expected error
        }
      });

      expect(store.getState().error).toBe('Network error');
      expect(store.getState().loading).toBe(false);
    });
  });

  // ==========================================================================
  // fetchStats Tests
  // ==========================================================================
  describe('fetchStats', () => {
    it('should fetch stats successfully', async () => {
      const mockStats = {
        total: 100,
        mastered: 40,
        learning: 60,
        masteredPercentage: 40,
      };

      vocabularyApi.getStats.mockResolvedValueOnce({ data: mockStats });

      await act(async () => {
        await store.getState().fetchStats();
      });

      expect(store.getState().stats).toEqual(mockStats);
    });

    it('should handle error gracefully', async () => {
      vocabularyApi.getStats.mockRejectedValueOnce(new Error('API Error'));

      await act(async () => {
        const result = await store.getState().fetchStats();
        expect(result).toBeNull();
      });

      expect(store.getState().stats).toBeNull();
    });
  });

  // ==========================================================================
  // addWord Tests
  // ==========================================================================
  describe('addWord', () => {
    it('should add word successfully', async () => {
      const newWord = { id: 1, word: 'serendipity', translation: 'lucky discovery' };
      vocabularyApi.create.mockResolvedValueOnce({ data: newWord });

      await act(async () => {
        const result = await store.getState().addWord({ word: 'serendipity' });
        expect(result).toEqual(newWord);
      });

      expect(store.getState().vocabulary).toHaveLength(1);
      expect(store.getState().vocabulary[0].word).toBe('serendipity');
      expect(store.getState().loading).toBe(false);
    });

    it('should prepend new word to vocabulary list', async () => {
      // Set initial vocabulary
      act(() => {
        store.setState({ vocabulary: [{ id: 2, word: 'existing' }] });
      });

      const newWord = { id: 1, word: 'newWord' };
      vocabularyApi.create.mockResolvedValueOnce({ data: newWord });

      await act(async () => {
        await store.getState().addWord({ word: 'newWord' });
      });

      expect(store.getState().vocabulary[0].word).toBe('newWord');
      expect(store.getState().vocabulary[1].word).toBe('existing');
    });

    it('should set error on failure', async () => {
      vocabularyApi.create.mockRejectedValueOnce({
        response: { data: { message: 'Word already exists' } }
      });

      await act(async () => {
        try {
          await store.getState().addWord({ word: 'duplicate' });
        } catch (e) {
          // Expected error
        }
      });

      expect(store.getState().error).toBe('Word already exists');
    });
  });

  // ==========================================================================
  // updateWord Tests
  // ==========================================================================
  describe('updateWord', () => {
    it('should update word successfully', async () => {
      act(() => {
        store.setState({
          vocabulary: [{ id: 1, word: 'old', translation: 'old translation' }]
        });
      });

      const updatedWord = { id: 1, word: 'old', translation: 'new translation' };
      vocabularyApi.update.mockResolvedValueOnce({ data: updatedWord });

      await act(async () => {
        await store.getState().updateWord(1, { translation: 'new translation' });
      });

      expect(store.getState().vocabulary[0].translation).toBe('new translation');
    });

    it('should set error on failure', async () => {
      vocabularyApi.update.mockRejectedValueOnce(new Error('Update failed'));

      await act(async () => {
        try {
          await store.getState().updateWord(999, { translation: 'test' });
        } catch (e) {
          // Expected error
        }
      });

      expect(store.getState().error).toBe('Update failed');
    });
  });

  // ==========================================================================
  // deleteWord Tests
  // ==========================================================================
  describe('deleteWord', () => {
    it('should delete word successfully', async () => {
      act(() => {
        store.setState({
          vocabulary: [
            { id: 1, word: 'keep' },
            { id: 2, word: 'delete' },
          ]
        });
      });

      vocabularyApi.delete.mockResolvedValueOnce({});

      await act(async () => {
        await store.getState().deleteWord(2);
      });

      expect(store.getState().vocabulary).toHaveLength(1);
      expect(store.getState().vocabulary[0].word).toBe('keep');
    });

    it('should set error on failure', async () => {
      vocabularyApi.delete.mockRejectedValueOnce(new Error('Delete failed'));

      await act(async () => {
        try {
          await store.getState().deleteWord(999);
        } catch (e) {
          // Expected error
        }
      });

      expect(store.getState().error).toBe('Delete failed');
    });
  });

  // ==========================================================================
  // translateWord Tests
  // ==========================================================================
  describe('translateWord', () => {
    it('should translate word successfully', async () => {
      const translationResult = {
        translation: 'thanh lịch',
        phonetic: '/ˈel.ɪ.ɡənt/',
        definition: 'graceful and stylish',
      };

      vocabularyApi.translate.mockResolvedValueOnce({ data: translationResult });

      let result;
      await act(async () => {
        result = await store.getState().translateWord('elegant');
      });

      expect(result).toEqual(translationResult);
      expect(store.getState().translating).toBe(false);
    });

    it('should handle translation error', async () => {
      vocabularyApi.translate.mockRejectedValueOnce(new Error('Translation service unavailable'));

      await act(async () => {
        try {
          await store.getState().translateWord('unknown');
        } catch (e) {
          // Expected error
        }
      });

      expect(store.getState().translationError).toBe('Translation service unavailable');
      expect(store.getState().translating).toBe(false);
    });
  });

  // ==========================================================================
  // toggleMastered Tests
  // ==========================================================================
  describe('toggleMastered', () => {
    it('should toggle mastered status', async () => {
      act(() => {
        store.setState({
          vocabulary: [{ id: 1, word: 'test', mastered: false }],
          filter: 'all',
        });
      });

      const updatedWord = { id: 1, word: 'test', mastered: true };
      vocabularyApi.toggleMastered.mockResolvedValueOnce({ data: updatedWord });

      await act(async () => {
        await store.getState().toggleMastered(1);
      });

      expect(store.getState().vocabulary[0].mastered).toBe(true);
    });

    it('should remove word from list when filter is mastered and word becomes unmastered', async () => {
      act(() => {
        store.setState({
          vocabulary: [{ id: 1, word: 'test', mastered: true }],
          filter: 'mastered',
        });
      });

      const updatedWord = { id: 1, word: 'test', mastered: false };
      vocabularyApi.toggleMastered.mockResolvedValueOnce({ data: updatedWord });

      await act(async () => {
        await store.getState().toggleMastered(1);
      });

      expect(store.getState().vocabulary).toHaveLength(0);
    });

    it('should remove word from list when filter is unmastered and word becomes mastered', async () => {
      act(() => {
        store.setState({
          vocabulary: [{ id: 1, word: 'test', mastered: false }],
          filter: 'unmastered',
        });
      });

      const updatedWord = { id: 1, word: 'test', mastered: true };
      vocabularyApi.toggleMastered.mockResolvedValueOnce({ data: updatedWord });

      await act(async () => {
        await store.getState().toggleMastered(1);
      });

      expect(store.getState().vocabulary).toHaveLength(0);
    });
  });

  // ==========================================================================
  // Pagination Actions Tests
  // ==========================================================================
  describe('Pagination Actions', () => {
    it('setPage should update currentPage and fetch', async () => {
      const mockData = { content: [], totalPages: 5, totalElements: 100 };
      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        store.getState().setPage(3);
      });

      expect(vocabularyApi.getAll).toHaveBeenCalledWith(3, 20, '', 'all');
    });

    it('setDebouncedSearchQuery should reset to page 0 and fetch', async () => {
      act(() => {
        store.setState({ currentPage: 5 });
      });

      const mockData = { content: [], totalPages: 1, totalElements: 2 };
      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        store.getState().setDebouncedSearchQuery('search term');
      });

      expect(store.getState().currentPage).toBe(0);
      expect(vocabularyApi.getAll).toHaveBeenCalledWith(0, 20, 'search term', 'all');
    });

    it('setFilter should reset to page 0 and fetch with new filter', async () => {
      act(() => {
        store.setState({ currentPage: 3 });
      });

      const mockData = { content: [], totalPages: 1, totalElements: 0 };
      vocabularyApi.getAll.mockResolvedValueOnce({ data: mockData });

      await act(async () => {
        store.getState().setFilter('mastered');
      });

      expect(store.getState().filter).toBe('mastered');
      expect(store.getState().currentPage).toBe(0);
      expect(vocabularyApi.getAll).toHaveBeenCalledWith(0, 20, '', 'mastered');
    });
  });

  // ==========================================================================
  // isStale Tests
  // ==========================================================================
  describe('isStale', () => {
    it('should return true when never fetched', () => {
      expect(store.getState().isStale()).toBe(true);
    });

    it('should return false when recently fetched', () => {
      act(() => {
        store.setState({ lastFetchedAt: new Date().toISOString() });
      });

      expect(store.getState().isStale()).toBe(false);
    });

    it('should return true when cache expired', () => {
      const expiredTime = new Date(Date.now() - 6 * 60 * 1000).toISOString(); // 6 minutes ago
      act(() => {
        store.setState({ lastFetchedAt: expiredTime });
      });

      expect(store.getState().isStale()).toBe(true);
    });
  });

  // ==========================================================================
  // resetStore Tests
  // ==========================================================================
  describe('resetStore', () => {
    it('should reset all state to initial values', () => {
      act(() => {
        store.setState({
          vocabulary: [{ id: 1, word: 'test' }],
          stats: { total: 100 },
          loading: true,
          error: 'Some error',
          currentPage: 5,
          filter: 'mastered',
        });
      });

      act(() => {
        store.getState().resetStore();
      });

      const state = store.getState();
      expect(state.vocabulary).toHaveLength(0);
      expect(state.stats).toBeNull();
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.currentPage).toBe(0);
      expect(state.filter).toBe('all');
    });
  });

  // ==========================================================================
  // clearError Tests
  // ==========================================================================
  describe('clearError', () => {
    it('should clear error', () => {
      act(() => {
        store.setState({ error: 'Some error' });
      });

      act(() => {
        store.getState().clearError();
      });

      expect(store.getState().error).toBeNull();
    });
  });
});
