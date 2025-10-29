import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FaGoogle, FaFacebook } from 'react-icons/fa';
import OTPVerification from '../components/OTPVerification';
import { profileApi } from '../api/backendApi';
import '../css/Login.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [username, setUsername] = useState('');
  const [isSignUp, setIsSignUp] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showOtpPopup, setShowOtpPopup] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');
  
  const { signIn, signUp, verifyOtp, resendOtp, signInWithGoogle, signInWithFacebook } = useAuth();
  const navigate = useNavigate();

  // Validate input
  const validateForm = () => {
    if (!email.trim()) {
      setError('Email is required');
      return false;
    }
    if (!password.trim()) {
      setError('Password is required');
      return false;
    }
    if (isSignUp) {
      if (!username.trim()) {
        setError('Username is required');
        return false;
      }
      if (password.length < 6) {
        setError('Password must be at least 6 characters');
        return false;
      }
      if (password !== confirmPassword) {
        setError('Passwords do not match');
        return false;
      }
    }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    console.log('=== handleSubmit called ===', { isSignUp, email });
    setError('');
    setSuccess('');
    setLoading(true);

    if (!validateForm()) {
      console.log('Validation failed');
      setLoading(false);
      return;
    }

    console.log('Validation passed, proceeding with', isSignUp ? 'signup' : 'login');

    try {
      if (isSignUp) {
        console.log('Starting signup process for:', email);
        
        // Try signup without timeout first to see actual error
        const { data, error } = await signUp(email, password, username);
        
        console.log('Signup completed:', { 
          hasData: !!data, 
          hasUser: !!data?.user,
          hasError: !!error,
          errorMessage: error?.message,
          errorDetails: error
        });
        
        if (error) {
          console.error('Signup error details:', error);
          throw error;
        }
        
        // Check if signup was successful
        if (data?.user) {
          console.log('User created successfully:', data.user.id);
          console.log('User confirmation status:', {
            confirmed_at: data.user.confirmed_at,
            email_confirmed_at: data.user.email_confirmed_at
          });
          
          // Check if email confirmation is required (confirmed_at is null means OTP required)
          if (!data.user.confirmed_at && !data.user.email_confirmed_at) {
            // Email confirmation is enabled - show OTP popup
            console.log('Email confirmation required - showing OTP popup');
            setPendingEmail(email);
            setShowOtpPopup(true);
            setSuccess('Verification code sent to your email! Check your inbox.');
          } else {
            // Email confirmation is disabled - user is already confirmed
            console.log('Email confirmation disabled - user auto-confirmed');
            
            // Create profile immediately
            try {
              await profileApi.create({
                id: data.user.id,
                username: username || email.split('@')[0],
              });
              console.log('Profile created successfully');
              setSuccess('Account created successfully! You can now login.');
              
              // Clear form and switch to login after delay
              setTimeout(() => {
                setEmail('');
                setPassword('');
                setConfirmPassword('');
                setUsername('');
                setIsSignUp(false);
              }, 2000);
            } catch (profileError) {
              console.error('Error creating profile:', profileError);
              setError('Account created but profile creation failed. Please contact support.');
            }
          }
        } else {
          console.error('No user data returned');
          throw new Error('Signup failed. Please try again.');
        }
        
        setLoading(false);
        
      } else {
        // Login flow - remove timeout, let Supabase handle it
        console.log('Starting login for:', email);
        const { data, error } = await signIn(email, password);
        
        console.log('Login result:', { hasData: !!data, hasSession: !!data?.session, hasUser: !!data?.user, hasError: !!error, error });
        
        if (error) {
          console.error('Login error:', error);
          throw error;
        }
        
        if (!data?.session || !data?.user) {
          console.error('No session or user returned from login');
          throw new Error('Login failed: No session created');
        }
        
        console.log('Login successful, user:', data.user.id, 'session:', data.session.access_token?.substring(0, 20) + '...');
        setSuccess('Logged in successfully! Redirecting...');
        setLoading(false);
        
        // Redirect immediately
        console.log('Redirecting to dashboard...');
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.message || 'An error occurred. Please try again.');
      console.error('Auth error:', err);
      setLoading(false);
    }
  };

  const handleSocialLogin = async (provider) => {
    setError('');
    setLoading(true);
    try {
        let error;
        if (provider === 'google') {
            ({ error } = await signInWithGoogle());
        } else if (provider === 'facebook') {
            ({ error } = await signInWithFacebook());
        }
        if (error) throw error;
        navigate('/dashboard');
    } catch (err) {
        setError(err.message || `An error occurred with ${provider} login.`);
    } finally {
        setLoading(false);
    }
  };

  // Handle OTP verification
  const handleVerifyOtp = async (otpCode) => {
    try {
      console.log('=== handleVerifyOtp START ===');
      console.log('Verifying OTP code:', otpCode);
      console.log('Pending email:', pendingEmail);
      
      const { data, error } = await verifyOtp(pendingEmail, otpCode);
      console.log('verifyOtp result:', { data, error });
      
      if (error) {
        console.error('OTP verification error:', error);
        throw error;
      }
      
      console.log('OTP verified successfully, data:', data);
      
      // Get username from sessionStorage
      const username = sessionStorage.getItem('pendingUsername');
      const userId = sessionStorage.getItem('pendingUserId');
      
      console.log('Retrieved from sessionStorage:', { username, userId });
      
      // Create profile in backend
      if (userId && username) {
        console.log('Creating profile:', { userId, username });
        try {
          // Add timeout to prevent hanging
          const profilePromise = profileApi.create({
            id: userId,
            username: username,
          });
          
          const timeoutPromise = new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Profile creation timeout')), 10000)
          );
          
          await Promise.race([profilePromise, timeoutPromise]);
          console.log('Profile created successfully');
        } catch (profileError) {
          console.error('Error creating profile:', profileError);
          // Continue anyway - profile might already exist or backend is down
          console.log('Continuing despite profile creation error...');
        }
        
        // Clear sessionStorage
        sessionStorage.removeItem('pendingUsername');
        sessionStorage.removeItem('pendingUserId');
      } else {
        console.warn('Missing userId or username in sessionStorage');
      }
      
      // Success - close popup and show success message
      console.log('Closing OTP popup and showing success message');
      setShowOtpPopup(false);
      setSuccess('Account verified successfully! Redirecting to login...');
      
      // Clear form
      setEmail('');
      setPassword('');
      setConfirmPassword('');
      setUsername('');
      setPendingEmail('');
      
      // Switch to login after 2 seconds
      setTimeout(() => {
        setIsSignUp(false);
        setSuccess('');
      }, 2000);
      
      console.log('=== handleVerifyOtp END ===');
    } catch (error) {
      console.error('=== handleVerifyOtp ERROR ===', error);
      throw error; // Re-throw to let OTPVerification component handle it
    }
  };

  // Handle resend OTP
  const handleResendOtp = async () => {
    const { error } = await resendOtp(pendingEmail);
    if (error) {
      throw error;
    }
  };

  // Close OTP popup
  const handleCloseOtp = () => {
    setShowOtpPopup(false);
    setError('Verification cancelled. Please sign up again if needed.');
  };

  const FormContent = (
    <form className="login-form" onSubmit={handleSubmit}>
      <h2>{isSignUp ? 'Create Account' : 'Login'}</h2>
      
      {error && (
        <div style={{ 
          background: '#fee', 
          color: '#c33', 
          padding: '10px', 
          borderRadius: '4px', 
          marginBottom: '15px',
          textAlign: 'center',
          fontSize: '14px'
        }}>
          ⚠️ {error}
        </div>
      )}
      
      {success && (
        <div style={{ 
          background: '#efe', 
          color: '#3c3', 
          padding: '10px', 
          borderRadius: '4px', 
          marginBottom: '15px',
          textAlign: 'center',
          fontSize: '14px'
        }}>
          ✓ {success}
        </div>
      )}
      
      {isSignUp && (
        <div className="input-box">
          <input 
            type="text" 
            required={isSignUp}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={loading}
          />
          <span>Username</span>
          <i></i>
        </div>
      )}

      <div className="input-box">
        <input 
          type="email" 
          required 
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={loading}
        />
        <span>Email</span>
        <i></i>
      </div>

      <div className="input-box">
        <input 
          type="password" 
          required 
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={loading}
        />
        <span>Password</span>
        <i></i>
      </div>

      {isSignUp && (
        <div className="input-box">
          <input 
            type="password" 
            required={isSignUp}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={loading}
          />
          <span>Confirm Password</span>
          <i></i>
        </div>
      )}

      <div className="links">
        {!isSignUp && <a href="#">Forgot Password?</a>}
        <a href="#" onClick={(e) => { 
          e.preventDefault(); 
          setIsSignUp(!isSignUp); 
          setError(''); 
          setSuccess('');
          setEmail('');
          setPassword('');
          setUsername('');
          setConfirmPassword('');
        }}>
          {isSignUp ? '← Back to Login' : 'Create Account'}
        </a>
      </div>

      <input 
        type="submit" 
        value={loading ? '...' : (isSignUp ? 'Sign Up' : 'Login')} 
        disabled={loading}
      />

      {!isSignUp && (
        <div className="social-login">
          <p>Or sign in with:</p>
          <div className="social-icons">
            <button type="button" onClick={() => handleSocialLogin('google')} aria-label="Login with Google" disabled={loading}>
              <FaGoogle />
            </button>
            <button type="button" onClick={() => handleSocialLogin('facebook')} aria-label="Login with Facebook" disabled={loading}>
              <FaFacebook />
            </button>
          </div>
        </div>
      )}
    </form>
  );

  return (
    <div className="login-page-container">
        {/* OTP Verification Popup */}
        {showOtpPopup && (
          <OTPVerification
            email={pendingEmail}
            onVerify={handleVerifyOtp}
            onResend={handleResendOtp}
            onClose={handleCloseOtp}
          />
        )}
        
        <div className="form-section">
          <div className="login-box">
            <div className="login-box-border"></div>
            <div className="login-initial">{isSignUp ? 'SIGN UP' : 'LOGIN'}</div>
            <div className="login-form-wrapper">
              {FormContent}
            </div>
          </div>
        </div>
        
        <div className="intro-section">
          <div className="intro-content">
            <svg className="float-animation" width="160" height="160" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="svg-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor="rgba(255,255,255,0.8)"/>
                  <stop offset="100%" stopColor="rgba(255,255,255,1)"/>
                </linearGradient>
              </defs>
              <rect x="30" y="50" width="140" height="100" rx="10" fill="url(#svg-grad)" fillOpacity="0.2" stroke="white" strokeWidth="2.5"/>
              <circle cx="75" cy="100" r="20" fill="url(#svg-grad)" fillOpacity="0.3" stroke="white" strokeWidth="2"/>
              <path d="M 68 100 l 5 5 l 10 -10" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M 110 90 v 20 M 122 82 v 28 M 134 95 v 10" stroke="white" strokeWidth="3" strokeLinecap="round" />
              <path d="M 175 65 l 10 -10 M 170 50 l 15 -15" stroke="white" strokeWidth="2" strokeLinecap="round" opacity="0.8"/>
              <path d="M 25 65 l -10 -10 M 30 50 l -15 -15" stroke="white" strokeWidth="2" strokeLinecap="round" opacity="0.8"/>
              <path d="M 100 30 v -15" stroke="white" strokeWidth="2" strokeLinecap="round" opacity="0.8"/>
            </svg>
            <h1>Welcome to Cramer</h1>
            <p>Your personal platform for mastering new subjects through interactive quizzes and smart learning tools. Log in to continue your journey or sign up to get started!</p>
          </div>
        </div>
    </div>
  );
}
