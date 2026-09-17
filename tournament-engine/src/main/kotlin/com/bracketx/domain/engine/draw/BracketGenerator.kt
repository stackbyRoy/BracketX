package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchSlot
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus
import com.bracketx.domain.model.TournamentParticipant
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object BracketGenerator {

    /**
     * Calculates the smallest power of 2 >= count.
     * Special case: count <= 1 returns 1.
     */
    fun nextPowerOfTwo(count: Int): Int {
        if (count <= 1) return 1
        var power = 1
        while (power < count) {
            power = power shl 1
        }
        return power
    }

    /**
     * Generates standard canonical seed pairings for a power-of-two bracket.
     * E.g. for 8: [1, 8, 4, 5, 3, 6, 2, 7]
     */
    fun canonicalSeedOrder(bracketSize: Int): List<Int> {
        if (bracketSize <= 1) return listOf(1)
        var rounds = listOf(1, 2)
        while (rounds.size < bracketSize) {
            val nextSize = rounds.size * 2
            val nextRound = mutableListOf<Int>()
            for (seed in rounds) {
                nextRound.add(seed)
                nextRound.add(nextSize + 1 - seed)
            }
            rounds = nextRound
        }
        return rounds
    }

    /**
     * Builds the entire knockout match tree for a given list of seeded participant assignments to slots.
     * If a slot has null participant, it is treated as a BYE.
     */
    fun buildKnockoutTree(
        tournamentId: String,
        bracketSize: Int,
        orderedParticipants: List<TournamentParticipant?>
    ): List<Match> {
        require(orderedParticipants.size == bracketSize) {
            "Expected $bracketSize slot assignments, but got ${orderedParticipants.size}"
        }

        if (bracketSize <= 1) {
            // Single participant tournament edge case
            val p = orderedParticipants.firstOrNull()
            return listOf(
                Match(
                    id = UUID.randomUUID().toString(),
                    tournamentId = tournamentId,
                    stage = MatchStage.FINAL,
                    roundNumber = 1,
                    matchNumber = 1,
                    participantAId = p?.id,
                    participantBId = null,
                    winnerId = p?.id,
                    status = MatchStatus.COMPLETED,
                    isBye = true
                )
            )
        }

        val totalRounds = (log2(bracketSize.toDouble())).toInt()
        val matchesByRound = mutableMapOf<Int, MutableList<Match>>()

        // 1. Create skeleton matches for each round from totalRounds (Final) down to Round 1
        // Round totalRounds: 1 match (Final)
        // Round totalRounds - 1: 2 matches (Semis)
        // ...
        // Round 1: bracketSize / 2 matches
        for (round in totalRounds downTo 1) {
            val matchCountInRound = 2.0.pow((totalRounds - round).toDouble()).toInt()
            val matches = mutableListOf<Match>()
            val stage = MatchStage.fromRoundNumber(round, totalRounds)

            for (matchIndex in 0 until matchCountInRound) {
                matches.add(
                    Match(
                        id = UUID.randomUUID().toString(),
                        tournamentId = tournamentId,
                        stage = stage,
                        roundNumber = round,
                        matchNumber = matchIndex + 1,
                        status = MatchStatus.SCHEDULED
                    )
                )
            }
            matchesByRound[round] = matches
        }

        // 2. Link each match in round R to its parent in round R + 1
        for (round in 1 until totalRounds) {
            val currentRoundMatches = matchesByRound[round]!!
            val nextRoundMatches = matchesByRound[round + 1]!!

            for (i in currentRoundMatches.indices) {
                val parentIndex = i / 2
                val slot = if (i % 2 == 0) MatchSlot.SLOT_A else MatchSlot.SLOT_B
                val parentMatch = nextRoundMatches[parentIndex]

                currentRoundMatches[i] = currentRoundMatches[i].copy(
                    nextMatchId = parentMatch.id,
                    nextMatchSlot = slot
                )
            }
        }

        // 3. Populate Round 1 matches with participants / byes
        val round1Matches = matchesByRound[1]!!
        val updatedNextRoundMatches = matchesByRound[2]?.toMutableList()

        for (i in round1Matches.indices) {
            val pA = orderedParticipants[i * 2]
            val pB = orderedParticipants[i * 2 + 1]

            val isBye = (pA == null || pB == null)
            val winnerId = when {
                pA != null && pB == null -> pA.id
                pA == null && pB != null -> pB.id
                else -> null
            }
            val status = if (isBye && winnerId != null) MatchStatus.COMPLETED else MatchStatus.SCHEDULED

            val updatedMatch = round1Matches[i].copy(
                participantAId = pA?.id,
                participantBId = pB?.id,
                winnerId = winnerId,
                status = status,
                isBye = isBye,
                scoreA = if (isBye) 0 else null,
                scoreB = if (isBye) 0 else null
            )
            round1Matches[i] = updatedMatch

            // If this match is a BYE and already has a winner, advance winner to Round 2
            if (isBye && winnerId != null && updatedNextRoundMatches != null) {
                val nextMatchIndex = i / 2
                val nextSlot = updatedMatch.nextMatchSlot
                val targetMatch = updatedNextRoundMatches[nextMatchIndex]

                updatedNextRoundMatches[nextMatchIndex] = if (nextSlot == MatchSlot.SLOT_A) {
                    targetMatch.copy(participantAId = winnerId)
                } else {
                    targetMatch.copy(participantBId = winnerId)
                }
            }
        }

        if (updatedNextRoundMatches != null) {
            matchesByRound[2] = updatedNextRoundMatches
        }

        return matchesByRound.values.flatten()
    }
}
