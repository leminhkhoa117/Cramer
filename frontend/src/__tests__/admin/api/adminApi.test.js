import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../../lib/api', () => ({
    http: { defaults: { baseURL: '/api' } },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    del: vi.fn(),
    getApiError: (error) => ({ message: error?.message || 'Unknown error' }),
    setupApiClient: vi.fn(),
    setUnauthorizedHandler: vi.fn(),
    currentAuthToken: vi.fn(() => null),
    authApi: {},
    profileApi: {},
    courseApi: {},
    testApi: {},
    attemptApi: {},
    writingApi: {},
    speakingApi: {},
    subscriptionApi: {},
    quotaApi: {},
    paymentApi: {},
    creditApi: {},
    chatApi: {},
    dashboardApi: {},
    vocabularyApi: {},
    abtsApi: {},
    openAbtsStream: vi.fn(),
}));

import { del, get, post, put } from '../../../lib/api';
import adminApi, { hashtagsApi, testSetsApi, testsApi } from '../../../admin/api/adminApi';

describe('adminApi compatibility exports', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('keeps facade domain shape and compatibility aliases on default export', () => {
        expect(adminApi).toEqual(expect.objectContaining({
            users: expect.objectContaining({ getList: expect.any(Function) }),
            finance: expect.objectContaining({ getOverview: expect.any(Function) }),
            content: expect.objectContaining({ getOverview: expect.any(Function) }),
            activity: expect.objectContaining({ getUserActivities: expect.any(Function) }),
            dashboard: expect.objectContaining({ getStats: expect.any(Function) }),
        }));
        expect(adminApi.testSetsApi).toBe(testSetsApi);
        expect(adminApi.testsApi).toBe(testsApi);
        expect(adminApi.hashtagsApi).toBe(hashtagsApi);
    });

    it('calls a representative default facade endpoint through the shared client', async () => {
        const responseData = { totalTests: 12 };
        get.mockResolvedValueOnce(responseData);

        const result = await adminApi.content.getOverview();

        expect(result).toEqual(responseData);
        expect(get).toHaveBeenCalledWith('/admin/content/overview');
    });

    it('creates tests through the test set hierarchy endpoint', async () => {
        const payload = { testNumber: 5, name: 'AI Test 5' };
        const responseData = { id: 42, ...payload };
        post.mockResolvedValueOnce(responseData);

        const result = await testsApi.create(7, payload);

        expect(result).toEqual(responseData);
        expect(post).toHaveBeenCalledWith('/admin/test-sets/7/tests', payload);
    });

    it('uses the shared verb helpers for every module', async () => {
        put.mockResolvedValueOnce({ ok: true });
        await testSetsApi.update(1, { name: 'X' });
        expect(put).toHaveBeenCalledWith('/admin/test-sets/1', { name: 'X' });

        del.mockResolvedValueOnce(undefined);
        await testSetsApi.delete(1);
        expect(del).toHaveBeenCalledWith('/admin/test-sets/1');
    });
});
