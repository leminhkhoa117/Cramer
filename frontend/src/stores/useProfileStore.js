import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { profileApi, getApiError } from '../lib/api';
import useAuthStore from './useAuthStore';

/**
 * Profile store. The backend owns profile lifecycle (Supabase trigger creates the row); the
 * frontend only reads (GET /profiles/{id}) and updates (PUT /profiles/{id}). API responses are
 * the ProfileResponse object directly.
 */
const useProfileStore = create(
  devtools(
    (set, get) => ({
      profile: null,
      loading: false,
      lastLoadedUserId: null,
      error: null,

      loadProfile: async (userId, forceReload = false) => {
        const state = get();
        if (!forceReload && state.profile && state.lastLoadedUserId === userId) return state.profile;
        if (!userId) return null;
        set({ loading: true, error: null }, false, 'loadProfile/start');
        try {
          const profile = await profileApi.get(userId);
          set({ profile, lastLoadedUserId: userId, loading: false, error: null }, false, 'loadProfile/success');
          return profile;
        } catch (error) {
          set({ profile: null, loading: false, error: getApiError(error).message }, false, 'loadProfile/error');
          return null;
        }
      },

      updateProfile: async (newData) => {
        const state = get();
        if (!state.profile) return null;
        set({ loading: true, error: null }, false, 'updateProfile/start');
        try {
          const updated = await profileApi.update(state.profile.id, newData);
          set({ profile: { ...state.profile, ...updated }, loading: false, error: null }, false, 'updateProfile/success');
          return updated;
        } catch (error) {
          set({ loading: false, error: getApiError(error).message }, false, 'updateProfile/error');
          throw error;
        }
      },

      clearProfile: () =>
        set({ profile: null, lastLoadedUserId: null, loading: false, error: null }, false, 'clearProfile'),
    }),
    { name: 'ProfileStore' }
  )
);

// Auto-load / clear profile on auth change.
const unsubscribe = useAuthStore.subscribe(
  (state) => state.user,
  (user, previousUser) => {
    const store = useProfileStore.getState();
    if (user && user.id !== previousUser?.id) store.loadProfile(user.id);
    else if (!user && previousUser) store.clearProfile();
  }
);

export const cleanupProfileSubscription = unsubscribe;
export default useProfileStore;
