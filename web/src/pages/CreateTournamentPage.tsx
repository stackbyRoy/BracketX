import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Check, Shield, AlertCircle, Loader2 } from 'lucide-react';
import type { GameType, TournamentFormat, TournamentSettings } from '../engine/types';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/supabase';

export const CreateTournamentPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState(1);
  const [name, setName] = useState('');
  const [game, setGame] = useState<GameType>('fc_mobile');
  const [format, setFormat] = useState<TournamentFormat>('knockout');
  const [maxParticipants, setMaxParticipants] = useState<number>(32);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) {
    return (
      <div className="app-container" style={{ padding: '60px 16px', textAlign: 'center' }}>
        <div className="glass-card" style={{ maxWidth: '440px', margin: '0 auto', padding: '36px' }}>
          <Shield size={36} color="var(--accent-blue)" style={{ margin: '0 auto 12px auto' }} />
          <h2 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px' }}>Authentication Required</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '24px' }}>
            You need to be signed in to create and organize tournaments.
          </p>
          <Link to="/auth" className="btn-primary" style={{ width: '100%' }}>
            Sign Up or Sign In
          </Link>
        </div>
      </div>
    );
  }

  const handleCreate = async () => {
    if (!name.trim()) {
      setError('Please provide a tournament name.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const isFc = game === 'fc_mobile';
      const settings: TournamentSettings = {
        minMetric: isFc ? 60 : 1500,
        maxMetric: isFc ? 120 : 3500,
        groupCount: 2,
        qualifyCountPerGroup: 2,
        pointsForWin: 3,
        pointsForDraw: 1,
        pointsForLoss: 0,
      };

      const created = await api.createTournament({
        hostId: user.id,
        name: name.trim(),
        game,
        format,
        status: 'draft',
        maxParticipants: Math.min(32, Math.max(2, maxParticipants)),
        registrationOpen: false,
        drawLocked: false,
        settings,
      });

      navigate(`/tournament/${created.id}?created=true`);
    } catch (err: any) {
      setError(err.message || 'Failed to create tournament.');
      setLoading(false);
    }
  };

  return (
    <div className="app-container" style={{ padding: '32px 16px 80px 16px', maxWidth: '640px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '28px' }}>
        <button
          onClick={() => (step > 1 ? setStep(step - 1) : navigate(-1))}
          className="btn-secondary"
          style={{ padding: '8px 12px' }}
        >
          <ArrowLeft size={16} />
        </button>
        <div>
          <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--accent-blue)', letterSpacing: '0.05em' }}>
            STEP {step} OF 4
          </div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)' }}>
            Create Tournament
          </h1>
        </div>
      </div>

      {error && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '8px',
          background: 'rgba(235, 87, 87, 0.15)',
          border: '1px solid rgba(235, 87, 87, 0.3)',
          borderRadius: 'var(--radius-sm)',
          padding: '12px 16px',
          color: 'var(--status-error)',
          fontSize: '13px',
          marginBottom: '20px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
          {error.toLowerCase().includes('sign in') && (
            <Link to="/auth" style={{ color: '#FFFFFF', textDecoration: 'underline', fontWeight: 600, fontSize: '13px' }}>
              Sign In
            </Link>
          )}
        </div>
      )}

      <div className="glass-card" style={{ padding: '28px', borderRadius: 'var(--radius-lg)' }}>
        {/* Step 1: Tournament Name */}
        {step === 1 && (
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '6px' }}>Tournament Details</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Give your tournament a recognizable title for competitors.
            </p>

            <div>
              <label className="input-label">Tournament Name</label>
              <input
                type="text"
                className="input-field"
                value={name}
                onChange={(e) => { setName(e.target.value); setError(null); }}
                autoFocus
              />
            </div>
          </div>
        )}

        {/* Step 2: Game Selection */}
        {step === 2 && (
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '6px' }}>Select Game</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Determines the verified competitive rating scale and validation limits.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <SelectableCard
                title="EA SPORTS FC Mobile"
                subtitle="Competitive Metric: OVR (60–120)"
                selected={game === 'fc_mobile'}
                onClick={() => setGame('fc_mobile')}
              />
              <SelectableCard
                title="eFootball Mobile"
                subtitle="Competitive Metric: Team Strength (1500–3500)"
                selected={game === 'efootball'}
                onClick={() => setGame('efootball')}
              />
            </div>
          </div>
        )}

        {/* Step 3: Format & Player Capacity */}
        {step === 3 && (
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '6px' }}>Tournament Format & Capacity</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Choose competition structure and maximum participant capacity.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
              <SelectableCard
                title="Single Elimination"
                subtitle="Classic knockout bracket with canonical bye allocation"
                selected={format === 'knockout'}
                onClick={() => setFormat('knockout')}
              />
              <SelectableCard
                title="Round Robin / League Groups"
                subtitle="Balanced groups with round-robin matches and standings"
                selected={format === 'league'}
                onClick={() => setFormat('league')}
              />
              <SelectableCard
                title="Groups → Knockout"
                subtitle="Group stage followed by qualified single-elimination phase"
                selected={format === 'groups_knockout'}
                onClick={() => setFormat('groups_knockout')}
              />
            </div>

            <label className="input-label" style={{ marginBottom: '10px' }}>
              Player Capacity (Highest: 32 Players)
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px' }}>
              {[4, 8, 16, 32].map((cap) => {
                const isSelected = maxParticipants === cap;
                return (
                  <button
                    key={cap}
                    type="button"
                    onClick={() => setMaxParticipants(cap)}
                    style={{
                      padding: '14px 0',
                      border: isSelected ? '1px solid var(--accent-blue)' : '1px solid var(--border-subtle)',
                      background: isSelected ? 'var(--accent-blue)' : 'var(--bg-surface)',
                      borderRadius: 'var(--radius-sm)',
                      cursor: 'pointer',
                      color: isSelected ? '#FFFFFF' : 'var(--text-primary)',
                      fontWeight: 700,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '2px',
                    }}
                  >
                    <span style={{ fontSize: '16px' }}>{cap}</span>
                    {cap === 32 && (
                      <span style={{
                        fontSize: '9px',
                        letterSpacing: '0.05em',
                        color: isSelected ? 'rgba(255,255,255,0.9)' : 'var(--accent-cyan)',
                        fontWeight: 800,
                      }}>
                        MAX
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Step 4: Review & Confirm */}
        {step === 4 && (
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '6px' }}>Review & Confirm</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Verify tournament parameters before publishing draft.
            </p>

            <div style={{
              background: 'var(--bg-surface)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              padding: '18px',
              display: 'flex',
              flexDirection: 'column',
              gap: '14px',
            }}>
              <ReviewItem label="Tournament Name" value={name} />
              <ReviewItem label="Game" value={game === 'fc_mobile' ? 'EA SPORTS FC Mobile' : 'eFootball'} />
              <ReviewItem label="Metric" value={game === 'fc_mobile' ? 'OVR (60–120)' : 'Team Strength (1500–3500)'} />
              <ReviewItem label="Format" value={format.replace('_', ' ').toUpperCase()} />
              <ReviewItem label="Max Participants" value={`${maxParticipants} Players (Highest: 32)`} />
            </div>
          </div>
        )}

        {/* Controls */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '28px', gap: '12px' }}>
          {step > 1 ? (
            <button
              type="button"
              onClick={() => setStep(step - 1)}
              className="btn-secondary"
            >
              Back
            </button>
          ) : <div />}

          {step < 4 ? (
            <button
              type="button"
              onClick={() => {
                if (step === 1 && !name.trim()) {
                  setError('Tournament name is required.');
                  return;
                }
                setError(null);
                setStep(step + 1);
              }}
              className="btn-primary"
            >
              Continue
            </button>
          ) : (
            <button
              type="button"
              onClick={handleCreate}
              disabled={loading}
              className="btn-primary"
            >
              {loading ? <Loader2 size={16} className="animate-spin" /> : <Check size={16} />}
              <span>{loading ? 'Creating...' : 'Create Tournament'}</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

const SelectableCard: React.FC<{
  title: string;
  subtitle: string;
  selected: boolean;
  onClick: () => void;
}> = ({ title, subtitle, selected, onClick }) => {
  return (
    <div
      onClick={onClick}
      style={{
        padding: '16px',
        borderRadius: 'var(--radius-md)',
        background: selected ? 'rgba(47, 128, 237, 0.12)' : 'var(--bg-surface)',
        border: selected ? '1px solid var(--accent-blue)' : '1px solid var(--border-subtle)',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
      }}
    >
      <div style={{
        fontWeight: 700,
        fontSize: '15px',
        color: selected ? 'var(--accent-blue)' : 'var(--text-primary)',
        marginBottom: '4px',
      }}>
        {title}
      </div>
      <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
        {subtitle}
      </div>
    </div>
  );
};

const ReviewItem: React.FC<{ label: string; value: string }> = ({ label, value }) => {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{label}</span>
      <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>{value}</span>
    </div>
  );
};
