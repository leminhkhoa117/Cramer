/**
 * Tests for useCourseStore
 * 
 * Tests course fetching, caching, pagination, and search functionality.
 * 
 * @author Cramer Test Team
 * @since 2026-01-26
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act } from '@testing-library/react';

// Mock the courseApi
vi.mock('../../api/backendApi', () => ({
    courseApi: {
        getAll: vi.fn(),
        getAllV2: vi.fn(),
        getTestsByCourse: vi.fn(),
        getDetails: vi.fn(),
    }
}));

import useCourseStore from '../../stores/useCourseStore';
import { courseApi } from '../../api/backendApi';

describe('useCourseStore', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        
        // Reset store state
        const store = useCourseStore.getState();
        act(() => {
            store.courses = [];
            store.courseTests = {};
            store.courseDetails = {};
            store.loading = false;
            store.error = null;
            store.lastFetchedAt = null;
            store.currentPage = 0;
            store.pageSize = 10;
            store.totalPages = 0;
            store.totalElements = 0;
            store.searchQuery = '';
            store.debouncedSearchQuery = '';
        });
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
            expect(state.currentPage).toBe(0);
            expect(state.pageSize).toBe(10);
        });
    });

    // =========================================================================
    // FETCH COURSES TESTS
    // =========================================================================
    describe('fetchCourses()', () => {
        it('should set loading state when fetching', async () => {
            courseApi.getAll.mockImplementation(() => 
                new Promise(resolve => setTimeout(() => resolve({ data: { content: [] } }), 100))
            );

            const state = useCourseStore.getState();
            const fetchPromise = state.fetchCourses(0, 10, '');
            
            expect(useCourseStore.getState().loading).toBe(true);
            
            await fetchPromise;
        });

        it('should fetch courses successfully', async () => {
            const mockResponse = {
                data: {
                    content: [
                        { code: 'cam17', name: 'Cambridge 17' },
                        { code: 'cam18', name: 'Cambridge 18' }
                    ],
                    number: 0,
                    size: 10,
                    totalPages: 1,
                    totalElements: 2
                }
            };

            courseApi.getAll.mockResolvedValueOnce(mockResponse);

            const state = useCourseStore.getState();
            await act(async () => {
                await state.fetchCourses(0, 10, '');
            });

            const newState = useCourseStore.getState();
            expect(newState.courses).toHaveLength(2);
            expect(newState.courses[0].code).toBe('cam17');
            expect(newState.loading).toBe(false);
            expect(newState.currentPage).toBe(0);
            expect(newState.totalElements).toBe(2);
        });

        it('should handle fetch error gracefully', async () => {
            courseApi.getAll.mockRejectedValueOnce(new Error('Network error'));

            const state = useCourseStore.getState();
            
            await expect(state.fetchCourses(0, 10, '')).rejects.toThrow();

            const newState = useCourseStore.getState();
            expect(newState.loading).toBe(false);
            expect(newState.error).toBe('Network error');
        });

        it('should preserve pagination info from response', async () => {
            const mockResponse = {
                data: {
                    content: [{ code: 'cam17' }],
                    number: 2,
                    size: 5,
                    totalPages: 10,
                    totalElements: 50
                }
            };

            courseApi.getAll.mockResolvedValueOnce(mockResponse);

            const state = useCourseStore.getState();
            await act(async () => {
                await state.fetchCourses(2, 5, '');
            });

            const newState = useCourseStore.getState();
            expect(newState.currentPage).toBe(2);
            expect(newState.pageSize).toBe(5);
            expect(newState.totalPages).toBe(10);
            expect(newState.totalElements).toBe(50);
        });
    });

    // =========================================================================
    // FETCH COURSES V2 TESTS
    // =========================================================================
    describe('fetchCoursesV2()', () => {
        it('should fetch all courses without pagination', async () => {
            const mockCourses = [
                { id: 1, code: 'cam17', name: 'Cambridge 17', description: 'IELTS 17' },
                { id: 2, code: 'cam18', name: 'Cambridge 18', description: 'IELTS 18' }
            ];

            courseApi.getAllV2.mockResolvedValueOnce({ data: mockCourses });

            const state = useCourseStore.getState();
            await act(async () => {
                await state.fetchCoursesV2();
            });

            const newState = useCourseStore.getState();
            expect(newState.courses).toHaveLength(2);
            expect(newState.loading).toBe(false);
            expect(newState.lastFetchedAt).toBeInstanceOf(Date);
        });

        it('should handle V2 fetch error', async () => {
            courseApi.getAllV2.mockRejectedValueOnce(new Error('API error'));

            const state = useCourseStore.getState();
            
            await expect(state.fetchCoursesV2()).rejects.toThrow();

            const newState = useCourseStore.getState();
            expect(newState.error).toBe('API error');
        });
    });

    // =========================================================================
    // FETCH COURSE TESTS TESTS
    // =========================================================================
    describe('fetchCourseTests()', () => {
        it('should fetch tests for a specific course', async () => {
            const mockTests = [
                { id: 1, testNumber: 1, skill: 'reading' },
                { id: 2, testNumber: 1, skill: 'listening' }
            ];

            courseApi.getTestsByCourse.mockResolvedValueOnce({ data: mockTests });

            const state = useCourseStore.getState();
            await act(async () => {
                await state.fetchCourseTests('cam17');
            });

            const newState = useCourseStore.getState();
            expect(newState.courseTests['cam17']).toHaveLength(2);
            expect(newState.loading).toBe(false);
        });

        it('should return cached tests if available', async () => {
            // Pre-populate cache
            useCourseStore.setState({
                courseTests: {
                    'cam17': [{ id: 1, testNumber: 1 }]
                }
            });

            const state = useCourseStore.getState();
            const result = await state.fetchCourseTests('cam17');

            expect(courseApi.getTestsByCourse).not.toHaveBeenCalled();
            expect(result).toHaveLength(1);
        });

        it('should handle fetch error for course tests', async () => {
            courseApi.getTestsByCourse.mockRejectedValueOnce(new Error('Not found'));

            const state = useCourseStore.getState();
            
            await expect(state.fetchCourseTests('invalid')).rejects.toThrow();

            const newState = useCourseStore.getState();
            expect(newState.error).toBeTruthy();
        });
    });

    // =========================================================================
    // FETCH COURSE DETAILS TESTS
    // =========================================================================
    describe('fetchCourseDetails()', () => {
        it('should fetch course details by code', async () => {
            const mockDetails = {
                id: 1,
                code: 'cam17',
                name: 'Cambridge IELTS 17',
                description: 'Official IELTS test 17'
            };

            courseApi.getDetails.mockResolvedValueOnce({ data: mockDetails });

            const state = useCourseStore.getState();
            await act(async () => {
                await state.fetchCourseDetails('cam17');
            });

            const newState = useCourseStore.getState();
            expect(newState.courseDetails['cam17']).toEqual(mockDetails);
        });

        it('should return cached details if available', async () => {
            const cachedDetails = { code: 'cam17', name: 'Cached' };
            useCourseStore.setState({
                courseDetails: { 'cam17': cachedDetails }
            });

            const state = useCourseStore.getState();
            const result = await state.fetchCourseDetails('cam17');

            expect(courseApi.getDetails).not.toHaveBeenCalled();
            expect(result).toEqual(cachedDetails);
        });

        it('should return null on error without throwing', async () => {
            courseApi.getDetails.mockRejectedValueOnce(new Error('Not found'));

            const state = useCourseStore.getState();
            const result = await state.fetchCourseDetails('invalid');

            expect(result).toBeNull();
        });
    });

    // =========================================================================
    // GET CACHED METHODS TESTS
    // =========================================================================
    describe('getCachedDetails()', () => {
        it('should return cached details', () => {
            const details = { code: 'cam17', name: 'Cambridge 17' };
            useCourseStore.setState({
                courseDetails: { 'cam17': details }
            });

            const state = useCourseStore.getState();
            expect(state.getCachedDetails('cam17')).toEqual(details);
        });

        it('should return null if not cached', () => {
            const state = useCourseStore.getState();
            expect(state.getCachedDetails('unknown')).toBeNull();
        });
    });

    describe('getCachedTests()', () => {
        it('should return cached tests', () => {
            const tests = [{ id: 1 }, { id: 2 }];
            useCourseStore.setState({
                courseTests: { 'cam17': tests }
            });

            const state = useCourseStore.getState();
            expect(state.getCachedTests('cam17')).toEqual(tests);
        });

        it('should return null if not cached', () => {
            const state = useCourseStore.getState();
            expect(state.getCachedTests('unknown')).toBeNull();
        });
    });

    // =========================================================================
    // PAGINATION ACTIONS TESTS
    // =========================================================================
    describe('setPage()', () => {
        it('should update current page', () => {
            const state = useCourseStore.getState();
            act(() => {
                state.setPage(5);
            });

            expect(useCourseStore.getState().currentPage).toBe(5);
        });
    });

    describe('setPageSize()', () => {
        it('should update page size and reset to page 0', () => {
            useCourseStore.setState({ currentPage: 3 });

            const state = useCourseStore.getState();
            act(() => {
                state.setPageSize(25);
            });

            const newState = useCourseStore.getState();
            expect(newState.pageSize).toBe(25);
            expect(newState.currentPage).toBe(0);
        });
    });

    // =========================================================================
    // SEARCH ACTIONS TESTS
    // =========================================================================
    describe('setSearchQuery()', () => {
        it('should update search query', () => {
            const state = useCourseStore.getState();
            act(() => {
                state.setSearchQuery('cambridge');
            });

            expect(useCourseStore.getState().searchQuery).toBe('cambridge');
        });
    });

    describe('setDebouncedSearchQuery()', () => {
        it('should update debounced search and reset to page 0', () => {
            useCourseStore.setState({ currentPage: 5 });

            const state = useCourseStore.getState();
            act(() => {
                state.setDebouncedSearchQuery('ielts');
            });

            const newState = useCourseStore.getState();
            expect(newState.debouncedSearchQuery).toBe('ielts');
            expect(newState.currentPage).toBe(0);
        });
    });

    // =========================================================================
    // CLEAR CACHE TESTS
    // =========================================================================
    describe('clearTestsCache()', () => {
        it('should clear all cached tests', () => {
            useCourseStore.setState({
                courseTests: {
                    'cam17': [{ id: 1 }],
                    'cam18': [{ id: 2 }]
                }
            });

            const state = useCourseStore.getState();
            act(() => {
                state.clearTestsCache();
            });

            expect(useCourseStore.getState().courseTests).toEqual({});
        });
    });

    // =========================================================================
    // RESET FILTERS TESTS
    // =========================================================================
    describe('resetFilters()', () => {
        it('should reset pagination and search to defaults', () => {
            useCourseStore.setState({
                currentPage: 5,
                pageSize: 25,
                searchQuery: 'test',
                debouncedSearchQuery: 'test'
            });

            const state = useCourseStore.getState();
            act(() => {
                state.resetFilters();
            });

            const newState = useCourseStore.getState();
            expect(newState.currentPage).toBe(0);
            expect(newState.pageSize).toBe(10);
            expect(newState.searchQuery).toBe('');
            expect(newState.debouncedSearchQuery).toBe('');
        });
    });
});
