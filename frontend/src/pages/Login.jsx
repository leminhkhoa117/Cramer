import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FaGoogle, FaFacebook } from 'react-icons/fa';
import '../css/Login.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [username, setUsername] = useState('');
  const [isSignUp, setIsSignUp] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { signIn, signUp, signInWithGoogle, signInWithFacebook } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isSignUp) {
        const { error } = await signUp(email, password, username);
        if (error) throw error;
        alert('Check your email for confirmation link!');
        setIsSignUp(false); // Switch back to login after sign up
      } else {
        const { error } = await signIn(email, password);
        if (error) throw error;
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.message || 'An error occurred');
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

  const FormContent = (
    <form className="login-form" onSubmit={handleSubmit}>
      <h2>{isSignUp ? 'Sign Up' : 'Login'}</h2>
      {error && <p style={{ color: 'red', textAlign: 'center', marginTop: '10px' }}>{error}</p>}
      
      {isSignUp && (
        <div className="input-box">
          <input 
            type="text" 
            required={isSignUp}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
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
        />
        <span>Password</span>
        <i></i>
      </div>

      <div className="links">
        <a href="#">Forgot Password?</a>
        <a href="#" onClick={(e) => { e.preventDefault(); setIsSignUp(!isSignUp); setError(''); }}>
          {isSignUp ? 'Login' : 'Sign Up'}
        </a>
      </div>

      <input type="submit" value={loading ? '...' : (isSignUp ? 'Sign Up' : 'Login')} disabled={loading} />

      <div className="social-login">
        <p>Or sign in with:</p>
        <div className="social-icons">
          <button type="button" onClick={() => handleSocialLogin('google')} aria-label="Login with Google">
            <FaGoogle />
          </button>
          <button type="button" onClick={() => handleSocialLogin('facebook')} aria-label="Login with Facebook">
            <FaFacebook />
          </button>
        </div>
      </div>
    </form>
  );

  return (
    <div className="login-page-container">
        <div className="intro-section">
          <h1>Welcome to Cramer</h1>
          <p>Your personal platform for mastering new subjects through interactive quizzes and smart learning tools. Log in to continue your journey or sign up to get started!</p>
        </div>
        <div className="form-section">
          <div className="login-box">
            <div className="login-box-border">
              <div className="login-mask"></div>
              <div className="login-initial">{isSignUp ? 'SIGN UP' : 'LOGIN'}</div>
              <div className="login-form-wrapper">
                {FormContent}
              </div>
            </div>
          </div>
        </div>
    </div>
  );
}
