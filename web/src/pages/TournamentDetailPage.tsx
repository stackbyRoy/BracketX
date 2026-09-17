import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Play, CheckCircle2, Lock, Shuffle, Loader2 } from 'lucide-react';
import type { Tournament, Participant, Match, Group, Standing, FairnessScore } from '../engine/types';
import { api, supabase } from '../api/supabase';
import { useAuth } from '../context/AuthContext';
import { BracketView } from '../components/BracketView';
import { StandingsTable } from '../components/StandingsTable';
import { RegistrationModal } from '../components/RegistrationModal';
import { ScoreModal } from '../components/ScoreModal';
import { DrawPreviewModal } from '../components/DrawPreviewModal';
import { FairDrawEngine } from '../engine/FairDrawEngine';

export const TournamentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [standings, setStandings] = useState<Standing[]>([]);
  const [loading, setLoading] = useState(true);

  const [activeTab, setActiveTab] = useState<'bracket' | 'matches' | 'standings' | 'participants'>('bracket');

  // Modals state
  const [showRegModal, setShowRegModal] = useState(false);
  const [showDrawModal, setShowDrawModal] = useState(false);
  const [selectedMatch, setSelectedMatch] = useState<Match | null>(null);
  const [fairnessScore, setFairnessScore] = useState<FairnessScore>({
    totalScore: 90.0,
    strengthDistribution: 92.0,
    bracketBalance: 90.0,
    competitiveDiversity: 88.0,
    opportunityDistribution: 91.0,
    controlledRandomness: 89.0,
  });

  useEffect(() => {
    if (id) {
      loadData(id);

      // Realtime subscription for matches and participants
      const channel = supabase
        .channel(`tournament-${id}`)
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'matches', filter: `tournament_id=eq.${id}` },
          () => loadMatches(id)
        )
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'participants', filter: `tournament_id=eq.${id}` },
          () => loadParticipants(id)
        )
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'tournaments', filter: `id=eq.${id}` },
          () => loadTournament(id)
        )
        .subscribe();

      return () => {
        supabase.removeChannel(channel);
      };
    }
  }, [id]);

  const loadData = async (tId: string) => {
    setLoading(true);
    await Promise.all([
      loadTournament(tId),
      loadParticipants(tId),
      loadMatches(tId),
      loadGroupsAndStandings(tId),
    ]);
    setLoading(false);
  };

  const loadTournament = async (tId: string) => {
    const t = await api.getTournament(tId);
    setTournament(t);
  };

  const loadParticipants = async (tId: string) => {
    const list = await api.getParticipants(tId);
    setParticipants(list);
  };

  const loadMatches = async (tId: string) => {
    const list = await api.getMatches(tId);
    setMatches(list);
  };

  const loadGroupsAndStandings = async (tId: string) => {
    const grps = await api.getGroups(tId);
    setGroups(grps);
    if (grps.length > 0) {
      const stdList: Standing[] = [];
      for (const g of grps) {
        const s = await api.getStandings(g.id);
        stdList.push(...s);
      }
      setStandings(stdList);
    }
  };

  if (loading || !tournament) {
    return (
      <div style={{ padding: '80px 0', textAlign: 'center' }}>
        <Loader2 size={36} className="animate-spin" color="var(--accent-blue)" style={{ margin: '0 auto 12px auto' }} />
        <p style={{ color: 'var(--text-secondary)' }}>Loading competition details...</p>
      </div>
    );
  }

  const isHost = user?.id === tournament.hostId;
  const isRegistered = participants.some((p) => p.userId === user?.id);
  const isFull = participants.length >= tournament.maxParticipants;

  // Actions
  const handleOpenReg = async () => {
    await api.updateTournamentStatus(tournament.id, 'registration_open', true);
    await loadTournament(tournament.id);
  };

  const handleCloseReg = async () => {
    await api.updateTournamentStatus(tournament.id, 'registration_closed', false);
    await loadTournament(tournament.id);
  };

  const handleGenerateDraw = async () => {
    if (tournament.format === 'knockout') {
      const structure = FairDrawEngine.generateKnockoutDraw(tournament.id, participants, 42);
      setFairnessScore(structure.fairnessScore);
      await api.saveGeneratedDraw(tournament.id, structure);
      await loadData(tournament.id);
      setShowDrawModal(true);
    } else {
      const structure = FairDrawEngine.generateGroupDraw(tournament.id, participants, 2);
      setFairnessScore(structure.fairnessScore);
      await api.saveGeneratedDraw(tournament.id, structure);
      await loadData(tournament.id);
      setShowDrawModal(true);
    }
  };

  const handleRegenerate = async () => {
    const randomSeed = Math.floor(Math.random() * 100000);
    const structure = FairDrawEngine.generateKnockoutDraw(tournament.id, participants, randomSeed);
    setFairnessScore(structure.fairnessScore);
    await api.saveGeneratedDraw(tournament.id, structure);
    await loadData(tournament.id);
  };

  return (
    <div className="app-container" style={{ padding: '24px 16px 80px 16px' }}>
      {/* Back button */}
      <div style={{ marginBottom: '16px' }}>
        <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '13px', fontWeight: 600 }}>
          <ArrowLeft size={16} />
          <span>All Tournaments</span>
        </Link>
      </div>

      {/* Header Summary Card */}
      <div className="glass-card" style={{ padding: '24px', borderRadius: 'var(--radius-lg)', marginBottom: '24px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px', marginBottom: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <span className={`badge ${tournament.game === 'fc_mobile' ? 'badge-fc' : 'badge-efootball'}`}>
                {tournament.game === 'fc_mobile' ? 'EA SPORTS FC Mobile' : 'eFootball'}
              </span>
              <span className="badge badge-open" style={{ textTransform: 'capitalize' }}>
                {tournament.status.replace('_', ' ')}
              </span>
            </div>

            <h1 style={{ fontSize: '28px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '6px' }}>
              {tournament.name}
            </h1>

            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '13px', color: 'var(--text-secondary)' }}>
              <span>Format: {tournament.format.replace('_', ' ').toUpperCase()}</span>
              <span>•</span>
              <span style={{ color: isFull ? 'var(--status-warning)' : 'inherit', fontWeight: isFull ? 700 : 400 }}>
                {participants.length} / {tournament.maxParticipants} Registered Players
              </span>
            </div>
          </div>

          {/* Action Buttons */}
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {isHost ? (
              <>
                {tournament.status === 'draft' && (
                  <button onClick={handleOpenReg} className="btn-primary">
                    <Play size={16} />
                    <span>Open Registration</span>
                  </button>
                )}

                {tournament.status === 'registration_open' && (
                  <button onClick={handleCloseReg} className="btn-secondary">
                    <span>Close Registration</span>
                  </button>
                )}

                {tournament.status === 'registration_closed' && (
                  <button onClick={handleGenerateDraw} className="btn-primary">
                    <Shuffle size={16} />
                    <span>Generate Fair Draw</span>
                  </button>
                )}

                {tournament.status === 'draw_generated' && (
                  <button onClick={() => setShowDrawModal(true)} className="btn-primary">
                    <Lock size={16} />
                    <span>Preview / Lock Draw</span>
                  </button>
                )}

                {(tournament.status === 'in_progress' || tournament.status === 'draw_locked') && (
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    color: 'var(--status-success)',
                    fontSize: '13px',
                    fontWeight: 600,
                  }}>
                    <CheckCircle2 size={16} />
                    <span>Competition Active (Click match to submit score)</span>
                  </div>
                )}
              </>
            ) : (
              tournament.registrationOpen && !isRegistered ? (
                <button
                  onClick={() => setShowRegModal(true)}
                  disabled={isFull}
                  className="btn-primary"
                >
                  {isFull ? `Registration Full (Max ${tournament.maxParticipants})` : 'Join Tournament'}
                </button>
              ) : isRegistered ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--status-success)', fontSize: '13px', fontWeight: 600 }}>
                  <CheckCircle2 size={16} />
                  <span>You are registered for this event</span>
                </div>
              ) : null
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{
        display: 'flex',
        gap: '8px',
        borderBottom: '1px solid var(--border-subtle)',
        marginBottom: '24px',
        overflowX: 'auto',
      }}>
        {tournament.format !== 'league' && (
          <TabButton
            active={activeTab === 'bracket'}
            onClick={() => setActiveTab('bracket')}
            label="Bracket Tree"
          />
        )}
        {tournament.format !== 'knockout' && (
          <TabButton
            active={activeTab === 'standings'}
            onClick={() => setActiveTab('standings')}
            label="Group Standings"
          />
        )}
        <TabButton
          active={activeTab === 'matches'}
          onClick={() => setActiveTab('matches')}
          label={`Matches (${matches.length})`}
        />
        <TabButton
          active={activeTab === 'participants'}
          onClick={() => setActiveTab('participants')}
          label={`Players (${participants.length})`}
        />
      </div>

      {/* Tab Content */}
      {activeTab === 'bracket' && (
        <BracketView
          matches={matches}
          participants={participants}
          isHost={isHost}
          onMatchClick={(m) => setSelectedMatch(m)}
        />
      )}

      {activeTab === 'standings' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {groups.length === 0 ? (
            <div className="glass-card" style={{ padding: '36px', textAlign: 'center' }}>
              <p style={{ color: 'var(--text-secondary)' }}>No groups formed yet.</p>
            </div>
          ) : (
            groups.map((grp) => (
              <div key={grp.id}>
                <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '12px' }}>
                  {grp.name}
                </h3>
                <StandingsTable
                  standings={standings.filter((s) => s.groupId === grp.id)}
                  participants={participants}
                  currentUserId={user?.id}
                />
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === 'matches' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {matches.length === 0 ? (
            <div className="glass-card" style={{ padding: '36px', textAlign: 'center' }}>
              <p style={{ color: 'var(--text-secondary)' }}>Matches will be generated when the draw is created.</p>
            </div>
          ) : (
            matches.map((m) => {
              const pA = participants.find((p) => p.id === m.participantAId);
              const pB = participants.find((p) => p.id === m.participantBId);
              return (
                <div
                  key={m.id}
                  onClick={() => isHost && setSelectedMatch(m)}
                  className="glass-card"
                  style={{
                    padding: '14px 20px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: isHost ? 'pointer' : 'default',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>
                      R{m.roundNumber}·M{m.matchNumber}
                    </span>
                    <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {pA?.name || 'TBD'} vs {pB?.name || (m.isBye ? 'BYE' : 'TBD')}
                    </span>
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '15px' }}>
                    {m.scoreA !== null ? `${m.scoreA} - ${m.scoreB}` : 'vs'}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {activeTab === 'participants' && (
        <div className="glass-card" style={{ padding: '20px', borderRadius: 'var(--radius-md)' }}>
          {participants.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-secondary)' }}>
              No participants registered yet.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {participants.map((p, idx) => (
                <div
                  key={p.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    borderRadius: 'var(--radius-sm)',
                    background: 'var(--bg-surface)',
                    border: '1px solid var(--border-subtle)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ fontWeight: 700, fontSize: '13px', color: 'var(--text-muted)', width: '24px' }}>
                      #{idx + 1}
                    </span>
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{p.name}</div>
                      <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>IGN: {p.inGameId}</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    {p.competitiveBand && (
                      <span className="badge badge-fc" style={{ fontSize: '10px' }}>
                        Band {p.competitiveBand}
                      </span>
                    )}
                    <span style={{ fontWeight: 700, fontSize: '14px', color: 'var(--accent-blue)' }}>
                      {p.gameMetricValue} {p.gameMetricType}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      <RegistrationModal
        tournament={tournament}
        isOpen={showRegModal}
        onClose={() => setShowRegModal(false)}
        onSuccess={() => loadData(tournament.id)}
        currentParticipantCount={participants.length}
      />

      {selectedMatch && (
        <ScoreModal
          match={selectedMatch}
          participants={participants}
          isOpen={!!selectedMatch}
          onClose={() => setSelectedMatch(null)}
          onSuccess={() => loadData(tournament.id)}
        />
      )}

      <DrawPreviewModal
        tournamentId={tournament.id}
        fairnessScore={fairnessScore}
        isOpen={showDrawModal}
        onClose={() => setShowDrawModal(false)}
        onRegenerate={handleRegenerate}
        onLocked={() => loadData(tournament.id)}
      />
    </div>
  );
};

const TabButton: React.FC<{ active: boolean; onClick: () => void; label: string }> = ({
  active,
  onClick,
  label,
}) => {
  return (
    <button
      onClick={onClick}
      style={{
        padding: '12px 16px',
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        fontSize: '14px',
        fontWeight: active ? 700 : 500,
        color: active ? 'var(--accent-blue)' : 'var(--text-secondary)',
        borderBottom: active ? '2px solid var(--accent-blue)' : '2px solid transparent',
        transition: 'all 0.15s ease',
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </button>
  );
};
