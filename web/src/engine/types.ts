export type GameType = 'fc_mobile' | 'efootball';
export type TournamentFormat = 'knockout' | 'league' | 'groups_knockout';
export type TournamentStatus =
  | 'draft'
  | 'registration_open'
  | 'registration_closed'
  | 'draw_pending'
  | 'draw_generated'
  | 'draw_locked'
  | 'in_progress'
  | 'completed';

export type CompetitiveBand = 'S' | 'A' | 'B' | 'C' | 'D';
export type MatchStatus = 'scheduled' | 'in_progress' | 'completed' | 'disputed' | 'cancelled';
export type MatchSlot = 'A' | 'B';

export interface TournamentSettings {
  minMetric?: number;
  maxMetric?: number;
  groupCount?: number;
  qualifyCountPerGroup?: number;
  rematchRestrictions?: boolean;
  pointsForWin?: number;
  pointsForDraw?: number;
  pointsForLoss?: number;
}

export interface Tournament {
  id: string;
  hostId: string;
  name: string;
  game: GameType;
  format: TournamentFormat;
  status: TournamentStatus;
  maxParticipants: number;
  registrationOpen: boolean;
  drawLocked: boolean;
  settings: TournamentSettings;
  createdAt?: string;
  updatedAt?: string;
}

export interface Participant {
  id: string;
  tournamentId: string;
  userId: string;
  name: string;
  inGameId: string;
  gameMetricType: string;
  gameMetricValue: number;
  competitiveScore?: number;
  competitiveBand?: CompetitiveBand;
  seed?: number;
}

export interface Match {
  id: string;
  tournamentId: string;
  stage: 'knockout' | 'group' | 'final';
  roundNumber: number;
  matchNumber: number;
  groupId?: string | null;
  participantAId?: string | null;
  participantBId?: string | null;
  scoreA?: number | null;
  scoreB?: number | null;
  winnerId?: string | null;
  status: MatchStatus;
  nextMatchId?: string | null;
  nextSlot?: MatchSlot | null;
  isBye: boolean;
}

export interface Group {
  id: string;
  tournamentId: string;
  name: string;
  orderIndex: number;
}

export interface Standing {
  id: string;
  groupId: string;
  participantId: string;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
  rank: number;
}

export interface FairnessScore {
  totalScore: number;
  strengthDistribution: number;
  bracketBalance: number;
  competitiveDiversity: number;
  opportunityDistribution: number;
  controlledRandomness: number;
}

export interface TournamentStructure {
  matches: Match[];
  groups: Group[];
  standings: Standing[];
  fairnessScore: FairnessScore;
}
