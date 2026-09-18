package com.bracketx.data.repository

import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TournamentRepositoryVisibilityTest {

    private lateinit var repository: TournamentRepository

    @Before
    fun setup() {
        repository = InMemoryTournamentRepository()
    }

    @Test
    fun testCreatePublicTournament_GeneratesPublicId() = runBlocking {
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-1",
            name = "Public Cup",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PUBLIC
        )

        val result = repository.createTournament(tournament)
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()

        assertTrue(created.publicId.startsWith("BRX-"))
        assertEquals(TournamentVisibility.PUBLIC, created.visibility)
        assertNull(repository.getHostAccessCode(created.id))
    }

    @Test
    fun testCreatePrivateTournament_StoresAccessCode() = runBlocking {
        val accessCode = "4X9P7M"
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-1",
            name = "Private Invitational",
            game = GameType.EFOOTBALL,
            format = TournamentFormat.LEAGUE_GROUPS,
            visibility = TournamentVisibility.PRIVATE
        )

        val result = repository.createTournament(tournament, accessCode)
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()

        assertTrue(created.publicId.startsWith("BRX-"))
        assertEquals(TournamentVisibility.PRIVATE, created.visibility)
        assertEquals(accessCode, repository.getHostAccessCode(created.id))
    }

    @Test
    fun testSearchTournamentByPublicId_ExactAndCaseInsensitive() = runBlocking {
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            publicId = "BRX-7F92KQ",
            hostId = "host-1",
            name = "Searchable Tournament",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PUBLIC
        )
        repository.createTournament(tournament)

        // Exact match
        val exactMatch = repository.searchTournamentByPublicId("BRX-7F92KQ").getOrNull()
        assertNotNull(exactMatch)
        assertEquals(tournament.id, exactMatch?.id)

        // Lowercase match
        val lowerMatch = repository.searchTournamentByPublicId("brx-7f92kq").getOrNull()
        assertNotNull(lowerMatch)
        assertEquals(tournament.id, lowerMatch?.id)

        // Mixed case with whitespace
        val mixedMatch = repository.searchTournamentByPublicId("  Brx-7F92Kq  ").getOrNull()
        assertNotNull(mixedMatch)
        assertEquals(tournament.id, mixedMatch?.id)

        // Invalid ID
        val notFound = repository.searchTournamentByPublicId("BRX-NONEXIST").getOrNull()
        assertNull(notFound)
    }

    @Test
    fun testVerifyPrivateAccess_ValidAndInvalidCodes() = runBlocking {
        val correctCode = "4X9P7M"
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-1",
            name = "Guarded Tournament",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PRIVATE
        )
        val created = repository.createTournament(tournament, correctCode).getOrThrow()

        // Initially no access
        assertFalse(repository.hasPrivateAccess(created.id).first())

        // Wrong code fails
        val wrongResult = repository.verifyPrivateAccess(created.id, "WRONG1")
        assertFalse(wrongResult.isSuccess)
        assertFalse(repository.hasPrivateAccess(created.id).first())

        // Correct code succeeds (case-insensitive)
        val correctResult = repository.verifyPrivateAccess(created.id, "4x9p7m")
        assertTrue(correctResult.isSuccess)
        assertTrue(repository.hasPrivateAccess(created.id).first())
    }

    @Test
    fun testUpdateVisibility_SuccessInDraft() = runBlocking {
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-1",
            name = "Draft Tourney",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PUBLIC
        )
        val created = repository.createTournament(tournament).getOrThrow()

        val updatedResult = repository.updateTournamentVisibility(created.id, TournamentVisibility.PRIVATE, "NEWCOD")
        assertTrue(updatedResult.isSuccess)
        val updated = updatedResult.getOrThrow()

        assertEquals(TournamentVisibility.PRIVATE, updated.visibility)
        assertEquals("NEWCOD", repository.getHostAccessCode(created.id))
    }
}
