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
  const [profileLoading, setProfileLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    const { data: { subscription } } = supabase.auth.onAuthStateChange(async (event, session) => {
      setSession(session);
      setUser(session?.user ?? null);
      setLoading(false);
    });

    return () => {
      subscription?.unsubscribe();
    };
  }, []);

  useEffect(() => {
    if (user) {
      loadUserProfile(user.id);
    }
  }, [user]);

  const loadUserProfile = async (userId) => {
    if (!userId) {
      console.error('❌ loadUserProfile called without userId');
      return;
    }

    setProfileLoading(true);
    console.log('📥 Loading profile for user:', userId);

    try {
      const response = await profileApi.getById(userId);
      
      if (response?.data) {
        console.log('✅ Profile loaded successfully:', response.data.username);
        setProfile(response.data);
      } else {
        console.warn('⚠️ Empty response from backend');
        setProfile(null);
      }
    } catch (error) {
      console.error('❌ Failed to load profile:', error);
      console.error('Error details:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        data: error.response?.data
      });
      
      // If profile doesn't exist (404), try to create it
      if (error.response?.status === 404) {
        console.log('🔨 Profile not found (404), attempting to create...');
        await createProfileForUser(userId);
      } else if (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED') {
        console.error('🔌 Network error or timeout - backend might be down or not responding');
        setProfile(null);
      } else if (error.response?.status >= 500) {
        console.error('🔥 Server error');
        setProfile(null);
      } else {
        console.error('⚠️ Unknown error type');
        setProfile(null);
      }
    } finally {
      setProfileLoading(false);
      console.log('🏁 Profile loading finished. Profile set:', !!profile);
    }
  };

  const createProfileForUser = async (userId) => {
    try {
      console.log('📝 Creating profile for user:', userId);
      
      // Get user email from auth
      const { user: authUser } = await authHelpers.getUser();
      
      if (!authUser) {
        console.error('❌ Cannot create profile - no auth user found');
        return;
      }

      // Generate username from email or use a default
      const username = authUser.email?.split('@')[0] || `user_${userId.substring(0, 8)}`;
      
      const newProfile = {
        id: userId,
        username: username,
      };

      console.log('� Creating profile with data:', newProfile);
      const response = await profileApi.create(newProfile);
      
      if (response?.data) {
        console.log('✅ Profile created successfully:', response.data);
        setProfile(response.data);
      }
    } catch (createError) {
      console.error('❌ Failed to create profile:', createError);
      
      // If username already exists, try with timestamp
      if (createError.response?.status === 409 || createError.response?.status === 400) {
        console.log('⚠️ Username conflict, retrying with timestamp...');
        
        try {
          const timestampUsername = `user_${Date.now()}`;
          const retryProfile = {
            id: userId,
            username: timestampUsername,
          };
          
          const retryResponse = await profileApi.create(retryProfile);
          if (retryResponse?.data) {
            console.log('✅ Profile created with timestamp username:', retryResponse.data);
            setProfile(retryResponse.data);
          }
        } catch (retryError) {
          console.error('❌ Failed to create profile even with timestamp:', retryError);
        }
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
      console.log('🔑 Signing in user:', email);
      const { data, error } = await authHelpers.signIn(email, password);
      
      if (error) {
        console.error('❌ Sign in error:', error);
        return { data: null, error };
      }
      
      // If login successful, update state and load profile
      if (data?.session && data?.user) {
        console.log('✅ Sign in successful for user:', data.user.id);
        setSession(data.session);
        setUser(data.user);
        
        // Load profile and WAIT for it
        await loadUserProfile(data.user.id);
        
        console.log('✅ Profile loaded after sign in');
      }
      
      return { data, error: null };
    } catch (error) {
      console.error('❌ Sign in exception:', error);
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
    profileLoading,
    signUp,
    signIn,
    signOut,
    verifyOtp,
    resendOtp,
    signInWithGoogle,
    signInWithFacebook,
    loadUserProfile, // Expose for manual reload if needed
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
