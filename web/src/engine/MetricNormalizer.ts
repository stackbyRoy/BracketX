import type { GameType, Participant, CompetitiveBand } from './types';

export class MetricNormalizer {
  static normalize(game: GameType, value: number, minOverride?: number, maxOverride?: number): number {
    const min = minOverride ?? (game === 'fc_mobile' ? 60 : 1500);
    const max = maxOverride ?? (game === 'fc_mobile' ? 120 : 3500);

    if (max <= min) return 50.0;
    const clamped = Math.max(min, Math.min(max, value));
    const score = ((clamped - min) / (max - min)) * 100.0;
    return Math.round(score * 10.0) / 10.0;
  }
}

export class CompetitiveBandClassifier {
  static classify(participants: Participant[]): Participant[] {
    if (participants.length === 0) return [];

    const sorted = [...participants].sort(
      (a, b) => (b.competitiveScore ?? 0) - (a.competitiveScore ?? 0)
    );

    const n = sorted.length;
    return sorted.map((p, idx) => {
      const percentile = idx / n;
      let band: CompetitiveBand = 'B';

      if (percentile < 0.10) {
        band = 'S';
      } else if (percentile < 0.30) {
        band = 'A';
      } else if (percentile < 0.70) {
        band = 'B';
      } else if (percentile < 0.90) {
        band = 'C';
      } else {
        band = 'D';
      }

      return {
        ...p,
        competitiveBand: band,
      };
    });
  }
}
