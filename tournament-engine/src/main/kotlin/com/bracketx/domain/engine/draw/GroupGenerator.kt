package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.Group
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.TournamentParticipant
import java.util.UUID

object GroupGenerator {

    /**
     * Distributes seeded participants into groups using serpentine distribution.
     * E.g. for 4 groups:
     * Row 0: A (1), B (2), C (3), D (4)
     * Row 1: D (5), C (6), B (7), A (8)
     * Row 2: A (9), B (10), C (11), D (12)...
     */
    fun distributeSerpentine(
        tournamentId: String,
        seededParticipants: List<TournamentParticipant>,
        groupCount: Int
    ): List<Group> {
        require(groupCount > 0) { "groupCount must be greater than 0" }
        if (seededParticipants.isEmpty()) return emptyList()

        val actualGroupCount = minOf(groupCount, seededParticipants.size)
        val groupBuckets = List(actualGroupCount) { mutableListOf<String>() }

        var goingForward = true
        var currentGroupIndex = 0

        for (participant in seededParticipants) {
            groupBuckets[currentGroupIndex].add(participant.id)

            if (goingForward) {
                if (currentGroupIndex == actualGroupCount - 1) {
                    goingForward = false
                } else {
                    currentGroupIndex++
                }
            } else {
                if (currentGroupIndex == 0) {
                    goingForward = true
                } else {
                    currentGroupIndex--
                }
            }
        }

        return groupBuckets.mapIndexed { index, memberIds ->
            val groupLetter = ('A'.code + index).toChar().toString()
            Group(
                id = UUID.randomUUID().toString(),
                tournamentId = tournamentId,
                name = "Group $groupLetter",
                order = index + 1,
                participantIds = memberIds
            )
        }
    }

    /**
     * Generates round-robin matches for each group.
     * Each participant plays every other participant in their group once: N * (N - 1) / 2 matches.
     */
    fun generateGroupMatches(
        tournamentId: String,
        groups: List<Group>
    ): List<Match> {
        val matches = mutableListOf<Match>()
        var globalMatchNumber = 1

        for (group in groups) {
            val members = group.participantIds
            for (i in 0 until members.size) {
                for (j in (i + 1) until members.size) {
                    matches.add(
                        Match(
                            id = UUID.randomUUID().toString(),
                            tournamentId = tournamentId,
                            stage = MatchStage.GROUP,
                            roundNumber = 1,
                            matchNumber = globalMatchNumber++,
                            groupId = group.id,
                            participantAId = members[i],
                            participantBId = members[j],
                            status = MatchStatus.SCHEDULED
                        )
                    )
                }
            }
        }
        return matches
    }

    /**
     * Initializes empty standings for all group members.
     */
    fun initializeStandings(
        tournamentId: String,
        groups: List<Group>
    ): List<Standing> {
        val standings = mutableListOf<Standing>()
        for (group in groups) {
            for (participantId in group.participantIds) {
                standings.add(
                    Standing(
                        id = UUID.randomUUID().toString(),
                        tournamentId = tournamentId,
                        groupId = group.id,
                        participantId = participantId
                    )
                )
            }
        }
        return standings
    }
}
