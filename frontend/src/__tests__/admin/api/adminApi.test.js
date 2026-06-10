import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('axios', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        patch: vi.fn(),
        delete: vi.fn(),
    },
}));

vi.mock('../../../api/supabaseClient', () => ({
    supabase: {
        auth: {
            getSession: vi.fn(),
        },
    },
}));

import axios from 'axios';
import adminApi, { hashtagsApi, testSetsApi, testsApi } from '../../../admin/api/adminApi';
import { supabase } from '../../../api/supabaseClient';

describe('adminApi compatibility exports', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        supabase.auth.getSession.mockResolvedValue({
            data: {
                session: {
                    access_token: 'test-token',
                    user: { id: 'admin-user-id' },
                },
            },
        });
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

    it('calls a representative default facade endpoint with auth headers', async () => {
        const responseData = { totalTests: 12 };
        axios.get.mockResolvedValue({ data: responseData });

        const result = await adminApi.content.getOverview();

        expect(result).toEqual(responseData);
        expect(axios.get).toHaveBeenCalledWith(
            'http://localhost:8080/api/admin/content/overview',
            {
                headers: {
                    Authorization: 'Bearer test-token',
                    'X-User-Id': 'admin-user-id',
                    'Content-Type': 'application/json',
                },
            }
        );
    });

    it('creates tests through the test set hierarchy endpoint', async () => {
        const payload = { testNumber: 5, name: 'AI Test 5' };
        const responseData = { id: 42, ...payload };
        axios.post.mockResolvedValue({ data: responseData });

        const result = await testsApi.create(7, payload);

        expect(result).toEqual(responseData);
        expect(axios.post).toHaveBeenCalledWith(
            'http://localhost:8080/api/admin/test-sets/7/tests',
            payload,
            {
                headers: {
                    Authorization: 'Bearer test-token',
                    'X-User-Id': 'admin-user-id',
                    'Content-Type': 'application/json',
                },
            }
        );
    });
});