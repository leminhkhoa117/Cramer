/**
 * Tests for useCourseStore
 *
 * Tests course fetching, caching, and search functionality against the
 * current lib/api-based store API (listV2 + per-course caching).
 *
 * @author Cramer Test Team
 * @since 2026-01-26
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act } from '@testing-library/react';

vi.mock('../../lib/api', () => ({
    courseApi: {
        listV2: vi.fn(),
        tests: vi.fn(),
        details: vi.fn(),
    },
    getApiError: (error) => ({ message: error?.message || 'Unknown error' }),
}));

import useCourseStore from '../../stores/useCourseStore';
import { courseApi } from '../../lib/api';

describe('useCourseStore', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();

        act(() => {
            useCourseStore.setState({
                courses: [],
                courseTests: {},
                courseDetails: {},
                loading: false,
                error: null,
                lastFetchedAt: null,
            });
        });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // =========================================================================
    // INITIAL STATE TESTS
    // =========================================================================
    describe('Initial State', () => {
        it('should have correct initial values', () => {
            const state = useCourseStore.getState();

            expect(state.courses).toEqual([]);
            expect(state.courseTests).toEqual({});
            expect(state.courseDetails).toEqual({});
            expect(state.loading).toBe(false);
            expect(state.error).toBe(null);
            expect(state.lastFetchedAt).toBe(null);
        });
    });

    // =========================================================================
    // FETCH COURSES TESTS
    // =========================================================================
    describe('fetchCourses()', () => {
        it('should set loading state when fetching', async () => {
            courseApi.listV2.mockImplementation(() =>
                new Promise(resolve => setTimeout(() => resolve([]), 100))
            );

            const fetchPromise = useCourseStore.getState().fetchCourses(true);

            expect(useCourseStore.getState().loading).toBe(true);

            await act(async () => {
                vi.advanceTimersByTime(100);
                await fetchPromise;
            });
        });

        it('should fetch courses successfully', async () => {
            const mockCourses = [
                { code: 'cam17', name: 'Cambridge 17' },
                { code: 'cam18', name: 'Cambridge 18' }
            ];

            courseApi.listV2.mockResolvedValueOnce(mockCourses);

            await act(async () => {
                await useCourseStore.getState().fetchCourses(true);
            });

            const newState = useCourseStore.getState();
            expect(newState.courses).toHaveLength(2);
            expect(newState.courses[0].code).toBe('cam17');
            expect(newState.loading).toBe(false);
            expect(newState.lastFetchedAt).not.toBe(null);
        });

        it('should handle fetch error gracefully', async () => {
            courseApi.listV2.mockRejectedValueOnce(new Error('Network error'));

            await act(async () => {
                await useCourseStore.getState().fetchCourses(true);
            });

            const newState = useCourseStore.getState();
            expect(newState.loading).toBe(false);
            expect(newState.error).toBe('Network error');
            expect(newState.courses).toEqual([]);
        });

        it('should serve from cache within 5 minutes', async () => {
            courseApi.listV2.mockResolvedValueOnce([
                { code: 'cam17', name: 'Cambridge 17' }
            ]);

            await act(async () => {
                await useCourseStore.getState().fetchCourses(true);
            });
            expect(courseApi.listV2).toHaveBeenCalledTimes(1);

            await act(async () => {
                await useCourseStore.getState().fetchCourses();
            });
            expect(courseApi.listV2).toHaveBeenCalledTimes(1);
        });

        it('should refetch after cache TTL expires', async () => {
            courseApi.listV2.mockResolvedValue([
                { code: 'cam17', name: 'Cambridge 17' }
            ]);

            await act(async () => {
                await useCourseStore.getState().fetchCourses(true);
            });
            expect(courseApi.listV2).toHaveBeenCalledTimes(1);

            await act(async () => {
                vi.advanceTimersByTime(6 * 60 * 1000);
                await useCourseStore.getState().fetchCourses();
            });
            expect(courseApi.listV2).toHaveBeenCalledTimes(2);
        });

        it('should skip fetch when already loading', async () => {
            courseApi.listV2.mockImplementation(() =>
                new Promise(resolve => setTimeout(() => resolve([]), 100))
            );

            const state = useCourseStore.getState();
            const first = state.fetchCourses(true);
            const second = state.fetchCourses(true);

            await act(async () => {
                vi.advanceTimersByTime(100);
                await first;
                await second;
            });

            expect(courseApi.listV2).toHaveBeenCalledTimes(1);
        });
    });

    // =========================================================================
    // FETCH COURSE TESTS TESTS
    // =========================================================================
    describe('fetchCourseTests()', () => {
        it('should fetch tests for a course', async () => {
            courseApi.tests.mockResolvedValueOnce([1, 2, 3]);

            let result;
            await act(async () => {
                result = await useCourseStore.getState().fetchCourseTests('cam17');
            });

            expect(result).toEqual([1, 2, 3]);
            expect(courseApi.tests).toHaveBeenCalledWith('cam17');
            expect(useCourseStore.getState().courseTests['cam17']).toEqual([1, 2, 3]);
        });

        it('should cache test lists per course', async () => {
            courseApi.tests.mockResolvedValueOnce([1, 2]);

            await act(async () => {
                await useCourseStore.getState().fetchCourseTests('cam17');
                await useCourseStore.getState().fetchCourseTests('cam17');
            });

            expect(courseApi.tests).toHaveBeenCalledTimes(1);
        });

        it('should return empty list on error', async () => {
            courseApi.tests.mockRejectedValueOnce(new Error('Not found'));

            let result;
            await act(async () => {
                result = await useCourseStore.getState().fetchCourseTests('nope');
            });

            expect(result).toEqual([]);
            expect(useCourseStore.getState().error).toBe('Not found');
        });
    });

    // =========================================================================
    // FETCH COURSE DETAILS TESTS
    // =========================================================================
    describe('fetchCourseDetails()', () => {
        it('should fetch details for a course', async () => {
            const mockDetails = { code: 'cam17', name: 'Cambridge 17', totalTests: 4 };
            courseApi.details.mockResolvedValueOnce(mockDetails);

            let result;
            await act(async () => {
                result = await useCourseStore.getState().fetchCourseDetails('cam17');
            });

            expect(result).toEqual(mockDetails);
            expect(useCourseStore.getState().courseDetails['cam17']).toEqual(mockDetails);
        });

        it('should cache details per course', async () => {
            courseApi.details.mockResolvedValueOnce({ code: 'cam17' });

            await act(async () => {
                await useCourseStore.getState().fetchCourseDetails('cam17');
                await useCourseStore.getState().fetchCourseDetails('cam17');
            });

            expect(courseApi.details).toHaveBeenCalledTimes(1);
        });

        it('should return null on error', async () => {
            courseApi.details.mockRejectedValueOnce(new Error('Not found'));

            let result;
            await act(async () => {
                result = await useCourseStore.getState().fetchCourseDetails('nope');
            });

            expect(result).toBe(null);
        });
    });

    // =========================================================================
    // CACHE GETTERS
    // =========================================================================
    describe('cache getters', () => {
        it('should return cached values or null', () => {
            act(() => {
                useCourseStore.setState({
                    courseTests: { cam17: [1, 2] },
                    courseDetails: { cam17: { code: 'cam17' } },
                });
            });

            const state = useCourseStore.getState();
            expect(state.getCachedTests('cam17')).toEqual([1, 2]);
            expect(state.getCachedTests('cam18')).toBe(null);
            expect(state.getCachedDetails('cam17')).toEqual({ code: 'cam17' });
            expect(state.getCachedDetails('cam18')).toBe(null);
        });
    });
});
