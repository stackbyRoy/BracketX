package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MatchStage(val displayName: String) {
    GROUP("Group Stage"),
    ROUND_OF_64("Round of 64"),
    ROUND_OF_32("Round of 32"),
    ROUND_OF_16("Round of 16"),
    QUARTER_FINAL("Quarter Final"),
    SEMI_FINAL("Semi Final"),
    FINAL("Final");

    companion object {
        fun fromRoundNumber(round: Int, totalRounds: Int): MatchStage {
            val roundsFromFinal = totalRounds - round
            return when (roundsFromFinal) {
                0 -> FINAL
                1 -> SEMI_FINAL
                2 -> QUARTER_FINAL
                3 -> ROUND_OF_16
                4 -> ROUND_OF_32
                5 -> ROUND_OF_64
                else -> ROUND_OF_64
            }
        }
    }
}

@Serializable
enum class MatchStatus {
    SCHEDULED,
    LIVE,
    COMPLETED,
    DISPUTED,
    CANCELLED
}

@Serializable
enum class MatchSlot {
    SLOT_A,
    SLOT_B
}

@Serializable
data class Match(
    val id: String,
    val tournamentId: String,
    val stage: MatchStage,
    val roundNumber: Int,
    val matchNumber: Int,
    val groupId: String? = null,
    val participantAId: String? = null,
    val participantBId: String? = null,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val winnerId: String? = null,
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val nextMatchId: String? = null,
    val nextMatchSlot: MatchSlot? = null,
    val isBye: Boolean = false,
    val scheduledAt: String? = null,
    val completedAt: String? = null
) {
    val isCompleted: Boolean get() = status == MatchStatus.COMPLETED
    val isReady: Boolean get() = (participantAId != null && participantBId != null) || isBye
}
