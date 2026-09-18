package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TieBreakerRule {
    POINTS,
    GOAL_DIFFERENCE,
    GOALS_SCORED,
    HEAD_TO_HEAD,
    ORGANIZER_TIEBREAKER
}

@Serializable
data class TournamentSettings(
    val minMetric: Double? = null,
    val maxMetric: Double? = null,
    val groupCount: Int? = null,
    val qualifyCountPerGroup: Int? = null,
    val rematchRestrictions: Boolean = true,
    val pointsForWin: Int = 3,
    val pointsForDraw: Int = 1,
    val pointsForLoss: Int = 0,
    val tieBreakers: List<TieBreakerRule> = listOf(
        TieBreakerRule.POINTS,
        TieBreakerRule.GOAL_DIFFERENCE,
        TieBreakerRule.GOALS_SCORED,
        TieBreakerRule.HEAD_TO_HEAD,
        TieBreakerRule.ORGANIZER_TIEBREAKER
    )
)

@Serializable
data class Tournament(
    val id: String,
    val publicId: String = "",
    val hostId: String,
    val name: String,
    val game: GameType,
    val format: TournamentFormat,
    val status: TournamentStatus = TournamentStatus.DRAFT,
    val visibility: TournamentVisibility = TournamentVisibility.PUBLIC,
    val maxParticipants: Int = 32,
    val registrationOpen: Boolean = false,
    val drawLocked: Boolean = false,
    val settings: TournamentSettings = TournamentSettings(),
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    init {
        require(maxParticipants in 2..32) {
            "Participant count must be between 2 and 32 (highest allowed is 32)"
        }
    }
}
