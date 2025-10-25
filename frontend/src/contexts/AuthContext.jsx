import { createContext, useContext, useEffect, useState } from 'react';
import { supabase, authHelpers } from '../api/supabaseClient';
import { profileApi } from '../api/backendApi';

const AuthContext = createContext({});

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Get initial session
    authHelpers.getSession().then(({ session, error }) => {
      if (session) {
        setSession(session);
        setUser(session.user);
        loadUserProfile(session.user.id);
      }
      setLoading(false);
    });

    // Listen for auth changes
    const { data: { subscription } } = authHelpers.onAuthStateChange(
      async (event, session) => {
        console.log('Auth state changed:', event);
        setSession(session);
        setUser(session?.user ?? null);
        
        if (session?.user) {
          await loadUserProfile(session.user.id);
        } else {
          setProfile(null);
        }
      }
    );

    return () => {
      subscription?.unsubscribe();
    };
  }, []);

  const loadUserProfile = async (userId) => {
    try {
      const response = await profileApi.getById(userId);
      setProfile(response.data);
    } catch (error) {
      console.error('Error loading profile:', error);
      // Profile doesn't exist, create one
      if (error.response?.status === 404 && user?.email) {
        try {
          const newProfile = {
            id: userId,
            username: user.email.split('@')[0], // Default username from email
          };
          const response = await profileApi.create(newProfile);
          setProfile(response.data);
        } catch (createError) {
          console.error('Error creating profile:', createError);
        }
      }
    }
  };

  const signUp = async (email, password, username) => {
    try {
      const { data, error } = await authHelpers.signUp(email, password);
      if (error) throw error;

      // Create profile in backend
      if (data.user) {
        await profileApi.create({
          id: data.user.id,
          username: username || email.split('@')[0],
        });
      }

      return { data, error: null };
    } catch (error) {
      return { data: null, error };
    }
  };

  const signIn = async (email, password) => {
    try {
      const { data, error } = await authHelpers.signIn(email, password);
      return { data, error };
    } catch (error) {
      return { data: null, error };
    }
  };

  const signOut = async () => {
    try {
      const { error } = await authHelpers.signOut();
      setUser(null);
      setProfile(null);
      setSession(null);
      return { error };
    } catch (error) {
      return { error };
    }
  };

  const value = {
    user,
    profile,
    session,
    loading,
    signUp,
    signIn,
    signOut,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
