/**
 * Zustand store for Hashtag management in Admin CMS.
 * Manages hashtags for test categorization and filtering.
 * Includes caching and search functionality.
 * 
 * @since 2025-12-26 - Phase 4: Test Storage Management System Overhaul
 */

import { create } from 'zustand';
import { hashtagsApi } from '../api/adminApi';

// Cache duration: 10 minutes (hashtags change less often)
const CACHE_DURATION = 10 * 60 * 1000;

const useHashtagStore = create((set, get) => ({
    // ==================== STATE ====================
    
    // All hashtags
    hashtags: [],
    
    // Grouped by category (e.g., { topic: [...], theme: [...], difficulty: [...] })
    byCategory: {},
    
    // Search results
    searchResults: [],
    
    // Popular hashtags
    popularHashtags: [],
    
    // Loading states
    isLoading: false,
    isSearching: false,
    
    // Error handling
    error: null,
    
    // Cache timestamp
    lastFetch: null,

    // ==================== ACTIONS ====================

    /**
     * Fetch all hashtags from API.
     * Groups hashtags by category for easy access.
     * Uses cache if available and not expired.
     * @param {boolean} force - Force refresh regardless of cache
     * @returns {Promise<Array>} List of hashtags
     */
    fetchHashtags: async (force = false) => {
        const { lastFetch, hashtags, isLoading } = get();
        
        // Skip if already loading
        if (isLoading) return hashtags;

        // Return cached if fresh
        if (!force && lastFetch && Date.now() - lastFetch < CACHE_DURATION && hashtags.length > 0) {
            return hashtags;
        }

        set({ isLoading: true, error: null });
        try {
            const data = await hashtagsApi.getAll();

            // Group by category
            const byCategory = data.reduce((acc, tag) => {
                const cat = tag.category || 'other';
                if (!acc[cat]) acc[cat] = [];
                acc[cat].push(tag);
                return acc;
            }, {});

            set({
                hashtags: data,
                byCategory,
                lastFetch: Date.now(),
                isLoading: false
            });
            return data;
        } catch (error) {
            console.error('[HashtagStore] Error fetching hashtags:', error);
            set({ error: error.message || 'Failed to fetch hashtags', isLoading: false });
            throw error;
        }
    },

    /**
     * Fetch hashtags by category.
     * @param {string} category - Hashtag category
     * @returns {Promise<Array>} List of hashtags in category
     */
    fetchByCategory: async (category) => {
        set({ isLoading: true, error: null });
        try {
            const data = await hashtagsApi.getByCategory(category);
            set(state => ({
                byCategory: { ...state.byCategory, [category]: data },
                isLoading: false
            }));
            return data;
        } catch (error) {
            console.error('[HashtagStore] Error fetching category:', error);
            set({ error: error.message || 'Failed to fetch category', isLoading: false });
            throw error;
        }
    },

    /**
     * Search hashtags by query.
     * Minimum 2 characters required.
     * @param {string} query - Search query
     * @returns {Promise<Array>} Matching hashtags
     */
    searchHashtags: async (query) => {
        if (!query || query.length < 2) {
            set({ searchResults: [] });
            return [];
        }

        set({ isSearching: true });
        try {
            const data = await hashtagsApi.search(query);
            set({ searchResults: data, isSearching: false });
            return data;
        } catch (error) {
            console.error('[HashtagStore] Error searching hashtags:', error);
            set({ error: error.message || 'Failed to search hashtags', isSearching: false });
            throw error;
        }
    },

    /**
     * Fetch popular hashtags.
     * @param {number} limit - Maximum number of hashtags to return
     * @returns {Promise<Array>} Popular hashtags
     */
    fetchPopular: async (limit = 10) => {
        try {
            const data = await hashtagsApi.getPopular(limit);
            set({ popularHashtags: data });
            return data;
        } catch (error) {
            console.error('[HashtagStore] Error fetching popular hashtags:', error);
            set({ error: error.message || 'Failed to fetch popular hashtags' });
            throw error;
        }
    },

    /**
     * Create a new hashtag.
     * @param {Object} data - Hashtag data (name, category, code, displayName)
     * @returns {Promise<Object>} Created hashtag
     */
    createHashtag: async (data) => {
        set({ isLoading: true, error: null });
        try {
            const newTag = await hashtagsApi.create(data);
            set(state => {
                const category = newTag.category || 'other';
                return {
                    hashtags: [...state.hashtags, newTag],
                    byCategory: {
                        ...state.byCategory,
                        [category]: [...(state.byCategory[category] || []), newTag]
                    },
                    isLoading: false
                };
            });
            return newTag;
        } catch (error) {
            console.error('[HashtagStore] Error creating hashtag:', error);
            set({ error: error.message || 'Failed to create hashtag', isLoading: false });
            throw error;
        }
    },

    /**
     * Update an existing hashtag.
     * Invalidates byCategory cache to ensure consistency.
     * @param {number} id - Hashtag ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated hashtag
     */
    updateHashtag: async (id, data) => {
        set({ isLoading: true, error: null });
        try {
            const updated = await hashtagsApi.update(id, data);
            set(state => ({
                hashtags: state.hashtags.map(h => h.id === id ? updated : h),
                isLoading: false
            }));
            // Invalidate byCategory cache since category might have changed
            get().fetchHashtags(true);
            return updated;
        } catch (error) {
            console.error('[HashtagStore] Error updating hashtag:', error);
            set({ error: error.message || 'Failed to update hashtag', isLoading: false });
            throw error;
        }
    },

    /**
     * Delete a hashtag.
     * @param {number} id - Hashtag ID
     */
    deleteHashtag: async (id) => {
        set({ isLoading: true, error: null });
        try {
            await hashtagsApi.delete(id);
            set(state => ({
                hashtags: state.hashtags.filter(h => h.id !== id),
                isLoading: false
            }));
            // Invalidate byCategory cache
            get().fetchHashtags(true);
        } catch (error) {
            console.error('[HashtagStore] Error deleting hashtag:', error);
            set({ error: error.message || 'Failed to delete hashtag', isLoading: false });
            throw error;
        }
    },

    // ==================== SELECTORS ====================

    /**
     * Get hashtags by their IDs from local state.
     * @param {Array<number>} ids - List of hashtag IDs
     * @returns {Array} Matching hashtags
     */
    getHashtagsByIds: (ids) => {
        const { hashtags } = get();
        if (!ids || ids.length === 0) return [];
        return hashtags.filter(h => ids.includes(h.id));
    },

    /**
     * Get hashtags by their codes from local state.
     * @param {Array<string>} codes - List of hashtag codes
     * @returns {Array} Matching hashtags
     */
    getHashtagsByCodes: (codes) => {
        const { hashtags } = get();
        if (!codes || codes.length === 0) return [];
        return hashtags.filter(h => codes.includes(h.code));
    },

    /**
     * Get hashtag by ID from local state.
     * @param {number} id - Hashtag ID
     * @returns {Object|undefined} Hashtag or undefined
     */
    getHashtagById: (id) => {
        const { hashtags } = get();
        return hashtags.find(h => h.id === id);
    },

    /**
     * Get hashtag by code from local state.
     * @param {string} code - Hashtag code
     * @returns {Object|undefined} Hashtag or undefined
     */
    getHashtagByCode: (code) => {
        const { hashtags } = get();
        return hashtags.find(h => h.code === code);
    },

    /**
     * Get all categories available.
     * @returns {Array<string>} List of category names
     */
    getCategories: () => {
        const { byCategory } = get();
        return Object.keys(byCategory);
    },

    /**
     * Get hashtags for a specific category from local state.
     * @param {string} category - Category name
     * @returns {Array} Hashtags in category
     */
    getByCategory: (category) => {
        const { byCategory } = get();
        return byCategory[category] || [];
    },

    // ==================== UTILITIES ====================

    /**
     * Clear search results.
     */
    clearSearch: () => set({ searchResults: [] }),

    /**
     * Clear error state.
     */
    clearError: () => set({ error: null }),

    /**
     * Invalidate cache to force next fetch.
     */
    invalidateCache: () => set({ lastFetch: null }),

    /**
     * Reset store to initial state.
     */
    reset: () => set({
        hashtags: [],
        byCategory: {},
        searchResults: [],
        popularHashtags: [],
        isLoading: false,
        isSearching: false,
        error: null,
        lastFetch: null
    })
}));

export default useHashtagStore;
