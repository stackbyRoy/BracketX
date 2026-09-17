import React, { useState } from 'react';
import { X, Check } from 'lucide-react';
import type { Match, Participant } from '../engine/types';
import { api } from '../api/supabase';

interface ScoreModalProps {
  match: Match;
  participants: Participant[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const ScoreModal: React.FC<ScoreModalProps> = ({
  match,
  participants,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const pA = participants.find((p) => p.id === match.participantAId);
  const pB = participants.find((p) => p.id === match.participantBId);

  const [scoreA, setScoreA] = useState<number | string>(match.scoreA ?? '');
  const [scoreB, setScoreB] = useState<number | string>(match.scoreB ?? '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const numA = Number(scoreA);
    const numB = Number(scoreB);

    if (isNaN(numA) || isNaN(numB) || numA < 0 || numB < 0) {
      setError('Scores must be valid non-negative numbers.');
      return;
    }

    if (match.stage !== 'group' && numA === numB) {
      setError('Ties are not allowed in knockout matches.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await api.submitMatchResult(match.id, numA, numB);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to submit score.');
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
        maxWidth: '420px',
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
          Enter Match Result
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
          Round {match.roundNumber} · Match #{match.matchNumber}
        </p>

        {error && (
          <div style={{
            background: 'rgba(235, 87, 87, 0.15)',
            border: '1px solid rgba(235, 87, 87, 0.3)',
            borderRadius: 'var(--radius-sm)',
            padding: '10px 14px',
            color: 'var(--status-error)',
            fontSize: '13px',
            marginBottom: '16px',
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Competitor A */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'var(--bg-surface)',
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--border-subtle)',
          }}>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '15px' }}>
                {pA?.name || 'TBD'}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                {pA?.inGameId || 'Slot A'}
              </div>
            </div>
            <input
              type="number"
              min="0"
              className="input-field"
              style={{ width: '70px', textAlign: 'center', fontSize: '18px', fontWeight: 700 }}
              value={scoreA}
              onChange={(e) => setScoreA(e.target.value)}
              required
              autoFocus
            />
          </div>

          <div style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '12px', fontWeight: 800, letterSpacing: '0.1em' }}>
            VS
          </div>

          {/* Competitor B */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'var(--bg-surface)',
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--border-subtle)',
          }}>
            <div>
              <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '15px' }}>
                {pB?.name || 'TBD'}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                {pB?.inGameId || 'Slot B'}
              </div>
            </div>
            <input
              type="number"
              min="0"
              className="input-field"
              style={{ width: '70px', textAlign: 'center', fontSize: '18px', fontWeight: 700 }}
              value={scoreB}
              onChange={(e) => setScoreB(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
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
              disabled={loading || !match.participantAId || !match.participantBId}
              style={{ flex: 1.5 }}
            >
              <Check size={16} />
              <span>{loading ? 'Submitting...' : 'Commit Score'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
