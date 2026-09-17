package com.bracketx.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FairnessWeights(
    val strengthDistribution: Double = 0.30,
    val bracketBalance: Double = 0.25,
    val competitiveDiversity: Double = 0.20,
    val opportunity: Double = 0.15,
    val randomness: Double = 0.10
) {
    init {
        val sum = strengthDistribution + bracketBalance + competitiveDiversity + opportunity + randomness
        require(kotlin.math.abs(sum - 1.0) < 0.001) { "Fairness weights must sum to 1.0, but got $sum" }
    }
}

@Serializable
data class FairnessScore(
    val total: Double,
    val strengthDistribution: Double,
    val bracketBalance: Double,
    val competitiveDiversity: Double,
    val opportunity: Double,
    val randomness: Double
)

@Serializable
data class DrawConfiguration(
    val candidateCount: Int = 500,
    val weights: FairnessWeights = FairnessWeights(),
    val algorithmVersion: String = "fair-draw-v1",
    val tieTolerance: Double = 0.10
)

@Serializable
data class DrawCandidate(
    val id: String,
    val participants: List<TournamentParticipant>,
    val matches: List<Match>,
    val groups: List<Group> = emptyList(),
    val bracketSize: Int,
    val fairnessScore: FairnessScore? = null,
    val randomSeed: Long
)

@Serializable
data class TournamentStructure(
    val participants: List<TournamentParticipant>,
    val bracketSize: Int?,
    val groups: List<Group> = emptyList(),
    val matches: List<Match>,
    val standings: List<Standing> = emptyList(),
    val fairnessScore: FairnessScore,
    val algorithmVersion: String,
    val randomSeed: Long
)

@Serializable
data class DrawGeneration(
    val id: String,
    val tournamentId: String,
    val algorithmVersion: String,
    val randomSeed: String,
    val fairnessScore: Double?,
    val candidateCount: Int,
    val generatedBy: String,
    val generatedAt: String,
    val lockedAt: String? = null,
    val snapshotJson: String? = null
)

@Serializable
data class AuditLog(
    val id: String,
    val tournamentId: String,
    val actorId: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val metadata: Map<String, String>? = null,
    val createdAt: String
)
