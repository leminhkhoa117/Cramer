import { createContext, useContext, useEffect, useState, useMemo, useRef } from 'react';
import { supabase, authHelpers } from '../api/supabaseClient';
import { profileApi, setupApiClient } from '../api/backendApi';

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
  const [error, setError] = useState(null);
  
  // Track the last user ID we loaded profile for to prevent duplicate loads
  const lastLoadedUserIdRef = useRef(null);

  useEffect(() => {
    setLoading(true);
    
    // Try to set up auth state listener with error handling for browser security issues
    try {
      const { data: { subscription } } = supabase.auth.onAuthStateChange(async (event, session) => {
        console.log('🔐 Auth state change:', event);
        
        // On tab focus, Supabase client might refresh the token, triggering this.
        // We only want to trigger a profile reload if the user actually changes (signs in/out).
        // The 'TOKEN_REFRESHED' event should not cause a full user state update.
        if (event !== 'TOKEN_REFRESHED') {
          setUser(session?.user ?? null);
          // Update session for non-refresh events (login, logout, etc.)
          setSession(session);
        } else {
          // For TOKEN_REFRESHED, only update if session actually changed
          // This prevents unnecessary re-renders and API calls
          setSession(prevSession => {
            // Only update if the access token actually changed
            if (prevSession?.access_token !== session?.access_token) {
              console.log('🔄 Token refreshed, updating session');
              return session;
            }
            return prevSession;
          });
        }
        
        setLoading(false);
      });

      return () => {
        subscription?.unsubscribe();
      };
    } catch (error) {
      console.error('❌ Failed to initialize auth listener:', error);
      
      // If we get a SecurityError (Edge/browser blocking storage), 
      // show a helpful error message
      if (error.name === 'SecurityError') {
        console.error('🔒 Browser security settings are blocking authentication.');
        console.error('💡 Please enable cookies/storage for this site in your browser settings.');
        console.error('💡 Or try using a different browser (Chrome, Firefox).');
      }
      
      setLoading(false);
      setError('Trình duyệt đang chặn xác thực. Vui lòng bật cookies hoặc thử trình duyệt khác.');
    }
  }, []);

  // Set up the API client with the token whenever the session changes
  // Use access_token as dependency to avoid unnecessary updates
  useEffect(() => {
    if (session?.access_token) {
      setupApiClient(() => session.access_token);
    }
  }, [session?.access_token]);

  useEffect(() => {
    const userId = user?.id;
    
    // Only load profile if:
    // 1. We have a user ID
    // 2. We haven't loaded this user's profile yet
    if (userId && userId !== lastLoadedUserIdRef.current) {
      loadUserProfile(userId);
    } else if (!userId) {
      // User logged out, clear profile
      setProfile(null);
      lastLoadedUserIdRef.current = null;
    }
  }, [user?.id]); // Only depend on user.id, not the entire user object

  const loadUserProfile = async (userId) => {
    if (!userId) {
      console.error('❌ loadUserProfile called without userId');
      return;
    }
    
    // Check if we already loaded this user's profile
    if (userId === lastLoadedUserIdRef.current && profile !== null) {
      console.log('✅ Profile already loaded for user:', userId);
      return;
    }

    setProfileLoading(true);
    console.log('📥 Loading profile for user:', userId);

    try {
      const response = await profileApi.getById(userId);
      
      if (response?.data) {
        console.log('✅ Profile loaded successfully:', response.data.username);
        setProfile(response.data);
        lastLoadedUserIdRef.current = userId; // Mark as loaded
      } else {
        console.warn('⚠️ Empty response from backend');
        setProfile(null);
      }
    } catch (error) {
      console.error('❌ Failed to load profile:', error);
      // Only set profile to null if there wasn't one to begin with.
      // A failed background refresh shouldn't clear the user's existing data.
      if (!profile) {
        setProfile(null);
      }
      
      // If profile doesn't exist (404), try to create it
      if (error.response?.status === 404) {
        console.log('🔨 Profile not found (404), attempting to create...');
        await createProfileForUser(userId);
      } else if (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED') {
        console.error('🔌 Network error or timeout - backend might be down or not responding');
      } else if (error.response?.status >= 500) {
        console.error('🔥 Server error');
      } else {
        console.error('⚠️ Unknown error type');
      }
    } finally {
      setProfileLoading(false);
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
        lastLoadedUserIdRef.current = userId; // Mark as loaded
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
            lastLoadedUserIdRef.current = userId; // Mark as loaded
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
      
      // If login successful, update state. The useEffect hooks will handle the rest.
      if (data?.session && data?.user) {
        console.log('✅ Sign in successful for user:', data.user.id);
        setSession(data.session);
        setUser(data.user);
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
      lastLoadedUserIdRef.current = null; // Reset the loaded user ID ref
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

  const value = useMemo(() => ({
    user,
    profile,
    session,
    loading,
    profileLoading,
    error,
    signUp,
    signIn,
    signOut,
    verifyOtp,
    resendOtp,
    signInWithGoogle,
    signInWithFacebook,
    loadUserProfile, // Expose for manual reload if needed
  }), [
    user,
    profile,
    session,
    loading,
    error,
    profileLoading,
    signUp,
    signIn,
    signOut,
    verifyOtp,
    resendOtp,
    signInWithGoogle,
    signInWithFacebook,
    loadUserProfile,
  ]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
