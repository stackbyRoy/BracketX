import React, { useState } from 'react';
import { X, Lock, RefreshCw, Zap } from 'lucide-react';
import confetti from 'canvas-confetti';
import type { FairnessScore } from '../engine/types';
import { api } from '../api/supabase';

interface DrawPreviewModalProps {
  tournamentId: string;
  fairnessScore: FairnessScore;
  isOpen: boolean;
  onClose: () => void;
  onRegenerate: () => void;
  onLocked: () => void;
}

export const DrawPreviewModal: React.FC<DrawPreviewModalProps> = ({
  tournamentId,
  fairnessScore,
  isOpen,
  onClose,
  onRegenerate,
  onLocked,
}) => {
  const [locking, setLocking] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  if (!isOpen) return null;

  const handleLock = async () => {
    setLocking(true);
    try {
      await api.lockDraw(tournamentId);
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
      });
      onLocked();
      onClose();
    } catch (err) {
      console.error('Failed to lock draw:', err);
    } finally {
      setLocking(false);
    }
  };

  const getScoreColor = (val: number) => {
    if (val >= 85) return 'var(--status-success)';
    if (val >= 70) return 'var(--status-warning)';
    return 'var(--status-error)';
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0, 0, 0, 0.8)',
      backdropFilter: 'blur(10px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '16px',
      zIndex: 1000,
    }}>
      <div className="glass-card animate-fade-in" style={{
        maxWidth: '520px',
        width: '100%',
        padding: '30px',
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

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <Zap size={22} color="var(--accent-blue)" />
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-primary)' }}>
            Fair Draw Preview & Verification
          </h2>
        </div>

        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '24px' }}>
          5-Component Algorithmic Fairness breakdown evaluated per specifications.
        </p>

        {/* Overall Fairness Badge */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'linear-gradient(135deg, rgba(47, 128, 237, 0.15) 0%, rgba(39, 174, 96, 0.15) 100%)',
          border: '1px solid var(--border-accent)',
          padding: '16px 20px',
          borderRadius: 'var(--radius-md)',
          marginBottom: '24px',
        }}>
          <div>
            <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Overall Fairness Score
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
              Mathematical balance & competitive diversity
            </div>
          </div>
          <div style={{
            fontSize: '32px',
            fontWeight: 900,
            fontFamily: 'var(--font-display)',
            color: getScoreColor(fairnessScore.totalScore),
          }}>
            {fairnessScore.totalScore.toFixed(1)}
            <span style={{ fontSize: '16px', color: 'var(--text-muted)' }}>/100</span>
          </div>
        </div>

        {/* 5 Component Bars */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '28px' }}>
          <ScoreBar
            title="Strength Distribution"
            weight="30%"
            score={fairnessScore.strengthDistribution}
            desc="Even division of participant skill across halves/quarters"
          />
          <ScoreBar
            title="Bracket Balance"
            weight="25%"
            score={fairnessScore.bracketBalance}
            desc="Spread between strongest and weakest sections"
          />
          <ScoreBar
            title="Competitive Diversity"
            weight="20%"
            score={fairnessScore.competitiveDiversity}
            desc="Avoids early round-1 same-band matchups"
          />
          <ScoreBar
            title="Opportunity Distribution"
            weight="15%"
            score={fairnessScore.opportunityDistribution}
            desc="Fair probability distribution across seeds"
          />
          <ScoreBar
            title="Controlled Randomness"
            weight="10%"
            score={fairnessScore.controlledRandomness}
            desc="Deterministic entropy to prevent predictable meta"
          />
        </div>

        {showConfirm ? (
          <div style={{
            background: 'rgba(235, 87, 87, 0.12)',
            border: '1px solid rgba(235, 87, 87, 0.3)',
            borderRadius: 'var(--radius-sm)',
            padding: '16px',
            marginBottom: '16px',
          }}>
            <h4 style={{ fontWeight: 700, color: 'var(--status-error)', marginBottom: '4px' }}>
              Confirm Draw Lock?
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '14px' }}>
              Once locked, the tournament moves to IN_PROGRESS. Pairings become official and cannot be regenerated.
            </p>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setShowConfirm(false)}
                className="btn-secondary"
                style={{ flex: 1, padding: '8px' }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleLock}
                disabled={locking}
                className="btn-primary"
                style={{ flex: 1.5, padding: '8px', background: 'var(--status-success)' }}
              >
                <Lock size={15} />
                <span>{locking ? 'Locking...' : 'Yes, Lock Draw'}</span>
              </button>
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              type="button"
              onClick={onRegenerate}
              className="btn-secondary"
              style={{ flex: 1 }}
            >
              <RefreshCw size={16} />
              <span>Regenerate Candidate</span>
            </button>
            <button
              type="button"
              onClick={() => setShowConfirm(true)}
              className="btn-primary"
              style={{ flex: 1.5 }}
            >
              <Lock size={16} />
              <span>Lock & Start Competition</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

const ScoreBar: React.FC<{
  title: string;
  weight: string;
  score: number;
  desc: string;
}> = ({ title, weight, score, desc }) => {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
        <div>
          <span style={{ fontWeight: 600, fontSize: '13px', color: 'var(--text-primary)' }}>{title}</span>
          <span style={{ fontSize: '11px', color: 'var(--accent-blue)', marginLeft: '6px', fontWeight: 700 }}>
            {weight}
          </span>
        </div>
        <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-primary)' }}>
          {score.toFixed(1)}%
        </span>
      </div>
      <div style={{
        height: '6px',
        background: 'rgba(255, 255, 255, 0.08)',
        borderRadius: '999px',
        overflow: 'hidden',
      }}>
        <div style={{
          width: `${Math.min(100, Math.max(0, score))}%`,
          height: '100%',
          background: 'linear-gradient(90deg, #2F80ED 0%, #27AE60 100%)',
          borderRadius: '999px',
          transition: 'width 0.4s ease',
        }} />
      </div>
      <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '3px' }}>
        {desc}
      </div>
    </div>
  );
};
