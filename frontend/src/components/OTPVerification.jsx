import { useState, useEffect } from 'react';

export default function OTPVerification({ email, onVerify, onResend, onClose }) {
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  // Countdown for resend button
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => setResendCooldown(resendCooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (otp.length !== 6) {
      setError('Please enter 6-digit OTP');
      return;
    }

    setLoading(true);
    try {
      await onVerify(otp);
    } catch (err) {
      setError(err.message || 'Invalid OTP code');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setLoading(true);
    try {
      await onResend();
      setResendCooldown(60);
      setOtp('');
    } catch (err) {
      setError(err.message || 'Failed to resend OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '16px',
        padding: '40px',
        maxWidth: '500px',
        width: '100%',
        boxShadow: '0 10px 40px rgba(0, 0, 0, 0.2)',
        position: 'relative',
        textAlign: 'center'
      }}>
        {/* Close button */}
        <button
          onClick={onClose}
          disabled={loading}
          style={{
            position: 'absolute',
            top: '16px',
            right: '16px',
            background: 'none',
            border: 'none',
            fontSize: '28px',
            cursor: 'pointer',
            color: '#999',
            lineHeight: '1'
          }}
        >
          ×
        </button>

        {/* Illustration */}
        <div style={{ marginBottom: '24px' }}>
          <svg width="200" height="160" viewBox="0 0 200 160" fill="none" style={{ margin: '0 auto' }}>
            {/* Phone */}
            <rect x="70" y="20" width="80" height="120" rx="8" fill="#6B7FFF" stroke="#5568E0" strokeWidth="2"/>
            {/* Screen */}
            <rect x="78" y="35" width="64" height="90" rx="4" fill="#E8EBFF"/>
            {/* Shield */}
            <circle cx="150" cy="40" r="18" fill="#FF5757"/>
            <path d="M150 32 L144 38 L148 42 L156 34 Z" fill="white" strokeWidth="2"/>
            {/* Person */}
            <circle cx="40" cy="80" r="12" fill="#FFB8B8"/>
            <rect x="28" y="92" width="24" height="28" rx="4" fill="#FF6B6B"/>
            <rect x="28" y="115" width="24" height="25" rx="4" fill="#4A5FD9"/>
            {/* Laptop */}
            <rect x="25" y="125" width="40" height="4" rx="2" fill="#FF6B6B"/>
            {/* OTP boxes */}
            <rect x="82" y="55" width="12" height="16" rx="2" fill="white" stroke="#6B7FFF" strokeWidth="1"/>
            <rect x="96" y="55" width="12" height="16" rx="2" fill="white" stroke="#6B7FFF" strokeWidth="1"/>
            <rect x="110" y="55" width="12" height="16" rx="2" fill="white" stroke="#6B7FFF" strokeWidth="1"/>
            <rect x="124" y="55" width="12" height="16" rx="2" fill="white" stroke="#6B7FFF" strokeWidth="1"/>
            {/* Locks */}
            <circle cx="25" cy="45" r="4" fill="#DDD"/>
            <rect x="22" y="45" width="6" height="6" rx="1" fill="#BBB"/>
            <circle cx="175" cy="100" r="4" fill="#DDD"/>
            <rect x="172" y="100" width="6" height="6" rx="1" fill="#BBB"/>
          </svg>
        </div>

        {/* Title */}
        <h2 style={{
          fontSize: '28px',
          fontWeight: '700',
          color: '#1a1a1a',
          marginBottom: '12px'
        }}>
          OTP Verification
        </h2>

        {/* Description */}
        <p style={{
          fontSize: '14px',
          color: '#666',
          marginBottom: '32px',
          lineHeight: '1.5'
        }}>
          One-Time Password sent to your registered<br />
          mobile number and email address
        </p>

        {/* Error message */}
        {error && (
          <div style={{
            backgroundColor: '#FFE5E5',
            color: '#D32F2F',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '20px',
            fontSize: '14px'
          }}>
            {error}
          </div>
        )}

        {/* OTP Input */}
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Enter OTP"
            value={otp}
            onChange={(e) => {
              const value = e.target.value.replace(/\D/g, '');
              if (value.length <= 6) setOtp(value);
            }}
            disabled={loading}
            maxLength={6}
            style={{
              width: '100%',
              padding: '16px 20px',
              fontSize: '16px',
              border: '2px solid #E0E0E0',
              borderRadius: '12px',
              marginBottom: '16px',
              textAlign: 'center',
              letterSpacing: '8px',
              fontWeight: '600',
              outline: 'none',
              transition: 'border-color 0.3s'
            }}
            onFocus={(e) => e.target.style.borderColor = '#6B7FFF'}
            onBlur={(e) => e.target.style.borderColor = '#E0E0E0'}
          />

          {/* Resend link */}
          <p style={{
            fontSize: '14px',
            color: '#666',
            marginBottom: '24px'
          }}>
            Didn't receive the OTP?{' '}
            {resendCooldown > 0 ? (
              <span style={{ color: '#999' }}>Resend in {resendCooldown}s</span>
            ) : (
              <button
                type="button"
                onClick={handleResend}
                disabled={loading}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#6B7FFF',
                  fontWeight: '600',
                  cursor: 'pointer',
                  padding: 0,
                  textDecoration: 'none'
                }}
                onMouseOver={(e) => e.target.style.textDecoration = 'underline'}
                onMouseOut={(e) => e.target.style.textDecoration = 'none'}
              >
                Resend OTP
              </button>
            )}
          </p>

          {/* Verify button */}
          <button
            type="submit"
            disabled={loading || otp.length !== 6}
            style={{
              width: '100%',
              padding: '16px',
              fontSize: '16px',
              fontWeight: '600',
              color: 'white',
              backgroundColor: (loading || otp.length !== 6) ? '#CCC' : '#6B7FFF',
              border: 'none',
              borderRadius: '12px',
              cursor: (loading || otp.length !== 6) ? 'not-allowed' : 'pointer',
              transition: 'background-color 0.3s'
            }}
            onMouseOver={(e) => {
              if (!loading && otp.length === 6) {
                e.target.style.backgroundColor = '#5568E0';
              }
            }}
            onMouseOut={(e) => {
              if (!loading && otp.length === 6) {
                e.target.style.backgroundColor = '#6B7FFF';
              }
            }}
          >
            {loading ? 'Verifying...' : 'VERIFY'}
          </button>
        </form>
      </div>
    </div>
  );
}
