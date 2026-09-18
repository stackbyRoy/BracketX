package com.bracketx.util

import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentShareHelperTest {

    @Test
    fun testGetTournamentUrl_UsesPublicIdAndCanonicalRoute() {
        val tournament = Tournament(
            id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            publicId = "BRX-7F92KQ",
            hostId = "host-1",
            name = "Champions Cup",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PUBLIC
        )

        val url = TournamentShareHelper.getTournamentUrl(tournament)
        assertTrue(url.endsWith("/t/BRX-7F92KQ"))
        assertFalse(url.contains("f47ac10b"))
    }

    @Test
    fun testGetTournamentUrl_FallbackToIdIfPublicIdBlank() {
        val tournament = Tournament(
            id = "test-uuid",
            publicId = "",
            hostId = "host-1",
            name = "Test",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION
        )

        val url = TournamentShareHelper.getTournamentUrl(tournament)
        assertTrue(url.endsWith("/t/test-uuid"))
    }
}
