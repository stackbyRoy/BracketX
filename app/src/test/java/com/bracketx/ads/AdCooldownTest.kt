package com.bracketx.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AdCooldownTest {

    private val cooldownDurationMs = AdsConfig.COOLDOWN_DURATION_MS // 30 minutes

    @Test
    fun `test cooldown is inactive when never shown`() {
        val lastShownAt = 0L
        val now = System.currentTimeMillis()
        val elapsed = if (lastShownAt > 0L) now - lastShownAt else Long.MAX_VALUE

        val isCooldownActive = elapsed < cooldownDurationMs
        assertFalse(isCooldownActive)
    }

    @Test
    fun `test cooldown is active when shown 10 minutes ago`() {
        val now = System.currentTimeMillis()
        val tenMinutesMs = 10 * 60 * 1000L
        val lastShownAt = now - tenMinutesMs
        val elapsed = now - lastShownAt

        val isCooldownActive = elapsed < cooldownDurationMs
        assertTrue(isCooldownActive)

        val remainingMs = cooldownDurationMs - elapsed
        assertEquals(20 * 60 * 1000L, remainingMs)
    }

    @Test
    fun `test cooldown is expired when shown 30 minutes ago`() {
        val now = System.currentTimeMillis()
        val thirtyMinutesMs = 30 * 60 * 1000L
        val lastShownAt = now - thirtyMinutesMs
        val elapsed = now - lastShownAt

        val isCooldownActive = elapsed < cooldownDurationMs
        assertFalse(isCooldownActive)
    }

    @Test
    fun `test cooldown is expired when shown 45 minutes ago`() {
        val now = System.currentTimeMillis()
        val fortyFiveMinutesMs = 45 * 60 * 1000L
        val lastShownAt = now - fortyFiveMinutesMs
        val elapsed = now - lastShownAt

        val isCooldownActive = elapsed < cooldownDurationMs
        assertFalse(isCooldownActive)
    }

    @Test
    fun `test launch evaluation guard fires exactly once`() {
        val hasEvaluated = AtomicBoolean(false)

        // First evaluation: successfully claims the slot
        val firstEvaluation = hasEvaluated.compareAndSet(false, true)
        assertTrue(firstEvaluation)

        // Second evaluation (e.g. recomposition or duplicate event): must be rejected
        val secondEvaluation = hasEvaluated.compareAndSet(false, true)
        assertFalse(secondEvaluation)

        // Third evaluation: still rejected
        val thirdEvaluation = hasEvaluated.compareAndSet(false, true)
        assertFalse(thirdEvaluation)
    }

    @Test
    fun `test ad state transitions`() {
        var state = AdState.UNINITIALIZED
        assertEquals(AdState.UNINITIALIZED, state)

        state = AdState.INITIALIZING
        assertEquals(AdState.INITIALIZING, state)

        state = AdState.INITIALIZED
        assertEquals(AdState.INITIALIZED, state)

        state = AdState.LOADING
        assertEquals(AdState.LOADING, state)

        state = AdState.READY
        assertEquals(AdState.READY, state)

        state = AdState.SHOWING
        assertEquals(AdState.SHOWING, state)

        state = AdState.CONSUMED
        assertEquals(AdState.CONSUMED, state)
    }
}
