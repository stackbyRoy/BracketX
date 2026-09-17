import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Trophy, PlusCircle, User as UserIcon, LogIn, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Navbar: React.FC = () => {
  const { user, signOut } = useAuth();
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="ios-safe-top" style={{
      background: 'rgba(11, 13, 16, 0.85)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
      borderBottom: '1px solid var(--border-subtle)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div className="app-container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '64px',
      }}>
        {/* Brand */}
        <Link to="/" style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          textDecoration: 'none',
        }}>
          <img
            src="/logo.png"
            alt="BracketX Logo"
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              objectFit: 'cover',
              boxShadow: '0 0 16px -2px rgba(47, 128, 237, 0.5)',
            }}
          />
          <div>
            <span style={{
              fontFamily: 'var(--font-display)',
              fontSize: '20px',
              fontWeight: 800,
              letterSpacing: '0.04em',
              color: 'var(--text-primary)',
            }}>
              BRACKET<span style={{ color: 'var(--accent-blue)' }}>X</span>
            </span>
          </div>
        </Link>

        {/* Navigation Links */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Link
            to="/"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: 'var(--radius-sm)',
              textDecoration: 'none',
              fontSize: '14px',
              fontWeight: 600,
              color: isActive('/') ? 'var(--accent-blue)' : 'var(--text-secondary)',
              background: isActive('/') ? 'rgba(47, 128, 237, 0.1)' : 'transparent',
              transition: 'all 0.2s ease',
            }}
          >
            <Trophy size={16} />
            <span className="hide-on-mobile">Tournaments</span>
          </Link>

          <Link
            to="/create"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: 'var(--radius-sm)',
              textDecoration: 'none',
              fontSize: '14px',
              fontWeight: 600,
              color: isActive('/create') ? 'var(--accent-blue)' : 'var(--text-secondary)',
              background: isActive('/create') ? 'rgba(47, 128, 237, 0.1)' : 'transparent',
              transition: 'all 0.2s ease',
            }}
          >
            <PlusCircle size={16} />
            <span className="hide-on-mobile">Create</span>
          </Link>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginLeft: '6px' }}>
              <Link
                to="/profile"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '8px 14px',
                  borderRadius: 'var(--radius-sm)',
                  textDecoration: 'none',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: isActive('/profile') ? 'var(--accent-blue)' : 'var(--text-secondary)',
                  background: isActive('/profile') ? 'rgba(47, 128, 237, 0.1)' : 'transparent',
                  transition: 'all 0.2s ease',
                }}
              >
                <UserIcon size={16} />
                <span>{user.displayName}</span>
              </Link>

              <button
                onClick={() => signOut()}
                className="btn-secondary"
                style={{ padding: '8px 12px', fontSize: '13px' }}
                title="Sign Out"
              >
                <LogOut size={15} color="var(--status-error)" />
              </button>
            </div>
          ) : (
            <Link
              to="/auth"
              className="btn-primary"
              style={{ padding: '8px 16px', fontSize: '13px', marginLeft: '6px' }}
            >
              <LogIn size={15} />
              <span>Sign Up / Sign In</span>
            </Link>
          )}
        </nav>
      </div>

      <style>{`
        @media (max-width: 640px) {
          .hide-on-mobile {
            display: none;
          }
        }
      `}</style>
    </header>
  );
};
