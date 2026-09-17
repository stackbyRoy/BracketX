package com.bracketx.domain.engine

import com.bracketx.domain.model.Match
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.TieBreakerRule
import com.bracketx.domain.model.TournamentSettings

object StandingsCalculator {

    /**
     * Recalculates standings for a group from its completed matches.
     */
    fun calculateGroupStandings(
        groupId: String,
        tournamentId: String,
        participantIds: List<String>,
        groupMatches: List<Match>,
        settings: TournamentSettings = TournamentSettings()
    ): List<Standing> {
        val completedMatches = groupMatches.filter { it.groupId == groupId && it.isCompleted && it.scoreA != null && it.scoreB != null }

        val standingMap = participantIds.associateWith { participantId ->
            Standing(
                id = "${groupId}_$participantId",
                tournamentId = tournamentId,
                groupId = groupId,
                participantId = participantId
            )
        }.toMutableMap()

        for (match in completedMatches) {
            val pA = match.participantAId ?: continue
            val pB = match.participantBId ?: continue
            val scoreA = match.scoreA ?: continue
            val scoreB = match.scoreB ?: continue

            val stA = standingMap[pA] ?: continue
            val stB = standingMap[pB] ?: continue

            val (ptsA, ptsB) = when {
                scoreA > scoreB -> settings.pointsForWin to settings.pointsForLoss
                scoreB > scoreA -> settings.pointsForLoss to settings.pointsForWin
                else -> settings.pointsForDraw to settings.pointsForDraw
            }

            standingMap[pA] = stA.copy(
                played = stA.played + 1,
                wins = stA.wins + if (scoreA > scoreB) 1 else 0,
                draws = stA.draws + if (scoreA == scoreB) 1 else 0,
                losses = stA.losses + if (scoreA < scoreB) 1 else 0,
                goalsFor = stA.goalsFor + scoreA,
                goalsAgainst = stA.goalsAgainst + scoreB,
                goalDifference = stA.goalDifference + (scoreA - scoreB),
                points = stA.points + ptsA
            )

            standingMap[pB] = stB.copy(
                played = stB.played + 1,
                wins = stB.wins + if (scoreB > scoreA) 1 else 0,
                draws = stB.draws + if (scoreA == scoreB) 1 else 0,
                losses = stB.losses + if (scoreB < scoreA) 1 else 0,
                goalsFor = stB.goalsFor + scoreB,
                goalsAgainst = stB.goalsAgainst + scoreA,
                goalDifference = stB.goalDifference + (scoreB - scoreA),
                points = stB.points + ptsB
            )
        }

        val standingsList = standingMap.values.toList()

        // Sort by configured tiebreakers
        return standingsList.sortedWith { a, b ->
            compareStandings(a, b, completedMatches, settings.tieBreakers)
        }
    }

    private fun compareStandings(
        a: Standing,
        b: Standing,
        matches: List<Match>,
        rules: List<TieBreakerRule>
    ): Int {
        for (rule in rules) {
            val result = when (rule) {
                TieBreakerRule.POINTS -> b.points.compareTo(a.points)
                TieBreakerRule.GOAL_DIFFERENCE -> b.goalDifference.compareTo(a.goalDifference)
                TieBreakerRule.GOALS_SCORED -> b.goalsFor.compareTo(a.goalsFor)
                TieBreakerRule.HEAD_TO_HEAD -> compareHeadToHead(a.participantId, b.participantId, matches)
                TieBreakerRule.ORGANIZER_TIEBREAKER -> 0
            }
            if (result != 0) return result
        }
        return 0
    }

    private fun compareHeadToHead(participantA: String, participantB: String, matches: List<Match>): Int {
        val h2hMatches = matches.filter {
            (it.participantAId == participantA && it.participantBId == participantB) ||
            (it.participantAId == participantB && it.participantBId == participantA)
        }
        if (h2hMatches.isEmpty()) return 0

        var ptsA = 0
        var ptsB = 0
        for (m in h2hMatches) {
            val sA = (if (m.participantAId == participantA) m.scoreA else m.scoreB) ?: 0
            val sB = (if (m.participantAId == participantA) m.scoreB else m.scoreA) ?: 0
            when {
                sA > sB -> ptsA += 3
                sB > sA -> ptsB += 3
                else -> {
                    ptsA += 1
                    ptsB += 1
                }
            }
        }
        return ptsB.compareTo(ptsA)
    }
}
