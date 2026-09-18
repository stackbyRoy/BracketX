package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameType(val displayName: String, val metricLabel: String, val defaultMinMetric: Double, val defaultMaxMetric: Double) {
    FC_MOBILE("FC Mobile", "OVR", 60.0, 120.0),
    EFOOTBALL("eFootball", "Team Strength", 1500.0, 3500.0);

    companion object {
        fun fromString(value: String): GameType = when (value.lowercase()) {
            "fc_mobile", "fcmobile", "fc mobile" -> FC_MOBILE
            "efootball", "e_football", "efootball mobile" -> EFOOTBALL
            else -> throw IllegalArgumentException("Unknown game type: $value")
        }
    }
}

@Serializable
enum class TournamentFormat(val displayName: String) {
    SINGLE_ELIMINATION("Single Elimination"),
    LEAGUE_GROUPS("League / Groups"),
    GROUPS_KNOCKOUT("Groups → Knockout");

    companion object {
        fun fromString(value: String): TournamentFormat = when (value.lowercase()) {
            "single_elimination", "knockout", "single elimination" -> SINGLE_ELIMINATION
            "league_groups", "league", "groups" -> LEAGUE_GROUPS
            "groups_knockout", "groups_to_knockout", "groups → knockout" -> GROUPS_KNOCKOUT
            else -> throw IllegalArgumentException("Unknown tournament format: $value")
        }
    }
}

@Serializable
enum class TournamentStatus(val displayName: String) {
    DRAFT("Draft"),
    REGISTRATION_OPEN("Registration Open"),
    REGISTRATION_CLOSED("Registration Closed"),
    DRAW_PENDING("Draw Pending"),
    DRAW_GENERATED("Draw Generated"),
    DRAW_LOCKED("Draw Locked"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromString(value: String): TournamentStatus = when (value.lowercase()) {
            "draft" -> DRAFT
            "registration_open" -> REGISTRATION_OPEN
            "registration_closed" -> REGISTRATION_CLOSED
            "draw_pending" -> DRAW_PENDING
            "draw_generated" -> DRAW_GENERATED
            "draw_locked" -> DRAW_LOCKED
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            else -> throw IllegalArgumentException("Unknown tournament status: $value")
        }
    }
}

@Serializable
enum class TournamentRole {
    HOST,
    PARTICIPANT
}

@Serializable
enum class TournamentVisibility(val displayName: String, val description: String) {
    PUBLIC("Public", "Anyone can discover and register."),
    PRIVATE("Private", "Tournament is not publicly listed and requires an access code.");

    companion object {
        fun fromString(value: String): TournamentVisibility = when (value.lowercase()) {
            "public" -> PUBLIC
            "private" -> PRIVATE
            else -> PUBLIC
        }
    }
}
