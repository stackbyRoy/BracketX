package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PropertyBasedDrawTest {

    private fun generateRandomParticipants(count: Int, seed: Long): List<TournamentParticipant> {
        val rng = Random(seed)
        return (1..count).map { i ->
            TournamentParticipant(
                id = "p_$i",
                tournamentId = "t_prop",
                userId = "u_$i",
                displayName = "Player $i",
                inGameId = "IGN_$i",
                game = GameType.FC_MOBILE,
                metricType = "OVR",
                metricValue = 70.0 + rng.nextDouble(0.0, 45.0)
            )
        }
    }

    @Test
    fun `knockout invariants hold across varying participant counts`() {
        val participantCounts = listOf(4, 5, 8, 10, 12, 16, 20, 24, 32)
        val config = DrawConfiguration(candidateCount = 50)

        for (count in participantCounts) {
            val participants = generateRandomParticipants(count, seed = count.toLong() * 31)
            val draw = FairDrawEngine.generateDraw(
                tournamentId = "t_prop",
                participants = participants,
                format = TournamentFormat.SINGLE_ELIMINATION,
                config = config,
                seed = count.toLong() * 100
            )

            // Invariant 1: Bracket size is power of two >= count
            val expectedSize = BracketGenerator.nextPowerOfTwo(count)
            assertEquals(expectedSize, draw.bracketSize)

            // Invariant 2: Every participant appears exactly once in Round 1
            val r1Matches = draw.matches.filter { it.roundNumber == 1 }
            val assignedIds = mutableListOf<String>()
            for (m in r1Matches) {
                m.participantAId?.let { assignedIds.add(it) }
                m.participantBId?.let { assignedIds.add(it) }
            }

            assertEquals(count, assignedIds.size)
            assertEquals(participants.map { it.id }.toSet(), assignedIds.toSet())

            // Invariant 3: Fairness score is strictly bounded [0, 100]
            assertTrue(draw.fairnessScore.total in 0.0..100.0)

            // Invariant 4: No match has broken reference
            val matchIds = draw.matches.map { it.id }.toSet()
            for (m in draw.matches) {
                m.nextMatchId?.let { nextId ->
                    assertTrue(matchIds.contains(nextId))
                    assertNotEquals(m.id, nextId)
                }
            }
        }
    }

    @Test
    fun `group invariants hold across varying group counts`() {
        val counts = listOf(8, 12, 16, 24, 32)
        val config = DrawConfiguration(candidateCount = 20)

        for (count in counts) {
            val participants = generateRandomParticipants(count, seed = count.toLong() * 17)
            val draw = FairDrawEngine.generateDraw(
                tournamentId = "t_prop",
                participants = participants,
                format = TournamentFormat.LEAGUE_GROUPS,
                settings = TournamentSettings(groupCount = 4),
                config = config,
                seed = count.toLong() * 77
            )

            // Every participant belongs to exactly one group
            val allMembers = draw.groups.flatMap { it.participantIds }
            assertEquals(count, allMembers.size)
            assertEquals(participants.map { it.id }.toSet(), allMembers.toSet())

            // Generated round-robin matches count matches formula sum(Ni * (Ni-1) / 2)
            val expectedMatches = draw.groups.sumOf { g ->
                val n = g.participantIds.size
                n * (n - 1) / 2
            }
            assertEquals(expectedMatches, draw.matches.size)
        }
    }
}
