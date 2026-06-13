import { useState, useEffect } from 'react';
import { useAuthStore } from '../stores';
import { useNavigate, useLocation } from 'react-router-dom';
import { FiMail, FiLock, FiUser, FiArrowLeft, FiEye, FiEyeOff, FiCheck } from 'react-icons/fi';
import { FcGoogle } from 'react-icons/fc';
import OTPVerification from '../components/OTPVerification';
import { authApi, getApiError } from '../lib/api';
import { authHelpers } from '../api/supabaseClient';
import { Button, Input, Alert, Spinner } from '../ui';
import logoImage from '../../pictures/logo/Icon.png';

/* ── Forgot-password flow (email → new password → OTP) ─────────────────── */
function ForgotPasswordForm({ onSwitchToLogin, signOut }) {
  const [step, setStep] = useState('enterEmail');
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const submitEmail = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    if (!email.includes('@')) return setError('Vui lòng nhập email hợp lệ');
    setLoading(true);
    try {
      const data = await authApi.checkEmail(email);
      if (!data || data.exists === false) { setError('Email không tồn tại trong hệ thống.'); return; }
      setStep('enterPassword');
      setSuccess('Email được xác nhận. Vui lòng nhập mật khẩu mới.');
    } catch (err) {
      setError(getApiError(err).message);
    } finally { setLoading(false); }
  };

  const submitPassword = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    if (newPassword.length < 6) return setError('Mật khẩu phải có ít nhất 6 ký tự');
    if (newPassword !== confirmPassword) return setError('Mật khẩu và xác nhận không khớp');
    setLoading(true);
    try {
      const { error: resetError } = await authHelpers.requestPasswordReset(email);
      if (resetError) throw resetError;
      setSuccess('Mã xác thực đã được gửi tới email của bạn.');
      setStep('verifyOtp');
    } catch (err) {
      setError(err?.status === 429 ? 'Bạn đã gửi quá nhiều yêu cầu. Thử lại sau vài phút.' : (err.message || 'Không thể gửi OTP.'));
    } finally { setLoading(false); }
  };

  const submitOtp = async (otpCode) => {
    setError('');
    try {
      const { error: verifyError } = await authHelpers.verifyRecoveryOtp(email, otpCode);
      if (verifyError) throw verifyError;
      const { error: updateError } = await authHelpers.updatePassword(newPassword);
      if (updateError) throw updateError;
      await signOut();
      setSuccess('Mật khẩu đã được cập nhật thành công!');
      setStep('done');
      setTimeout(onSwitchToLogin, 1800);
    } catch (err) {
      setError(err.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
      throw err;
    }
  };

  if (step === 'enterEmail') {
    return (
      <form className="flex flex-col gap-4" onSubmit={submitEmail}>
        <Header title="Quên mật khẩu" subtitle="Nhập email để khôi phục mật khẩu" />
        {error && <Alert variant="danger">{error}</Alert>}
        <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} iconLeft={<FiMail size={16} />} placeholder="ban@email.com" required disabled={loading} />
        <Button type="submit" fullWidth loading={loading}>Tiếp tục</Button>
        <Button type="button" variant="ghost" fullWidth iconLeft={<FiArrowLeft size={16} />} onClick={onSwitchToLogin}>Quay lại đăng nhập</Button>
      </form>
    );
  }
  if (step === 'enterPassword') {
    return (
      <form className="flex flex-col gap-4" onSubmit={submitPassword}>
        <Header title="Tạo mật khẩu mới" subtitle="Nhập mật khẩu mới cho tài khoản của bạn" />
        {success && <Alert variant="success">{success}</Alert>}
        {error && <Alert variant="danger">{error}</Alert>}
        <Input label="Mật khẩu mới" type={showPassword ? 'text' : 'password'} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} iconLeft={<FiLock size={16} />} required disabled={loading} />
        <Input label="Xác nhận mật khẩu" type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} iconLeft={<FiLock size={16} />} required disabled={loading} />
        <label className="flex items-center gap-2 text-sm text-muted">
          <input type="checkbox" checked={showPassword} onChange={(e) => setShowPassword(e.target.checked)} /> Hiện mật khẩu
        </label>
        <Button type="submit" fullWidth loading={loading}>Gửi mã xác thực</Button>
        <Button type="button" variant="ghost" fullWidth onClick={onSwitchToLogin}>Hủy</Button>
      </form>
    );
  }
  if (step === 'verifyOtp') {
    return (
      <div className="flex flex-col gap-4">
        <Header title="Xác thực OTP" subtitle={`Nhập mã 6 chữ số gửi đến ${email}`} />
        {success && <Alert variant="success">{success}</Alert>}
        <OTPVerification email={email} onVerify={submitOtp} onResend={() => {}} onClose={onSwitchToLogin} />
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4 text-center">
      <Header title="Hoàn tất!" subtitle="Đang chuyển về trang đăng nhập…" />
      <Alert variant="success">{success}</Alert>
    </div>
  );
}

function Header({ title, subtitle }) {
  return (
    <div>
      <h2 className="text-2xl font-bold text-ink">{title}</h2>
      {subtitle && <p className="mt-1 text-base text-muted">{subtitle}</p>}
    </div>
  );
}

/* ── Main auth page ────────────────────────────────────────────────────── */
export default function Login() {
  const location = useLocation();
  const navigate = useNavigate();

  const redirectTarget = (() => {
    const from = location.state?.from;
    if (!from) return '/dashboard';
    if (typeof from === 'string') return from === '/login' ? '/dashboard' : from;
    const path = `${from.pathname || '/dashboard'}${from.search || ''}${from.hash || ''}`;
    return path === '/login' ? '/dashboard' : path;
  })();

  const [mode, setMode] = useState('login'); // login | signup | forgot
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showOtp, setShowOtp] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');

  const user = useAuthStore((s) => s.user);
  const authLoading = useAuthStore((s) => s.loading);
  const { signOut, signIn, signUp, verifyOtp, resendOtp, signInWithGoogle } = useAuthStore.getState();

  useEffect(() => { window.scrollTo(0, 0); }, []);
  useEffect(() => {
    if (location.state?.prefillEmail) {
      setEmail(location.state.prefillEmail);
      if (location.state.mode === 'signup') setMode('signup');
    }
  }, [location.state]);
  useEffect(() => {
    if (user && mode !== 'forgot') navigate(redirectTarget, { replace: true });
  }, [user, mode, navigate, redirectTarget]);

  if (authLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center gradient-brand">
        <Spinner size="lg" className="text-white" />
      </div>
    );
  }

  const clear = () => { setEmail(''); setPassword(''); setConfirmPassword(''); setUsername(''); setError(''); setSuccess(''); };

  const validate = () => {
    if (!email.trim()) return (setError('Vui lòng nhập email'), false);
    if (!password.trim()) return (setError('Vui lòng nhập mật khẩu'), false);
    if (mode === 'signup') {
      if (!username.trim()) return (setError('Vui lòng nhập tên người dùng'), false);
      if (password.length < 6) return (setError('Mật khẩu phải có ít nhất 6 ký tự'), false);
      if (password !== confirmPassword) return (setError('Mật khẩu xác nhận không khớp'), false);
    }
    return true;
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    if (!validate()) return;
    setLoading(true);
    try {
      if (mode === 'signup') {
        const { data, error } = await signUp(email, password, username);
        if (error) throw error;
        if (data?.user) {
          if (!data.user.confirmed_at && !data.user.email_confirmed_at) {
            setPendingEmail(email); setShowOtp(true);
            setSuccess('Mã xác thực đã được gửi! Kiểm tra email của bạn.');
          } else {
            setSuccess('Tạo tài khoản thành công! Bạn có thể đăng nhập ngay.');
            setTimeout(() => { clear(); setMode('login'); }, 1800);
          }
        } else throw new Error('Đăng ký thất bại. Vui lòng thử lại.');
      } else {
        const { data, error } = await signIn(email, password);
        if (error) throw error;
        if (!data?.session || !data?.user) throw new Error('Đăng nhập thất bại');
        setSuccess('Đăng nhập thành công! Đang chuyển hướng…');
      }
    } catch (err) {
      setError(err.message || 'Có lỗi xảy ra. Vui lòng thử lại.');
    } finally { setLoading(false); }
  };

  const googleLogin = async () => {
    setError(''); setLoading(true);
    try { const { error } = await signInWithGoogle(); if (error) throw error; }
    catch (err) { setError(err.message || 'Đăng nhập Google thất bại.'); }
    finally { setLoading(false); }
  };

  const handleVerifyOtp = async (otpCode) => {
    const { error } = await verifyOtp(pendingEmail, otpCode);
    if (error) throw error;
    setShowOtp(false);
    setSuccess('Xác thực thành công! Vui lòng đăng nhập.');
    clear(); setMode('login');
  };

  const isSignUp = mode === 'signup';

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {showOtp && (
        <OTPVerification
          email={pendingEmail}
          onVerify={handleVerifyOtp}
          onResend={() => resendOtp(pendingEmail)}
          onClose={() => { setShowOtp(false); setError('Xác thực đã bị hủy.'); }}
        />
      )}

      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden gradient-brand p-10 text-white lg:flex">
        <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
        <div className="absolute -bottom-20 -left-10 h-72 w-72 rounded-full bg-white/10 blur-3xl" />
        <img src={logoImage} alt="Cramer" className="relative h-10 w-auto" />
        <div className="relative">
          <h1 className="text-4xl font-bold leading-tight">Chinh phục IELTS<br />cùng Cramer</h1>
          <p className="mt-3 max-w-sm text-md text-white/85">
            Nền tảng luyện thi IELTS thông minh với AI, giúp bạn đạt band điểm mơ ước.
          </p>
          <ul className="mt-6 space-y-2.5">
            {['1000+ đề thi thực tế', 'AI đánh giá chi tiết', 'Lộ trình cá nhân hóa'].map((f) => (
              <li key={f} className="flex items-center gap-2.5 text-md">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20"><FiCheck size={14} /></span>
                {f}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-white/60">© {new Date().getFullYear()} Cramer</p>
      </div>

      {/* Form panel */}
      <div className="flex items-center justify-center bg-page p-6">
        <div className="w-full max-w-md">
          <img src={logoImage} alt="Cramer" className="mx-auto mb-6 h-10 w-auto lg:hidden" />
          {mode === 'forgot' ? (
            <ForgotPasswordForm onSwitchToLogin={() => setMode('login')} signOut={signOut} />
          ) : (
            <form className="flex flex-col gap-4" onSubmit={submit}>
              <Header
                title={isSignUp ? 'Tạo tài khoản' : 'Đăng nhập'}
                subtitle={isSignUp ? 'Đăng ký để bắt đầu hành trình IELTS' : 'Chào mừng trở lại! Đăng nhập để tiếp tục.'}
              />
              {error && <Alert variant="danger">{error}</Alert>}
              {success && <Alert variant="success">{success}</Alert>}

              {isSignUp && (
                <Input label="Tên người dùng" value={username} onChange={(e) => setUsername(e.target.value)} iconLeft={<FiUser size={16} />} placeholder="cramer_learner" required disabled={loading} />
              )}
              <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} iconLeft={<FiMail size={16} />} placeholder="ban@email.com" required disabled={loading} />
              <Input
                label="Mật khẩu"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                iconLeft={<FiLock size={16} />}
                required
                disabled={loading}
                inputClassName="pr-10"
              />
              {isSignUp && (
                <Input label="Xác nhận mật khẩu" type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} iconLeft={<FiLock size={16} />} required disabled={loading} />
              )}

              <div className="flex items-center justify-between">
                <label className="flex items-center gap-2 text-sm text-muted">
                  <input type="checkbox" checked={showPassword} onChange={(e) => setShowPassword(e.target.checked)} />
                  {showPassword ? <FiEyeOff size={14} /> : <FiEye size={14} />} Hiện mật khẩu
                </label>
                {!isSignUp && (
                  <button type="button" className="text-sm font-semibold text-brand-600 hover:text-brand-700" onClick={() => { setMode('forgot'); clear(); }}>
                    Quên mật khẩu?
                  </button>
                )}
              </div>

              <Button type="submit" fullWidth loading={loading}>{isSignUp ? 'Đăng ký' : 'Đăng nhập'}</Button>

              <div className="relative flex items-center gap-3 py-1 text-sm text-faint">
                <span className="h-px flex-1 bg-line" /> hoặc <span className="h-px flex-1 bg-line" />
              </div>

              <Button type="button" variant="outline" fullWidth iconLeft={<FcGoogle size={18} />} onClick={googleLogin} disabled={loading}>
                Tiếp tục với Google
              </Button>

              <p className="text-center text-base text-muted">
                {isSignUp ? 'Đã có tài khoản? ' : 'Chưa có tài khoản? '}
                <button type="button" className="font-semibold text-brand-600 hover:text-brand-700" onClick={() => { setMode(isSignUp ? 'login' : 'signup'); clear(); }}>
                  {isSignUp ? 'Đăng nhập' : 'Đăng ký ngay'}
                </button>
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
