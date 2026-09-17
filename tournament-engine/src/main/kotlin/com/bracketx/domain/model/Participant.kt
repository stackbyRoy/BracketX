package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CompetitiveBand(val code: String, val label: String) {
    S("S", "Elite"),
    A("A", "Strong"),
    B("B", "Mid"),
    C("C", "Developing"),
    D("D", "Underdog");

    companion object {
        fun fromCode(code: String): CompetitiveBand = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unknown competitive band: $code")
    }
}

@Serializable
enum class ParticipantStatus {
    ACTIVE,
    WITHDRAWN,
    DISQUALIFIED
}

@Serializable
data class TournamentParticipant(
    val id: String,
    val tournamentId: String,
    val userId: String,
    val displayName: String,
    val inGameId: String,
    val game: GameType,
    val metricType: String,
    val metricValue: Double,
    val normalizedScore: Double = 50.0,
    val competitiveBand: CompetitiveBand? = null,
    val seed: Int? = null,
    val organizerSeed: Int? = null,
    val status: ParticipantStatus = ParticipantStatus.ACTIVE,
    val createdAt: String? = null
)
