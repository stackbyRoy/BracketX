package com.bracketx.domain.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TournamentVisibilityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDefaultVisibilityIsPublic() {
        val tournament = Tournament(
            id = "test-123",
            hostId = "host-456",
            name = "Champions Cup",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION
        )

        assertEquals(TournamentVisibility.PUBLIC, tournament.visibility)
        assertEquals("", tournament.publicId)
    }

    @Test
    fun testPrivateTournamentProperties() {
        val tournament = Tournament(
            id = "test-123",
            publicId = "BRX-7F92KQ",
            hostId = "host-456",
            name = "Secret Invitational",
            game = GameType.EFOOTBALL,
            format = TournamentFormat.LEAGUE_GROUPS,
            visibility = TournamentVisibility.PRIVATE
        )

        assertEquals(TournamentVisibility.PRIVATE, tournament.visibility)
        assertEquals("BRX-7F92KQ", tournament.publicId)
    }

    @Test
    fun testVisibilityFromString() {
        assertEquals(TournamentVisibility.PUBLIC, TournamentVisibility.fromString("public"))
        assertEquals(TournamentVisibility.PUBLIC, TournamentVisibility.fromString("PUBLIC"))
        assertEquals(TournamentVisibility.PRIVATE, TournamentVisibility.fromString("private"))
        assertEquals(TournamentVisibility.PRIVATE, TournamentVisibility.fromString("PRIVATE"))
        assertEquals(TournamentVisibility.PUBLIC, TournamentVisibility.fromString("unknown"))
    }

    @Test
    fun testSerializationRoundTrip() {
        val tournament = Tournament(
            id = "test-123",
            publicId = "BRX-8K92LX",
            hostId = "host-456",
            name = "Super League",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.GROUPS_KNOCKOUT,
            visibility = TournamentVisibility.PRIVATE
        )

        val serialized = json.encodeToString(Tournament.serializer(), tournament)
        val deserialized = json.decodeFromString(Tournament.serializer(), serialized)

        assertEquals(tournament.id, deserialized.id)
        assertEquals(tournament.publicId, deserialized.publicId)
        assertEquals(tournament.visibility, deserialized.visibility)
    }
}
