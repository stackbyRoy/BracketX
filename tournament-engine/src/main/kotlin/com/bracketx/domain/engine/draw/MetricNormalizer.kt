package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.GameType
import kotlin.math.max
import kotlin.math.min

object MetricNormalizer {

    /**
     * Normalizes a raw competitive metric to a 0.0 - 100.0 score.
     * Uses global bounds if configured and valid, or falls back to tournament-relative normalization.
     */
    fun normalize(
        rawMetric: Double,
        game: GameType,
        customMin: Double? = null,
        customMax: Double? = null
    ): Double {
        val minBound = customMin ?: game.defaultMinMetric
        val maxBound = customMax ?: game.defaultMaxMetric

        if (maxBound <= minBound) {
            return 50.0
        }

        val normalized = ((rawMetric - minBound) / (maxBound - minBound)) * 100.0
        return clamp(normalized, 0.0, 100.0)
    }

    /**
     * Normalizes a list of metrics relative to the tournament's own min and max values.
     * Per Section 7.2 of DRAW_ALGORITHM_SPEC.md:
     * If tournamentMax == tournamentMin, every participant receives 50.0.
     */
    fun normalizeTournamentRelative(metrics: List<Double>): List<Double> {
        if (metrics.isEmpty()) return emptyList()

        val minMetric = metrics.minOrNull() ?: return emptyList()
        val maxMetric = metrics.maxOrNull() ?: return emptyList()

        if (maxMetric == minMetric) {
            return metrics.map { 50.0 }
        }

        val range = maxMetric - minMetric
        return metrics.map { metric ->
            val normalized = ((metric - minMetric) / range) * 100.0
            clamp(normalized, 0.0, 100.0)
        }
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        return max(min, min(max, value))
    }
}
