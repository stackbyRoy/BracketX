package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.CompetitiveBand
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.TournamentParticipant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FairnessScorerTest {

    private fun mockParticipant(id: String, score: Double, band: CompetitiveBand, seed: Int): TournamentParticipant {
        return TournamentParticipant(
            id = id,
            tournamentId = "t1",
            userId = "u_$id",
            displayName = "Player $id",
            inGameId = "IGN_$id",
            game = GameType.FC_MOBILE,
            metricType = "OVR",
            metricValue = 100.0,
            normalizedScore = score,
            competitiveBand = band,
            seed = seed
        )
    }

    @Test
    fun `all fairness score components remain strictly within 0 to 100`() {
        val participants = listOf(
            mockParticipant("p1", 95.0, CompetitiveBand.S, 1),
            mockParticipant("p2", 90.0, CompetitiveBand.A, 2),
            mockParticipant("p3", 85.0, CompetitiveBand.A, 3),
            mockParticipant("p4", 75.0, CompetitiveBand.B, 4),
            mockParticipant("p5", 70.0, CompetitiveBand.B, 5),
            mockParticipant("p6", 60.0, CompetitiveBand.C, 6),
            mockParticipant("p7", 55.0, CompetitiveBand.C, 7),
            mockParticipant("p8", 45.0, CompetitiveBand.D, 8)
        )

        val canonicalSlots = BracketGenerator.canonicalSeedOrder(8)
        val matches = BracketGenerator.buildKnockoutTree("t1", 8, participants)

        val score = FairnessScorer.scoreKnockout(
            orderedParticipants = participants,
            matches = matches,
            participants = participants,
            canonicalSeedSlots = canonicalSlots,
            config = DrawConfiguration()
        )

        assertTrue(score.total in 0.0..100.0, "Total: ${score.total}")
        assertTrue(score.strengthDistribution in 0.0..100.0, "StrengthDist: ${score.strengthDistribution}")
        assertTrue(score.bracketBalance in 0.0..100.0, "Balance: ${score.bracketBalance}")
        assertTrue(score.competitiveDiversity in 0.0..100.0, "Diversity: ${score.competitiveDiversity}")
        assertTrue(score.opportunity in 0.0..100.0, "Opportunity: ${score.opportunity}")
        assertTrue(score.randomness in 0.0..100.0, "Randomness: ${score.randomness}")
    }

    @Test
    fun `extreme skill gap handles gracefully`() {
        // Player A = 99.0, remaining players = 50.0
        val participants = mutableListOf(mockParticipant("p1", 99.0, CompetitiveBand.S, 1))
        for (i in 2..8) {
            participants.add(mockParticipant("p$i", 50.0, CompetitiveBand.B, i))
        }

        val canonicalSlots = BracketGenerator.canonicalSeedOrder(8)
        val matches = BracketGenerator.buildKnockoutTree("t1", 8, participants)

        val score = FairnessScorer.scoreKnockout(
            orderedParticipants = participants,
            matches = matches,
            participants = participants,
            canonicalSeedSlots = canonicalSlots,
            config = DrawConfiguration()
        )

        assertTrue(score.total in 0.0..100.0)
    }
}
