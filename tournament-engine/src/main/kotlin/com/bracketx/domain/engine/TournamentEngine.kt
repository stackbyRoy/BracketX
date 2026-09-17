package com.bracketx.domain.engine

import com.bracketx.domain.engine.draw.FairDrawEngine
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentStructure
import com.bracketx.domain.state.TournamentEvent
import com.bracketx.domain.state.TournamentStateMachine

object TournamentEngine {

    /**
     * Generates a complete fair tournament structure.
     */
    fun generateTournament(
        tournament: Tournament,
        participants: List<TournamentParticipant>,
        config: DrawConfiguration = DrawConfiguration(),
        seed: Long = System.currentTimeMillis()
    ): TournamentStructure {
        return FairDrawEngine.generateDraw(
            tournamentId = tournament.id,
            participants = participants,
            format = tournament.format,
            settings = tournament.settings,
            config = config,
            seed = seed
        )
    }

    /**
     * Records a match result, automatically progresses the winner in a knockout bracket,
     * and updates group standings if it was a group match.
     */
    fun submitScore(
        tournament: Tournament,
        currentMatches: List<Match>,
        matchId: String,
        scoreA: Int,
        scoreB: Int,
        isHostOverride: Boolean = false
    ): Pair<List<Match>, TournamentStatus> {
        val updatedMatches = ProgressionEngine.submitMatchScore(
            currentMatches = currentMatches,
            matchId = matchId,
            scoreA = scoreA,
            scoreB = scoreB,
            isHostOverride = isHostOverride
        )

        val nextStatus = if (ProgressionEngine.isTournamentFinished(updatedMatches)) {
            TournamentStatus.COMPLETED
        } else {
            TournamentStatus.IN_PROGRESS
        }

        return updatedMatches to nextStatus
    }

    /**
     * Recalculates all standings for a group.
     */
    fun recalculateGroupStandings(
        tournament: Tournament,
        groupId: String,
        participantIds: List<String>,
        matches: List<Match>
    ): List<Standing> {
        return StandingsCalculator.calculateGroupStandings(
            groupId = groupId,
            tournamentId = tournament.id,
            participantIds = participantIds,
            groupMatches = matches,
            settings = tournament.settings
        )
    }
}
