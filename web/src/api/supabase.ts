import { createClient } from '@supabase/supabase-js';
import type { Tournament, Participant, Match, Group, Standing, TournamentStructure } from '../engine/types';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://cfiraopvfrrvdmqkcwsg.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
});

export const api = {
  // Tournaments
  async getTournaments(): Promise<Tournament[]> {
    const { data, error } = await supabase
      .from('tournaments')
      .select('*')
      .order('created_at', { ascending: false });

    if (error) throw error;
    return (data || []).map(mapTournament);
  },

  async deleteTournament(id: string, hostId?: string): Promise<void> {
    if (hostId) {
      const { error: rpcError } = await supabase.rpc('delete_tournament_as_host', {
        p_tournament_id: id,
        p_host_id: hostId,
      });
      if (!rpcError) return;
    }
    const { error } = await supabase
      .from('tournaments')
      .delete()
      .eq('id', id);

    if (error) throw error;
  },

  async getTournament(id: string): Promise<Tournament | null> {
    const clean = id.trim();
    const isPublicId = clean.toUpperCase().startsWith('BRX-');
    if (isPublicId) {
      const { data } = await supabase
        .from('tournaments')
        .select('*')
        .ilike('public_id', clean)
        .maybeSingle();

      if (data) return mapTournament(data);

      const { data: rpcData } = await supabase.rpc('resolve_tournament_by_public_id', {
        p_public_id: clean,
      });
      if (rpcData && rpcData.found) {
        return mapTournament(rpcData);
      }
      return null;
    }

    const { data } = await supabase
      .from('tournaments')
      .select('*')
      .eq('id', clean)
      .maybeSingle();

    if (data) return mapTournament(data);

    const { data: rpcData } = await supabase.rpc('resolve_tournament_by_public_id', {
      p_public_id: clean,
    });
    if (rpcData && rpcData.found) {
      return mapTournament(rpcData);
    }
    return null;
  },

  async createTournament(t: Partial<Tournament>): Promise<Tournament> {
    const { data: { session } } = await supabase.auth.getSession();
    if (!session?.user) {
      throw new Error('Authentication session missing or expired. Please sign in again.');
    }

    const hostId = session.user.id;
    if (t.hostId && t.hostId !== hostId) {
      throw new Error('Host ID does not match authenticated user.');
    }

    const { data, error } = await supabase
      .from('tournaments')
      .insert({
        id: t.id || crypto.randomUUID(),
        host_id: hostId,
        name: t.name,
        game: t.game,
        format: t.format,
        status: t.status || 'draft',
        visibility: t.visibility || 'public',
        rules: t.rules || [],
        max_participants: t.maxParticipants || 32,
        registration_open: t.registrationOpen ?? false,
        draw_locked: t.drawLocked ?? false,
        settings: t.settings || {},
      })
      .select()
      .single();

    if (error) throw error;
    return mapTournament(data);
  },

  async updateTournamentStatus(id: string, status: string, registrationOpen?: boolean, drawLocked?: boolean): Promise<void> {
    const updates: any = { status };
    if (registrationOpen !== undefined) updates.registration_open = registrationOpen;
    if (drawLocked !== undefined) updates.draw_locked = drawLocked;

    const { error } = await supabase
      .from('tournaments')
      .update(updates)
      .eq('id', id);

    if (error) throw error;
  },

  async updateRules(tournamentId: string, rules: string[]): Promise<Tournament> {
    const { data, error } = await supabase
      .from('tournaments')
      .update({ rules })
      .eq('id', tournamentId)
      .select()
      .single();

    if (error) throw error;
    return mapTournament(data);
  },

  // Participants
  async getParticipants(tournamentId: string): Promise<Participant[]> {
    const { data, error } = await supabase
      .from('participants')
      .select('*')
      .eq('tournament_id', tournamentId)
      .order('seed', { ascending: true, nullsFirst: false });

    if (error) throw error;
    return (data || []).map(mapParticipant);
  },

  async registerParticipant(p: {
    tournamentId: string;
    userId: string;
    name: string;
    inGameId: string;
    gameMetricType: string;
    gameMetricValue: number;
    competitiveScore?: number;
    competitiveBand?: string;
  }): Promise<Participant> {
    // 1. Insert registration record
    await supabase.from('registrations').insert({
      id: crypto.randomUUID(),
      tournament_id: p.tournamentId,
      user_id: p.userId,
      in_game_id: p.inGameId,
      game_metric_type: p.gameMetricType,
      game_metric_value: p.gameMetricValue,
      status: 'confirmed',
    });

    // 2. Insert participant record
    const { data, error } = await supabase
      .from('participants')
      .insert({
        id: crypto.randomUUID(),
        tournament_id: p.tournamentId,
        user_id: p.userId,
        name: p.name,
        in_game_id: p.inGameId,
        game_metric_type: p.gameMetricType,
        game_metric_value: p.gameMetricValue,
        competitive_score: p.competitiveScore ?? 50.0,
        competitive_band: p.competitiveBand ?? 'B',
      })
      .select()
      .single();

    if (error) throw error;
    return mapParticipant(data);
  },

  // Matches
  async getMatches(tournamentId: string): Promise<Match[]> {
    const { data, error } = await supabase
      .from('matches')
      .select('*')
      .eq('tournament_id', tournamentId)
      .order('round_number', { ascending: true })
      .order('match_number', { ascending: true });

    if (error) throw error;
    return (data || []).map(mapMatch);
  },

  async saveGeneratedDraw(tournamentId: string, structure: TournamentStructure): Promise<void> {
    // Delete existing matches/groups for this tournament
    await supabase.from('matches').delete().eq('tournament_id', tournamentId);
    await supabase.from('groups').delete().eq('tournament_id', tournamentId);

    // Save Groups
    if (structure.groups.length > 0) {
      const groupRows = structure.groups.map((g) => ({
        id: g.id,
        tournament_id: tournamentId,
        name: g.name,
        order_index: g.orderIndex,
      }));
      await supabase.from('groups').insert(groupRows);
    }

    // Save Matches
    if (structure.matches.length > 0) {
      const matchRows = structure.matches.map((m) => ({
        id: m.id,
        tournament_id: tournamentId,
        stage: m.stage,
        round_number: m.roundNumber,
        match_number: m.matchNumber,
        group_id: m.groupId || null,
        participant_a_id: m.participantAId || null,
        participant_b_id: m.participantBId || null,
        score_a: m.scoreA ?? null,
        score_b: m.scoreB ?? null,
        winner_id: m.winnerId || null,
        status: m.status,
        next_match_id: m.nextMatchId || null,
        next_match_slot: m.nextSlot || null,
        is_bye: m.isBye,
      }));
      await supabase.from('matches').insert(matchRows);
    }

    // Update tournament status
    await this.updateTournamentStatus(tournamentId, 'draw_generated');
  },

  async lockDraw(tournamentId: string): Promise<void> {
    // Try RPC first
    try {
      const { error } = await supabase.rpc('lock_tournament_draw', { p_tournament_id: tournamentId });
      if (!error) return;
    } catch (_) {}

    // Fallback direct table update
    await this.updateTournamentStatus(tournamentId, 'in_progress', false, true);
  },

  async submitMatchResult(matchId: string, scoreA: number, scoreB: number): Promise<void> {
    // Try RPC first
    try {
      const { error } = await supabase.rpc('submit_match_result', {
        p_match_id: matchId,
        p_score_a: scoreA,
        p_score_b: scoreB,
      });
      if (!error) return;
    } catch (_) {}

    // Fallback: fetch match and update directly
    const { data: matchData } = await supabase.from('matches').select('*').eq('id', matchId).single();
    if (!matchData) return;

    let winnerId: string | null = null;
    if (scoreA > scoreB) winnerId = matchData.participant_a_id;
    else if (scoreB > scoreA) winnerId = matchData.participant_b_id;

    await supabase.from('matches').update({
      score_a: scoreA,
      score_b: scoreB,
      winner_id: winnerId,
      status: 'completed',
    }).eq('id', matchId);

    // If next match slot exists, advance winner
    if (winnerId && matchData.next_match_id && matchData.next_match_slot) {
      const updateField = matchData.next_match_slot === 'A' ? 'participant_a_id' : 'participant_b_id';
      await supabase.from('matches').update({
        [updateField]: winnerId,
      }).eq('id', matchData.next_match_id);
    }
  },

  // Groups & Standings
  async getGroups(tournamentId: string): Promise<Group[]> {
    const { data, error } = await supabase
      .from('groups')
      .select('*')
      .eq('tournament_id', tournamentId)
      .order('order_index', { ascending: true });

    if (error) return [];
    return (data || []).map((g) => ({
      id: g.id,
      tournamentId: g.tournament_id,
      name: g.name,
      orderIndex: g.order_index,
    }));
  },

  async getStandings(groupId: string): Promise<Standing[]> {
    const { data, error } = await supabase
      .from('standings')
      .select('*')
      .eq('group_id', groupId)
      .order('rank', { ascending: true });

    if (error) return [];
    return (data || []).map((s) => ({
      id: s.id,
      groupId: s.group_id,
      participantId: s.participant_id,
      played: s.played,
      won: s.won,
      drawn: s.drawn,
      lost: s.lost,
      goalsFor: s.goals_for,
      goalsAgainst: s.goals_against,
      goalDifference: s.goal_difference,
      points: s.points,
      rank: s.rank,
    }));
  },
};

// Helper mappers
function mapTournament(raw: any): Tournament {
  return {
    id: raw.id,
    publicId: raw.public_id || raw.publicId || '',
    hostId: raw.host_id || raw.hostId || '',
    name: raw.name,
    game: raw.game,
    format: raw.format,
    status: raw.status,
    visibility: raw.visibility || 'public',
    maxParticipants: raw.max_participants || raw.maxParticipants || 32,
    registrationOpen: raw.registration_open ?? raw.registrationOpen ?? false,
    drawLocked: raw.draw_locked ?? raw.drawLocked ?? false,
    rules: Array.isArray(raw.rules) ? raw.rules : [],
    settings: raw.settings || {},
    createdAt: raw.created_at || raw.createdAt,
    updatedAt: raw.updated_at || raw.updatedAt,
  };
}

function mapParticipant(raw: any): Participant {
  return {
    id: raw.id,
    tournamentId: raw.tournament_id,
    userId: raw.user_id,
    name: raw.name,
    inGameId: raw.in_game_id,
    gameMetricType: raw.game_metric_type,
    gameMetricValue: Number(raw.game_metric_value),
    competitiveScore: Number(raw.competitive_score),
    competitiveBand: raw.competitive_band,
    seed: raw.seed,
  };
}

function mapMatch(raw: any): Match {
  return {
    id: raw.id,
    tournamentId: raw.tournament_id,
    stage: raw.stage,
    roundNumber: raw.round_number,
    matchNumber: raw.match_number,
    groupId: raw.group_id,
    participantAId: raw.participant_a_id,
    participantBId: raw.participant_b_id,
    scoreA: raw.score_a,
    scoreB: raw.score_b,
    winnerId: raw.winner_id,
    status: raw.status,
    nextMatchId: raw.next_match_id,
    nextSlot: raw.next_match_slot,
    isBye: raw.is_bye ?? false,
  };
}
