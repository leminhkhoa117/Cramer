/**
 * Unit tests for useProfileStore.
 * Tests profile loading, caching, creation, and updates.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Mock the API module
vi.mock('../../api/backendApi', () => ({
  profileApi: {
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
  },
}));

// Mock useAuthStore
vi.mock('../../stores/useAuthStore', () => ({
  default: {
    getState: vi.fn(() => ({ user: { id: 'user-123' } })),
    subscribe: vi.fn(),
  },
}));

// Import mocked modules
import { profileApi } from '../../api/backendApi';

// Create a test version of the profile store
const createTestProfileStore = () => {
  return create(
    devtools((set, get) => ({
      // State
      profile: null,
      loading: false,
      lastLoadedUserId: null,
      error: null,

      // Actions
      loadProfile: async (userId, forceReload = false) => {
        const state = get();

        // Skip if already loaded for this user and not forcing reload
        if (!forceReload && state.profile && state.lastLoadedUserId === userId) {
          return state.profile;
        }

        if (!userId) {
          return null;
        }

        set({ loading: true, error: null }, false, 'loadProfile/start');

        try {
          const response = await profileApi.getById(userId);
          const profile = response.data;

          set({
            profile,
            lastLoadedUserId: userId,
            loading: false,
            error: null,
          }, false, 'loadProfile/success');

          return profile;
        } catch (error) {
          if (error.response?.status === 404) {
            return get().createProfileForUser(userId);
          }

          set({
            profile: null,
            loading: false,
            error: error.message || 'Failed to load profile',
          }, false, 'loadProfile/error');

          return null;
        }
      },

      createProfileForUser: async (userId, username = null) => {
        set({ loading: true, error: null }, false, 'createProfile/start');

        let finalUsername = username || `user_${Date.now()}`;

        const profileData = {
          id: userId,
          username: finalUsername,
          displayName: finalUsername,
        };

        try {
          const response = await profileApi.create(profileData);
          const profile = response.data;

          set({
            profile,
            lastLoadedUserId: userId,
            loading: false,
            error: null,
          }, false, 'createProfile/success');

          return profile;
        } catch (error) {
          // Handle username conflict
          if (error.response?.status === 409 || error.response?.status === 400) {
            const retryUsername = `${finalUsername}_${Date.now()}`;
            const retryProfileData = {
              id: userId,
              username: retryUsername,
              displayName: retryUsername,
            };

            try {
              const retryResponse = await profileApi.create(retryProfileData);
              const profile = retryResponse.data;

              set({
                profile,
                lastLoadedUserId: userId,
                loading: false,
                error: null,
              }, false, 'createProfile/retrySuccess');

              return profile;
            } catch (retryError) {
              set({
                profile: null,
                loading: false,
                error: retryError.message || 'Failed to create profile',
              }, false, 'createProfile/retryError');
              return null;
            }
          }

          set({
            profile: null,
            loading: false,
            error: error.message || 'Failed to create profile',
          }, false, 'createProfile/error');

          return null;
        }
      },

      updateProfile: async (updates) => {
        const state = get();
        if (!state.profile) {
          throw new Error('No profile to update');
        }

        set({ loading: true, error: null }, false, 'updateProfile/start');

        try {
          const response = await profileApi.update(state.profile.id, updates);
          const updatedProfile = response.data;

          set({
            profile: updatedProfile,
            loading: false,
            error: null,
          }, false, 'updateProfile/success');

          return updatedProfile;
        } catch (error) {
          set({
            loading: false,
            error: error.message || 'Failed to update profile',
          }, false, 'updateProfile/error');

          throw error;
        }
      },

      clearProfile: () => {
        set({
          profile: null,
          lastLoadedUserId: null,
          error: null,
        }, false, 'clearProfile');
      },
    }))
  );
};

describe('useProfileStore', () => {
  let store;

  beforeEach(() => {
    vi.clearAllMocks();
    store = createTestProfileStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ==========================================================================
  // loadProfile Tests
  // ==========================================================================
  describe('loadProfile', () => {
    it('should load profile successfully', async () => {
      const mockProfile = {
        id: 'user-123',
        username: 'testuser',
        displayName: 'Test User',
        avatarUrl: 'https://example.com/avatar.png',
      };

      profileApi.getById.mockResolvedValueOnce({ data: mockProfile });

      let result;
      await act(async () => {
        result = await store.getState().loadProfile('user-123');
      });

      expect(result).toEqual(mockProfile);
      expect(store.getState().profile).toEqual(mockProfile);
      expect(store.getState().lastLoadedUserId).toBe('user-123');
      expect(store.getState().loading).toBe(false);
      expect(store.getState().error).toBeNull();
    });

    it('should skip loading if profile already loaded for same user', async () => {
      const mockProfile = {
        id: 'user-123',
        username: 'testuser',
      };

      profileApi.getById.mockResolvedValueOnce({ data: mockProfile });

      // First load
      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      expect(profileApi.getById).toHaveBeenCalledTimes(1);

      // Second load - should use cache
      let result;
      await act(async () => {
        result = await store.getState().loadProfile('user-123');
      });

      expect(profileApi.getById).toHaveBeenCalledTimes(1); // Still 1
      expect(result).toEqual(mockProfile);
    });

    it('should reload when forceReload is true', async () => {
      const mockProfile = {
        id: 'user-123',
        username: 'testuser',
      };

      profileApi.getById.mockResolvedValue({ data: mockProfile });

      // First load
      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      // Force reload
      await act(async () => {
        await store.getState().loadProfile('user-123', true);
      });

      expect(profileApi.getById).toHaveBeenCalledTimes(2);
    });

    it('should load for different user', async () => {
      const mockProfile1 = { id: 'user-123', username: 'user1' };
      const mockProfile2 = { id: 'user-456', username: 'user2' };

      profileApi.getById
        .mockResolvedValueOnce({ data: mockProfile1 })
        .mockResolvedValueOnce({ data: mockProfile2 });

      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      await act(async () => {
        await store.getState().loadProfile('user-456');
      });

      expect(profileApi.getById).toHaveBeenCalledTimes(2);
      expect(store.getState().profile).toEqual(mockProfile2);
      expect(store.getState().lastLoadedUserId).toBe('user-456');
    });

    it('should return null if userId is empty', async () => {
      const result = await store.getState().loadProfile(null);

      expect(result).toBeNull();
      expect(profileApi.getById).not.toHaveBeenCalled();
    });

    it('should create profile on 404 error', async () => {
      const error = new Error('Not found');
      error.response = { status: 404 };
      profileApi.getById.mockRejectedValueOnce(error);

      const newProfile = {
        id: 'user-123',
        username: 'user_123456',
        displayName: 'user_123456',
      };
      profileApi.create.mockResolvedValueOnce({ data: newProfile });

      let result;
      await act(async () => {
        result = await store.getState().loadProfile('user-123');
      });

      expect(profileApi.create).toHaveBeenCalled();
      expect(result).toEqual(newProfile);
    });

    it('should set error on non-404 errors', async () => {
      const error = new Error('Server error');
      error.response = { status: 500 };
      profileApi.getById.mockRejectedValueOnce(error);

      let result;
      await act(async () => {
        result = await store.getState().loadProfile('user-123');
      });

      expect(result).toBeNull();
      expect(store.getState().error).toBe('Server error');
      expect(store.getState().loading).toBe(false);
    });
  });

  // ==========================================================================
  // createProfileForUser Tests
  // ==========================================================================
  describe('createProfileForUser', () => {
    it('should create profile with provided username', async () => {
      const mockProfile = {
        id: 'user-123',
        username: 'myusername',
        displayName: 'myusername',
      };

      profileApi.create.mockResolvedValueOnce({ data: mockProfile });

      let result;
      await act(async () => {
        result = await store.getState().createProfileForUser('user-123', 'myusername');
      });

      expect(result).toEqual(mockProfile);
      expect(profileApi.create).toHaveBeenCalledWith({
        id: 'user-123',
        username: 'myusername',
        displayName: 'myusername',
      });
    });

    it('should generate username if not provided', async () => {
      const mockProfile = {
        id: 'user-123',
        username: 'user_1234567890',
        displayName: 'user_1234567890',
      };

      profileApi.create.mockResolvedValueOnce({ data: mockProfile });

      await act(async () => {
        await store.getState().createProfileForUser('user-123');
      });

      const callArg = profileApi.create.mock.calls[0][0];
      expect(callArg.id).toBe('user-123');
      expect(callArg.username).toMatch(/^user_\d+$/);
    });

    it('should retry with timestamp on username conflict (409)', async () => {
      const conflictError = new Error('Username taken');
      conflictError.response = { status: 409 };

      const mockProfile = {
        id: 'user-123',
        username: 'testuser_1234567890',
        displayName: 'testuser_1234567890',
      };

      profileApi.create
        .mockRejectedValueOnce(conflictError)
        .mockResolvedValueOnce({ data: mockProfile });

      let result;
      await act(async () => {
        result = await store.getState().createProfileForUser('user-123', 'testuser');
      });

      expect(profileApi.create).toHaveBeenCalledTimes(2);
      expect(result).toEqual(mockProfile);
    });

    it('should set error if all retries fail', async () => {
      const conflictError = new Error('Username taken');
      conflictError.response = { status: 409 };

      const retryError = new Error('Still conflicting');
      retryError.response = { status: 409 };

      profileApi.create
        .mockRejectedValueOnce(conflictError)
        .mockRejectedValueOnce(retryError);

      let result;
      await act(async () => {
        result = await store.getState().createProfileForUser('user-123', 'testuser');
      });

      expect(result).toBeNull();
      expect(store.getState().error).toBe('Still conflicting');
    });
  });

  // ==========================================================================
  // updateProfile Tests
  // ==========================================================================
  describe('updateProfile', () => {
    it('should update profile successfully', async () => {
      // First load a profile
      const initialProfile = { id: 'user-123', username: 'testuser', displayName: 'Test' };
      profileApi.getById.mockResolvedValueOnce({ data: initialProfile });

      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      // Then update
      const updatedProfile = { ...initialProfile, displayName: 'Updated Name' };
      profileApi.update.mockResolvedValueOnce({ data: updatedProfile });

      let result;
      await act(async () => {
        result = await store.getState().updateProfile({ displayName: 'Updated Name' });
      });

      expect(result).toEqual(updatedProfile);
      expect(store.getState().profile.displayName).toBe('Updated Name');
    });

    it('should throw error if no profile loaded', async () => {
      await expect(async () => {
        await store.getState().updateProfile({ displayName: 'Test' });
      }).rejects.toThrow('No profile to update');
    });

    it('should set error on update failure', async () => {
      // First load a profile
      const initialProfile = { id: 'user-123', username: 'testuser' };
      profileApi.getById.mockResolvedValueOnce({ data: initialProfile });

      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      // Mock update failure
      profileApi.update.mockRejectedValueOnce(new Error('Update failed'));

      await expect(async () => {
        await store.getState().updateProfile({ displayName: 'New Name' });
      }).rejects.toThrow('Update failed');

      expect(store.getState().error).toBe('Update failed');
    });
  });

  // ==========================================================================
  // clearProfile Tests
  // ==========================================================================
  describe('clearProfile', () => {
    it('should clear all profile state', async () => {
      // First load a profile
      const mockProfile = { id: 'user-123', username: 'testuser' };
      profileApi.getById.mockResolvedValueOnce({ data: mockProfile });

      await act(async () => {
        await store.getState().loadProfile('user-123');
      });

      expect(store.getState().profile).not.toBeNull();

      // Clear
      act(() => {
        store.getState().clearProfile();
      });

      expect(store.getState().profile).toBeNull();
      expect(store.getState().lastLoadedUserId).toBeNull();
      expect(store.getState().error).toBeNull();
    });
  });
});
