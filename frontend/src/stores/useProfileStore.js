import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { profileApi } from '../api/backendApi';
import useAuthStore from './useAuthStore';

/**
 * Zustand store for user profile management.
 * Handles profile loading, caching, and automatic reload on auth changes.
 */
const useProfileStore = create(
  devtools(
    (set, get) => ({
      // ===== STATE =====
      profile: null,
      loading: false,
      lastLoadedUserId: null,
      error: null,

      // ===== ACTIONS =====

      /**
       * Load profile for a given user ID.
       * Will skip loading if the profile is already loaded for this user.
       * @param {string} userId - The user ID to load profile for
       * @param {boolean} forceReload - Force reload even if already loaded
       */
      loadProfile: async (userId, forceReload = false) => {
        const state = get();

        // Skip if already loaded for this user and not forcing reload
        if (!forceReload && state.profile && state.lastLoadedUserId === userId) {
          console.log('📋 Profile already loaded for user:', userId);
          return state.profile;
        }

        // Skip if no userId provided
        if (!userId) {
          console.warn('⚠️ loadProfile called without userId');
          return null;
        }

        console.log('📋 Loading profile for user:', userId);
        set({ loading: true, error: null }, false, 'loadProfile/start');

        try {
          const response = await profileApi.getById(userId);
          const profile = response.data;

          set(
            {
              profile,
              lastLoadedUserId: userId,
              loading: false,
              error: null,
            },
            false,
            'loadProfile/success'
          );

          console.log('✅ Profile loaded successfully:', profile);
          return profile;
        } catch (error) {
          console.error('❌ Failed to load profile:', error);

          // If 404, try to create a new profile
          if (error.response?.status === 404) {
            console.log('📋 Profile not found, attempting to create...');
            return get().createProfileForUser(userId);
          }

          set(
            {
              profile: null,
              loading: false,
              error: error.message || 'Failed to load profile',
            },
            false,
            'loadProfile/error'
          );

          return null;
        }
      },

      /**
       * Create a new profile for a user.
       * Handles username conflicts by retrying with a timestamp suffix.
       * @param {string} userId - The user ID to create profile for
       * @param {string} username - Optional username (defaults to 'user')
       */
      createProfileForUser: async (userId, username = null) => {
        set({ loading: true, error: null }, false, 'createProfile/start');

        // Try to get pending username from sessionStorage (set during signup)
        let finalUsername = username;
        if (!finalUsername) {
          try {
            finalUsername = sessionStorage.getItem('pendingUsername');
            if (finalUsername) {
              sessionStorage.removeItem('pendingUsername');
              sessionStorage.removeItem('pendingUserId');
            }
          } catch (e) {
            console.warn('⚠️ Could not access sessionStorage:', e);
          }
        }

        // Default username if none provided
        if (!finalUsername) {
          finalUsername = `user_${Date.now()}`;
        }

        const profileData = {
          id: userId,
          username: finalUsername,
          displayName: finalUsername,
        };

        try {
          console.log('📋 Creating profile:', profileData);
          const response = await profileApi.create(profileData);
          const profile = response.data;

          set(
            {
              profile,
              lastLoadedUserId: userId,
              loading: false,
              error: null,
            },
            false,
            'createProfile/success'
          );

          console.log('✅ Profile created successfully:', profile);
          return profile;
        } catch (error) {
          console.error('❌ Failed to create profile:', error);

          // Handle username conflict (409 Conflict or 400 Bad Request)
          if (error.response?.status === 409 || error.response?.status === 400) {
            console.log('⚠️ Username conflict, retrying with timestamp...');

            const retryUsername = `${finalUsername}_${Date.now()}`;
            const retryProfileData = {
              id: userId,
              username: retryUsername,
              displayName: retryUsername,
            };

            try {
              const retryResponse = await profileApi.create(retryProfileData);
              const profile = retryResponse.data;

              set(
                {
                  profile,
                  lastLoadedUserId: userId,
                  loading: false,
                  error: null,
                },
                false,
                'createProfile/retrySuccess'
              );

              console.log('✅ Profile created with fallback username:', profile);
              return profile;
            } catch (retryError) {
              console.error('❌ Failed to create profile with fallback username:', retryError);
              set(
                {
                  profile: null,
                  loading: false,
                  error: retryError.message || 'Failed to create profile',
                },
                false,
                'createProfile/retryError'
              );
              return null;
            }
          }

          set(
            {
              profile: null,
              loading: false,
              error: error.message || 'Failed to create profile',
            },
            false,
            'createProfile/error'
          );

          return null;
        }
      },

      /**
       * Update the profile with new data (merges with existing).
       * @param {object} newData - New profile data to merge
       */
      updateProfile: async (newData) => {
        const state = get();

        if (!state.profile) {
          console.warn('⚠️ Cannot update profile: no profile loaded');
          return null;
        }

        set({ loading: true, error: null }, false, 'updateProfile/start');

        try {
          const response = await profileApi.update(state.profile.id, newData);
          const updatedProfile = response.data;

          // Merge the updated data with existing profile
          set(
            {
              profile: { ...state.profile, ...updatedProfile },
              loading: false,
              error: null,
            },
            false,
            'updateProfile/success'
          );

          console.log('✅ Profile updated successfully:', updatedProfile);
          return updatedProfile;
        } catch (error) {
          console.error('❌ Failed to update profile:', error);

          set(
            {
              loading: false,
              error: error.message || 'Failed to update profile',
            },
            false,
            'updateProfile/error'
          );

          return null;
        }
      },

      /**
       * Clear profile state (used on sign out).
       */
      clearProfile: () => {
        console.log('📋 Clearing profile state');
        set(
          {
            profile: null,
            lastLoadedUserId: null,
            loading: false,
            error: null,
          },
          false,
          'clearProfile'
        );
      },
    }),
    { name: 'ProfileStore' }
  )
);

// ===== AUTO-LOAD PROFILE ON AUTH CHANGE =====
/**
 * Subscribe to auth store changes and auto-load/clear profile.
 * This runs once when the module is loaded.
 */
const unsubscribe = useAuthStore.subscribe(
  (state) => state.user,
  (user, previousUser) => {
    const profileStore = useProfileStore.getState();

    if (user && user.id !== previousUser?.id) {
      // New user logged in - load their profile
      console.log('🔄 Auth user changed, loading profile for:', user.id);
      profileStore.loadProfile(user.id);
    } else if (!user && previousUser) {
      // User logged out - clear profile
      console.log('🔄 User logged out, clearing profile');
      profileStore.clearProfile();
    }
  }
);

// Optional: Export unsubscribe for cleanup in tests
export const cleanupProfileSubscription = unsubscribe;

export default useProfileStore;
