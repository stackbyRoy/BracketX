import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { User, LogOut, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const ProfilePage: React.FC = () => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return (
      <div className="app-container" style={{ padding: '60px 16px', textAlign: 'center' }}>
        <div className="glass-card" style={{ maxWidth: '400px', margin: '0 auto', padding: '32px' }}>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>You are not signed in.</p>
          <Link to="/auth" className="btn-primary" style={{ width: '100%' }}>
            Sign Up or Sign In
          </Link>
        </div>
      </div>
    );
  }

  const handleSignOut = async () => {
    await signOut();
    navigate('/auth');
  };

  return (
    <div className="app-container" style={{ padding: '32px 16px 80px 16px', maxWidth: '560px' }}>
      <h1 style={{ fontSize: '26px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '24px' }}>
        Profile & Settings
      </h1>

      {/* User Card */}
      <div className="glass-card" style={{ padding: '24px', borderRadius: 'var(--radius-md)', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{
            width: '56px',
            height: '56px',
            borderRadius: '50%',
            background: 'var(--bg-elevated)',
            border: '2px solid var(--accent-blue)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <User size={28} color="var(--accent-blue)" />
          </div>

          <div>
            <div style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>
              {user.displayName}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
              {user.email || `ID: ${user.id}`}
            </div>
          </div>
        </div>
      </div>

      {/* Unified Account Model card */}
      <div className="glass-card" style={{ padding: '20px', borderRadius: 'var(--radius-md)', marginBottom: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <ShieldCheck size={18} color="var(--accent-cyan)" />
          <h3 style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)' }}>
            Unified Account Model
          </h3>
        </div>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
          BracketX utilizes a single unified account system. You can host your own tournaments and participate as a competitor in other campus tournaments seamlessly using the same profile.
        </p>
      </div>

      {/* Sign Out Action */}
      <button
        onClick={handleSignOut}
        className="btn-danger"
        style={{ width: '100%', padding: '12px', fontSize: '14px' }}
      >
        <LogOut size={16} />
        <span>Sign Out</span>
      </button>

      <div style={{ textAlign: 'center', marginTop: '40px', fontSize: '12px', color: 'var(--text-muted)' }}>
        BracketX Web v1.0.0 · Fair Tournament Operating System
      </div>
    </div>
  );
};
