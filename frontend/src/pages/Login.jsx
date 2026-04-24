import { useState, useEffect } from 'react';
import { useAuthStore, useProfileStore } from '../stores';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaGoogle, FaEnvelope, FaLock, FaUser, FaArrowLeft, FaEye, FaEyeSlash } from 'react-icons/fa';
import OTPVerification from '../components/OTPVerification';
import { authApi } from '../api/backendApi';
import { authHelpers } from '../api/supabaseClient';
import logoImage from '../../pictures/logo/Icon.png';
import '../css/login.css';

// ====================================================================
// FORGOT PASSWORD FORM
// ====================================================================
function ForgotPasswordForm({ onSwitchToLogin, signOut }) {
  const [step, setStep] = useState('enterEmail');
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showPassword, setShowPassword] = useState(false);

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
      setStep('enterPassword');
      setSuccess('Email được xác nhận. Vui lòng nhập mật khẩu mới.');
    } catch (err) {
      setError(err.message || 'Không thể kiểm tra email. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

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
      const { error: resetError } = await authHelpers.requestPasswordReset(email);
      if (resetError) throw resetError;
      setSuccess('Mã xác thực đã được gửi tới email của bạn.');
      setStep('verifyOtp');
    } catch (err) {
      if (err?.status === 429 || err?.message?.includes('rate')) {
        setError('Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau vài phút.');
      } else {
        setError(err.message || 'Không thể gửi yêu cầu OTP. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (otpCode) => {
    setError('');
    setLoading(true);
    try {
      const { error: verifyError } = await authHelpers.verifyRecoveryOtp(email, otpCode);
      if (verifyError) throw verifyError;
      const { error: updateError } = await authHelpers.updatePassword(newPassword);
      if (updateError) throw updateError;
      await signOut();
      setSuccess('Mật khẩu đã được cập nhật thành công!');
      setStep('done');
      setTimeout(() => onSwitchToLogin(), 2000);
    } catch (err) {
      setError(err.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  if (step === 'enterEmail') {
    return (
      <form className="auth-form" onSubmit={handleEmailSubmit}>
        <h2 className="auth-title">Quên mật khẩu</h2>
        <p className="auth-subtitle">Nhập email để khôi phục mật khẩu</p>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}

        <div className="auth-input-group">
          <FaEnvelope className="auth-input-icon" />
          <input
            type="email"
            placeholder="Địa chỉ email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
            required
          />
        </div>

        <button type="submit" className="auth-btn auth-btn--primary" disabled={loading}>
          {loading ? 'Đang xử lý...' : 'Tiếp tục'}
        </button>

        <button type="button" className="auth-btn auth-btn--text" onClick={onSwitchToLogin}>
          <FaArrowLeft /> Quay lại đăng nhập
        </button>
      </form>
    );
  }

  if (step === 'enterPassword') {
    return (
      <form className="auth-form" onSubmit={handlePasswordSubmit}>
        <h2 className="auth-title">Tạo mật khẩu mới</h2>
        <p className="auth-subtitle">Nhập mật khẩu mới cho tài khoản của bạn</p>

        {success && <div className="auth-alert auth-alert--success">{success}</div>}
        {error && <div className="auth-alert auth-alert--error">{error}</div>}

        <div className="auth-input-group">
          <FaLock className="auth-input-icon" />
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Mật khẩu mới"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={loading}
            required
          />
          <button type="button" className="auth-password-toggle" onClick={() => setShowPassword(!showPassword)}>
            {showPassword ? <FaEyeSlash /> : <FaEye />}
          </button>
        </div>

        <div className="auth-input-group">
          <FaLock className="auth-input-icon" />
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Xác nhận mật khẩu"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={loading}
            required
          />
        </div>

        <button type="submit" className="auth-btn auth-btn--primary" disabled={loading}>
          {loading ? 'Đang xử lý...' : 'Gửi mã xác thực'}
        </button>

        <button type="button" className="auth-btn auth-btn--text" onClick={onSwitchToLogin}>
          Hủy
        </button>
      </form>
    );
  }

  if (step === 'verifyOtp') {
    return (
      <div className="auth-form">
        <h2 className="auth-title">Xác thực OTP</h2>
        <p className="auth-subtitle">Nhập mã 6 chữ số được gửi đến <strong>{email}</strong></p>
        {success && <div className="auth-alert auth-alert--success">{success}</div>}
        <OTPVerification email={email} onVerify={handleOtpSubmit} onResend={() => { }} onClose={onSwitchToLogin} />
      </div>
    );
  }

  return (
    <div className="auth-form">
      <h2 className="auth-title">Hoàn tất!</h2>
      <div className="auth-alert auth-alert--success">{success}</div>
      <p className="auth-subtitle">Đang chuyển về trang đăng nhập...</p>
    </div>
  );
}

// ====================================================================
// MAIN LOGIN COMPONENT
// ====================================================================
export default function Login() {
  const location = useLocation();
  const [formState, setFormState] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showOtpPopup, setShowOtpPopup] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');

  // Zustand store selectors
  const user = useAuthStore(state => state.user);
  const authLoading = useAuthStore(state => state.loading);
  const signOut = useAuthStore(state => state.signOut);
  const signIn = useAuthStore(state => state.signIn);
  const signUp = useAuthStore(state => state.signUp);
  const verifyOtp = useAuthStore(state => state.verifyOtp);
  const resendOtp = useAuthStore(state => state.resendOtp);
  const signInWithGoogle = useAuthStore(state => state.signInWithGoogle);
  const createProfileForUser = useProfileStore(state => state.createProfileForUser);
  const navigate = useNavigate();

  // Scroll to top on mount
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // Handle prefilled email from homepage
  useEffect(() => {
    if (location.state?.prefillEmail) {
      setEmail(location.state.prefillEmail);
      if (location.state.mode === 'signup') {
        setFormState('signup');
      }
    }
  }, [location.state]);

  useEffect(() => {
    if (user && formState !== 'forgot') {
      navigate('/dashboard');
    }
  }, [user, formState, navigate]);

  // Show a styled loading indicator while auth state is being determined
  // This prevents the blank page issue during logout → login transition
  if (authLoading) {
    return (
      <div className="login-page">
        <div className="login-bg-orbs">
          <div className="login-orb login-orb--1" />
          <div className="login-orb login-orb--2" />
          <div className="login-orb login-orb--3" />
        </div>
        <div className="login-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
          <div style={{ color: 'white', fontSize: '1.2rem' }}>Đang tải...</div>
        </div>
      </div>
    );
  }

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
      setError('Vui lòng nhập email');
      return false;
    }
    if (!password.trim()) {
      setError('Vui lòng nhập mật khẩu');
      return false;
    }
    if (formState === 'signup') {
      if (!username.trim()) {
        setError('Vui lòng nhập tên người dùng');
        return false;
      }
      if (password.length < 6) {
        setError('Mật khẩu phải có ít nhất 6 ký tự');
        return false;
      }
      if (password !== confirmPassword) {
        setError('Mật khẩu xác nhận không khớp');
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
            setSuccess('Mã xác thực đã được gửi! Kiểm tra email của bạn.');
          } else {
            await createProfileForUser(data.user.id, username || email.split('@')[0]);
            setSuccess('Tạo tài khoản thành công! Bạn có thể đăng nhập ngay.');
            setTimeout(() => {
              clearForm();
              setFormState('login');
            }, 2000);
          }
        } else {
          throw new Error('Đăng ký thất bại. Vui lòng thử lại.');
        }
      } else {
        const { data, error } = await signIn(email, password);
        if (error) throw error;
        if (!data?.session || !data?.user) throw new Error('Đăng nhập thất bại');
        setSuccess('Đăng nhập thành công! Đang chuyển hướng...');
      }
    } catch (err) {
      setError(err.message || 'Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError('');
    setLoading(true);
    try {
      const { error } = await signInWithGoogle();
      if (error) throw error;
    } catch (err) {
      setError(err.message || 'Đăng nhập Google thất bại.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (otpCode) => {
    try {
      const { data, error } = await verifyOtp(pendingEmail, otpCode);
      if (error) throw error;

      const storedUsername = sessionStorage.getItem('pendingUsername');
      const userId = sessionStorage.getItem('pendingUserId');

      if (userId && storedUsername) {
        await createProfileForUser(userId, storedUsername);
        sessionStorage.removeItem('pendingUsername');
        sessionStorage.removeItem('pendingUserId');
      }

      setShowOtpPopup(false);
      setSuccess('Xác thực thành công! Vui lòng đăng nhập.');
      clearForm();
      setFormState('login');
    } catch (error) {
      throw error;
    }
  };

  const renderFormContent = () => {
    if (formState === 'forgot') {
      return <ForgotPasswordForm onSwitchToLogin={() => setFormState('login')} signOut={signOut} />;
    }

    const isSignUp = formState === 'signup';

    return (
      <form className="auth-form" onSubmit={handleSubmit}>
        <h2 className="auth-title">{isSignUp ? 'Tạo tài khoản' : 'Đăng nhập'}</h2>
        <p className="auth-subtitle">
          {isSignUp ? 'Đăng ký để bắt đầu hành trình chinh phục IELTS' : 'Chào mừng trở lại! Đăng nhập để tiếp tục.'}
        </p>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}
        {success && <div className="auth-alert auth-alert--success">{success}</div>}

        {isSignUp && (
          <div className="auth-input-group">
            <FaUser className="auth-input-icon" />
            <input
              type="text"
              placeholder="Tên người dùng"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              required
            />
          </div>
        )}

        <div className="auth-input-group">
          <FaEnvelope className="auth-input-icon" />
          <input
            type="email"
            placeholder="Địa chỉ email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
            required
          />
        </div>

        <div className="auth-input-group">
          <FaLock className="auth-input-icon" />
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Mật khẩu"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
            required
          />
          <button type="button" className="auth-password-toggle" onClick={() => setShowPassword(!showPassword)}>
            {showPassword ? <FaEyeSlash /> : <FaEye />}
          </button>
        </div>

        {isSignUp && (
          <div className="auth-input-group">
            <FaLock className="auth-input-icon" />
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Xác nhận mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={loading}
              required
            />
          </div>
        )}

        {!isSignUp && (
          <div className="auth-links">
            <button type="button" className="auth-link" onClick={() => { setFormState('forgot'); clearForm(); }}>
              Quên mật khẩu?
            </button>
          </div>
        )}

        <button type="submit" className="auth-btn auth-btn--primary" disabled={loading}>
          {loading ? 'Đang xử lý...' : (isSignUp ? 'Đăng ký' : 'Đăng nhập')}
        </button>

        <div className="auth-divider">
          <span>hoặc</span>
        </div>

        <button type="button" className="auth-btn auth-btn--google" onClick={handleGoogleLogin} disabled={loading}>
          <FaGoogle />
          <span>Tiếp tục với Google</span>
        </button>

        <p className="auth-switch">
          {isSignUp ? 'Đã có tài khoản?' : 'Chưa có tài khoản?'}
          <button type="button" onClick={() => { setFormState(isSignUp ? 'login' : 'signup'); clearForm(); }}>
            {isSignUp ? 'Đăng nhập' : 'Đăng ký ngay'}
          </button>
        </p>
      </form>
    );
  };

  return (
    <div className="login-page">
      {showOtpPopup && (
        <OTPVerification
          email={pendingEmail}
          onVerify={handleVerifyOtp}
          onResend={() => resendOtp(pendingEmail)}
          onClose={() => { setShowOtpPopup(false); setError('Xác thực đã bị hủy.'); }}
        />
      )}

      {/* Background orbs */}
      <div className="login-bg-orbs">
        <div className="login-orb login-orb--1" />
        <div className="login-orb login-orb--2" />
        <div className="login-orb login-orb--3" />
      </div>

      <div className="login-container">
        {/* Left side - Branding */}
        <div className="login-branding">
          <div className="login-branding-content">
            <img src={logoImage} alt="Cramer" className="login-logo" />
            <h1 className="login-headline">
              Chinh phục IELTS
              <br />
              <span>cùng Cramer</span>
            </h1>
            <p className="login-tagline">
              Nền tảng luyện thi IELTS thông minh với công nghệ AI,
              giúp bạn đạt band điểm mơ ước.
            </p>

            <div className="login-features">
              <div className="login-feature">
                <div className="login-feature-icon">✓</div>
                <span>1000+ đề thi thực tế</span>
              </div>
              <div className="login-feature">
                <div className="login-feature-icon">✓</div>
                <span>AI đánh giá chi tiết</span>
              </div>
              <div className="login-feature">
                <div className="login-feature-icon">✓</div>
                <span>Lộ trình cá nhân hóa</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right side - Form */}
        <div className="login-form-section">
          <div className="login-form-card">
            <div className="login-form-card-border" />
            <div className="login-form-card-inner">
              {renderFormContent()}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
