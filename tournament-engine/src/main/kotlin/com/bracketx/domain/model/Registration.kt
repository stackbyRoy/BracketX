package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RegistrationStatus {
    PENDING,
    APPROVED,
    CONFIRMED,
    REJECTED
}

@Serializable
data class Registration(
    val id: String,
    val tournamentId: String,
    val userId: String,
    val name: String,
    val inGameId: String,
    val gameMetricType: String,
    val gameMetricValue: Double,
    val status: RegistrationStatus = RegistrationStatus.CONFIRMED,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class User(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val createdAt: String? = null
)
