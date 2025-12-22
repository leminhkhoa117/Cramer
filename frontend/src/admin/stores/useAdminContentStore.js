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
    // Topics and tests data
    topics: [],
    selectedTest: null,

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

    // Error
    error: null,

    // Cache timestamps
    lastTopicsFetch: null,
    lastOverviewFetch: null,
    isInitialized: false,

    // =====================
    // ACTIONS
    // =====================

    /**
     * Fetch topics with tests from API
     * @param {boolean} force - Force refresh regardless of cache
     */
    fetchTopics: async (force = false) => {
        const { lastTopicsFetch, isLoading, searchQuery: prevSearch, statusFilter: prevStatus } = get();
        const now = Date.now();

        // Skip if already loading
        if (isLoading) return;

        // Skip if cache is still valid (only for initial load, not for filter changes)
        if (!force && lastTopicsFetch && (now - lastTopicsFetch) < CACHE_DURATION_MS) {
            return;
        }

        set({ isLoading: true, error: null });

        try {
            const { searchQuery, statusFilter } = get();
            const topics = await adminApi.content.getTopics({
                search: searchQuery || undefined,
                status: statusFilter !== 'ALL' ? statusFilter : undefined
            });

            // Auto-expand first 2 topics if not already expanded
            const { expandedTopics } = get();
            let newExpandedTopics = [...expandedTopics];
            if (expandedTopics.length === 0 && topics.length > 0) {
                newExpandedTopics = topics.slice(0, 2).map(t => t.id);
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
                error: 'Không thể tải danh sách đề thi',
                isLoading: false
            });
        }
    },

    /**
     * Fetch content overview stats
     * @param {boolean} force - Force refresh regardless of cache
     */
    fetchOverview: async (force = false) => {
        const { lastOverviewFetch, isLoadingOverview } = get();
        const now = Date.now();

        // Skip if already loading
        if (isLoadingOverview) return;

        // Skip if cache is still valid
        if (!force && lastOverviewFetch && (now - lastOverviewFetch) < CACHE_DURATION_MS) {
            return;
        }

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

    /**
     * Initialize content page - uses cache if available
     */
    initializeContent: async () => {
        const { fetchTopics, fetchOverview } = get();
        await Promise.all([
            fetchTopics(),
            fetchOverview()
        ]);
    },

    /**
     * Fetch test details
     */
    fetchTestDetails: async (examSource, testNumber) => {
        set({ isLoadingTest: true, selectedTest: null, error: null });

        try {
            const testDetails = await adminApi.content.getTestDetails(examSource, testNumber);
            set({
                selectedTest: testDetails,
                isLoadingTest: false
            });
            return testDetails;
        } catch (error) {
            console.error('Error fetching test details:', error);
            set({
                error: 'Không tìm thấy đề thi',
                isLoadingTest: false
            });
            return null;
        }
    },

    /**
     * Set search query
     */
    setSearchQuery: (query) => {
        set({ searchQuery: query });
    },

    /**
     * Set status filter and refetch
     */
    setStatusFilter: (status) => {
        set({ statusFilter: status, lastTopicsFetch: null }); // Invalidate cache
        get().fetchTopics(true);
    },

    /**
     * Set view mode
     */
    setViewMode: (mode) => {
        set({ viewMode: mode });
    },

    /**
     * Toggle topic expansion
     */
    toggleExpand: (topicId) => {
        set(state => ({
            expandedTopics: state.expandedTopics.includes(topicId)
                ? state.expandedTopics.filter(id => id !== topicId)
                : [...state.expandedTopics, topicId]
        }));
    },

    /**
     * Expand all topics
     */
    expandAll: () => {
        set(state => ({
            expandedTopics: state.topics.map(t => t.id)
        }));
    },

    /**
     * Collapse all topics
     */
    collapseAll: () => {
        set({ expandedTopics: [] });
    },

    /**
     * Clear selected test
     */
    clearSelectedTest: () => {
        set({ selectedTest: null });
    },

    /**
     * Reset all filters
     */
    resetFilters: () => {
        set({
            searchQuery: '',
            statusFilter: 'ALL',
            lastTopicsFetch: null // Invalidate cache
        });
        get().fetchTopics(true);
    },

    /**
     * Get all tests as flat list (for grid view)
     */
    getAllTests: () => {
        const { topics } = get();
        const tests = [];
        topics.forEach(topic => {
            (topic.tests || []).forEach(test => {
                tests.push({
                    ...test,
                    topicId: topic.id,
                    topicName: topic.displayName,
                    topicSource: topic.source
                });
            });
        });
        return tests;
    },

    /**
     * Get filtered tests based on search (client-side filtering)
     */
    getFilteredTopics: () => {
        const { topics, searchQuery, statusFilter } = get();

        if (!searchQuery && statusFilter === 'ALL') {
            return topics;
        }

        return topics.map(topic => ({
            ...topic,
            tests: (topic.tests || []).filter(test => {
                const matchesSearch = !searchQuery ||
                    test.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    topic.displayName.toLowerCase().includes(searchQuery.toLowerCase());
                const matchesStatus = statusFilter === 'ALL' || test.status === statusFilter;
                return matchesSearch && matchesStatus;
            })
        })).filter(topic => topic.tests.length > 0 || !searchQuery);
    },

    /**
     * Create a new test
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
    }
}));

export default useAdminContentStore;
