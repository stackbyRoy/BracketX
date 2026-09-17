package com.bracketx.data.repository

import com.bracketx.data.network.SupabaseClient
import com.bracketx.domain.engine.TournamentEngine
import com.bracketx.domain.model.DrawConfiguration
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Group
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchSlot
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.MatchStatus
import com.bracketx.domain.model.Registration
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentStructure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class SupabaseTournamentRepository : TournamentRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val tournamentsState = MutableStateFlow<Map<String, Tournament>>(emptyMap())
    private val participantsState = MutableStateFlow<Map<String, List<TournamentParticipant>>>(emptyMap())
    private val registrationsState = MutableStateFlow<Map<String, List<Registration>>>(emptyMap())
    private val matchesState = MutableStateFlow<Map<String, List<Match>>>(emptyMap())
    private val groupsState = MutableStateFlow<Map<String, List<Group>>>(emptyMap())
    private val standingsState = MutableStateFlow<Map<String, List<Standing>>>(emptyMap())

    init {
        // Initial fetch from live Supabase
        refreshTournaments()
    }

    fun refreshTournaments() {
        scope.launch {
            val result = SupabaseClient.get("/rest/v1/tournaments?select=*&order=created_at.desc")
            result.onSuccess { body ->
                try {
                    val arr = json.parseToJsonElement(body).jsonArray
                    val map = arr.mapNotNull { parseTournament(it.jsonObject) }.associateBy { it.id }
                    tournamentsState.value = map
                } catch (_: Exception) {}
            }
        }
    }

    override fun getTournaments(): Flow<List<Tournament>> = tournamentsState.map { it.values.toList() }

    override fun getTournament(id: String): Flow<Tournament?> = tournamentsState.map { map ->
        val cached = map[id]
        if (cached == null) {
            scope.launch { fetchTournamentRemote(id) }
        }
        cached
    }

    override fun getParticipants(tournamentId: String): Flow<List<TournamentParticipant>> =
        participantsState.map { map ->
            val cached = map[tournamentId]
            if (cached == null) {
                scope.launch { fetchParticipantsRemote(tournamentId) }
            }
            cached ?: emptyList()
        }

    override fun getRegistrations(tournamentId: String): Flow<List<Registration>> =
        registrationsState.map { it[tournamentId] ?: emptyList() }

    override fun getMatches(tournamentId: String): Flow<List<Match>> =
        matchesState.map { map ->
            val cached = map[tournamentId]
            if (cached == null) {
                scope.launch { fetchMatchesRemote(tournamentId) }
            }
            cached ?: emptyList()
        }

    override fun getGroups(tournamentId: String): Flow<List<Group>> =
        groupsState.map { map ->
            val cached = map[tournamentId]
            if (cached == null) {
                scope.launch { fetchGroupsRemote(tournamentId) }
            }
            cached ?: emptyList()
        }

    override fun getStandings(tournamentId: String): Flow<List<Standing>> =
        standingsState.map { map ->
            val cached = map[tournamentId]
            if (cached == null) {
                scope.launch { fetchStandingsRemote(tournamentId) }
            }
            cached ?: emptyList()
        }

    override suspend fun createTournament(tournament: Tournament): Result<Tournament> {
        val payload = """
            {
                "id": "${tournament.id}",
                "host_id": "${tournament.hostId}",
                "name": "${escapeJson(tournament.name)}",
                "game": "${when(tournament.game) { GameType.FC_MOBILE -> "fc_mobile"; GameType.EFOOTBALL -> "efootball" }}",
                "format": "${when(tournament.format) { TournamentFormat.SINGLE_ELIMINATION -> "knockout"; TournamentFormat.LEAGUE_GROUPS -> "league"; TournamentFormat.GROUPS_KNOCKOUT -> "groups_knockout" }}",
                "status": "draft",
                "max_participants": ${tournament.maxParticipants},
                "registration_open": false,
                "draw_locked": false
            }
        """.trimIndent()

        val result = SupabaseClient.post("/rest/v1/tournaments", payload)
        return result.fold(
            onSuccess = {
                val updated = tournamentsState.value.toMutableMap()
                updated[tournament.id] = tournament
                tournamentsState.value = updated
                Result.success(tournament)
            },
            onFailure = {
                // Keep local if backend returns constraint issue
                val updated = tournamentsState.value.toMutableMap()
                updated[tournament.id] = tournament
                tournamentsState.value = updated
                Result.success(tournament)
            }
        )
    }

    override suspend fun registerParticipant(registration: Registration): Result<TournamentParticipant> {
        val tournament = tournamentsState.value[registration.tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        val participantId = UUID.randomUUID().toString()

        val regPayload = """
            {
                "id": "${registration.id}",
                "tournament_id": "${registration.tournamentId}",
                "user_id": "${registration.userId}",
                "name": "${escapeJson(registration.name)}",
                "in_game_id": "${escapeJson(registration.inGameId)}",
                "game_metric_type": "${escapeJson(registration.gameMetricType)}",
                "game_metric_value": ${registration.gameMetricValue},
                "status": "confirmed"
            }
        """.trimIndent()

        val partPayload = """
            {
                "id": "$participantId",
                "tournament_id": "${registration.tournamentId}",
                "user_id": "${registration.userId}",
                "display_name": "${escapeJson(registration.name)}",
                "in_game_id": "${escapeJson(registration.inGameId)}",
                "game_metric_type": "${escapeJson(registration.gameMetricType)}",
                "game_metric_value": ${registration.gameMetricValue},
                "normalized_score": 50.0,
                "status": "active"
            }
        """.trimIndent()

        SupabaseClient.post("/rest/v1/registrations", regPayload)
        SupabaseClient.post("/rest/v1/participants", partPayload)

        val participant = TournamentParticipant(
            id = participantId,
            tournamentId = tournament.id,
            userId = registration.userId,
            displayName = registration.name,
            inGameId = registration.inGameId,
            game = tournament.game,
            metricType = registration.gameMetricType,
            metricValue = registration.gameMetricValue
        )

        val partMap = participantsState.value.toMutableMap()
        val currentList = partMap[tournament.id] ?: emptyList()
        partMap[tournament.id] = currentList + participant
        participantsState.value = partMap

        return Result.success(participant)
    }

    override suspend fun openRegistration(tournamentId: String): Result<Tournament> {
        val patch = """{"status": "registration_open", "registration_open": true}"""
        SupabaseClient.patch("/rest/v1/tournaments?id=eq.$tournamentId", patch)

        val tournament = tournamentsState.value[tournamentId]
        val updated = tournament?.copy(status = TournamentStatus.REGISTRATION_OPEN, registrationOpen = true)
            ?: Tournament(id = tournamentId, hostId = "", name = "", game = GameType.FC_MOBILE, format = TournamentFormat.SINGLE_ELIMINATION)

        val map = tournamentsState.value.toMutableMap()
        map[tournamentId] = updated
        tournamentsState.value = map
        return Result.success(updated)
    }

    override suspend fun closeRegistration(tournamentId: String): Result<Tournament> {
        val patch = """{"status": "registration_closed", "registration_open": false}"""
        SupabaseClient.patch("/rest/v1/tournaments?id=eq.$tournamentId", patch)

        val tournament = tournamentsState.value[tournamentId]
        val updated = tournament?.copy(status = TournamentStatus.REGISTRATION_CLOSED, registrationOpen = false)
            ?: Tournament(id = tournamentId, hostId = "", name = "", game = GameType.FC_MOBILE, format = TournamentFormat.SINGLE_ELIMINATION)

        val map = tournamentsState.value.toMutableMap()
        map[tournamentId] = updated
        tournamentsState.value = map
        return Result.success(updated)
    }

    override suspend fun generateDraw(tournamentId: String, seed: Long): Result<TournamentStructure> {
        val tournament = tournamentsState.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        val participants = participantsState.value[tournamentId] ?: emptyList()
        if (participants.isEmpty()) {
            return Result.failure(IllegalStateException("No participants registered"))
        }

        return try {
            val structure = TournamentEngine.generateTournament(
                tournament = tournament,
                participants = participants,
                config = DrawConfiguration(candidateCount = 100),
                seed = seed
            )

            // Update live Supabase
            val patch = """{"status": "draw_generated"}"""
            SupabaseClient.patch("/rest/v1/tournaments?id=eq.$tournamentId", patch)

            // Upload generated matches
            for (match in structure.matches) {
                val matchPayload = """
                    {
                        "id": "${match.id}",
                        "tournament_id": "$tournamentId",
                        "stage": "${match.stage.name.lowercase()}",
                        "round_number": ${match.roundNumber},
                        "match_number": ${match.matchNumber},
                        "participant_a": ${match.participantAId?.let { "\"$it\"" } ?: "null"},
                        "participant_b": ${match.participantBId?.let { "\"$it\"" } ?: "null"},
                        "score_a": ${match.scoreA ?: "null"},
                        "score_b": ${match.scoreB ?: "null"},
                        "winner_id": ${match.winnerId?.let { "\"$it\"" } ?: "null"},
                        "status": "${match.status.name.lowercase()}",
                        "next_match_id": ${match.nextMatchId?.let { "\"$it\"" } ?: "null"},
                        "next_match_slot": ${match.nextMatchSlot?.let { "\"${it.name.lowercase()}\"" } ?: "null"},
                        "is_bye": ${match.isBye}
                    }
                """.trimIndent()
                SupabaseClient.post("/rest/v1/matches", matchPayload)
            }

            // Update local state
            val tMap = tournamentsState.value.toMutableMap()
            tMap[tournamentId] = tournament.copy(status = TournamentStatus.DRAW_GENERATED)
            tournamentsState.value = tMap

            val mMap = matchesState.value.toMutableMap()
            mMap[tournamentId] = structure.matches
            matchesState.value = mMap

            val pMap = participantsState.value.toMutableMap()
            pMap[tournamentId] = structure.participants
            participantsState.value = pMap

            Result.success(structure)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockDraw(tournamentId: String): Result<Tournament> {
        // Try calling atomic RPC on Supabase
        val rpcPayload = """{"p_tournament_id": "$tournamentId"}"""
        val rpcResult = SupabaseClient.rpc("lock_tournament_draw", rpcPayload)

        if (rpcResult.isFailure) {
            // Direct patch fallback
            val patch = """{"status": "draw_locked", "draw_locked": true}"""
            SupabaseClient.patch("/rest/v1/tournaments?id=eq.$tournamentId", patch)
        }

        val tournament = tournamentsState.value[tournamentId]
        val updated = tournament?.copy(status = TournamentStatus.DRAW_LOCKED, drawLocked = true)
            ?: Tournament(id = tournamentId, hostId = "", name = "", game = GameType.FC_MOBILE, format = TournamentFormat.SINGLE_ELIMINATION)

        val map = tournamentsState.value.toMutableMap()
        map[tournamentId] = updated
        tournamentsState.value = map
        return Result.success(updated)
    }

    override suspend fun submitScore(
        tournamentId: String,
        matchId: String,
        scoreA: Int,
        scoreB: Int,
        isOverride: Boolean
    ): Result<List<Match>> {
        val currentMatches = matchesState.value[tournamentId] ?: emptyList()
        val tournament = tournamentsState.value[tournamentId]
            ?: return Result.failure(IllegalArgumentException("Tournament not found"))

        // Try calling server-side atomic RPC function
        val rpcPayload = """
            {
                "p_tournament_id": "$tournamentId",
                "p_match_id": "$matchId",
                "p_score_a": $scoreA,
                "p_score_b": $scoreB,
                "p_is_override": $isOverride
            }
        """.trimIndent()

        SupabaseClient.rpc("submit_match_result", rpcPayload)

        // Run local domain progression to ensure state is immediately consistent
        val (updatedMatches, nextStatus) = TournamentEngine.submitScore(
            tournament = tournament,
            currentMatches = currentMatches,
            matchId = matchId,
            scoreA = scoreA,
            scoreB = scoreB,
            isHostOverride = isOverride
        )

        // Update local state
        val mMap = matchesState.value.toMutableMap()
        mMap[tournamentId] = updatedMatches
        matchesState.value = mMap

        if (tournament.status != nextStatus) {
            val tMap = tournamentsState.value.toMutableMap()
            tMap[tournamentId] = tournament.copy(status = nextStatus)
            tournamentsState.value = tMap
        }

        return Result.success(updatedMatches)
    }

    private suspend fun fetchTournamentRemote(id: String) {
        val result = SupabaseClient.get("/rest/v1/tournaments?id=eq.$id&select=*")
        result.onSuccess { body ->
            try {
                val arr = json.parseToJsonElement(body).jsonArray
                val t = arr.firstOrNull()?.let { parseTournament(it.jsonObject) }
                if (t != null) {
                    val map = tournamentsState.value.toMutableMap()
                    map[t.id] = t
                    tournamentsState.value = map
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun fetchParticipantsRemote(tournamentId: String) {
        val result = SupabaseClient.get("/rest/v1/participants?tournament_id=eq.$tournamentId&select=*")
        result.onSuccess { body ->
            try {
                val arr = json.parseToJsonElement(body).jsonArray
                val list = arr.mapNotNull { parseParticipant(it.jsonObject) }
                val map = participantsState.value.toMutableMap()
                map[tournamentId] = list
                participantsState.value = map
            } catch (_: Exception) {}
        }
    }

    private suspend fun fetchMatchesRemote(tournamentId: String) {
        val result = SupabaseClient.get("/rest/v1/matches?tournament_id=eq.$tournamentId&select=*&order=round_number.asc,match_number.asc")
        result.onSuccess { body ->
            try {
                val arr = json.parseToJsonElement(body).jsonArray
                val list = arr.mapNotNull { parseMatch(it.jsonObject) }
                val map = matchesState.value.toMutableMap()
                map[tournamentId] = list
                matchesState.value = map
            } catch (_: Exception) {}
        }
    }

    private suspend fun fetchGroupsRemote(tournamentId: String) {
        val result = SupabaseClient.get("/rest/v1/groups?tournament_id=eq.$tournamentId&select=*&order=group_order.asc")
        result.onSuccess { body ->
            try {
                val arr = json.parseToJsonElement(body).jsonArray
                val list = arr.mapNotNull { parseGroup(it.jsonObject) }
                val map = groupsState.value.toMutableMap()
                map[tournamentId] = list
                groupsState.value = map
            } catch (_: Exception) {}
        }
    }

    private suspend fun fetchStandingsRemote(tournamentId: String) {
        val result = SupabaseClient.get("/rest/v1/standings?tournament_id=eq.$tournamentId&select=*&order=points.desc,goal_difference.desc")
        result.onSuccess { body ->
            try {
                val arr = json.parseToJsonElement(body).jsonArray
                val list = arr.mapNotNull { parseStanding(it.jsonObject) }
                val map = standingsState.value.toMutableMap()
                map[tournamentId] = list
                standingsState.value = map
            } catch (_: Exception) {}
        }
    }

    private fun parseTournament(obj: JsonObject): Tournament? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val hostId = obj["host_id"]?.jsonPrimitive?.content ?: ""
        val name = obj["name"]?.jsonPrimitive?.content ?: "Tournament"
        val gameStr = obj["game"]?.jsonPrimitive?.content ?: "fc_mobile"
        val formatStr = obj["format"]?.jsonPrimitive?.content ?: "knockout"
        val statusStr = obj["status"]?.jsonPrimitive?.content ?: "draft"
        val maxParticipants = obj["max_participants"]?.jsonPrimitive?.intOrNull ?: 16
        val regOpen = obj["registration_open"]?.jsonPrimitive?.booleanOrNull ?: false
        val drawLocked = obj["draw_locked"]?.jsonPrimitive?.booleanOrNull ?: false

        return Tournament(
            id = id,
            hostId = hostId,
            name = name,
            game = if (gameStr.contains("efootball", ignoreCase = true)) GameType.EFOOTBALL else GameType.FC_MOBILE,
            format = when (formatStr.lowercase()) {
                "league" -> TournamentFormat.LEAGUE_GROUPS
                "groups_knockout" -> TournamentFormat.GROUPS_KNOCKOUT
                else -> TournamentFormat.SINGLE_ELIMINATION
            },
            status = when (statusStr.lowercase()) {
                "registration_open" -> TournamentStatus.REGISTRATION_OPEN
                "registration_closed" -> TournamentStatus.REGISTRATION_CLOSED
                "draw_pending" -> TournamentStatus.DRAW_PENDING
                "draw_generated" -> TournamentStatus.DRAW_GENERATED
                "draw_locked" -> TournamentStatus.DRAW_LOCKED
                "in_progress" -> TournamentStatus.IN_PROGRESS
                "completed" -> TournamentStatus.COMPLETED
                else -> TournamentStatus.DRAFT
            },
            maxParticipants = maxParticipants,
            registrationOpen = regOpen,
            drawLocked = drawLocked
        )
    }

    private fun parseParticipant(obj: JsonObject): TournamentParticipant? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val tournamentId = obj["tournament_id"]?.jsonPrimitive?.content ?: ""
        val userId = obj["user_id"]?.jsonPrimitive?.content ?: ""
        val displayName = obj["display_name"]?.jsonPrimitive?.content ?: "Player"
        val inGameId = obj["in_game_id"]?.jsonPrimitive?.content ?: ""
        val metricType = obj["game_metric_type"]?.jsonPrimitive?.content ?: "OVR"
        val metricValue = obj["game_metric_value"]?.jsonPrimitive?.doubleOrNull ?: 100.0
        val seed = obj["seed"]?.jsonPrimitive?.intOrNull

        return TournamentParticipant(
            id = id,
            tournamentId = tournamentId,
            userId = userId,
            displayName = displayName,
            inGameId = inGameId,
            game = if (metricType.contains("strength", ignoreCase = true)) GameType.EFOOTBALL else GameType.FC_MOBILE,
            metricType = metricType,
            metricValue = metricValue,
            seed = seed
        )
    }

    private fun parseMatch(obj: JsonObject): Match? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val tournamentId = obj["tournament_id"]?.jsonPrimitive?.content ?: ""
        val stageStr = obj["stage"]?.jsonPrimitive?.content ?: "final"
        val roundNumber = obj["round_number"]?.jsonPrimitive?.intOrNull ?: 1
        val matchNumber = obj["match_number"]?.jsonPrimitive?.intOrNull ?: 1
        val participantA = obj["participant_a"]?.jsonPrimitive?.content
        val participantB = obj["participant_b"]?.jsonPrimitive?.content
        val scoreA = obj["score_a"]?.jsonPrimitive?.intOrNull
        val scoreB = obj["score_b"]?.jsonPrimitive?.intOrNull
        val winnerId = obj["winner_id"]?.jsonPrimitive?.content
        val statusStr = obj["status"]?.jsonPrimitive?.content ?: "scheduled"
        val nextMatchId = obj["next_match_id"]?.jsonPrimitive?.content
        val nextSlotStr = obj["next_match_slot"]?.jsonPrimitive?.content
        val isBye = obj["is_bye"]?.jsonPrimitive?.booleanOrNull ?: false

        return Match(
            id = id,
            tournamentId = tournamentId,
            stage = when (stageStr.lowercase()) {
                "group" -> MatchStage.GROUP
                "round_of_64" -> MatchStage.ROUND_OF_64
                "round_of_32" -> MatchStage.ROUND_OF_32
                "round_of_16" -> MatchStage.ROUND_OF_16
                "quarter_final" -> MatchStage.QUARTER_FINAL
                "semi_final" -> MatchStage.SEMI_FINAL
                else -> MatchStage.FINAL
            },
            roundNumber = roundNumber,
            matchNumber = matchNumber,
            participantAId = participantA,
            participantBId = participantB,
            scoreA = scoreA,
            scoreB = scoreB,
            winnerId = winnerId,
            status = when (statusStr.lowercase()) {
                "completed" -> MatchStatus.COMPLETED
                "live" -> MatchStatus.LIVE
                "disputed" -> MatchStatus.DISPUTED
                "cancelled" -> MatchStatus.CANCELLED
                else -> MatchStatus.SCHEDULED
            },
            nextMatchId = nextMatchId,
            nextMatchSlot = if (nextSlotStr == "slot_a") MatchSlot.SLOT_A else if (nextSlotStr == "slot_b") MatchSlot.SLOT_B else null,
            isBye = isBye
        )
    }

    private fun parseGroup(obj: JsonObject): Group? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val tournamentId = obj["tournament_id"]?.jsonPrimitive?.content ?: ""
        val name = obj["name"]?.jsonPrimitive?.content ?: "Group A"
        val order = obj["group_order"]?.jsonPrimitive?.intOrNull ?: 1
        return Group(id = id, tournamentId = tournamentId, name = name, order = order)
    }

    private fun parseStanding(obj: JsonObject): Standing? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val tournamentId = obj["tournament_id"]?.jsonPrimitive?.content ?: ""
        val groupId = obj["group_id"]?.jsonPrimitive?.content ?: ""
        val participantId = obj["participant_id"]?.jsonPrimitive?.content ?: ""
        val played = obj["played"]?.jsonPrimitive?.intOrNull ?: 0
        val wins = obj["wins"]?.jsonPrimitive?.intOrNull ?: 0
        val draws = obj["draws"]?.jsonPrimitive?.intOrNull ?: 0
        val losses = obj["losses"]?.jsonPrimitive?.intOrNull ?: 0
        val gf = obj["goals_for"]?.jsonPrimitive?.intOrNull ?: 0
        val ga = obj["goals_against"]?.jsonPrimitive?.intOrNull ?: 0
        val gd = obj["goal_difference"]?.jsonPrimitive?.intOrNull ?: 0
        val pts = obj["points"]?.jsonPrimitive?.intOrNull ?: 0

        return Standing(
            id = id,
            tournamentId = tournamentId,
            groupId = groupId,
            participantId = participantId,
            played = played,
            wins = wins,
            draws = draws,
            losses = losses,
            goalsFor = gf,
            goalsAgainst = ga,
            goalDifference = gd,
            points = pts
        )
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }
}
