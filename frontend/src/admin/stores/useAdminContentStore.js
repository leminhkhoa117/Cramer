/**
 * Admin Content Store - Zustand store cho quản lý nội dung đề thi trong Admin CMS
 * 
 * Quản lý state cho danh sách topics/tests, filters, và các actions.
 * Bao gồm caching để tránh fetch lại mỗi khi navigate.
 */
import { create } from 'zustand';
import adminApi from '../api/adminApi';

// Cache duration: 5 minutes
const CACHE_DURATION_MS = 5 * 60 * 1000;

const useAdminContentStore = create((set, get) => ({
    // Topics and tests data (Hierarchical: Topic/TestSet -> Tests)
    topics: [],
    selectedTest: null,
    selectedSet: null,

    // Overview stats
    overview: {
        totalTopics: 0,
        totalTests: 0,
        publishedTests: 0,
        draftTests: 0,
        reviewTests: 0,
        totalQuestions: 0,
        totalAttempts: 0
    },

    // Filters
    searchQuery: '',
    statusFilter: 'ALL',
    viewMode: 'tree', // 'tree' | 'grid'

    // Expanded topics (for tree view)
    expandedTopics: [],

    // Loading states
    isLoading: false,
    isLoadingOverview: false,
    isLoadingTest: false,
    isLoadingSet: false,

    // Error
    error: null,

    // Cache timestamps
    lastTopicsFetch: null,
    lastOverviewFetch: null,
    isInitialized: false,

    // =====================
    // TOPIC / TEST SET ACTIONS
    // =====================

    /**
     * Fetch topics with tests from API
     * @param {boolean} force - Force refresh regardless of cache
     */
    fetchTopics: async (force = false) => {
        const { lastTopicsFetch, isLoading } = get();
        const now = Date.now();

        if (isLoading) return;
        if (!force && lastTopicsFetch && (now - lastTopicsFetch) < CACHE_DURATION_MS) return;

        set({ isLoading: true, error: null });

        try {
            const { searchQuery, statusFilter } = get();
            const topics = await adminApi.content.getTopics({
                search: searchQuery || undefined,
                status: statusFilter !== 'ALL' ? statusFilter : undefined
            });

            // Auto-expand first few topics if not already expanded
            const { expandedTopics } = get();
            let newExpandedTopics = [...expandedTopics];
            if (expandedTopics.length === 0 && topics.length > 0) {
                newExpandedTopics = topics.slice(0, 3).map(t => t.id);
            }

            set({
                topics,
                expandedTopics: newExpandedTopics,
                isLoading: false,
                lastTopicsFetch: Date.now(),
                isInitialized: true
            });
        } catch (error) {
            console.error('Error fetching topics:', error);
            set({
                error: 'Không thể tải danh sách bộ đề',
                isLoading: false
            });
        }
    },

    /**
     * Create a new test set (Topic)
     */
    createTestSet: async (setData) => {
        set({ isLoadingSet: true, error: null });
        try {
            const result = await adminApi.testSetsApi.create(setData);
            set({ lastTopicsFetch: null }); // Invalidate topics cache
            await get().fetchTopics(true);
            set({ isLoadingSet: false });
            return result;
        } catch (error) {
            console.error('Error creating test set:', error);
            set({
                error: error.response?.data?.error || 'Không thể tạo bộ đề mới',
                isLoadingSet: false
            });
            throw error;
        }
    },

    /**
     * Update an existing test set
     */
    updateTestSet: async (setId, setData) => {
        set({ isLoadingSet: true, error: null });
        try {
            const result = await adminApi.testSetsApi.update(setId, setData);
            set(state => ({
                topics: state.topics.map(t => t.id === setId ? { ...t, ...result } : t),
                isLoadingSet: false
            }));
            return result;
        } catch (error) {
            console.error('Error updating test set:', error);
            set({
                error: error.response?.data?.error || 'Không thể cập nhật bộ đề',
                isLoadingSet: false
            });
            throw error;
        }
    },

    /**
     * Delete a test set
     */
    deleteTestSet: async (setId) => {
        set({ isLoadingSet: true, error: null });
        try {
            await adminApi.testSetsApi.delete(setId);
            set(state => ({
                topics: state.topics.filter(t => t.id !== setId),
                isLoadingSet: false,
                lastTopicsFetch: null
            }));
        } catch (error) {
            console.error('Error deleting test set:', error);
            set({
                error: error.response?.data?.error || 'Không thể xóa bộ đề',
                isLoadingSet: false
            });
            throw error;
        }
    },

    // =====================
    // TEST ACTIONS
    // =====================

    /**
     * Create a new test within a set
     */
    createTest: async (testData) => {
        set({ isLoading: true, error: null });
        try {
            const result = await adminApi.content.createTest(testData);
            set({ lastTopicsFetch: null }); // Invalidate cache
            await get().fetchTopics(true); // Refresh list
            return result;
        } catch (error) {
            console.error('Error creating test:', error);
            set({
                error: error.response?.data?.error || 'Không thể tạo đề thi',
                isLoading: false
            });
            throw error;
        }
    },

    /**
     * Update test information
     */
    updateTest: async (testId, testData) => {
        set({ isLoadingTest: true, error: null });
        try {
            const result = await adminApi.testsApi.update(testId, testData);
            // Refresh hierarchical list to show updates
            await get().fetchTopics(true);
            set({ isLoadingTest: false });
            return result;
        } catch (error) {
            console.error('Error updating test:', error);
            set({
                error: error.response?.data?.error || 'Không thể cập nhật đề thi',
                isLoadingTest: false
            });
            throw error;
        }
    },

    /**
     * Delete a test
     */
    deleteTest: async (testId) => {
        set({ isLoading: true, error: null });
        try {
            await adminApi.testsApi.delete(testId);
            set({ lastTopicsFetch: null });
            await get().fetchTopics(true);
            set({ isLoading: false });
        } catch (error) {
            console.error('Error deleting test:', error);
            set({
                error: error.response?.data?.error || 'Không thể xóa đề thi',
                isLoading: false
            });
            throw error;
        }
    },

    /**
     * Update test hashtags
     */
    updateTestHashtags: async (testId, hashtagIds, primaryHashtagId) => {
        try {
            const result = await adminApi.testsApi.updateHashtags(testId, hashtagIds, primaryHashtagId);
            await get().fetchTopics(true);
            return result;
        } catch (error) {
            console.error('Error updating hashtags:', error);
            throw error;
        }
    },

    // =====================
    // CORE / SELECTOR ACTIONS
    // =====================

    /**
     * Fetch content overview stats
     */
    fetchOverview: async (force = false) => {
        const { lastOverviewFetch, isLoadingOverview } = get();
        if (isLoadingOverview) return;
        if (!force && lastOverviewFetch && (Date.now() - lastOverviewFetch) < CACHE_DURATION_MS) return;

        set({ isLoadingOverview: true });
        try {
            const overview = await adminApi.content.getOverview();
            set({
                overview,
                isLoadingOverview: false,
                lastOverviewFetch: Date.now()
            });
        } catch (error) {
            console.error('Error fetching overview:', error);
            set({ isLoadingOverview: false });
        }
    },

    initializeContent: async () => {
        const { fetchTopics, fetchOverview } = get();
        await Promise.all([fetchTopics(), fetchOverview()]);
    },

    fetchTestDetails: async (examSource, testNumber) => {
        set({ isLoadingTest: true, selectedTest: null, error: null });
        try {
            const testDetails = await adminApi.content.getTestDetails(examSource, testNumber);
            set({ selectedTest: testDetails, isLoadingTest: false });
            return testDetails;
        } catch (error) {
            console.error('Error fetching test details:', error);
            set({ error: 'Không tìm thấy đề thi', isLoadingTest: false });
            return null;
        }
    },

    setSearchQuery: (query) => set({ searchQuery: query }),
    setStatusFilter: (status) => {
        set({ statusFilter: status, lastTopicsFetch: null });
        get().fetchTopics(true);
    },
    setViewMode: (mode) => set({ viewMode: mode }),
    toggleExpand: (topicId) => set(state => ({
        expandedTopics: state.expandedTopics.includes(topicId)
            ? state.expandedTopics.filter(id => id !== topicId)
            : [...state.expandedTopics, topicId]
    })),
    expandAll: () => set(state => ({ expandedTopics: state.topics.map(t => t.id) })),
    collapseAll: () => set({ expandedTopics: [] }),
    clearSelectedTest: () => set({ selectedTest: null }),
    resetFilters: () => {
        set({ searchQuery: '', statusFilter: 'ALL', lastTopicsFetch: null });
        get().fetchTopics(true);
    },

    getAllTests: () => {
        const { topics } = get();
        const tests = [];
        topics.forEach(topic => {
            (topic.tests || []).forEach(test => {
                tests.push({
                    ...test,
                    topicId: topic.id,
                    topicName: topic.nameVi || topic.code,
                    topicSource: topic.code
                });
            });
        });
        return tests;
    },

    getFilteredTopics: () => {
        const { topics, searchQuery, statusFilter } = get();
        if (!searchQuery && statusFilter === 'ALL') return topics;

        return topics.map(topic => ({
            ...topic,
            tests: (topic.tests || []).filter(test => {
                const matchesSearch = !searchQuery ||
                    (test.nameVi && test.nameVi.toLowerCase().includes(searchQuery.toLowerCase())) ||
                    (test.nameEn && test.nameEn.toLowerCase().includes(searchQuery.toLowerCase())) ||
                    (topic.nameVi && topic.nameVi.toLowerCase().includes(searchQuery.toLowerCase())) ||
                    topic.code.toLowerCase().includes(searchQuery.toLowerCase());

                const testStatus = test.isPublished ? 'PUBLISHED' : 'DRAFT';
                const matchesStatus = statusFilter === 'ALL' || testStatus === statusFilter;
                return matchesSearch && matchesStatus;
            })
        })).filter(topic => topic.tests.length > 0 || !searchQuery);
    }
}));

export default useAdminContentStore;
