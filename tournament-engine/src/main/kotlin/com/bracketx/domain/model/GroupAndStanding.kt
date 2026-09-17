package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val tournamentId: String,
    val name: String,
    val order: Int,
    val participantIds: List<String> = emptyList()
)

@Serializable
data class Standing(
    val id: String,
    val tournamentId: String,
    val groupId: String,
    val participantId: String,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDifference: Int = 0,
    val points: Int = 0
)
