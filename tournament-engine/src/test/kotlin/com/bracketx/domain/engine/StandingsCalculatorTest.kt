package com.bracketx.domain.engine

import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StandingsCalculatorTest {

    @Test
    fun `calculates points, goal difference, and sorts standings accurately`() {
        val groupId = "grp_A"
        val tournamentId = "t1"
        val participants = listOf("p1", "p2", "p3", "p4")

        // Matches:
        // p1 vs p2: 3 - 1 (p1: 3pts, +2 GD; p2: 0pts, -2 GD)
        // p3 vs p4: 0 - 0 (p3: 1pt, 0 GD; p4: 1pt, 0 GD)
        // p1 vs p3: 2 - 0 (p1: +3pts = 6pts, +4 GD; p3: +0pts = 1pt, -2 GD)
        // p2 vs p4: 2 - 1 (p2: +3pts = 3pts, -1 GD; p4: +0pts = 1pt, -1 GD)

        val matches = listOf(
            Match(
                id = "m1", tournamentId = tournamentId, stage = MatchStage.GROUP,
                roundNumber = 1, matchNumber = 1, groupId = groupId,
                participantAId = "p1", participantBId = "p2",
                scoreA = 3, scoreB = 1, winnerId = "p1", status = MatchStatus.COMPLETED
            ),
            Match(
                id = "m2", tournamentId = tournamentId, stage = MatchStage.GROUP,
                roundNumber = 1, matchNumber = 2, groupId = groupId,
                participantAId = "p3", participantBId = "p4",
                scoreA = 0, scoreB = 0, winnerId = null, status = MatchStatus.COMPLETED
            ),
            Match(
                id = "m3", tournamentId = tournamentId, stage = MatchStage.GROUP,
                roundNumber = 1, matchNumber = 3, groupId = groupId,
                participantAId = "p1", participantBId = "p3",
                scoreA = 2, scoreB = 0, winnerId = "p1", status = MatchStatus.COMPLETED
            ),
            Match(
                id = "m4", tournamentId = tournamentId, stage = MatchStage.GROUP,
                roundNumber = 1, matchNumber = 4, groupId = groupId,
                participantAId = "p2", participantBId = "p4",
                scoreA = 2, scoreB = 1, winnerId = "p2", status = MatchStatus.COMPLETED
            )
        )

        val standings = StandingsCalculator.calculateGroupStandings(
            groupId = groupId,
            tournamentId = tournamentId,
            participantIds = participants,
            groupMatches = matches
        )

        assertEquals(4, standings.size)

        // 1st place: p1 with 6 points, +4 GD
        assertEquals("p1", standings[0].participantId)
        assertEquals(6, standings[0].points)
        assertEquals(2, standings[0].wins)
        assertEquals(4, standings[0].goalDifference)

        // 2nd place: p2 with 3 points, -1 GD
        assertEquals("p2", standings[1].participantId)
        assertEquals(3, standings[1].points)
        assertEquals(1, standings[1].wins)
        assertEquals(-1, standings[1].goalDifference)

        // 3rd place: p4 with 1 point, -1 GD (GF: 1 vs p3 GF: 0)
        assertEquals("p4", standings[2].participantId)
        assertEquals(1, standings[2].points)
        assertEquals(-1, standings[2].goalDifference)

        // 4th place: p3 with 1 point, -2 GD
        assertEquals("p3", standings[3].participantId)
        assertEquals(1, standings[3].points)
        assertEquals(-2, standings[3].goalDifference)
    }
}
