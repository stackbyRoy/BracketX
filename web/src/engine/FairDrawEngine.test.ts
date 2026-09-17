import { describe, it, expect } from 'vitest';
import { MetricNormalizer } from './MetricNormalizer';
import { BracketGenerator, FairDrawEngine } from './FairDrawEngine';
import { ProgressionEngine } from './ProgressionEngine';
import type { Participant } from './types';

describe('TypeScript Tournament Engine Parity Tests', () => {
  it('MetricNormalizer normalizes within [0, 100]', () => {
    expect(MetricNormalizer.normalize('fc_mobile', 60)).toBe(0.0);
    expect(MetricNormalizer.normalize('fc_mobile', 120)).toBe(100.0);
    expect(MetricNormalizer.normalize('fc_mobile', 90)).toBe(50.0);

    expect(MetricNormalizer.normalize('efootball', 1500)).toBe(0.0);
    expect(MetricNormalizer.normalize('efootball', 3500)).toBe(100.0);
    expect(MetricNormalizer.normalize('efootball', 2500)).toBe(50.0);
  });

  it('BracketGenerator canonical seeds are valid', () => {
    expect(BracketGenerator.canonicalSeedOrder(2)).toEqual([1, 2]);
    expect(BracketGenerator.canonicalSeedOrder(4)).toEqual([1, 4, 2, 3]);
    expect(BracketGenerator.canonicalSeedOrder(8)).toEqual([1, 8, 4, 5, 2, 7, 3, 6]);
    expect(BracketGenerator.canonicalSeedOrder(16).length).toBe(16);
    expect(BracketGenerator.canonicalSeedOrder(32).length).toBe(32);
  });

  it('FairDrawEngine creates valid 8-player knockout draw', () => {
    const participants: Participant[] = Array.from({ length: 8 }, (_, i) => ({
      id: `p-${i + 1}`,
      tournamentId: 't-1',
      userId: `u-${i + 1}`,
      name: `Player ${i + 1}`,
      inGameId: `ign_${i + 1}`,
      gameMetricType: 'OVR',
      gameMetricValue: 100 - i * 5,
      competitiveScore: 100 - i * 10,
    }));

    const structure = FairDrawEngine.generateKnockoutDraw('t-1', participants, 42);
    expect(structure.matches.length).toBe(7); // 4 + 2 + 1 = 7 matches
    expect(structure.fairnessScore.totalScore).toBeGreaterThanOrEqual(0);
    expect(structure.fairnessScore.totalScore).toBeLessThanOrEqual(100);
  });

  it('ProgressionEngine advances winner and prevents ties in knockout', () => {
    const participants: Participant[] = [
      { id: 'p1', tournamentId: 't-1', userId: 'u1', name: 'Alice', inGameId: 'ign1', gameMetricType: 'OVR', gameMetricValue: 100 },
      { id: 'p2', tournamentId: 't-1', userId: 'u2', name: 'Bob', inGameId: 'ign2', gameMetricType: 'OVR', gameMetricValue: 90 },
      { id: 'p3', tournamentId: 't-1', userId: 'u3', name: 'Charlie', inGameId: 'ign3', gameMetricType: 'OVR', gameMetricValue: 85 },
      { id: 'p4', tournamentId: 't-1', userId: 'u4', name: 'Dave', inGameId: 'ign4', gameMetricType: 'OVR', gameMetricValue: 80 },
    ];

    const structure = FairDrawEngine.generateKnockoutDraw('t-1', participants, 42);
    const round1Matches = structure.matches.filter((m) => m.roundNumber === 1);
    const m1 = round1Matches[0];

    // Knockout tie check
    expect(() => ProgressionEngine.submitScore(m1.id, 2, 2, structure.matches)).toThrow();

    // Valid win submission
    const result = ProgressionEngine.submitScore(m1.id, 3, 1, structure.matches);
    expect(result.winnerId).toBe(m1.participantAId);

    const finalMatch = result.updatedMatches.find((m) => m.id === m1.nextMatchId);
    expect(finalMatch).toBeDefined();
    expect(finalMatch?.participantAId).toBe(m1.participantAId);
  });
});
