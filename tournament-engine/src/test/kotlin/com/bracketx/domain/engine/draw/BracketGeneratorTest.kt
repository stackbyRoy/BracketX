package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.TournamentParticipant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BracketGeneratorTest {

    private fun createParticipants(count: Int): List<TournamentParticipant> {
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
    fun `nextPowerOfTwo calculates correct bracket sizes`() {
        assertEquals(1, BracketGenerator.nextPowerOfTwo(1))
        assertEquals(2, BracketGenerator.nextPowerOfTwo(2))
        assertEquals(4, BracketGenerator.nextPowerOfTwo(3))
        assertEquals(4, BracketGenerator.nextPowerOfTwo(4))
        assertEquals(8, BracketGenerator.nextPowerOfTwo(5))
        assertEquals(8, BracketGenerator.nextPowerOfTwo(7))
        assertEquals(8, BracketGenerator.nextPowerOfTwo(8))
        assertEquals(16, BracketGenerator.nextPowerOfTwo(9))
        assertEquals(16, BracketGenerator.nextPowerOfTwo(16))
        assertEquals(32, BracketGenerator.nextPowerOfTwo(17))
        assertEquals(32, BracketGenerator.nextPowerOfTwo(32))
        assertEquals(64, BracketGenerator.nextPowerOfTwo(33))
        assertEquals(64, BracketGenerator.nextPowerOfTwo(64))
    }

    @Test
    fun `canonicalSeedOrder generates proper binary pairings`() {
        val size8 = BracketGenerator.canonicalSeedOrder(8)
        assertEquals(8, size8.size)
        // Seeds 1 and 2 must be in opposite halves
        val firstHalf = size8.take(4)
        val secondHalf = size8.drop(4)
        assertTrue(firstHalf.contains(1))
        assertTrue(secondHalf.contains(2))
    }

    @Test
    fun `buildKnockoutTree produces valid match trees for test participant counts`() {
        val testCounts = listOf(1, 2, 3, 4, 5, 7, 8, 9, 16, 17, 32, 33, 64)

        for (count in testCounts) {
            val participants = createParticipants(count)
            val bracketSize = BracketGenerator.nextPowerOfTwo(count)
            val slots = (0 until bracketSize).map { participants.getOrNull(it) }

            val matches = BracketGenerator.buildKnockoutTree("t1", bracketSize, slots)

            if (count > 1) {
                val round1Matches = matches.filter { it.roundNumber == 1 }
                assertEquals(bracketSize / 2, round1Matches.size)

                // Validation of hard constraints
                val validation = ConstraintValidator.validateKnockoutDraw(participants, bracketSize, matches)
                assertTrue(validation.isValid, "Failed for count $count: ${validation.errors}")
            } else {
                assertEquals(1, matches.size)
                assertTrue(matches.first().isCompleted)
            }
        }
    }
}
