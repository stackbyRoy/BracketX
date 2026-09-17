package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeterministicDrawTest {

    private fun sampleParticipants(count: Int): List<TournamentParticipant> {
        val ratings = listOf(115.0, 112.0, 110.0, 108.0, 106.0, 104.0, 102.0, 100.0, 98.0, 96.0, 94.0, 92.0, 90.0, 88.0, 86.0, 84.0)
        return (1..count).map { i ->
            TournamentParticipant(
                id = "part_$i",
                tournamentId = "t_det",
                userId = "user_$i",
                displayName = "Player $i",
                inGameId = "IGN_$i",
                game = GameType.FC_MOBILE,
                metricType = "OVR",
                metricValue = ratings[(i - 1) % ratings.size]
            )
        }
    }

    @Test
    fun `repeated draw generation with identical inputs and seed produces identical structure`() {
        val participants = sampleParticipants(16)
        val config = DrawConfiguration(candidateCount = 100, algorithmVersion = "fair-draw-v1")
        val fixedSeed = 9876543210L

        val draw1 = FairDrawEngine.generateDraw(
            tournamentId = "t_det",
            participants = participants,
            format = TournamentFormat.SINGLE_ELIMINATION,
            settings = TournamentSettings(),
            config = config,
            seed = fixedSeed
        )

        val draw2 = FairDrawEngine.generateDraw(
            tournamentId = "t_det",
            participants = participants,
            format = TournamentFormat.SINGLE_ELIMINATION,
            settings = TournamentSettings(),
            config = config,
            seed = fixedSeed
        )

        assertEquals(draw1.bracketSize, draw2.bracketSize)
        assertEquals(draw1.randomSeed, draw2.randomSeed)
        assertEquals(draw1.algorithmVersion, draw2.algorithmVersion)
        assertEquals(draw1.fairnessScore.total, draw2.fairnessScore.total, 0.0001)
        assertEquals(draw1.matches.size, draw2.matches.size)

        // Compare match pairings in Round 1
        val r1Draw1 = draw1.matches.filter { it.roundNumber == 1 }.sortedBy { it.matchNumber }
        val r1Draw2 = draw2.matches.filter { it.roundNumber == 1 }.sortedBy { it.matchNumber }

        for (i in r1Draw1.indices) {
            assertEquals(r1Draw1[i].participantAId, r1Draw2[i].participantAId)
            assertEquals(r1Draw1[i].participantBId, r1Draw2[i].participantBId)
        }
    }

    @Test
    fun `different seeds normally produce distinct structures`() {
        val participants = sampleParticipants(16)
        val config = DrawConfiguration(candidateCount = 100)

        val drawA = FairDrawEngine.generateDraw(
            tournamentId = "t_det",
            participants = participants,
            format = TournamentFormat.SINGLE_ELIMINATION,
            seed = 11111L,
            config = config
        )

        val drawB = FairDrawEngine.generateDraw(
            tournamentId = "t_det",
            participants = participants,
            format = TournamentFormat.SINGLE_ELIMINATION,
            seed = 99999L,
            config = config
        )

        val r1A = drawA.matches.filter { it.roundNumber == 1 }.map { it.participantAId to it.participantBId }
        val r1B = drawB.matches.filter { it.roundNumber == 1 }.map { it.participantAId to it.participantBId }

        assertNotEquals(r1A, r1B, "Different seeds should produce different pairings")
    }
}
