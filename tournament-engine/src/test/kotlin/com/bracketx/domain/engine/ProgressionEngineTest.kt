package com.bracketx.domain.engine

import com.bracketx.domain.engine.draw.BracketGenerator
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus
import com.bracketx.domain.model.TournamentParticipant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProgressionEngineTest {

    private fun sampleParticipants(count: Int): List<TournamentParticipant> {
        return (1..count).map { i ->
            TournamentParticipant(
                id = "p$i",
                tournamentId = "t1",
                userId = "u$i",
                displayName = "Player $i",
                inGameId = "IGN_$i",
                game = GameType.FC_MOBILE,
                metricType = "OVR",
                metricValue = 100.0 + i,
                normalizedScore = 50.0 + i,
                seed = i
            )
        }
    }

    @Test
    fun `winner advances automatically to next match slot`() {
        val participants = sampleParticipants(4)
        val matches = BracketGenerator.buildKnockoutTree("t1", 4, participants)

        // Semi 1: p1 vs p4
        val semi1 = matches.first { it.roundNumber == 1 && it.matchNumber == 1 }
        val semi2 = matches.first { it.roundNumber == 1 && it.matchNumber == 2 }
        val finalMatch = matches.first { it.roundNumber == 2 }

        // Semi 1: participantA vs participantB
        val winner1 = semi1.participantAId!!
        val loser1 = semi1.participantBId!!
        val updatedMatches1 = ProgressionEngine.submitMatchScore(matches, semi1.id, 3, 1)
        val updatedFinal1 = updatedMatches1.first { it.id == finalMatch.id }
        assertEquals(winner1, updatedFinal1.participantAId)
        assertNull(updatedFinal1.participantBId)

        // Semi 2: submit score so participantB wins
        val winner2 = semi2.participantBId!!
        val updatedMatches2 = ProgressionEngine.submitMatchScore(updatedMatches1, semi2.id, 0, 2)
        val updatedFinal2 = updatedMatches2.first { it.id == finalMatch.id }
        assertEquals(winner1, updatedFinal2.participantAId)
        assertEquals(winner2, updatedFinal2.participantBId)
        assertTrue(updatedFinal2.isReady)

        // Final: winner1 vs winner2, score 4 - 2
        val completedTournamentMatches = ProgressionEngine.submitMatchScore(updatedMatches2, finalMatch.id, 4, 2)
        assertTrue(ProgressionEngine.isTournamentFinished(completedTournamentMatches))
        assertEquals(winner1, ProgressionEngine.getChampionId(completedTournamentMatches))
    }

    @Test
    fun `knockout matches cannot end in a draw`() {
        val participants = sampleParticipants(4)
        val matches = BracketGenerator.buildKnockoutTree("t1", 4, participants)
        val semi1 = matches.first { it.roundNumber == 1 && it.matchNumber == 1 }

        assertThrows<MatchResultException> {
            ProgressionEngine.submitMatchScore(matches, semi1.id, 2, 2)
        }
    }

    @Test
    fun `negative scores are rejected`() {
        val participants = sampleParticipants(4)
        val matches = BracketGenerator.buildKnockoutTree("t1", 4, participants)
        val semi1 = matches.first { it.roundNumber == 1 && it.matchNumber == 1 }

        assertThrows<MatchResultException> {
            ProgressionEngine.submitMatchScore(matches, semi1.id, -1, 3)
        }
    }

    @Test
    fun `completed match cannot be accidentally modified without host override`() {
        val participants = sampleParticipants(4)
        val matches = BracketGenerator.buildKnockoutTree("t1", 4, participants)
        val semi1 = matches.first { it.roundNumber == 1 && it.matchNumber == 1 }

        val updated = ProgressionEngine.submitMatchScore(matches, semi1.id, 3, 1)

        // Attempting to submit again without override must throw
        assertThrows<MatchResultException> {
            ProgressionEngine.submitMatchScore(updated, semi1.id, 3, 2, isHostOverride = false)
        }

        // With host override it succeeds
        val overridden = ProgressionEngine.submitMatchScore(updated, semi1.id, 3, 2, isHostOverride = true)
        val updatedSemi = overridden.first { it.id == semi1.id }
        assertEquals(3, updatedSemi.scoreA)
        assertEquals(2, updatedSemi.scoreB)
    }
}
