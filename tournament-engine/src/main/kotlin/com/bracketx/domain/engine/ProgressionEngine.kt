package com.bracketx.domain.engine

import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchSlot
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus

class MatchResultException(message: String) : IllegalArgumentException(message)

object ProgressionEngine {

    /**
     * Submits an official score for a match, determines the winner, and advances the winner
     * to the next match slot in the tournament structure.
     *
     * Invariants enforced:
     * - scoreA >= 0 and scoreB >= 0
     * - For knockout matches: scoreA != scoreB
     * - Completed matches cannot be accidentally overwritten without override flag
     * - Winner is placed into nextMatchId according to nextMatchSlot
     */
    fun submitMatchScore(
        currentMatches: List<Match>,
        matchId: String,
        scoreA: Int,
        scoreB: Int,
        isHostOverride: Boolean = false
    ): List<Match> {
        if (scoreA < 0 || scoreB < 0) {
            throw MatchResultException("Scores must be non-negative: scoreA=$scoreA, scoreB=$scoreB")
        }

        val matchIndex = currentMatches.indexOfFirst { it.id == matchId }
        if (matchIndex == -1) {
            throw MatchResultException("Match with id $matchId not found")
        }

        val match = currentMatches[matchIndex]

        if (match.isCompleted && !isHostOverride) {
            throw MatchResultException("Match $matchId is already completed. Host override required to modify.")
        }

        if (match.stage != MatchStage.GROUP && scoreA == scoreB) {
            throw MatchResultException("Knockout match cannot end in a draw: $scoreA - $scoreB")
        }

        val winnerId = when {
            scoreA > scoreB -> match.participantAId ?: throw MatchResultException("Participant A is missing")
            scoreB > scoreA -> match.participantBId ?: throw MatchResultException("Participant B is missing")
            else -> null // Group draw
        }

        val updatedMatch = match.copy(
            scoreA = scoreA,
            scoreB = scoreB,
            winnerId = winnerId,
            status = MatchStatus.COMPLETED
        )

        val updatedMatches = currentMatches.toMutableList()
        updatedMatches[matchIndex] = updatedMatch

        // Advance winner to next match if applicable
        if (winnerId != null && match.nextMatchId != null) {
            val nextMatchIndex = updatedMatches.indexOfFirst { it.id == match.nextMatchId }
            if (nextMatchIndex != -1) {
                val nextMatch = updatedMatches[nextMatchIndex]
                val updatedNextMatch = when (match.nextMatchSlot) {
                    MatchSlot.SLOT_A -> nextMatch.copy(participantAId = winnerId)
                    MatchSlot.SLOT_B -> nextMatch.copy(participantBId = winnerId)
                    null -> nextMatch.copy(
                        participantAId = nextMatch.participantAId ?: winnerId,
                        participantBId = if (nextMatch.participantAId != null) winnerId else nextMatch.participantBId
                    )
                }
                updatedMatches[nextMatchIndex] = updatedNextMatch
            }
        }

        return updatedMatches
    }

    /**
     * Checks whether all matches in the tournament are completed.
     */
    fun isTournamentFinished(matches: List<Match>): Boolean {
        return matches.isNotEmpty() && matches.all { it.isCompleted }
    }

    /**
     * Returns the champion participant ID if the final match has completed.
     */
    fun getChampionId(matches: List<Match>): String? {
        val finalMatch = matches.firstOrNull { it.stage == MatchStage.FINAL }
        return if (finalMatch?.isCompleted == true) finalMatch.winnerId else null
    }
}
