import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { supabase, authHelpers } from '../api/supabaseClient';
import { setupApiClient } from '../api/backendApi';

/**
 * Zustand store for authentication state management.
 * Replaces AuthContext with a more efficient, subscription-based approach.
 */
const useAuthStore = create(
  devtools(
    subscribeWithSelector((set, get) => ({
      // ===== STATE =====
      user: null,
      session: null,
      loading: true,
      error: null,

      // ===== INTERNAL ACTIONS =====
      /**
       * Set the user state
       */
      setUser: (user) => set({ user }, false, 'setUser'),

      /**
       * Set the session state
       */
      setSession: (session) => set({ session }, false, 'setSession'),

      /**
       * Set loading state
       */
      setLoading: (loading) => set({ loading }, false, 'setLoading'),

      /**
       * Set error state
       */
      setError: (error) => set({ error }, false, 'setError'),

      /**
       * Clear all auth state (used on sign out)
       */
      clearAuth: () =>
        set(
          {
            user: null,
            session: null,
            error: null,
          },
          false,
          'clearAuth'
        ),

      // ===== AUTH ACTIONS =====

      /**
       * Sign up new user with email and password.
       * Stores pending username and userId in sessionStorage for profile creation after OTP verification.
       */
      signUp: async (email, password, username) => {
        console.log('useAuthStore signUp called with:', { email, username });
        set({ error: null }, false, 'signUp/start');

        try {
          console.log('Calling Supabase signUp...');
          const { data, error } = await authHelpers.signUp(email, password);
          console.log('Supabase signUp response:', { data, error });

          if (error) {
            console.error('Supabase signup error:', error);
            set({ error: error.message }, false, 'signUp/error');
            throw error;
          }

          // Store username temporarily for later use after OTP verification
          if (data.user) {
            console.log('Storing pending user info in sessionStorage');
            try {
              sessionStorage.setItem('pendingUsername', username);
              sessionStorage.setItem('pendingUserId', data.user.id);
            } catch (storageError) {
              console.warn('⚠️ Could not save to sessionStorage:', storageError);
              // Continue anyway - profile creation may need alternative handling
            }
          }

          console.log('SignUp completed successfully');
          return { data, error: null };
        } catch (error) {
          console.error('Sign up error in useAuthStore:', error);
          return { data: null, error };
        }
      },

      /**
       * Sign in existing user with email and password.
       */
      signIn: async (email, password) => {
        console.log('🔑 Signing in user:', email);
        set({ error: null }, false, 'signIn/start');

        try {
          const { data, error } = await authHelpers.signIn(email, password);

          if (error) {
            console.error('❌ Sign in error:', error);
            set({ error: error.message }, false, 'signIn/error');
            return { data: null, error };
          }

          // If login successful, update state
          if (data?.session && data?.user) {
            console.log('✅ Sign in successful for user:', data.user.id);
            set(
              {
                session: data.session,
                user: data.user,
              },
              false,
              'signIn/success'
            );
          }

          return { data, error: null };
        } catch (error) {
          console.error('❌ Sign in exception:', error);
          set({ error: error.message }, false, 'signIn/exception');
          return { data: null, error };
        }
      },

      /**
       * Sign out current user.
       */
      signOut: async () => {
        console.log('🚪 Signing out user');
        const { setLoading, clearAuth } = get();

        try {
          // Briefly set loading to prevent race conditions during state transitions
          set({ loading: true }, false, 'signOut/start');
          const { error } = await authHelpers.signOut();

          clearAuth();

          // Small delay to ensure auth state is fully cleared before next navigation
          await new Promise((resolve) => setTimeout(resolve, 100));
          set({ loading: false }, false, 'signOut/complete');

          return { error };
        } catch (error) {
          console.error('❌ Sign out error:', error);
          set({ loading: false, error: error.message }, false, 'signOut/error');
          return { error };
        }
      },

      /**
       * Verify OTP code for email verification.
       */
      verifyOtp: async (email, otpCode) => {
        console.log('useAuthStore: Verifying OTP for:', email);
        set({ error: null }, false, 'verifyOtp/start');

        try {
          const { data, error } = await authHelpers.verifyOtp(email, otpCode);

          if (error) {
            console.error('useAuthStore: OTP verification failed:', error);
            set({ error: error.message }, false, 'verifyOtp/error');
            throw error;
          }

          console.log('useAuthStore: OTP verified successfully', data);
          // Profile will be created in the component's handler
          // Don't create it here to avoid duplication

          return { data, error: null };
        } catch (error) {
          console.error('useAuthStore: OTP verification error:', error);
          return { data: null, error };
        }
      },

      /**
       * Resend OTP email.
       */
      resendOtp: async (email) => {
        console.log('📧 Resending OTP to:', email);
        set({ error: null }, false, 'resendOtp/start');

        try {
          const { data, error } = await authHelpers.resendOtp(email);

          if (error) {
            set({ error: error.message }, false, 'resendOtp/error');
            throw error;
          }

          return { data, error: null };
        } catch (error) {
          console.error('Resend OTP error:', error);
          return { data: null, error };
        }
      },

      /**
       * Sign in with Google OAuth.
       */
      signInWithGoogle: async () => {
        console.log('🔐 Signing in with Google');
        set({ error: null }, false, 'signInWithGoogle/start');

        try {
          const result = await supabase.auth.signInWithOAuth({
            provider: 'google',
          });
          return result;
        } catch (error) {
          console.error('❌ Google sign in error:', error);
          set({ error: error.message }, false, 'signInWithGoogle/error');
          return { data: null, error };
        }
      },

      /**
       * Sign in with Facebook OAuth.
       */
      signInWithFacebook: async () => {
        console.log('🔐 Signing in with Facebook');
        set({ error: null }, false, 'signInWithFacebook/start');

        try {
          const result = await supabase.auth.signInWithOAuth({
            provider: 'facebook',
          });
          return result;
        } catch (error) {
          console.error('❌ Facebook sign in error:', error);
          set({ error: error.message }, false, 'signInWithFacebook/error');
          return { data: null, error };
        }
      },

      /**
       * Initialize auth state listener.
       * Call this once on app mount.
       * Returns an unsubscribe function.
       */
      initializeAuth: () => {
        console.log('🔄 Initializing auth state listener');
        set({ loading: true }, false, 'initializeAuth/start');

        try {
          const {
            data: { subscription },
          } = supabase.auth.onAuthStateChange(async (event, session) => {
            console.log('🔐 Auth state change:', event);

            // On tab focus, Supabase client might refresh the token, triggering this.
            // We only want to trigger a state update if the user actually changes (signs in/out).
            // The 'TOKEN_REFRESHED' event should not cause a full user state update.
            if (event !== 'TOKEN_REFRESHED') {
              set(
                {
                  user: session?.user ?? null,
                  session: session,
                  loading: false,
                },
                false,
                `authStateChange/${event}`
              );
            } else {
              // For TOKEN_REFRESHED, only update if session actually changed
              // This prevents unnecessary re-renders and API calls
              const currentSession = get().session;
              if (currentSession?.access_token !== session?.access_token) {
                console.log('🔄 Token refreshed, updating session');
                set({ session }, false, 'authStateChange/TOKEN_REFRESHED');
              }
            }
          });

          // Set up the API client token provider subscription
          // This will update the API client whenever the session changes
          const unsubscribeApiClient = useAuthStore.subscribe(
            (state) => state.session?.access_token,
            (accessToken) => {
              if (accessToken) {
                console.log('🔑 Setting up API client with new token');
                setupApiClient(() => accessToken);
              }
            },
            { fireImmediately: true }
          );

          return () => {
            console.log('🧹 Cleaning up auth subscriptions');
            subscription?.unsubscribe();
            unsubscribeApiClient();
          };
        } catch (error) {
          console.error('❌ Failed to initialize auth listener:', error);

          // If we get a SecurityError (Edge/browser blocking storage),
          // show a helpful error message
          if (error.name === 'SecurityError') {
            console.error('🔒 Browser security settings are blocking authentication.');
            console.error(
              '💡 Please enable cookies/storage for this site in your browser settings.'
            );
            console.error('💡 Or try using a different browser (Chrome, Firefox).');
          }

          set(
            {
              loading: false,
              error:
                'Trình duyệt đang chặn xác thực. Vui lòng bật cookies hoặc thử trình duyệt khác.',
            },
            false,
            'initializeAuth/error'
          );

          // Return a no-op unsubscribe function
          return () => {};
        }
      },
    })),
    {
      name: 'auth-store',
      enabled: import.meta.env.DEV, // Only enable devtools in development
    }
  )
);

// ===== SELECTORS =====
// Use these for optimal performance - components only re-render when selected state changes

export const selectUser = (state) => state.user;
export const selectSession = (state) => state.session;
export const selectLoading = (state) => state.loading;
export const selectError = (state) => state.error;
export const selectIsAuthenticated = (state) => !!state.user && !!state.session;

// ===== ACTIONS (for use outside React components) =====
export const authActions = {
  signUp: useAuthStore.getState().signUp,
  signIn: useAuthStore.getState().signIn,
  signOut: useAuthStore.getState().signOut,
  verifyOtp: useAuthStore.getState().verifyOtp,
  resendOtp: useAuthStore.getState().resendOtp,
  signInWithGoogle: useAuthStore.getState().signInWithGoogle,
  signInWithFacebook: useAuthStore.getState().signInWithFacebook,
  initializeAuth: useAuthStore.getState().initializeAuth,
};

export default useAuthStore;
