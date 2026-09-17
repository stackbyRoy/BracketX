import type { Match, Standing } from './types';

export class ProgressionEngine {
  static submitScore(
    matchId: string,
    scoreA: number,
    scoreB: number,
    matches: Match[],
    standings: Standing[] = []
  ): { updatedMatches: Match[]; updatedStandings: Standing[]; winnerId: string } {
    if (scoreA < 0 || scoreB < 0) {
      throw new Error('Scores cannot be negative');
    }

    const matchIndex = matches.findIndex((m) => m.id === matchId);
    if (matchIndex === -1) {
      throw new Error(`Match ${matchId} not found`);
    }

    const match = matches[matchIndex];
    if (match.stage !== 'group' && scoreA === scoreB) {
      throw new Error('Ties are not permitted in knockout matches');
    }

    let winnerId: string | null = null;
    if (scoreA > scoreB) {
      winnerId = match.participantAId ?? null;
    } else if (scoreB > scoreA) {
      winnerId = match.participantBId ?? null;
    }

    const updatedMatches = [...matches];
    updatedMatches[matchIndex] = {
      ...match,
      scoreA,
      scoreB,
      winnerId,
      status: 'completed',
    };

    // Knockout progression: advance winner to next match slot
    if (winnerId && match.nextMatchId && match.nextSlot) {
      const nextMatchIndex = updatedMatches.findIndex((m) => m.id === match.nextMatchId);
      if (nextMatchIndex !== -1) {
        const nextMatch = updatedMatches[nextMatchIndex];
        updatedMatches[nextMatchIndex] = {
          ...nextMatch,
          participantAId: match.nextSlot === 'A' ? winnerId : nextMatch.participantAId,
          participantBId: match.nextSlot === 'B' ? winnerId : nextMatch.participantBId,
        };
      }
    }

    // Group stage progression: recalculate standings
    let updatedStandings = [...standings];
    if (match.stage === 'group' && match.groupId && match.participantAId && match.participantBId) {
      updatedStandings = this.recalculateGroupStandings(
        match.groupId,
        updatedMatches.filter((m) => m.groupId === match.groupId),
        standings.filter((s) => s.groupId === match.groupId)
      );
    }

    return {
      updatedMatches,
      updatedStandings,
      winnerId: winnerId ?? '',
    };
  }

  private static recalculateGroupStandings(
    groupId: string,
    groupMatches: Match[],
    currentGroupStandings: Standing[]
  ): Standing[] {
    const statsMap = new Map<string, {
      played: number;
      won: number;
      drawn: number;
      lost: number;
      goalsFor: number;
      goalsAgainst: number;
      goalDifference: number;
      points: number;
    }>();

    currentGroupStandings.forEach((s) => {
      statsMap.set(s.participantId, {
        played: 0,
        won: 0,
        drawn: 0,
        lost: 0,
        goalsFor: 0,
        goalsAgainst: 0,
        goalDifference: 0,
        points: 0,
      });
    });

    for (const m of groupMatches) {
      const sA = m.scoreA;
      const sB = m.scoreB;
      if (m.status !== 'completed' || sA === null || sA === undefined || sB === null || sB === undefined || !m.participantAId || !m.participantBId) {
        continue;
      }

      const statA = statsMap.get(m.participantAId);
      const statB = statsMap.get(m.participantBId);
      if (!statA || !statB) continue;

      statA.played++;
      statB.played++;
      statA.goalsFor += sA;
      statA.goalsAgainst += sB;
      statB.goalsFor += sB;
      statB.goalsAgainst += sA;
      statA.goalDifference = statA.goalsFor - statA.goalsAgainst;
      statB.goalDifference = statB.goalsFor - statB.goalsAgainst;

      if (sA > sB) {
        statA.won++;
        statA.points += 3;
        statB.lost++;
      } else if (sB > sA) {
        statB.won++;
        statB.points += 3;
        statA.lost++;
      } else {
        statA.drawn++;
        statB.drawn++;
        statA.points += 1;
        statB.points += 1;
      }
    }

    // Sort by points desc, then GD desc, then GF desc
    const sortedParticipants = Array.from(statsMap.entries()).sort((a, b) => {
      if (b[1].points !== a[1].points) return b[1].points - a[1].points;
      if (b[1].goalDifference !== a[1].goalDifference) return b[1].goalDifference - a[1].goalDifference;
      return b[1].goalsFor - a[1].goalsFor;
    });

    return sortedParticipants.map(([participantId, stat], rankIdx) => {
      const existing = currentGroupStandings.find((s) => s.participantId === participantId);
      return {
        id: existing?.id ?? crypto.randomUUID(),
        groupId,
        participantId,
        played: stat.played,
        won: stat.won,
        drawn: stat.drawn,
        lost: stat.lost,
        goalsFor: stat.goalsFor,
        goalsAgainst: stat.goalsAgainst,
        goalDifference: stat.goalDifference,
        points: stat.points,
        rank: rankIdx + 1,
      };
    });
  }
}
