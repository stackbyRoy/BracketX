package com.bracketx.domain.engine.draw

import com.bracketx.domain.model.GameType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MetricNormalizerTest {

    @Test
    fun `normalize global bounds clamps and scales correctly`() {
        // FC Mobile bounds: 60.0 to 120.0
        val midScore = MetricNormalizer.normalize(90.0, GameType.FC_MOBILE)
        assertEquals(50.0, midScore, 0.001)

        val minScore = MetricNormalizer.normalize(60.0, GameType.FC_MOBILE)
        assertEquals(0.0, minScore, 0.001)

        val maxScore = MetricNormalizer.normalize(120.0, GameType.FC_MOBILE)
        assertEquals(100.0, maxScore, 0.001)

        // Above max clamps to 100
        val aboveMax = MetricNormalizer.normalize(130.0, GameType.FC_MOBILE)
        assertEquals(100.0, aboveMax, 0.001)

        // Below min clamps to 0
        val belowMin = MetricNormalizer.normalize(50.0, GameType.FC_MOBILE)
        assertEquals(0.0, belowMin, 0.001)
    }

    @Test
    fun `normalizeTournamentRelative scales relative to tournament bounds`() {
        val metrics = listOf(100.0, 110.0, 120.0)
        val normalized = MetricNormalizer.normalizeTournamentRelative(metrics)

        assertEquals(0.0, normalized[0], 0.001)
        assertEquals(50.0, normalized[1], 0.001)
        assertEquals(100.0, normalized[2], 0.001)
    }

    @Test
    fun `normalizeTournamentRelative assigns 50 when all metrics are identical`() {
        // Per Section 7.2 of DRAW_ALGORITHM_SPEC.md:
        // If tournamentMax == tournamentMin, every participant receives 50.0
        val uniformMetrics = listOf(105.0, 105.0, 105.0, 105.0)
        val normalized = MetricNormalizer.normalizeTournamentRelative(uniformMetrics)

        assertEquals(4, normalized.size)
        assertTrue(normalized.all { it == 50.0 })
    }
}
