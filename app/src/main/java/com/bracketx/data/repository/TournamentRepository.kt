package com.bracketx.data.repository

import com.bracketx.domain.engine.TournamentEngine
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.Group
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.Registration
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentRole
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentStructure
import com.bracketx.domain.state.TournamentEvent
import com.bracketx.domain.state.TournamentStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface TournamentRepository {
    fun getTournaments(): Flow<List<Tournament>>
    fun getTournament(id: String): Flow<Tournament?>
    fun getParticipants(tournamentId: String): Flow<List<TournamentParticipant>>
    fun getRegistrations(tournamentId: String): Flow<List<Registration>>
    fun getMatches(tournamentId: String): Flow<List<Match>>
    fun getGroups(tournamentId: String): Flow<List<Group>>
    fun getStandings(tournamentId: String): Flow<List<Standing>>

    suspend fun createTournament(tournament: Tournament): Result<Tournament>
    suspend fun registerParticipant(registration: Registration): Result<TournamentParticipant>
    suspend fun openRegistration(tournamentId: String): Result<Tournament>
    suspend fun closeRegistration(tournamentId: String): Result<Tournament>
    suspend fun generateDraw(tournamentId: String, seed: Long = System.currentTimeMillis()): Result<TournamentStructure>
    suspend fun lockDraw(tournamentId: String): Result<Tournament>
    suspend fun submitScore(tournamentId: String, matchId: String, scoreA: Int, scoreB: Int, isOverride: Boolean = false): Result<List<Match>>
}

class InMemoryTournamentRepository : TournamentRepository {

    private val tournamentsFlow = MutableStateFlow<Map<String, Tournament>>(emptyMap())
    private val participantsFlow = MutableStateFlow<Map<String, List<TournamentParticipant>>>(emptyMap())
    private val registrationsFlow = MutableStateFlow<Map<String, List<Registration>>>(emptyMap())
    private val matchesFlow = MutableStateFlow<Map<String, List<Match>>>(emptyMap())
    private val groupsFlow = MutableStateFlow<Map<String, List<Group>>>(emptyMap())
    private val standingsFlow = MutableStateFlow<Map<String, List<Standing>>>(emptyMap())
    private val structuresFlow = MutableStateFlow<Map<String, TournamentStructure>>(emptyMap())

    override fun getTournaments(): Flow<List<Tournament>> = tournamentsFlow.map { it.values.toList() }

    override fun getTournament(id: String): Flow<Tournament?> = tournamentsFlow.map { it[id] }

    override fun getParticipants(tournamentId: String): Flow<List<TournamentParticipant>> =
        participantsFlow.map { it[tournamentId] ?: emptyList() }

    override fun getRegistrations(tournamentId: String): Flow<List<Registration>> =
        registrationsFlow.map { it[tournamentId] ?: emptyList() }

    override fun getMatches(tournamentId: String): Flow<List<Match>> =
        matchesFlow.map { it[tournamentId] ?: emptyList() }

    override fun getGroups(tournamentId: String): Flow<List<Group>> =
        groupsFlow.map { it[tournamentId] ?: emptyList() }

    override fun getStandings(tournamentId: String): Flow<List<Standing>> =
        standingsFlow.map { it[tournamentId] ?: emptyList() }

    override suspend fun createTournament(tournament: Tournament): Result<Tournament> {
        val updated = tournamentsFlow.value.toMutableMap()
        updated[tournament.id] = tournament
        tournamentsFlow.value = updated
        return Result.success(tournament)
    }

    override suspend fun registerParticipant(registration: Registration): Result<TournamentParticipant> {
        val tournament = tournamentsFlow.value[registration.tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        if (!tournament.registrationOpen) {
            return Result.failure(IllegalStateException("Registration is closed for this tournament"))
        }

        // Check duplicate registration
        val existingRegistrations = registrationsFlow.value[tournament.id] ?: emptyList()
        if (existingRegistrations.any { it.userId == registration.userId }) {
            return Result.failure(IllegalStateException("You are already registered for this tournament"))
        }

        // Validate metric bounds
        val minBound = tournament.settings.minMetric ?: tournament.game.defaultMinMetric
        val maxBound = tournament.settings.maxMetric ?: tournament.game.defaultMaxMetric
        if (registration.gameMetricValue < minBound || registration.gameMetricValue > maxBound) {
            return Result.failure(IllegalArgumentException("Metric must be between $minBound and $maxBound"))
        }

        // Update registrations
        val updatedRegMap = registrationsFlow.value.toMutableMap()
        updatedRegMap[tournament.id] = existingRegistrations + registration
        registrationsFlow.value = updatedRegMap

        // Create participant
        val participant = TournamentParticipant(
            id = UUID.randomUUID().toString(),
            tournamentId = tournament.id,
            userId = registration.userId,
            displayName = registration.name,
            inGameId = registration.inGameId,
            game = tournament.game,
            metricType = registration.gameMetricType,
            metricValue = registration.gameMetricValue
        )

        val updatedPartMap = participantsFlow.value.toMutableMap()
        val currentParticipants = updatedPartMap[tournament.id] ?: emptyList()
        updatedPartMap[tournament.id] = currentParticipants + participant
        participantsFlow.value = updatedPartMap

        return Result.success(participant)
    }

    override suspend fun openRegistration(tournamentId: String): Result<Tournament> {
        val tournament = tournamentsFlow.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        val nextStatus = TournamentStateMachine.handleEvent(tournament.status, TournamentEvent.OpenRegistration)
        val updated = tournament.copy(
            status = nextStatus,
            registrationOpen = true
        )
        val map = tournamentsFlow.value.toMutableMap()
        map[tournamentId] = updated
        tournamentsFlow.value = map
        return Result.success(updated)
    }

    override suspend fun closeRegistration(tournamentId: String): Result<Tournament> {
        val tournament = tournamentsFlow.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        val nextStatus = TournamentStateMachine.handleEvent(tournament.status, TournamentEvent.CloseRegistration)
        val updated = tournament.copy(
            status = nextStatus,
            registrationOpen = false
        )
        val map = tournamentsFlow.value.toMutableMap()
        map[tournamentId] = updated
        tournamentsFlow.value = map
        return Result.success(updated)
    }

    override suspend fun generateDraw(tournamentId: String, seed: Long): Result<TournamentStructure> {
        val tournament = tournamentsFlow.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        if (tournament.drawLocked) {
            return Result.failure(IllegalStateException("Cannot regenerate a locked draw"))
        }

        val participants = participantsFlow.value[tournamentId] ?: emptyList()
        if (participants.isEmpty()) {
            return Result.failure(IllegalStateException("No participants registered to generate draw"))
        }

        return try {
            val structure = TournamentEngine.generateTournament(
                tournament = tournament,
                participants = participants,
                config = DrawConfiguration(candidateCount = 100),
                seed = seed
            )

            // Update state
            val nextStatus = TournamentStateMachine.transition(tournament.status, TournamentStatus.DRAW_GENERATED)
            val updatedTournament = tournament.copy(status = nextStatus)

            val tMap = tournamentsFlow.value.toMutableMap()
            tMap[tournamentId] = updatedTournament
            tournamentsFlow.value = tMap

            val pMap = participantsFlow.value.toMutableMap()
            pMap[tournamentId] = structure.participants
            participantsFlow.value = pMap

            val mMap = matchesFlow.value.toMutableMap()
            mMap[tournamentId] = structure.matches
            matchesFlow.value = mMap

            val gMap = groupsFlow.value.toMutableMap()
            gMap[tournamentId] = structure.groups
            groupsFlow.value = gMap

            val sMap = standingsFlow.value.toMutableMap()
            sMap[tournamentId] = structure.standings
            standingsFlow.value = sMap

            val structMap = structuresFlow.value.toMutableMap()
            structMap[tournamentId] = structure
            structuresFlow.value = structMap

            Result.success(structure)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockDraw(tournamentId: String): Result<Tournament> {
        val tournament = tournamentsFlow.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        return try {
            val nextStatus = TournamentStateMachine.handleEvent(tournament.status, TournamentEvent.LockDraw)
            val updated = tournament.copy(
                status = nextStatus,
                drawLocked = true
            )
            val map = tournamentsFlow.value.toMutableMap()
            map[tournamentId] = updated
            tournamentsFlow.value = map
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitScore(
        tournamentId: String,
        matchId: String,
        scoreA: Int,
        scoreB: Int,
        isOverride: Boolean
    ): Result<List<Match>> {
        val tournament = tournamentsFlow.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        val currentMatches = matchesFlow.value[tournamentId] ?: emptyList()

        return try {
            val (updatedMatches, nextStatus) = TournamentEngine.submitScore(
                tournament = tournament,
                currentMatches = currentMatches,
                matchId = matchId,
                scoreA = scoreA,
                scoreB = scoreB,
                isHostOverride = isOverride
            )

            // Update matches
            val mMap = matchesFlow.value.toMutableMap()
            mMap[tournamentId] = updatedMatches
            matchesFlow.value = mMap

            // If tournament status advanced, update tournament
            if (tournament.status != nextStatus) {
                val tMap = tournamentsFlow.value.toMutableMap()
                tMap[tournamentId] = tournament.copy(status = nextStatus)
                tournamentsFlow.value = tMap
            }

            // Recalculate standings if group stage match
            val modifiedMatch = updatedMatches.firstOrNull { it.id == matchId }
            if (modifiedMatch?.groupId != null) {
                val group = groupsFlow.value[tournamentId]?.firstOrNull { it.id == modifiedMatch.groupId }
                if (group != null) {
                    val updatedStandings = TournamentEngine.recalculateGroupStandings(
                        tournament = tournament,
                        groupId = group.id,
                        participantIds = group.participantIds,
                        matches = updatedMatches
                    )
                    val sMap = standingsFlow.value.toMutableMap()
                    sMap[tournamentId] = updatedStandings
                    standingsFlow.value = sMap
                }
            }

            Result.success(updatedMatches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
