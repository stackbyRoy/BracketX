import React, { useState } from 'react';
import { X, ShieldAlert } from 'lucide-react';
import type { Tournament } from '../engine/types';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/supabase';
import { MetricNormalizer } from '../engine/MetricNormalizer';

interface RegistrationModalProps {
  tournament: Tournament;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  currentParticipantCount: number;
}

export const RegistrationModal: React.FC<RegistrationModalProps> = ({
  tournament,
  isOpen,
  onClose,
  onSuccess,
  currentParticipantCount,
}) => {
  const { user } = useAuth();

  const isFcMobile = tournament.game === 'fc_mobile';
  const metricLabel = isFcMobile ? 'OVR' : 'Team Strength';
  const minMetric = tournament.settings.minMetric ?? (isFcMobile ? 60 : 1500);
  const maxMetric = tournament.settings.maxMetric ?? (isFcMobile ? 120 : 3500);

  const [name, setName] = useState(user?.displayName || '');
  const [inGameId, setInGameId] = useState('');
  const [metricValue, setMetricValue] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const isFull = currentParticipantCount >= tournament.maxParticipants;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      setError('You must be signed in to join.');
      return;
    }

    if (isFull) {
      setError(`Tournament is at maximum capacity (${tournament.maxParticipants} players).`);
      return;
    }

    const val = parseFloat(metricValue);
    if (isNaN(val) || val < minMetric || val > maxMetric) {
      setError(`${metricLabel} must be between ${minMetric} and ${maxMetric}.`);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const normalizedScore = MetricNormalizer.normalize(tournament.game, val, minMetric, maxMetric);

      await api.registerParticipant({
        tournamentId: tournament.id,
        userId: user.id,
        name: name.trim(),
        inGameId: inGameId.trim(),
        gameMetricType: metricLabel,
        gameMetricValue: val,
        competitiveScore: normalizedScore,
      });

      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to register. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '16px',
      zIndex: 1000,
    }}>
      <div className="glass-card animate-fade-in" style={{
        maxWidth: '460px',
        width: '100%',
        padding: '28px',
        position: 'relative',
        borderRadius: 'var(--radius-lg)',
      }}>
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            right: '20px',
            top: '20px',
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>

        <h2 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '6px', color: 'var(--text-primary)' }}>
          Join {tournament.name}
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
          Enter your in-game identity and verified competitive metric.
        </p>

        {isFull ? (
          <div style={{
            background: 'rgba(235, 87, 87, 0.15)',
            border: '1px solid rgba(235, 87, 87, 0.3)',
            borderRadius: 'var(--radius-sm)',
            padding: '16px',
            textAlign: 'center',
            color: 'var(--status-error)',
          }}>
            <ShieldAlert size={28} style={{ margin: '0 auto 8px auto' }} />
            <h4 style={{ fontWeight: 700, marginBottom: '4px' }}>Registration Full</h4>
            <p style={{ fontSize: '13px' }}>
              This tournament has reached its maximum capacity of {tournament.maxParticipants} players.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {error && (
              <div style={{
                background: 'rgba(235, 87, 87, 0.15)',
                border: '1px solid rgba(235, 87, 87, 0.3)',
                borderRadius: 'var(--radius-sm)',
                padding: '10px 14px',
                color: 'var(--status-error)',
                fontSize: '13px',
              }}>
                {error}
              </div>
            )}

            <div>
              <label className="input-label">Display Name</label>
              <input
                type="text"
                className="input-field"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="input-label">In-Game ID (IGN)</label>
              <input
                type="text"
                className="input-field"
                value={inGameId}
                onChange={(e) => setInGameId(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="input-label">{metricLabel} ({minMetric} – {maxMetric})</label>
              <input
                type="number"
                step="any"
                min={minMetric}
                max={maxMetric}
                className="input-field"
                value={metricValue}
                onChange={(e) => setMetricValue(e.target.value)}
                required
              />
              <span style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                Used for fair draw seed derivation and strength distribution.
              </span>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginTop: '12px' }}>
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary"
                style={{ flex: 1 }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={loading}
                style={{ flex: 1.5 }}
              >
                {loading ? 'Submitting...' : 'Confirm Registration'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
