import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Trophy, Users, Gamepad2, ArrowRight, Loader2 } from 'lucide-react';
import type { Tournament } from '../engine/types';
import { api } from '../api/supabase';
import { useAuth } from '../context/AuthContext';

export const HomePage: React.FC = () => {
  const { user } = useAuth();
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTournaments();
  }, []);

  const loadTournaments = async () => {
    setLoading(true);
    try {
      const list = await api.getTournaments();
      setTournaments(list);
    } catch (err) {
      console.error('Error fetching tournaments:', err);
    } finally {
      setLoading(false);
    }
  };

  const myTournaments = tournaments.filter((t) => user && t.hostId === user.id);
  const discoverTournaments = tournaments.filter((t) => !user || t.hostId !== user.id);

  return (
    <div className="app-container" style={{ padding: '32px 16px 64px 16px' }}>
      {/* Hero Welcome */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '20px',
        marginBottom: '36px',
      }}>
        <div>
          <div style={{ fontSize: '14px', color: 'var(--text-secondary)', fontWeight: 500 }}>
            {user ? 'Welcome back,' : 'Tournament Platform'}
          </div>
          <h1 style={{ fontSize: '32px', fontWeight: 800, color: 'var(--text-primary)', marginTop: '4px' }}>
            {user ? user.displayName : 'Explore Competitions'}
          </h1>
        </div>

        <Link to="/create" className="btn-primary" style={{ padding: '12px 22px', fontSize: '15px' }}>
          <Plus size={18} />
          <span>Create Tournament</span>
        </Link>
      </div>

      {loading ? (
        <div style={{ padding: '60px 0', textAlign: 'center' }}>
          <Loader2 size={36} className="animate-spin" color="var(--accent-blue)" style={{ margin: '0 auto 12px auto' }} />
          <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>Loading competitions...</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '40px' }}>
          {/* Section 1: My Tournaments */}
          <section>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Trophy size={18} color="var(--accent-blue)" />
              <h2 style={{ fontSize: '18px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>
                My Tournaments
              </h2>
            </div>

            {myTournaments.length === 0 ? (
              <div className="glass-card" style={{ padding: '36px', textAlign: 'center' }}>
                <Trophy size={36} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
                <h3 style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '6px' }}>
                  You haven't organized any tournaments yet
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px', marginBottom: '20px' }}>
                  Set up a competition with automated fair draws and instant brackets.
                </p>
                <Link to="/create" className="btn-secondary">
                  <Plus size={16} />
                  <span>Create Your First Tournament</span>
                </Link>
              </div>
            ) : (
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '16px',
              }}>
                {myTournaments.map((t) => (
                  <TournamentCard key={t.id} tournament={t} />
                ))}
              </div>
            )}
          </section>

          {/* Section 2: Discover Tournaments */}
          <section>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Gamepad2 size={18} color="var(--accent-cyan)" />
              <h2 style={{ fontSize: '18px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)' }}>
                Discover Tournaments
              </h2>
            </div>

            {discoverTournaments.length === 0 ? (
              <div className="glass-card" style={{ padding: '36px', textAlign: 'center' }}>
                <Users size={36} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
                <h3 style={{ color: 'var(--text-primary)', fontSize: '16px', marginBottom: '6px' }}>
                  No Open Competitions Found
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                  Check back later or host one yourself to invite campus competitors.
                </p>
              </div>
            ) : (
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '16px',
              }}>
                {discoverTournaments.map((t) => (
                  <TournamentCard key={t.id} tournament={t} />
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
};

const TournamentCard: React.FC<{ tournament: Tournament }> = ({ tournament }) => {
  const isFc = tournament.game === 'fc_mobile';

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'registration_open':
        return <span className="badge badge-open">Registration Open</span>;
      case 'in_progress':
        return <span className="badge badge-in-progress">In Progress</span>;
      case 'completed':
        return <span className="badge badge-completed">Completed</span>;
      default:
        return <span className="badge badge-draft">Draft</span>;
    }
  };

  return (
    <Link
      to={`/tournament/${tournament.id}`}
      className="glass-card glass-card-interactive"
      style={{
        padding: '20px',
        textDecoration: 'none',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
          <span className={`badge ${isFc ? 'badge-fc' : 'badge-efootball'}`}>
            {isFc ? 'EA SPORTS FC Mobile' : 'eFootball'}
          </span>
          {getStatusBadge(tournament.status)}
        </div>

        <h3 style={{
          fontSize: '18px',
          fontWeight: 700,
          color: 'var(--text-primary)',
          marginBottom: '8px',
          lineHeight: 1.3,
        }}>
          {tournament.name}
        </h3>

        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '12px', color: 'var(--text-secondary)' }}>
          <span style={{ textTransform: 'capitalize' }}>
            {tournament.format.replace('_', ' ')}
          </span>
          <span>•</span>
          <span>Max {tournament.maxParticipants} players</span>
        </div>
      </div>

      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: '6px',
        marginTop: '20px',
        color: 'var(--accent-blue)',
        fontSize: '13px',
        fontWeight: 600,
      }}>
        <span>View Details</span>
        <ArrowRight size={14} />
      </div>
    </Link>
  );
};
