import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { vocabularyApi, getApiError } from '../lib/api';

/**
 * Vocabulary store (SPEC-F16). Server-side paged/filtered list + stats + CRUD + AI translation.
 * Backend returns PageResponse<VocabularyView>.
 */
const useVocabularyStore = create(
  devtools(
    (set, get) => ({
      vocabulary: [],
      stats: null,
      loading: false,
      error: null,
      page: 0,
      size: 20,
      totalPages: 0,
      totalElements: 0,
      search: '',
      filter: 'all', // all | mastered | unmastered
      translating: false,

      fetchVocabulary: async () => {
        const { page, size, search, filter } = get();
        set({ loading: true, error: null }, false, 'fetchVocabulary/start');
        try {
          const res = await vocabularyApi.list({ page, size, search: search || undefined, filter });
          set({
            vocabulary: res.content || [],
            totalPages: res.totalPages || 0,
            totalElements: res.totalElements || 0,
            loading: false,
          }, false, 'fetchVocabulary/success');
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchVocabulary/error');
        }
      },

      fetchStats: async () => {
        try {
          set({ stats: await vocabularyApi.stats() }, false, 'fetchStats');
        } catch { /* non-critical */ }
      },

      setPage: (page) => { set({ page }); return get().fetchVocabulary(); },
      setSearch: (search) => set({ search }),
      setFilter: (filter) => { set({ filter, page: 0 }); return get().fetchVocabulary(); },

      addWord: async (body) => {
        const created = await vocabularyApi.create(body);
        await get().fetchVocabulary();
        await get().fetchStats();
        return created;
      },
      updateWord: async (id, body) => {
        const updated = await vocabularyApi.update(id, body);
        set((s) => ({ vocabulary: s.vocabulary.map((v) => (v.id === id ? updated : v)) }), false, 'updateWord');
        return updated;
      },
      deleteWord: async (id) => {
        await vocabularyApi.remove(id);
        set((s) => ({ vocabulary: s.vocabulary.filter((v) => v.id !== id) }), false, 'deleteWord');
        await get().fetchStats();
      },
      toggleMastered: async (id) => {
        const updated = await vocabularyApi.toggleMastered(id);
        set((s) => ({ vocabulary: s.vocabulary.map((v) => (v.id === id ? updated : v)) }), false, 'toggleMastered');
        get().fetchStats();
        return updated;
      },
      translateWord: async (word) => {
        set({ translating: true }, false, 'translateWord/start');
        try {
          return await vocabularyApi.translate(word);
        } finally {
          set({ translating: false }, false, 'translateWord/done');
        }
      },
    }),
    { name: 'VocabularyStore' }
  )
);

export default useVocabularyStore;
