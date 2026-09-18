import React, { useEffect, useState } from 'react';
import { useParams, Link, useSearchParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, CheckCircle2, Lock, Shuffle, Loader2, Share2, Check, Trash2, Edit3, Plus, X } from 'lucide-react';
import type { Tournament, Participant, Match, Group, Standing, FairnessScore } from '../engine/types';
import { api, supabase } from '../api/supabase';
import { useAuth } from '../context/AuthContext';
import { BracketView } from '../components/BracketView';
import { StandingsTable } from '../components/StandingsTable';
import { RegistrationModal } from '../components/RegistrationModal';
import { ScoreModal } from '../components/ScoreModal';
import { DrawPreviewModal } from '../components/DrawPreviewModal';
import { FairDrawEngine } from '../engine/FairDrawEngine';
import { shareTournament } from '../utils/share';

export const TournamentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [standings, setStandings] = useState<Standing[]>([]);
  const [loading, setLoading] = useState(true);

  const [activeTab, setActiveTab] = useState<'bracket' | 'matches' | 'standings' | 'participants' | 'rules'>('bracket');

  // Modals state
  const [showRegModal, setShowRegModal] = useState(false);
  const [showDrawModal, setShowDrawModal] = useState(false);
  const [showRulesModal, setShowRulesModal] = useState(false);
  const [rulesList, setRulesList] = useState<string[]>([]);
  const [newRuleText, setNewRuleText] = useState('');
  const [savingRules, setSavingRules] = useState(false);
  const [selectedMatch, setSelectedMatch] = useState<Match | null>(null);
  const [fairnessScore, setFairnessScore] = useState<FairnessScore>({
    totalScore: 90.0,
    strengthDistribution: 92.0,
    bracketBalance: 90.0,
    competitiveDiversity: 88.0,
    opportunityDistribution: 91.0,
    controlledRandomness: 89.0,
  });

  const [searchParams] = useSearchParams();
  const justCreated = searchParams.get('created') === 'true';
  const [shareFeedback, setShareFeedback] = useState<string | null>(null);
  const isAndroid = typeof navigator !== 'undefined' && /Android/i.test(navigator.userAgent);

  const handleShare = async () => {
    if (!tournament) return;
    const result = await shareTournament(tournament);
    if (result.copied) {
      setShareFeedback('Link Copied!');
      setTimeout(() => setShareFeedback(null), 3000);
    } else if (result.shared) {
      setShareFeedback('Shared!');
      setTimeout(() => setShareFeedback(null), 3000);
    }
  };

  const handleDeleteTournament = async () => {
    if (!tournament) return;
    if (!window.confirm(`Are you sure you want to delete "${tournament.name}"? This action cannot be undone. All matches, brackets, participants, and standings will be permanently deleted.`)) {
      return;
    }
    try {
      await api.deleteTournament(tournament.id, user?.id);
      navigate('/');
    } catch (err: any) {
      alert(err.message || 'Failed to delete tournament');
    }
  };

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
      {/* Top row: Back button & Android App Link prompt */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
        <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '13px', fontWeight: 600 }}>
          <ArrowLeft size={16} />
          <span>All Tournaments</span>
        </Link>

        {isAndroid && (
          <a
            href={`intent://bracketx.vercel.app/tournament/${tournament.id}#Intent;scheme=https;package=com.stackbyroy.bracketx;end`}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(47, 128, 237, 0.15)',
              border: '1px solid rgba(47, 128, 237, 0.3)',
              color: 'var(--accent-blue)',
              textDecoration: 'none',
              fontSize: '12px',
              fontWeight: 600,
            }}
          >
            <span>📱 Open in Android App</span>
          </a>
        )}
      </div>

      {/* Share / Invite Callout Banner */}
      {(tournament.registrationOpen || justCreated) && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '14px 18px',
          background: 'linear-gradient(90deg, rgba(47, 128, 237, 0.12) 0%, rgba(39, 174, 96, 0.08) 100%)',
          border: '1px solid rgba(47, 128, 237, 0.3)',
          borderRadius: 'var(--radius-lg)',
          marginBottom: '20px',
          flexWrap: 'wrap',
          gap: '12px',
        }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: '14px', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>{justCreated ? '🎉 Tournament Created Successfully!' : '📢 Player Registration is Open!'}</span>
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
              Share this link with players. On iOS and Desktop it opens the web app, and on Android it opens directly in the BracketX app!
            </div>
          </div>
          <button
            onClick={handleShare}
            className="btn-primary"
            style={{ padding: '8px 16px', fontSize: '13px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
          >
            {shareFeedback === 'Link Copied!' ? <Check size={15} /> : <Share2 size={15} />}
            <span>{shareFeedback || 'Share Tournament'}</span>
          </button>
        </div>
      )}

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
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Universal Share Button */}
            <button
              onClick={handleShare}
              className="btn-secondary"
              title="Share Tournament Link"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              {shareFeedback === 'Link Copied!' ? <Check size={16} /> : <Share2 size={16} />}
              <span>{shareFeedback || 'Share'}</span>
            </button>

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

                <button
                  onClick={handleDeleteTournament}
                  className="btn-secondary"
                  style={{
                    color: 'var(--status-danger)',
                    borderColor: 'rgba(235, 87, 87, 0.4)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                  title="Delete Tournament"
                >
                  <Trash2 size={16} />
                  <span>Delete Tournament</span>
                </button>
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
        <TabButton
          active={activeTab === 'rules'}
          onClick={() => setActiveTab('rules')}
          label={`Rules (${tournament.rules?.length || 0})`}
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

      {activeTab === 'rules' && (
        <div className="card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
            <div>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>
                Tournament Rules
              </h3>
              <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>
                Official guidelines and fair play regulations for all participants.
              </p>
            </div>
            {isHost && (
              <button
                onClick={() => {
                  setRulesList(tournament.rules || []);
                  setShowRulesModal(true);
                }}
                className="btn-secondary"
                style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}
              >
                <Edit3 size={14} />
                <span>{tournament.rules?.length ? 'Edit Rules' : 'Add Rules'}</span>
              </button>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {(tournament.rules && tournament.rules.length > 0 ? tournament.rules : [
              "Standard fair play rules apply to all matches.",
              "Both players must screenshot and confirm final match scores.",
              "Disconnections during active play must be reported to the host immediately.",
              "Toxic behavior, cheating, or manipulation leads to instant disqualification."
            ]).map((rule, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '12px',
                  padding: '12px 14px',
                  background: 'rgba(255, 255, 255, 0.02)',
                  borderRadius: '8px',
                  border: '1px solid var(--border-subtle)'
                }}
              >
                <span style={{ color: 'var(--accent-blue)', fontSize: '18px', lineHeight: '20px', fontWeight: 'bold' }}>•</span>
                <span style={{ color: 'var(--text-primary)', fontSize: '14px', lineHeight: '20px' }}>{rule}</span>
              </div>
            ))}
          </div>
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

      {showRulesModal && (
        <div className="modal-overlay" style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000, padding: '16px'
        }}>
          <div className="modal-content" style={{
            background: 'var(--surface-dark)',
            borderRadius: '12px',
            border: '1px solid var(--border-subtle)',
            maxWidth: '540px',
            width: '100%',
            padding: '24px',
            display: 'flex', flexDirection: 'column', gap: '16px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>
                Manage Tournament Rules
              </h3>
              <button
                onClick={() => setShowRulesModal(false)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            <p style={{ margin: 0, fontSize: '13px', color: 'var(--text-secondary)' }}>
              Manually type each rule. Each rule will be displayed as a distinct bullet to all users.
            </p>

            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                placeholder="Type a rule (e.g. Match duration 6 mins)..."
                value={newRuleText}
                onChange={(e) => setNewRuleText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    if (newRuleText.trim()) {
                      setRulesList([...rulesList, newRuleText.trim()]);
                      setNewRuleText('');
                    }
                  }
                }}
                className="input"
                style={{ flex: 1, padding: '10px 12px', borderRadius: '6px' }}
              />
              <button
                type="button"
                onClick={() => {
                  if (newRuleText.trim()) {
                    setRulesList([...rulesList, newRuleText.trim()]);
                    setNewRuleText('');
                  }
                }}
                disabled={!newRuleText.trim()}
                className="btn-primary"
                style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '10px 14px' }}
              >
                <Plus size={16} />
                <span>Add</span>
              </button>
            </div>

            {rulesList.length > 0 && (
              <div style={{ maxHeight: '220px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {rulesList.map((rule, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      background: 'rgba(255, 255, 255, 0.03)', padding: '8px 12px',
                      borderRadius: '6px', border: '1px solid var(--border-subtle)'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
                      <span style={{ color: 'var(--accent-blue)', fontWeight: 'bold' }}>•</span>
                      <span style={{ fontSize: '13px', color: 'var(--text-primary)' }}>{rule}</span>
                    </div>
                    <button
                      onClick={() => setRulesList(rulesList.filter((_, i) => i !== idx))}
                      style={{ background: 'transparent', border: 'none', color: 'var(--status-danger)', cursor: 'pointer', padding: '4px' }}
                      title="Remove rule"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '8px' }}>
              <button
                type="button"
                onClick={() => setShowRulesModal(false)}
                className="btn-secondary"
                disabled={savingRules}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={async () => {
                  if (!tournament) return;
                  setSavingRules(true);
                  try {
                    const updated = await api.updateRules(tournament.id, rulesList);
                    setTournament(updated);
                    setShowRulesModal(false);
                  } catch (err: any) {
                    alert(err?.message || 'Failed to save rules');
                  } finally {
                    setSavingRules(false);
                  }
                }}
                className="btn-primary"
                disabled={savingRules}
              >
                {savingRules ? 'Saving...' : 'Save Rules'}
              </button>
            </div>
          </div>
        </div>
      )}
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
