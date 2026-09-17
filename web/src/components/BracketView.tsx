import React from 'react';
import { Trophy, CheckCircle, Clock } from 'lucide-react';
import type { Match, Participant } from '../engine/types';

interface BracketViewProps {
  matches: Match[];
  participants: Participant[];
  isHost: boolean;
  onMatchClick: (match: Match) => void;
}

export const BracketView: React.FC<BracketViewProps> = ({
  matches,
  participants,
  isHost,
  onMatchClick,
}) => {
  if (matches.length === 0) {
    return (
      <div className="glass-card" style={{ padding: '48px', textAlign: 'center' }}>
        <Trophy size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
        <h3 style={{ color: 'var(--text-primary)', marginBottom: '6px' }}>Bracket Not Generated</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
          The tournament bracket will appear here once the host generates and verifies the fair draw.
        </p>
      </div>
    );
  }

  // Group matches by round number
  const roundMap = new Map<number, Match[]>();
  for (const m of matches) {
    if (!roundMap.has(m.roundNumber)) {
      roundMap.set(m.roundNumber, []);
    }
    roundMap.get(m.roundNumber)!.push(m);
  }

  const sortedRounds = Array.from(roundMap.keys()).sort((a, b) => a - b);
  const totalRounds = sortedRounds.length;

  const getRoundTitle = (round: number) => {
    if (round === totalRounds) return 'Final';
    if (round === totalRounds - 1) return 'Semifinals';
    if (round === totalRounds - 2) return 'Quarterfinals';
    if (round === totalRounds - 3) return 'Round of 16';
    return `Round ${round}`;
  };

  const getParticipant = (id: string | null | undefined) => {
    if (!id) return null;
    return participants.find((p) => p.id === id);
  };

  return (
    <div style={{
      width: '100%',
      overflowX: 'auto',
      WebkitOverflowScrolling: 'touch',
      padding: '16px 8px 32px 8px',
    }}>
      <div style={{
        display: 'inline-flex',
        gap: '40px',
        minWidth: '100%',
        alignItems: 'stretch',
      }}>
        {sortedRounds.map((round) => {
          const roundMatches = roundMap.get(round) || [];

          return (
            <div
              key={round}
              style={{
                display: 'flex',
                flexDirection: 'column',
                width: '280px',
                flexShrink: 0,
              }}
            >
              {/* Round Header */}
              <div style={{
                textAlign: 'center',
                padding: '10px 0',
                marginBottom: '20px',
                background: 'var(--bg-elevated)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border-subtle)',
              }}>
                <span style={{
                  fontSize: '13px',
                  fontWeight: 700,
                  color: round === totalRounds ? 'var(--accent-gold)' : 'var(--text-primary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.08em',
                }}>
                  {getRoundTitle(round)}
                </span>
              </div>

              {/* Match list distributed evenly */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-around',
                flex: 1,
                gap: '24px',
              }}>
                {roundMatches.map((m) => {
                  const pA = getParticipant(m.participantAId);
                  const pB = getParticipant(m.participantBId);
                  const isCompleted = m.status === 'completed';
                  const isReady = !!m.participantAId && !!m.participantBId && !m.isBye;

                  const canClick = isHost && (isReady || isCompleted);

                  return (
                    <div
                      key={m.id}
                      onClick={() => canClick && onMatchClick(m)}
                      className="glass-card"
                      style={{
                        padding: '12px',
                        cursor: canClick ? 'pointer' : 'default',
                        border: isCompleted
                          ? '1px solid rgba(39, 174, 96, 0.4)'
                          : canClick
                          ? '1px solid rgba(47, 128, 237, 0.3)'
                          : '1px solid var(--border-subtle)',
                        boxShadow: canClick ? '0 4px 14px -3px rgba(0,0,0,0.5)' : 'none',
                        position: 'relative',
                        transition: 'transform 0.15s ease, border-color 0.15s ease',
                      }}
                      onMouseEnter={(e) => {
                        if (canClick) {
                          e.currentTarget.style.borderColor = 'var(--accent-blue)';
                          e.currentTarget.style.transform = 'scale(1.02)';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (canClick) {
                          e.currentTarget.style.borderColor = isCompleted
                            ? 'rgba(39, 174, 96, 0.4)'
                            : 'rgba(47, 128, 237, 0.3)';
                          e.currentTarget.style.transform = 'scale(1)';
                        }
                      }}
                    >
                      {/* Match header badge */}
                      <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: '8px',
                        fontSize: '11px',
                        color: 'var(--text-muted)',
                      }}>
                        <span>Match #{m.matchNumber}</span>
                        {m.isBye ? (
                          <span style={{ color: 'var(--status-warning)', fontWeight: 600 }}>BYE</span>
                        ) : isCompleted ? (
                          <span style={{ color: 'var(--status-success)', display: 'flex', alignItems: 'center', gap: '3px' }}>
                            <CheckCircle size={12} /> Done
                          </span>
                        ) : isReady ? (
                          <span style={{ color: 'var(--accent-blue)', display: 'flex', alignItems: 'center', gap: '3px' }}>
                            <Clock size={12} /> Ready
                          </span>
                        ) : (
                          <span>Waiting</span>
                        )}
                      </div>

                      {/* Participant A */}
                      <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '6px 10px',
                        borderRadius: '6px',
                        background: m.winnerId === pA?.id && isCompleted
                          ? 'rgba(39, 174, 96, 0.15)'
                          : 'rgba(255, 255, 255, 0.03)',
                        marginBottom: '4px',
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', overflow: 'hidden' }}>
                          {pA?.seed && (
                            <span style={{
                              fontSize: '10px',
                              fontWeight: 800,
                              color: 'var(--text-muted)',
                              width: '16px',
                            }}>
                              #{pA.seed}
                            </span>
                          )}
                          <span style={{
                            fontSize: '13px',
                            fontWeight: m.winnerId === pA?.id ? 700 : 500,
                            color: pA ? 'var(--text-primary)' : 'var(--text-muted)',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}>
                            {pA?.name || 'TBD'}
                          </span>
                        </div>
                        <span style={{
                          fontWeight: 700,
                          fontSize: '14px',
                          color: m.winnerId === pA?.id ? 'var(--status-success)' : 'var(--text-primary)',
                        }}>
                          {m.scoreA !== null ? m.scoreA : '-'}
                        </span>
                      </div>

                      {/* Participant B */}
                      <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '6px 10px',
                        borderRadius: '6px',
                        background: m.winnerId === pB?.id && isCompleted
                          ? 'rgba(39, 174, 96, 0.15)'
                          : 'rgba(255, 255, 255, 0.03)',
                      }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', overflow: 'hidden' }}>
                          {pB?.seed && (
                            <span style={{
                              fontSize: '10px',
                              fontWeight: 800,
                              color: 'var(--text-muted)',
                              width: '16px',
                            }}>
                              #{pB.seed}
                            </span>
                          )}
                          <span style={{
                            fontSize: '13px',
                            fontWeight: m.winnerId === pB?.id ? 700 : 500,
                            color: pB ? 'var(--text-primary)' : 'var(--text-muted)',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}>
                            {pB?.name || (m.isBye ? 'BYE' : 'TBD')}
                          </span>
                        </div>
                        <span style={{
                          fontWeight: 700,
                          fontSize: '14px',
                          color: m.winnerId === pB?.id ? 'var(--status-success)' : 'var(--text-primary)',
                        }}>
                          {m.scoreB !== null ? m.scoreB : '-'}
                        </span>
                      </div>

                      {/* Tap to enter score hint for host */}
                      {canClick && !isCompleted && (
                        <div style={{
                          fontSize: '10px',
                          color: 'var(--accent-blue)',
                          textAlign: 'center',
                          marginTop: '6px',
                          fontWeight: 600,
                        }}>
                          Tap to enter score
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
