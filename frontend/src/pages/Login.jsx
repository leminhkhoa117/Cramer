import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FaGoogle, FaFacebook } from 'react-icons/fa';
import OTPVerification from '../components/OTPVerification';
import { profileApi, authApi } from '../api/backendApi';
import { authHelpers } from '../api/supabaseClient';
import '../css/Login.css';

// ====================================================================
// FORGOT PASSWORD FORM (New Workflow)
// ====================================================================
function ForgotPasswordForm({ onSwitchToLogin, signOut }) {
  const [step, setStep] = useState('enterEmail'); // enterEmail -> enterPassword -> verifyOtp -> done
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Handler for the first step: submitting the email
  const handleEmailSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (!email || !email.includes('@')) {
      setError('Vui lòng nhập email hợp lệ');
      return;
    }
    setLoading(true);
    try {
      const { data } = await authApi.checkEmail(email);
      if (!data || data.exists === false) {
        setError('Email không tồn tại trong hệ thống.');
        return;
      }
      // Email exists, move to the next step
      setStep('enterPassword');
      setSuccess('Email được xác nhận. Vui lòng nhập mật khẩu mới.');
    } catch (err) {
      setError(err.message || 'Không thể kiểm tra email. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Handler for the second step: submitting the new password to request an OTP
  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (newPassword.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu và xác nhận không khớp');
      return;
    }
    setLoading(true);
    try {
      // Passwords match, now request the OTP
      const { error: resetError } = await authHelpers.requestPasswordReset(email);
      if (resetError) throw resetError;
      
      setSuccess('Mật khẩu đã được lưu tạm. Mã xác thực đã được gửi tới email của bạn.');
      setStep('verifyOtp');
    } catch (err) {
      setError(err.message || 'Không thể gửi yêu cầu OTP. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Handler for the third step: verifying OTP and updating the password
  const handleOtpSubmit = async (otpCode) => {
    setError('');
    setLoading(true);
    try {
      // 1. Verify the OTP. This creates a temporary, authenticated session.
      const { error: verifyError } = await authHelpers.verifyRecoveryOtp(email, otpCode);
      if (verifyError) throw verifyError;

      // 2. Immediately use the new session to update the user's password.
      const { error: updateError } = await authHelpers.updatePassword(newPassword);
      if (updateError) throw updateError;

      // 3. Sign the user out to invalidate the recovery session and force a fresh login.
      await signOut();

      setSuccess('Mật khẩu đã được cập nhật thành công! Vui lòng đăng nhập lại.');
      setStep('done');
      setTimeout(() => onSwitchToLogin(), 3000);

    } catch (err) {
      setError(err.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
      // Re-throw for the OTP component to handle its internal error state
      throw err;
    } finally {
      setLoading(false);
    }
  };

  // Render different content based on the current step
  const renderStep = () => {
    switch (step) {
      case 'enterEmail':
        return (
          <form className="login-form" onSubmit={handleEmailSubmit}>
            <h2>Quên mật khẩu</h2>
            {error && <div style={{ background: '#fee', color: '#c33', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center' }}>{error}</div>}
            <div className="input-box">
              <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} disabled={loading} autoComplete="email" />
              <span>Email</span><i></i>
            </div>
            <div className="links">
              <a href="#" onClick={(e) => { e.preventDefault(); onSwitchToLogin(); }}>← Quay lại Đăng nhập</a>
            </div>
            <input type="submit" value={loading ? '...' : 'Xác nhận Email'} disabled={loading} />
          </form>
        );

      case 'enterPassword':
        return (
          <form className="login-form" onSubmit={handlePasswordSubmit}>
            <h2>Tạo mật khẩu mới</h2>
            {success && <div style={{ background: '#efe', color: '#3c3', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center' }}>{success}</div>}
            {error && <div style={{ background: '#fee', color: '#c33', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center' }}>{error}</div>}
            <div className="input-box">
              <input type="password" required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} disabled={loading} />
              <span>Mật khẩu mới</span><i></i>
            </div>
            <div className="input-box">
              <input type="password" required value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={loading} />
              <span>Xác nhận mật khẩu</span><i></i>
            </div>
            <div className="links">
              <a href="#" onClick={(e) => { e.preventDefault(); onSwitchToLogin(); }}>Hủy</a>
            </div>
            <input type="submit" value={loading ? '...' : 'Gửi mã xác thực'} disabled={loading} />
          </form>
        );

      case 'verifyOtp':
        return (
          <div className="login-form">
            <h2>Xác thực OTP</h2>
            {success && <div style={{ background: '#efe', color: '#3c3', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center' }}>{success}</div>}
            <p style={{ marginBottom: 8, textAlign: 'center' }}>Nhập mã 6 chữ số được gửi đến <strong>{email}</strong></p>
            <OTPVerification email={email} onVerify={handleOtpSubmit} onResend={() => {}} onClose={onSwitchToLogin} />
          </div>
        );
      
      case 'done':
        return (
          <div className="login-form">
            <h2>Hoàn tất</h2>
            {success && <div style={{ background: '#efe', color: '#3c3', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center' }}>{success}</div>}
            <p>Bạn sẽ được chuyển về trang đăng nhập.</p>
          </div>
        );

      default:
        return null;
    }
  };

  return renderStep();
}


// ====================================================================
// MAIN LOGIN COMPONENT
// ====================================================================
export default function Login() {
  const [formState, setFormState] = useState('login'); // 'login', 'signup', 'forgot'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showOtpPopup, setShowOtpPopup] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');
  
  const { user, signOut, signIn, signUp, verifyOtp, resendOtp, signInWithGoogle, signInWithFacebook } = useAuth();
  const navigate = useNavigate();

  // This effect handles redirection after a successful login.
  // It is guarded by checking the formState to prevent interrupting the password reset flow.
  useEffect(() => {
    if (user && formState !== 'forgot') {
      console.log('User detected and not in forgot password flow, navigating to dashboard...');
      navigate('/dashboard');
    }
  }, [user, formState, navigate]);

  const clearForm = () => {
    setEmail('');
    setPassword('');
    setConfirmPassword('');
    setUsername('');
    setError('');
    setSuccess('');
  };

  const validateForm = () => {
    if (!email.trim()) {
      setError('Email is required');
      return false;
    }
    if (!password.trim()) {
      setError('Password is required');
      return false;
    }
    if (formState === 'signup') {
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
    setError('');
    setSuccess('');
    setLoading(true);

    if (!validateForm()) {
      setLoading(false);
      return;
    }

    try {
      if (formState === 'signup') {
        const { data, error } = await signUp(email, password, username);
        if (error) throw error;
        
        if (data?.user) {
          if (!data.user.confirmed_at && !data.user.email_confirmed_at) {
            setPendingEmail(email);
            setShowOtpPopup(true);
            setSuccess('Verification code sent to your email! Check your inbox.');
          } else {
            await profileApi.create({ id: data.user.id, username: username || email.split('@')[0] });
            setSuccess('Account created successfully! You can now login.');
            setTimeout(() => {
              clearForm();
              setFormState('login');
            }, 2000);
          }
        } else {
          throw new Error('Signup failed. Please try again.');
        }
      } else { // 'login' state
        const { data, error } = await signIn(email, password);
        if (error) throw error;
        if (!data?.session || !data?.user) throw new Error('Login failed: No session created');
        setSuccess('Logged in successfully! Redirecting...');
        // The useEffect hook will handle the navigation
      }
    } catch (err) {
      setError(err.message || 'An error occurred. Please try again.');
    } finally {
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

  const handleVerifyOtp = async (otpCode) => {
    try {
      const { data, error } = await verifyOtp(pendingEmail, otpCode);
      if (error) throw error;
      
      const username = sessionStorage.getItem('pendingUsername');
      const userId = sessionStorage.getItem('pendingUserId');
      
      if (userId && username) {
        await profileApi.create({ id: userId, username: username });
        sessionStorage.removeItem('pendingUsername');
        sessionStorage.removeItem('pendingUserId');
      }
      
      setShowOtpPopup(false);
      setSuccess('Account verified successfully! Please login.');
      clearForm();
      setFormState('login');
      
    } catch (error) {
      throw error;
    }
  };

  const handleResendOtp = async () => {
    const { error } = await resendOtp(pendingEmail);
    if (error) throw error;
  };

  const handleCloseOtp = () => {
    setShowOtpPopup(false);
    setError('Verification cancelled. Please sign up again if needed.');
  };

  const renderFormContent = () => {
    if (formState === 'forgot') {
      return <ForgotPasswordForm onSwitchToLogin={() => setFormState('login')} signOut={signOut} />;
    }

    const isSignUp = formState === 'signup';

    return (
      <form className="login-form" onSubmit={handleSubmit}>
        <h2>{isSignUp ? 'Create Account' : 'Login'}</h2>
        
        {error && <div style={{ background: '#fee', color: '#c33', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center', fontSize: '14px' }}>⚠️ {error}</div>}
        {success && <div style={{ background: '#efe', color: '#3c3', padding: '10px', borderRadius: '4px', marginBottom: '15px', textAlign: 'center', fontSize: '14px' }}>✓ {success}</div>}
        
        {isSignUp && (
          <div className="input-box">
            <input type="text" required={isSignUp} value={username} onChange={(e) => setUsername(e.target.value)} disabled={loading} />
            <span>Username</span>
            <i></i>
          </div>
        )}

        <div className="input-box">
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} disabled={loading} autoComplete="email" />
          <span>Email</span>
          <i></i>
        </div>

        <div className="input-box">
          <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} disabled={loading} autoComplete={isSignUp ? 'new-password' : 'current-password'} />
          <span>Password</span>
          <i></i>
        </div>

        {isSignUp && (
          <div className="input-box">
            <input type="password" required={isSignUp} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} disabled={loading} />
            <span>Confirm Password</span>
            <i></i>
          </div>
        )}

        <div className="links">
          {!isSignUp && <a href="#" onClick={(e) => { e.preventDefault(); setFormState('forgot'); clearForm(); }}>Forgot Password?</a>}
          <a href="#" onClick={(e) => { 
            e.preventDefault(); 
            setFormState(isSignUp ? 'login' : 'signup'); 
            clearForm();
          }}>
            {isSignUp ? '← Back to Login' : 'Create Account'}
          </a>
        </div>

        <input type="submit" value={loading ? '...' : (isSignUp ? 'Sign Up' : 'Login')} disabled={loading} />

        {!isSignUp && (
          <div className="social-login">
            <p>Or sign in with:</p>
            <div className="social-icons">
              <button type="button" onClick={() => handleSocialLogin('google')} aria-label="Login with Google" disabled={loading}><FaGoogle /></button>
              <button type="button" onClick={() => handleSocialLogin('facebook')} aria-label="Login with Facebook" disabled={loading}><FaFacebook /></button>
            </div>
          </div>
        )}
      </form>
    );
  };

  const getBoxTitle = () => {
    if (formState === 'login') return 'LOGIN';
    if (formState === 'signup') return 'SIGN UP';
    if (formState === 'forgot') return 'RESET';
    return 'LOGIN';
  }

  return (
    <div className="login-page-container">
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
            <div className="login-initial">{getBoxTitle()}</div>
            <div className="login-form-wrapper">
              {renderFormContent()}
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
