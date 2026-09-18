package com.bracketx.data.repository

import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TournamentDeletionAndDashboardTest {

    private lateinit var repository: TournamentRepository

    @Before
    fun setup() {
        repository = InMemoryTournamentRepository()
    }

    @Test
    fun testHostCanDeleteTournament() = runBlocking {
        val hostId = "host-user-123"
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = hostId,
            name = "Cup to Delete",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PUBLIC
        )

        repository.createTournament(tournament)
        val beforeDelete = repository.getTournament(tournament.id).first()
        assertEquals(tournament.id, beforeDelete?.id)

        val deleteResult = repository.deleteTournament(tournament.id, hostId)
        assertTrue(deleteResult.isSuccess)

        val afterDelete = repository.getTournament(tournament.id).first()
        assertNull(afterDelete)
    }

    @Test
    fun testNonHostCannotDeleteTournament() = runBlocking {
        val hostId = "host-user-123"
        val nonHostId = "malicious-user-999"
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = hostId,
            name = "Protected Cup",
            game = GameType.EFOOTBALL,
            format = TournamentFormat.LEAGUE_GROUPS,
            visibility = TournamentVisibility.PRIVATE
        )

        repository.createTournament(tournament, "SECRET12")

        val deleteResult = repository.deleteTournament(tournament.id, nonHostId)
        assertFalse(deleteResult.isSuccess)

        // Tournament must still exist
        val stillExists = repository.getTournament(tournament.id).first()
        assertEquals(tournament.id, stillExists?.id)
    }

    @Test
    fun testHostSeesPrivateTournamentsOnDashboard() = runBlocking {
        val hostId = "my-host-id"
        val otherHostId = "other-host-id"

        val myPrivateTournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = hostId,
            name = "My Private Championship",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PRIVATE
        )

        val otherPrivateTournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = otherHostId,
            name = "Other Private Championship",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            visibility = TournamentVisibility.PRIVATE
        )

        repository.createTournament(myPrivateTournament, "PASS1")
        repository.createTournament(otherPrivateTournament, "PASS2")

        val allTournaments = repository.getTournaments().first()

        // Host filter simulation
        val myDashboardTournaments = allTournaments.filter { it.hostId == hostId }
        assertEquals(1, myDashboardTournaments.size)
        assertEquals("My Private Championship", myDashboardTournaments.first().name)
        assertEquals(TournamentVisibility.PRIVATE, myDashboardTournaments.first().visibility)
    }

    @Test
    fun testUpcomingLiveFinishedStatusCategorization() = runBlocking {
        val hostId = "host-filter-test"

        val draftTournament = Tournament(
            id = "t-1",
            hostId = hostId,
            name = "Upcoming Draft",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            status = TournamentStatus.DRAFT
        )

        val inProgressTournament = Tournament(
            id = "t-2",
            hostId = hostId,
            name = "Live Competition",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            status = TournamentStatus.IN_PROGRESS
        )

        val completedTournament = Tournament(
            id = "t-3",
            hostId = hostId,
            name = "Finished Championship",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            status = TournamentStatus.COMPLETED
        )

        repository.createTournament(draftTournament)
        repository.createTournament(inProgressTournament)
        repository.createTournament(completedTournament)

        val myTournaments = repository.getTournaments().first().filter { it.hostId == hostId }
        assertEquals(3, myTournaments.size)

        val live = myTournaments.filter { it.status == TournamentStatus.IN_PROGRESS }
        val upcoming = myTournaments.filter {
            it.status in listOf(
                TournamentStatus.DRAFT,
                TournamentStatus.REGISTRATION_OPEN,
                TournamentStatus.REGISTRATION_CLOSED,
                TournamentStatus.DRAW_PENDING,
                TournamentStatus.DRAW_GENERATED,
                TournamentStatus.DRAW_LOCKED
            )
        }
        val finished = myTournaments.filter { it.status == TournamentStatus.COMPLETED }

        assertEquals(1, live.size)
        assertEquals("Live Competition", live.first().name)

        assertEquals(1, upcoming.size)
        assertEquals("Upcoming Draft", upcoming.first().name)

        assertEquals(1, finished.size)
        assertEquals("Finished Championship", finished.first().name)
    }

    @Test
    fun testCreateTournamentWithRules() = runBlocking {
        val rules = listOf("6-minute halves", "Screenshots required", "Fair play only")
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-user-123",
            name = "Rules Tournament",
            game = GameType.FC_MOBILE,
            format = TournamentFormat.SINGLE_ELIMINATION,
            rules = rules
        )

        repository.createTournament(tournament)
        val fetched = repository.getTournament(tournament.id).first()

        assertEquals(3, fetched?.rules?.size)
        assertEquals("6-minute halves", fetched?.rules?.get(0))
        assertEquals("Screenshots required", fetched?.rules?.get(1))
        assertEquals("Fair play only", fetched?.rules?.get(2))
    }

    @Test
    fun testHostCanUpdateTournamentRules() = runBlocking {
        val tournament = Tournament(
            id = UUID.randomUUID().toString(),
            hostId = "host-user-123",
            name = "Dynamic Rules Tournament",
            game = GameType.EFOOTBALL,
            format = TournamentFormat.LEAGUE_GROUPS
        )

        repository.createTournament(tournament)
        val initial = repository.getTournament(tournament.id).first()
        assertTrue(initial?.rules?.isEmpty() == true)

        val newRules = listOf("Rule A: No lagging", "Rule B: Report disconnects within 5 mins")
        val result = repository.updateTournamentRules(tournament.id, newRules)
        assertTrue(result.isSuccess)

        val updated = repository.getTournament(tournament.id).first()
        assertEquals(2, updated?.rules?.size)
        assertEquals("Rule A: No lagging", updated?.rules?.get(0))
        assertEquals("Rule B: Report disconnects within 5 mins", updated?.rules?.get(1))
    }
}
