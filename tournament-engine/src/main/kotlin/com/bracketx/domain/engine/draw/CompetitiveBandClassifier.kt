package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.CompetitiveBand

object CompetitiveBandClassifier {

    /**
     * Classifies participants into competitive bands (S, A, B, C, D) based on relative percentiles.
     * Assumes the input scores are sorted descending (highest score first).
     */
    fun classify(scoresDescending: List<Double>): List<CompetitiveBand> {
        val count = scoresDescending.size
        if (count == 0) return emptyList()

        if (count == 1) return listOf(CompetitiveBand.B)
        if (count == 2) return listOf(CompetitiveBand.A, CompetitiveBand.C)
        if (count == 3) return listOf(CompetitiveBand.A, CompetitiveBand.B, CompetitiveBand.C)
        if (count == 4) return listOf(CompetitiveBand.S, CompetitiveBand.A, CompetitiveBand.C, CompetitiveBand.D)

        return scoresDescending.mapIndexed { index, _ ->
            val percentile = (index.toDouble() + 0.5) / count.toDouble()
            when {
                percentile <= 0.10 -> CompetitiveBand.S // Top 10%
                percentile <= 0.30 -> CompetitiveBand.A // Next 20%
                percentile <= 0.70 -> CompetitiveBand.B // Middle 40%
                percentile <= 0.90 -> CompetitiveBand.C // Next 20%
                else -> CompetitiveBand.D               // Bottom 10%
            }
        }
    }
}
