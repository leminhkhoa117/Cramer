import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { vocabularyApi } from '../api/backendApi';

const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

const useVocabularyStore = create(
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
      filter: 'all', // 'all', 'mastered', 'unmastered'

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
          // Server-side filtering: pass filter to API
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
          
          // Add to current list
          const currentVocabulary = get().vocabulary;
          set({
            vocabulary: [newWord, ...currentVocabulary],
            loading: false,
          });
          
          // Refresh stats
          get().fetchStats();
          
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
          
          // Update in current list
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
          
          // Remove from current list
          const currentVocabulary = get().vocabulary;
          set({
            vocabulary: currentVocabulary.filter(v => v.id !== id),
            loading: false,
          });
          
          // Refresh stats
          get().fetchStats();
          
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
          
          // Update in current list
          const currentVocabulary = get().vocabulary;
          const currentFilter = get().filter;
          
          // If filtering by mastered status, we may need to remove the item
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
          
          // Refresh stats
          get().fetchStats();
          
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
        // Re-fetch with the new search query
        get().fetchVocabulary(0, null, query);
      },

      setFilter: (newFilter) => {
        set({ filter: newFilter, currentPage: 0 });
        // Re-fetch with new filter (server-side)
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
    { name: 'vocabulary-store' }
  )
);

export default useVocabularyStore;
