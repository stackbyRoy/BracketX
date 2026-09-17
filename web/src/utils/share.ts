import type { Tournament } from '../engine/types';

export const getTournamentShareUrl = (tournamentId: string): string => {
  if (typeof window !== 'undefined' && window.location && window.location.origin) {
    return `${window.location.origin}/tournament/${tournamentId}`;
  }
  return `https://bracketx.vercel.app/tournament/${tournamentId}`;
};

export const shareTournament = async (
  tournament: Tournament
): Promise<{ shared: boolean; copied: boolean }> => {
  const url = getTournamentShareUrl(tournament.id);
  const gameName = tournament.game === 'fc_mobile' ? 'EA SPORTS FC Mobile' : 'eFootball';
  const formatName = tournament.format.replace('_', ' ').toUpperCase();

  const shareText = `🏆 Join my ${tournament.name} tournament on BracketX!\n🎮 Game: ${gameName}\n⚔️ Format: ${formatName} (${tournament.maxParticipants} players)\n\nTap the link below to view or register:\n${url}`;

  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share({
        title: `BracketX - ${tournament.name}`,
        text: shareText,
        url: url,
      });
      return { shared: true, copied: false };
    } catch (err: any) {
      if (err.name === 'AbortError') {
        return { shared: false, copied: false };
      }
      // Fall through to clipboard copy if Web Share failed
    }
  }

  // Fallback to clipboard
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(url);
      return { shared: false, copied: true };
    } catch {
      // ignore
    }
  }

  return { shared: false, copied: false };
};
