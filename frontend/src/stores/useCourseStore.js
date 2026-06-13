import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { courseApi, getApiError } from '../lib/api';

/**
 * Course catalog store (SPEC-F11). Uses /courses/v2 (List<TestSetView>) as the primary source,
 * with per-course test-number lists and details cached by code.
 */
const useCourseStore = create(
  devtools(
    (set, get) => ({
      courses: [],
      courseTests: {},     // code -> number[]
      courseDetails: {},   // code -> TestSetView
      loading: false,
      error: null,
      lastFetchedAt: null,

      fetchCourses: async (force = false) => {
        const { loading, lastFetchedAt } = get();
        if (loading) return;
        if (!force && lastFetchedAt && Date.now() - lastFetchedAt < 5 * 60 * 1000) return;
        set({ loading: true, error: null }, false, 'fetchCourses/start');
        try {
          const courses = await courseApi.listV2();
          set({ courses: courses || [], loading: false, lastFetchedAt: Date.now() }, false, 'fetchCourses/success');
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'fetchCourses/error');
        }
      },

      fetchCourseTests: async (code) => {
        if (get().courseTests[code]) return get().courseTests[code];
        try {
          const tests = await courseApi.tests(code);
          set((s) => ({ courseTests: { ...s.courseTests, [code]: tests || [] } }), false, 'fetchCourseTests');
          return tests || [];
        } catch (error) {
          set({ error: getApiError(error).message }, false, 'fetchCourseTests/error');
          return [];
        }
      },

      fetchCourseDetails: async (code) => {
        if (get().courseDetails[code]) return get().courseDetails[code];
        try {
          const details = await courseApi.details(code);
          set((s) => ({ courseDetails: { ...s.courseDetails, [code]: details } }), false, 'fetchCourseDetails');
          return details;
        } catch (error) {
          set({ error: getApiError(error).message }, false, 'fetchCourseDetails/error');
          return null;
        }
      },

      getCachedTests: (code) => get().courseTests[code] || null,
      getCachedDetails: (code) => get().courseDetails[code] || null,
    }),
    { name: 'CourseStore' }
  )
);

export default useCourseStore;
