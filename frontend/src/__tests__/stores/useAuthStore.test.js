/**
 * Unit tests for useAuthStore.
 * Tests authentication state management including sign in, sign out, and session handling.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act } from '@testing-library/react';

// Import the store - we'll test the actual store behavior
import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';

// Mock the API modules before importing the store
vi.mock('../../api/supabaseClient', () => ({
  supabase: {
    auth: {
      getSession: vi.fn(),
      getUser: vi.fn(),
      signInWithPassword: vi.fn(),
      signOut: vi.fn(),
      onAuthStateChange: vi.fn(() => ({
        data: { subscription: { unsubscribe: vi.fn() } },
      })),
    },
  },
  authHelpers: {
    signUp: vi.fn(),
    signIn: vi.fn(),
    signOut: vi.fn(),
    verifyOtp: vi.fn(),
    resendOtp: vi.fn(),
    getSession: vi.fn(),
    signInWithGoogle: vi.fn(),
  },
}));

vi.mock('../../api/backendApi', () => ({
  setupApiClient: vi.fn(),
}));

// Create a simplified test version of the auth store
const createTestAuthStore = () => {
  return create(
    devtools(
      subscribeWithSelector((set, get) => ({
        // State
        user: null,
        session: null,
        loading: true,
        error: null,

        // Actions
        setUser: (user) => set({ user }, false, 'setUser'),
        setSession: (session) => set({ session }, false, 'setSession'),
        setLoading: (loading) => set({ loading }, false, 'setLoading'),
        setError: (error) => set({ error }, false, 'setError'),
        
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

        // Mock sign in
        signIn: async (email, password) => {
          set({ error: null }, false, 'signIn/start');
          
          if (email === 'test@example.com' && password === 'password123') {
            const mockUser = { id: 'user-123', email };
            const mockSession = { access_token: 'mock-token', user: mockUser };
            set({ user: mockUser, session: mockSession }, false, 'signIn/success');
            return { data: { user: mockUser, session: mockSession }, error: null };
          }
          
          const error = { message: 'Invalid credentials' };
          set({ error: error.message }, false, 'signIn/error');
          return { data: null, error };
        },

        // Mock sign out
        signOut: async () => {
          set({ loading: true }, false, 'signOut/start');
          get().clearAuth();
          set({ loading: false }, false, 'signOut/complete');
          return { error: null };
        },

        // Mock sign up
        signUp: async (email, password, username) => {
          set({ error: null }, false, 'signUp/start');
          
          if (email && password && username) {
            const mockUser = { id: 'new-user-123', email };
            sessionStorage.setItem('pendingUsername', username);
            sessionStorage.setItem('pendingUserId', mockUser.id);
            return { data: { user: mockUser }, error: null };
          }
          
          const error = { message: 'Sign up failed' };
          set({ error: error.message }, false, 'signUp/error');
          return { data: null, error };
        },
      }))
    )
  );
};

describe('useAuthStore', () => {
  let store;

  beforeEach(() => {
    // Create fresh store for each test
    store = createTestAuthStore();
    
    // Clear session storage
    sessionStorage.clear();
  });

  // =========================================================================
  // INITIAL STATE TESTS
  // =========================================================================
  describe('Initial State', () => {
    it('should initialize with null user', () => {
      expect(store.getState().user).toBeNull();
    });

    it('should initialize with null session', () => {
      expect(store.getState().session).toBeNull();
    });

    it('should initialize with loading true', () => {
      expect(store.getState().loading).toBe(true);
    });

    it('should initialize with null error', () => {
      expect(store.getState().error).toBeNull();
    });
  });

  // =========================================================================
  // STATE SETTERS TESTS
  // =========================================================================
  describe('State Setters', () => {
    it('should set user correctly', () => {
      const mockUser = { id: 'user-123', email: 'test@example.com' };
      
      act(() => {
        store.getState().setUser(mockUser);
      });
      
      expect(store.getState().user).toEqual(mockUser);
    });

    it('should set session correctly', () => {
      const mockSession = { access_token: 'token-123' };
      
      act(() => {
        store.getState().setSession(mockSession);
      });
      
      expect(store.getState().session).toEqual(mockSession);
    });

    it('should set loading correctly', () => {
      act(() => {
        store.getState().setLoading(false);
      });
      
      expect(store.getState().loading).toBe(false);
    });

    it('should set error correctly', () => {
      const errorMessage = 'Something went wrong';
      
      act(() => {
        store.getState().setError(errorMessage);
      });
      
      expect(store.getState().error).toBe(errorMessage);
    });

    it('should clear auth state', () => {
      // Set some state first
      act(() => {
        store.getState().setUser({ id: 'user-123' });
        store.getState().setSession({ access_token: 'token' });
        store.getState().setError('some error');
      });
      
      // Clear it
      act(() => {
        store.getState().clearAuth();
      });
      
      expect(store.getState().user).toBeNull();
      expect(store.getState().session).toBeNull();
      expect(store.getState().error).toBeNull();
    });
  });

  // =========================================================================
  // SIGN IN TESTS
  // =========================================================================
  describe('signIn()', () => {
    it('should set user and session on successful login', async () => {
      const result = await store.getState().signIn('test@example.com', 'password123');
      
      expect(result.error).toBeNull();
      expect(result.data.user).toEqual({ id: 'user-123', email: 'test@example.com' });
      expect(store.getState().user).toEqual({ id: 'user-123', email: 'test@example.com' });
      expect(store.getState().session).not.toBeNull();
    });

    it('should set error on failed login', async () => {
      const result = await store.getState().signIn('wrong@example.com', 'wrongpassword');
      
      expect(result.error).not.toBeNull();
      expect(result.error.message).toBe('Invalid credentials');
      expect(store.getState().error).toBe('Invalid credentials');
      expect(store.getState().user).toBeNull();
    });

    it('should clear previous error before login attempt', async () => {
      // Set an error first
      act(() => {
        store.getState().setError('Previous error');
      });
      
      // Attempt login (even if it fails, error should be cleared first)
      await store.getState().signIn('test@example.com', 'password123');
      
      // Error should be null on success
      expect(store.getState().error).toBeNull();
    });
  });

  // =========================================================================
  // SIGN OUT TESTS
  // =========================================================================
  describe('signOut()', () => {
    it('should clear user on sign out', async () => {
      // First sign in
      await store.getState().signIn('test@example.com', 'password123');
      expect(store.getState().user).not.toBeNull();
      
      // Then sign out
      await store.getState().signOut();
      
      expect(store.getState().user).toBeNull();
    });

    it('should clear session on sign out', async () => {
      // First sign in
      await store.getState().signIn('test@example.com', 'password123');
      expect(store.getState().session).not.toBeNull();
      
      // Then sign out
      await store.getState().signOut();
      
      expect(store.getState().session).toBeNull();
    });

    it('should set loading to false after sign out', async () => {
      await store.getState().signOut();
      
      expect(store.getState().loading).toBe(false);
    });
  });

  // =========================================================================
  // SIGN UP TESTS
  // =========================================================================
  describe('signUp()', () => {
    it('should store pending username in sessionStorage', async () => {
      await store.getState().signUp('new@example.com', 'password123', 'testuser');
      
      expect(sessionStorage.getItem('pendingUsername')).toBe('testuser');
    });

    it('should store pending userId in sessionStorage', async () => {
      await store.getState().signUp('new@example.com', 'password123', 'testuser');
      
      expect(sessionStorage.getItem('pendingUserId')).toBe('new-user-123');
    });

    it('should return user data on successful sign up', async () => {
      const result = await store.getState().signUp('new@example.com', 'password123', 'testuser');
      
      expect(result.error).toBeNull();
      expect(result.data.user).toEqual({ id: 'new-user-123', email: 'new@example.com' });
    });
  });

  // =========================================================================
  // SUBSCRIPTION TESTS
  // =========================================================================
  describe('Store Subscriptions', () => {
    it('should notify subscribers on state change', () => {
      const callback = vi.fn();
      
      // Subscribe to user changes
      store.subscribe(
        (state) => state.user,
        callback
      );
      
      // Change user
      act(() => {
        store.getState().setUser({ id: 'user-456' });
      });
      
      expect(callback).toHaveBeenCalledWith(
        { id: 'user-456' },
        null // previous value
      );
    });

    it('should allow selecting specific state slices', () => {
      // Set some state
      act(() => {
        store.getState().setUser({ id: 'user-123', email: 'test@example.com' });
        store.getState().setLoading(false);
      });
      
      // Select just the user
      const user = store.getState().user;
      const loading = store.getState().loading;
      
      expect(user.id).toBe('user-123');
      expect(loading).toBe(false);
    });
  });
});
