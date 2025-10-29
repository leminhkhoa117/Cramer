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
      
      // DON'T auto-create profile - only load existing profiles
      // Profile will be created manually after OTP verification or in signup flow
      if (error.response?.status === 404) {
        console.log('Profile not found for user:', userId);
        setProfile(null);
      }
    }
  };

  const signUp = async (email, password, username) => {
    console.log('AuthContext signUp called with:', { email, username });
    
    try {
      // Sign up with Supabase Auth (will send OTP email if enabled)
      console.log('Calling Supabase signUp...');
      const { data, error } = await authHelpers.signUp(email, password);
      console.log('Supabase signUp response:', { data, error });
      
      if (error) {
        console.error('Supabase signup error:', error);
        throw error;
      }

      // Store username temporarily for later use after OTP verification
      if (data.user) {
        console.log('Storing pending user info in sessionStorage');
        sessionStorage.setItem('pendingUsername', username);
        sessionStorage.setItem('pendingUserId', data.user.id);
      }

      console.log('SignUp completed successfully');
      return { data, error: null };
    } catch (error) {
      console.error('Sign up error in AuthContext:', error);
      return { data: null, error };
    }
  };

  const verifyOtp = async (email, otpCode) => {
    try {
      console.log('AuthContext: Verifying OTP for:', email);
      // Verify OTP with Supabase
      const { data, error } = await authHelpers.verifyOtp(email, otpCode);
      
      if (error) {
        console.error('AuthContext: OTP verification failed:', error);
        throw error;
      }

      console.log('AuthContext: OTP verified successfully', data);
      
      // Profile will be created in Login.jsx handleVerifyOtp()
      // Don't create it here to avoid duplication

      return { data, error: null };
    } catch (error) {
      console.error('AuthContext: OTP verification error:', error);
      return { data: null, error };
    }
  };

  const resendOtp = async (email) => {
    try {
      const { data, error } = await authHelpers.resendOtp(email);
      if (error) throw error;
      return { data, error: null };
    } catch (error) {
      console.error('Resend OTP error:', error);
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

  const signInWithGoogle = async () => {
    return await supabase.auth.signInWithOAuth({
      provider: 'google',
    });
  };

  const signInWithFacebook = async () => {
    return await supabase.auth.signInWithOAuth({
      provider: 'facebook',
    });
  };

  const value = {
    user,
    profile,
    session,
    loading,
    signUp,
    signIn,
    signOut,
    verifyOtp,
    resendOtp,
    signInWithGoogle,
    signInWithFacebook,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
