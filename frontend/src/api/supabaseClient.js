import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseAnonKey);

if (!isSupabaseConfigured) {
  console.warn(
    'Supabase credentials not found. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in frontend/.env to enable Supabase features.',
  );
}

const createConfigurationError = () =>
  new Error('Supabase credentials are not configured. Update frontend/.env to enable authentication features.');

const supabaseClient = isSupabaseConfigured
  ? createClient(supabaseUrl, supabaseAnonKey)
  : null;

export const supabase = supabaseClient;

// Helper functions for auth
export const authHelpers = {
  // Sign up new user
  signUp: async (email, password) => {
    if (!supabase) {
      return { data: null, error: createConfigurationError() };
    }
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
    });
    return { data, error };
  },

  // Sign in existing user
  signIn: async (email, password) => {
    if (!supabase) {
      return { data: null, error: createConfigurationError() };
    }
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    return { data, error };
  },

  // Sign out current user
  signOut: async () => {
    if (!supabase) {
      return { error: createConfigurationError() };
    }
    const { error } = await supabase.auth.signOut();
    return { error };
  },

  // Get current session
  getSession: async () => {
    if (!supabase) {
      return { session: null, error: createConfigurationError() };
    }
    const {
      data: { session },
      error,
    } = await supabase.auth.getSession();
    return { session, error };
  },

  // Get current user
  getUser: async () => {
    if (!supabase) {
      return { user: null, error: createConfigurationError() };
    }
    const {
      data: { user },
      error,
    } = await supabase.auth.getUser();
    return { user, error };
  },

  // Listen to auth state changes
  onAuthStateChange: (callback) => {
    if (!supabase) {
      console.warn('Supabase auth listener not initialised because credentials are missing.');
      return { data: { subscription: { unsubscribe: () => {} } } };
    }
    return supabase.auth.onAuthStateChange(callback);
  },
};

export default supabase;
