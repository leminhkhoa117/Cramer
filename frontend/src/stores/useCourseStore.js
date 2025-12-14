import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { courseApi } from '../api/backendApi';

const useCourseStore = create(
  devtools(
    (set, get) => ({
      // STATE
      courses: [],
      courseTests: {}, // { [courseName]: array }
      loading: false,
      error: null,
      lastFetchedAt: null,

      // PAGINATION/FILTER STATE
      currentPage: 0,
      pageSize: 10,
      totalPages: 0,
      totalElements: 0,
      searchQuery: '',
      debouncedSearchQuery: '',

      // ACTIONS

      /**
       * Fetch courses with pagination and search
       */
      fetchCourses: async (page, size, search) => {
        set({ loading: true, error: null }, false, 'fetchCourses/pending');

        try {
          const response = await courseApi.getAll(page, size, search);
          const data = response.data; // Unwrap axios response
          
          set({
            courses: data.content || [],
            currentPage: data.number ?? page,
            pageSize: data.size ?? size,
            totalPages: data.totalPages ?? 0,
            totalElements: data.totalElements ?? 0,
            loading: false,
            lastFetchedAt: new Date(),
          }, false, 'fetchCourses/fulfilled');

          return data;
        } catch (error) {
          set({
            loading: false,
            error: error.message || 'Failed to fetch courses',
          }, false, 'fetchCourses/rejected');
          throw error;
        }
      },

      /**
       * Fetch tests for a specific course (with caching)
       */
      fetchCourseTests: async (courseName) => {
        const { courseTests } = get();

        // Return cached data if available
        if (courseTests[courseName]) {
          return courseTests[courseName];
        }

        set({ loading: true, error: null }, false, 'fetchCourseTests/pending');

        try {
          const response = await courseApi.getTestsByCourse(courseName);
          const tests = response.data; // Unwrap axios response
          
          set((state) => ({
            courseTests: {
              ...state.courseTests,
              [courseName]: tests,
            },
            loading: false,
          }), false, 'fetchCourseTests/fulfilled');

          return tests;
        } catch (error) {
          set({
            loading: false,
            error: error.message || `Failed to fetch tests for ${courseName}`,
          }, false, 'fetchCourseTests/rejected');
          throw error;
        }
      },

      /**
       * Set current page
       */
      setPage: (page) => {
        set({ currentPage: page }, false, 'setPage');
      },

      /**
       * Set page size
       */
      setPageSize: (size) => {
        set({ pageSize: size, currentPage: 0 }, false, 'setPageSize');
      },

      /**
       * Set search query (immediate)
       */
      setSearchQuery: (query) => {
        set({ searchQuery: query }, false, 'setSearchQuery');
      },

      /**
       * Set debounced search query (for API calls)
       */
      setDebouncedSearchQuery: (query) => {
        set({ debouncedSearchQuery: query, currentPage: 0 }, false, 'setDebouncedSearchQuery');
      },

      /**
       * Get cached tests for a course
       */
      getCachedTests: (courseName) => {
        const { courseTests } = get();
        return courseTests[courseName] || null;
      },

      /**
       * Clear tests cache
       */
      clearTestsCache: () => {
        set({ courseTests: {} }, false, 'clearTestsCache');
      },

      /**
       * Reset filters and pagination to defaults
       */
      resetFilters: () => {
        set({
          currentPage: 0,
          pageSize: 10,
          searchQuery: '',
          debouncedSearchQuery: '',
        }, false, 'resetFilters');
      },
    }),
    {
      name: 'course-store',
    }
  )
);

export default useCourseStore;
