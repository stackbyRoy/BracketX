import React from 'react';
import type { Standing, Participant } from '../engine/types';

interface StandingsTableProps {
  standings: Standing[];
  participants: Participant[];
  currentUserId?: string;
}

export const StandingsTable: React.FC<StandingsTableProps> = ({
  standings,
  participants,
  currentUserId,
}) => {
  const getParticipant = (id: string) => participants.find((p) => p.id === id);

  return (
    <div style={{
      overflowX: 'auto',
      borderRadius: 'var(--radius-md)',
      border: '1px solid var(--border-subtle)',
      background: 'var(--bg-card)',
    }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
        <thead>
          <tr style={{
            background: 'var(--bg-elevated)',
            borderBottom: '1px solid var(--border-subtle)',
            color: 'var(--text-secondary)',
            fontSize: '11px',
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
          }}>
            <th style={{ padding: '12px 16px' }}>#</th>
            <th style={{ padding: '12px 16px' }}>Participant</th>
            <th style={{ padding: '12px 8px', textAlign: 'center' }}>P</th>
            <th style={{ padding: '12px 8px', textAlign: 'center' }}>W</th>
            <th style={{ padding: '12px 8px', textAlign: 'center' }}>D</th>
            <th style={{ padding: '12px 8px', textAlign: 'center' }}>L</th>
            <th style={{ padding: '12px 8px', textAlign: 'center' }}>GD</th>
            <th style={{ padding: '12px 16px', textAlign: 'center', color: 'var(--accent-blue)', fontWeight: 700 }}>PTS</th>
          </tr>
        </thead>
        <tbody>
          {standings.map((s, idx) => {
            const p = getParticipant(s.participantId);
            const isUser = p?.userId === currentUserId;

            return (
              <tr
                key={s.id || idx}
                style={{
                  borderBottom: '1px solid var(--border-subtle)',
                  background: isUser ? 'rgba(47, 128, 237, 0.1)' : 'transparent',
                  fontWeight: isUser ? 700 : 400,
                  color: isUser ? 'var(--accent-blue)' : 'var(--text-primary)',
                }}
              >
                <td style={{ padding: '12px 16px', fontWeight: 700, color: 'var(--text-muted)' }}>
                  {idx + 1}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <div style={{ fontWeight: 600 }}>{p?.name || 'Unknown'}</div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{p?.inGameId}</div>
                </td>
                <td style={{ padding: '12px 8px', textAlign: 'center' }}>{s.played}</td>
                <td style={{ padding: '12px 8px', textAlign: 'center' }}>{s.won}</td>
                <td style={{ padding: '12px 8px', textAlign: 'center' }}>{s.drawn}</td>
                <td style={{ padding: '12px 8px', textAlign: 'center' }}>{s.lost}</td>
                <td style={{ padding: '12px 8px', textAlign: 'center', color: s.goalDifference > 0 ? 'var(--status-success)' : s.goalDifference < 0 ? 'var(--status-error)' : 'inherit' }}>
                  {s.goalDifference > 0 ? `+${s.goalDifference}` : s.goalDifference}
                </td>
                <td style={{ padding: '12px 16px', textAlign: 'center', fontWeight: 800, fontSize: '14px', color: 'var(--accent-blue)' }}>
                  {s.points}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
