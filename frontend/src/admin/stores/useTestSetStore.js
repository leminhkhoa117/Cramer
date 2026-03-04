/**
 * Zustand store for Test Set management in Admin CMS.
 * Manages test sets, tests within sets, and related CRUD operations.
 * Includes caching to avoid refetching on navigation.
 * 
 * @since 2025-12-26 - Phase 4: Test Storage Management System Overhaul
 */

import { create } from 'zustand';
import { testSetsApi, testsApi } from '../api/adminApi';

// Cache duration: 5 minutes
const CACHE_DURATION = 5 * 60 * 1000;

const useTestSetStore = create((set, get) => ({
    // ==================== STATE ====================
    
    // Test sets data
    testSets: [],
    selectedSet: null,
    selectedSetTests: [],
    
    // Loading states
    isLoading: false,
    isLoadingTests: false,
    
    // Error handling
    error: null,
    
    // Cache timestamp
    lastFetch: null,

    // ==================== ACTIONS - TEST SETS ====================

    /**
     * Fetch all test sets from API.
     * Uses cache if available and not expired.
     * @param {boolean} force - Force refresh regardless of cache
     * @returns {Promise<Array>} List of test sets
     */
    fetchTestSets: async (force = false) => {
        const { lastFetch, testSets, isLoading } = get();
        
        // Skip if already loading
        if (isLoading) return testSets;

        // Return cached if fresh
        if (!force && lastFetch && Date.now() - lastFetch < CACHE_DURATION && testSets.length > 0) {
            return testSets;
        }

        set({ isLoading: true, error: null });
        try {
            const data = await testSetsApi.getAll();
            set({ testSets: data, lastFetch: Date.now(), isLoading: false });
            return data;
        } catch (error) {
            console.error('[TestSetStore] Error fetching test sets:', error);
            set({ error: error.message || 'Failed to fetch test sets', isLoading: false });
            throw error;
        }
    },

    /**
     * Fetch a test set by ID (includes its tests).
     * @param {number} id - Test set ID
     * @returns {Promise<Object>} Test set with tests
     */
    fetchTestSetById: async (id) => {
        set({ isLoading: true, error: null });
        try {
            const data = await testSetsApi.getById(id);
            set({ 
                selectedSet: data, 
                selectedSetTests: data.tests || [], 
                isLoading: false 
            });
            return data;
        } catch (error) {
            console.error('[TestSetStore] Error fetching test set:', error);
            set({ error: error.message || 'Failed to fetch test set', isLoading: false });
            throw error;
        }
    },

    /**
     * Fetch tests for a specific test set.
     * @param {number} setId - Test set ID
     * @returns {Promise<Array>} List of tests
     */
    fetchTestsBySetId: async (setId) => {
        set({ isLoadingTests: true, error: null });
        try {
            const data = await testsApi.getBySetId(setId);
            set({ selectedSetTests: data, isLoadingTests: false });
            return data;
        } catch (error) {
            console.error('[TestSetStore] Error fetching tests:', error);
            set({ error: error.message || 'Failed to fetch tests', isLoadingTests: false });
            throw error;
        }
    },

    /**
     * Create a new test set.
     * @param {Object} data - Test set data
     * @returns {Promise<Object>} Created test set
     */
    createTestSet: async (data) => {
        set({ isLoading: true, error: null });
        try {
            const newSet = await testSetsApi.create(data);
            set(state => ({
                testSets: [...state.testSets, newSet],
                isLoading: false
            }));
            return newSet;
        } catch (error) {
            console.error('[TestSetStore] Error creating test set:', error);
            set({ error: error.message || 'Failed to create test set', isLoading: false });
            throw error;
        }
    },

    /**
     * Update an existing test set.
     * @param {number} id - Test set ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated test set
     */
    updateTestSet: async (id, data) => {
        set({ isLoading: true, error: null });
        try {
            const updated = await testSetsApi.update(id, data);
            set(state => ({
                testSets: state.testSets.map(s => s.id === id ? updated : s),
                selectedSet: state.selectedSet?.id === id 
                    ? { ...state.selectedSet, ...updated } 
                    : state.selectedSet,
                isLoading: false
            }));
            return updated;
        } catch (error) {
            console.error('[TestSetStore] Error updating test set:', error);
            set({ error: error.message || 'Failed to update test set', isLoading: false });
            throw error;
        }
    },

    /**
     * Delete a test set.
     * @param {number} id - Test set ID
     */
    deleteTestSet: async (id) => {
        set({ isLoading: true, error: null });
        try {
            await testSetsApi.delete(id);
            set(state => ({
                testSets: state.testSets.filter(s => s.id !== id),
                selectedSet: state.selectedSet?.id === id ? null : state.selectedSet,
                selectedSetTests: state.selectedSet?.id === id ? [] : state.selectedSetTests,
                isLoading: false
            }));
        } catch (error) {
            console.error('[TestSetStore] Error deleting test set:', error);
            set({ error: error.message || 'Failed to delete test set', isLoading: false });
            throw error;
        }
    },

    /**
     * Publish or unpublish a test set.
     * @param {number} id - Test set ID
     * @param {boolean} publish - True to publish, false to unpublish
     * @returns {Promise<Object>} Updated test set
     */
    publishTestSet: async (id, publish = true) => {
        try {
            const updated = publish
                ? await testSetsApi.publish(id)
                : await testSetsApi.unpublish(id);
            set(state => ({
                testSets: state.testSets.map(s => s.id === id ? updated : s),
                selectedSet: state.selectedSet?.id === id
                    ? { ...state.selectedSet, ...updated }
                    : state.selectedSet
            }));
            return updated;
        } catch (error) {
            console.error('[TestSetStore] Error publishing test set:', error);
            set({ error: error.message || 'Failed to publish test set' });
            throw error;
        }
    },

    // ==================== ACTIONS - TESTS ====================

    /**
     * Create a test in the selected set.
     * @param {number} setId - Test set ID
     * @param {Object} data - Test data
     * @returns {Promise<Object>} Created test
     */
    createTest: async (setId, data) => {
        set({ isLoadingTests: true, error: null });
        try {
            const newTest = await testsApi.create(setId, data);
            set(state => ({
                selectedSetTests: [...state.selectedSetTests, newTest],
                isLoadingTests: false
            }));
            return newTest;
        } catch (error) {
            console.error('[TestSetStore] Error creating test:', error);
            set({ error: error.message || 'Failed to create test', isLoadingTests: false });
            throw error;
        }
    },

    /**
     * Update a test.
     * @param {number} id - Test ID
     * @param {Object} data - Updated data
     * @returns {Promise<Object>} Updated test
     */
    updateTest: async (id, data) => {
        try {
            const updated = await testsApi.update(id, data);
            set(state => ({
                selectedSetTests: state.selectedSetTests.map(t => t.id === id ? updated : t)
            }));
            return updated;
        } catch (error) {
            console.error('[TestSetStore] Error updating test:', error);
            set({ error: error.message || 'Failed to update test' });
            throw error;
        }
    },

    /**
     * Delete a test.
     * @param {number} id - Test ID
     */
    deleteTest: async (id) => {
        try {
            await testsApi.delete(id);
            set(state => ({
                selectedSetTests: state.selectedSetTests.filter(t => t.id !== id)
            }));
        } catch (error) {
            console.error('[TestSetStore] Error deleting test:', error);
            set({ error: error.message || 'Failed to delete test' });
            throw error;
        }
    },

    /**
     * Publish or unpublish a test.
     * @param {number} id - Test ID
     * @param {boolean} publish - True to publish, false to unpublish
     * @returns {Promise<Object>} Updated test
     */
    publishTest: async (id, publish = true) => {
        try {
            const updated = publish
                ? await testsApi.publish(id)
                : await testsApi.unpublish(id);
            set(state => ({
                selectedSetTests: state.selectedSetTests.map(t => t.id === id ? updated : t)
            }));
            return updated;
        } catch (error) {
            console.error('[TestSetStore] Error publishing test:', error);
            set({ error: error.message || 'Failed to publish test' });
            throw error;
        }
    },

    /**
     * Update hashtags for a test.
     * @param {number} id - Test ID
     * @param {Array<string>} hashtagCodes - List of hashtag codes
     * @returns {Promise<Object>} Updated test
     */
    updateTestHashtags: async (id, hashtagCodes) => {
        try {
            const updated = await testsApi.updateHashtags(id, hashtagCodes);
            set(state => ({
                selectedSetTests: state.selectedSetTests.map(t => t.id === id ? updated : t)
            }));
            return updated;
        } catch (error) {
            console.error('[TestSetStore] Error updating test hashtags:', error);
            set({ error: error.message || 'Failed to update test hashtags' });
            throw error;
        }
    },

    /**
     * Duplicate a test.
     * @param {number} id - Test ID to duplicate
     * @param {number} newTestNumber - Test number for the duplicate
     * @returns {Promise<Object>} Created duplicate test
     */
    duplicateTest: async (id, newTestNumber) => {
        set({ isLoadingTests: true, error: null });
        try {
            const duplicate = await testsApi.duplicate(id, newTestNumber);
            set(state => ({
                selectedSetTests: [...state.selectedSetTests, duplicate],
                isLoadingTests: false
            }));
            return duplicate;
        } catch (error) {
            console.error('[TestSetStore] Error duplicating test:', error);
            set({ error: error.message || 'Failed to duplicate test', isLoadingTests: false });
            throw error;
        }
    },

    // ==================== SELECTORS & UTILITIES ====================

    /**
     * Select a test set (local state only).
     * @param {Object} testSet - Test set to select
     */
    selectSet: (testSet) => set({ selectedSet: testSet }),

    /**
     * Clear current selection.
     */
    clearSelection: () => set({ selectedSet: null, selectedSetTests: [] }),

    /**
     * Clear error state.
     */
    clearError: () => set({ error: null }),

    /**
     * Invalidate cache to force next fetch.
     */
    invalidateCache: () => set({ lastFetch: null }),

    /**
     * Get test set by ID from local state.
     * @param {number} id - Test set ID
     * @returns {Object|undefined} Test set or undefined
     */
    getTestSetById: (id) => {
        const { testSets } = get();
        return testSets.find(s => s.id === id);
    },

    /**
     * Get test set by code from local state.
     * @param {string} code - Test set code
     * @returns {Object|undefined} Test set or undefined
     */
    getTestSetByCode: (code) => {
        const { testSets } = get();
        return testSets.find(s => s.code === code);
    },

    /**
     * Get test by ID from selected set tests.
     * @param {number} id - Test ID
     * @returns {Object|undefined} Test or undefined
     */
    getTestById: (id) => {
        const { selectedSetTests } = get();
        return selectedSetTests.find(t => t.id === id);
    },

    /**
     * Reset store to initial state.
     */
    reset: () => set({
        testSets: [],
        selectedSet: null,
        selectedSetTests: [],
        isLoading: false,
        isLoadingTests: false,
        error: null,
        lastFetch: null
    })
}));

export default useTestSetStore;
