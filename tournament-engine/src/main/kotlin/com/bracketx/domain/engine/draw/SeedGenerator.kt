package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.TournamentParticipant
import kotlin.random.Random

object SeedGenerator {

    /**
     * Orders participants by competitive score and assigns 1-based seeds.
     * Uses organizerSeed as first tie-breaker, then deterministic random tie-breaker.
     */
    fun generateSeeds(
        participants: List<TournamentParticipant>,
        rng: Random
    ): List<TournamentParticipant> {
        if (participants.isEmpty()) return emptyList()

        // Assign a deterministic random tiebreaker to each participant
        val tieBreakerMap = participants.associate { it.id to rng.nextDouble() }

        val sorted = participants.sortedWith(
            compareByDescending<TournamentParticipant> { it.normalizedScore }
                .thenBy { it.organizerSeed ?: Int.MAX_VALUE }
                .thenBy { tieBreakerMap[it.id] ?: 0.0 }
        )

        val bands = CompetitiveBandClassifier.classify(sorted.map { it.normalizedScore })

        return sorted.mapIndexed { index, participant ->
            participant.copy(
                seed = index + 1,
                competitiveBand = bands.getOrNull(index)
            )
        }
    }
}
