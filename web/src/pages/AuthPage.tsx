import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Eye, EyeOff, Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const AuthPage: React.FC = () => {
  const { user, signUp, signIn } = useAuth();
  const navigate = useNavigate();

  const [isSignUp, setIsSignUp] = useState(true);
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [infoMsg, setInfoMsg] = useState<string | null>(null);

  // If already logged in, redirect
  React.useEffect(() => {
    if (user) {
      navigate('/');
    }
  }, [user, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim() || (isSignUp && !displayName.trim())) {
      setErrorMsg('Please complete all required fields.');
      return;
    }

    if (password.length < 6) {
      setErrorMsg('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);
    setErrorMsg(null);
    setInfoMsg(null);

    try {
      if (isSignUp) {
        const result = await signUp(email.trim(), password, displayName.trim());
        if (result.needsEmailConfirmation) {
          setInfoMsg('Account created! Please check your email to confirm your address, then sign in below.');
          setIsSignUp(false);
          setPassword('');
          return;
        }
      } else {
        await signIn(email.trim(), password);
      }
      navigate('/');
    } catch (err: any) {
      setErrorMsg(err.message || 'Authentication failed. Please verify your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 64px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '32px 16px',
    }}>
      <div className="glass-card animate-fade-in" style={{
        maxWidth: '440px',
        width: '100%',
        padding: '36px 32px',
        borderRadius: 'var(--radius-xl)',
        boxShadow: '0 20px 40px -15px rgba(0, 0, 0, 0.7)',
      }}>
        {/* Emblem */}
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <img
            src="/logo.png"
            alt="BracketX"
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '18px',
              objectFit: 'cover',
              boxShadow: '0 0 24px -2px rgba(47, 128, 237, 0.6)',
              marginBottom: '16px',
            }}
          />
          <h1 style={{ fontSize: '26px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '6px' }}>
            {isSignUp ? 'Create BracketX Account' : 'Welcome Back'}
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Fair Tournament Operating System for College Gaming
          </p>
        </div>

        {/* Mode Toggle Switch */}
        <div style={{
          display: 'flex',
          background: 'var(--bg-surface)',
          padding: '4px',
          borderRadius: 'var(--radius-sm)',
          marginBottom: '24px',
          border: '1px solid var(--border-subtle)',
        }}>
          <button
            type="button"
            onClick={() => { setIsSignUp(true); setErrorMsg(null); }}
            style={{
              flex: 1,
              padding: '10px 0',
              border: 'none',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              background: isSignUp ? 'var(--accent-blue)' : 'transparent',
              color: isSignUp ? '#FFFFFF' : 'var(--text-secondary)',
              transition: 'all 0.2s ease',
            }}
          >
            Sign Up
          </button>
          <button
            type="button"
            onClick={() => { setIsSignUp(false); setErrorMsg(null); setInfoMsg(null); }}
            style={{
              flex: 1,
              padding: '10px 0',
              border: 'none',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              background: !isSignUp ? 'var(--accent-blue)' : 'transparent',
              color: !isSignUp ? '#FFFFFF' : 'var(--text-secondary)',
              transition: 'all 0.2s ease',
            }}
          >
            Sign In
          </button>
        </div>

        {/* Info message */}
        {infoMsg && (
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            background: 'rgba(46, 204, 113, 0.12)',
            border: '1px solid rgba(46, 204, 113, 0.35)',
            borderRadius: 'var(--radius-sm)',
            padding: '12px 14px',
            marginBottom: '20px',
            color: '#2ECC71',
            fontSize: '13px',
            lineHeight: 1.4,
          }}>
            <CheckCircle2 size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>{infoMsg}</span>
          </div>
        )}

        {/* Error message */}
        {errorMsg && (
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            background: 'rgba(235, 87, 87, 0.12)',
            border: '1px solid rgba(235, 87, 87, 0.35)',
            borderRadius: 'var(--radius-sm)',
            padding: '12px 14px',
            marginBottom: '20px',
            color: 'var(--status-error)',
            fontSize: '13px',
            lineHeight: 1.4,
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {isSignUp && (
            <div>
              <label className="input-label">Display Name</label>
              <input
                type="text"
                className="input-field"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required={isSignUp}
                autoFocus
              />
            </div>
          )}

          <div>
            <label className="input-label">Email Address</label>
            <input
              type="email"
              className="input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="input-label">
              {isSignUp ? 'Password (min 6 characters)' : 'Password'}
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                className="input-field"
                style={{ paddingRight: '40px' }}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-muted)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="btn-primary"
            disabled={loading}
            style={{ marginTop: '8px', padding: '14px', fontSize: '15px' }}
          >
            {loading ? (
              <Loader2 size={18} className="animate-spin" />
            ) : isSignUp ? (
              'Create Account'
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        {/* Footer switch prompt */}
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <button
            type="button"
            onClick={() => { setIsSignUp(!isSignUp); setErrorMsg(null); }}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--accent-blue)',
              fontSize: '13px',
              cursor: 'pointer',
              fontWeight: 500,
            }}
          >
            {isSignUp ? 'Already have an account? Sign In' : "Don't have an account? Sign Up"}
          </button>
        </div>
      </div>
    </div>
  );
};
