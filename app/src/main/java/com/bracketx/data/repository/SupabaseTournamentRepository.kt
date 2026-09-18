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
import com.bracketx.domain.model.TournamentVisibility
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
import java.security.MessageDigest
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
    private val hostAccessCodes = mutableMapOf<String, String>()
    private val grantedAccessState = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        // Initial fetch from live Supabase
        refreshTournaments()
    }

    override fun refreshTournaments(userId: String?) {
        scope.launch {
            val result = if (!userId.isNullOrBlank()) {
                val payload = """{"p_user_id": "$userId"}"""
                SupabaseClient.rpc("get_dashboard_tournaments", payload)
            } else {
                SupabaseClient.get("/rest/v1/tournaments?select=*&order=created_at.desc")
            }

            result.onSuccess { body ->
                try {
                    val arr = json.parseToJsonElement(body).jsonArray
                    val remoteMap = arr.mapNotNull { parseTournament(it.jsonObject) }.associateBy { it.id }
                    // Merge with current state so locally cached host tournaments are preserved
                    val merged = tournamentsState.value.toMutableMap()
                    merged.putAll(remoteMap)
                    tournamentsState.value = merged
                } catch (_: Exception) {}
            }.onFailure {
                if (!userId.isNullOrBlank()) {
                    val fallback = SupabaseClient.get("/rest/v1/tournaments?select=*&order=created_at.desc")
                    fallback.onSuccess { body ->
                        try {
                            val arr = json.parseToJsonElement(body).jsonArray
                            val remoteMap = arr.mapNotNull { parseTournament(it.jsonObject) }.associateBy { it.id }
                            val merged = tournamentsState.value.toMutableMap()
                            merged.putAll(remoteMap)
                            tournamentsState.value = merged
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    override fun getTournaments(): Flow<List<Tournament>> = tournamentsState.map { it.values.toList() }

    override fun getTournament(id: String): Flow<Tournament?> = tournamentsState.map { map ->
        val cached = map[id] ?: map.values.firstOrNull { it.publicId.equals(id, ignoreCase = true) }
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

    override fun getHostAccessCode(tournamentId: String): String? = hostAccessCodes[tournamentId]

    override fun saveHostAccessCode(tournamentId: String, code: String) {
        hostAccessCodes[tournamentId] = code
    }

    override suspend fun createTournament(tournament: Tournament, privateAccessCode: String?): Result<Tournament> {
        val vis = tournament.visibility.name.lowercase()
        val codeClean = privateAccessCode?.trim()?.uppercase()
        val codeHash = if (tournament.visibility == TournamentVisibility.PRIVATE && !codeClean.isNullOrBlank()) {
            hashSha256(codeClean)
        } else {
            null
        }

        val payload = """
            {
                "id": "${tournament.id}",
                "host_id": "${tournament.hostId}",
                "name": "${escapeJson(tournament.name)}",
                "game": "${when(tournament.game) { GameType.FC_MOBILE -> "fc_mobile"; GameType.EFOOTBALL -> "efootball" }}",
                "format": "${when(tournament.format) { TournamentFormat.SINGLE_ELIMINATION -> "knockout"; TournamentFormat.LEAGUE_GROUPS -> "league"; TournamentFormat.GROUPS_KNOCKOUT -> "groups_knockout" }}",
                "status": "draft",
                "visibility": "$vis",
                ${if (codeHash != null) "\"private_access_code_hash\": \"$codeHash\"," else ""}
                "rules": [${tournament.rules.joinToString(",") { "\"${escapeJson(it)}\"" }}],
                "max_participants": ${tournament.maxParticipants},
                "registration_open": false,
                "draw_locked": false
            }
        """.trimIndent()

        if (tournament.visibility == TournamentVisibility.PRIVATE && !codeClean.isNullOrBlank()) {
            hostAccessCodes[tournament.id] = codeClean
        }

        val result = SupabaseClient.post("/rest/v1/tournaments", payload)
        return result.fold(
            onSuccess = { body ->
                var created = tournament
                try {
                    val arr = json.parseToJsonElement(body).jsonArray
                    val first = arr.firstOrNull()?.jsonObject
                    if (first != null) {
                        parseTournament(first)?.let { created = it }
                    }
                } catch (_: Exception) {}

                val updated = tournamentsState.value.toMutableMap()
                updated[created.id] = created
                tournamentsState.value = updated
                Result.success(created)
            },
            onFailure = {
                val fallbackPublicId = if (tournament.publicId.isBlank()) "BRX-" + UUID.randomUUID().toString().take(6).uppercase() else tournament.publicId
                val created = tournament.copy(publicId = fallbackPublicId)
                val updated = tournamentsState.value.toMutableMap()
                updated[created.id] = created
                tournamentsState.value = updated
                Result.success(created)
            }
        )
    }

    override suspend fun searchTournamentByPublicId(publicId: String): Result<Tournament?> {
        val clean = publicId.trim().uppercase()
        // Check local cache first
        val cached = tournamentsState.value.values.firstOrNull { it.publicId.equals(clean, ignoreCase = true) }
        if (cached != null) {
            return Result.success(cached)
        }

        val payload = """{"p_public_id": "${escapeJson(clean)}"}"""
        val result = SupabaseClient.rpc("resolve_tournament_by_public_id", payload)
        return result.fold(
            onSuccess = { body ->
                try {
                    val obj = json.parseToJsonElement(body).jsonObject
                    val found = obj["found"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (!found) {
                        return@fold Result.success(null)
                    }

                    val id = obj["id"]?.jsonPrimitive?.content ?: return@fold Result.success(null)
                    val hostId = obj["host_id"]?.jsonPrimitive?.content ?: ""
                    val name = obj["name"]?.jsonPrimitive?.content ?: "Tournament"
                    val gameStr = obj["game"]?.jsonPrimitive?.content ?: "fc_mobile"
                    val formatStr = obj["format"]?.jsonPrimitive?.content ?: "knockout"
                    val statusStr = obj["status"]?.jsonPrimitive?.content ?: "draft"
                    val maxParticipants = obj["max_participants"]?.jsonPrimitive?.intOrNull ?: 16
                    val regOpen = obj["registration_open"]?.jsonPrimitive?.booleanOrNull ?: false
                    val drawLocked = obj["draw_locked"]?.jsonPrimitive?.booleanOrNull ?: false
                    val visStr = obj["visibility"]?.jsonPrimitive?.content ?: "public"
                    val pubId = obj["public_id"]?.jsonPrimitive?.content ?: clean
                    val isAuth = obj["is_authorized"]?.jsonPrimitive?.booleanOrNull ?: false

                    if (isAuth) {
                        grantedAccessState.value = grantedAccessState.value + (id to true)
                    }

                    val tournament = Tournament(
                        id = id,
                        publicId = pubId,
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
                        visibility = TournamentVisibility.fromString(visStr),
                        maxParticipants = maxParticipants,
                        registrationOpen = regOpen,
                        drawLocked = drawLocked
                    )

                    val map = tournamentsState.value.toMutableMap()
                    map[id] = tournament
                    tournamentsState.value = map

                    Result.success(tournament)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { err -> Result.failure(err) }
        )
    }

    override suspend fun verifyPrivateAccess(tournamentId: String, accessCode: String): Result<Boolean> {
        val payload = """{"p_tournament_id": "$tournamentId", "p_access_code": "${escapeJson(accessCode.trim())}"}"""
        val result = SupabaseClient.rpc("verify_private_access", payload)
        return result.fold(
            onSuccess = {
                grantedAccessState.value = grantedAccessState.value + (tournamentId to true)
                Result.success(true)
            },
            onFailure = {
                Result.failure(IllegalArgumentException("Invalid access code"))
            }
        )
    }

    override fun hasPrivateAccess(tournamentId: String): Flow<Boolean> {
        return grantedAccessState.map { it[tournamentId] == true }
    }

    override suspend fun updateTournamentVisibility(
        tournamentId: String,
        visibility: TournamentVisibility,
        accessCode: String?
    ): Result<Tournament> {
        val codeClean = accessCode?.trim()?.uppercase()
        val codeParam = if (visibility == TournamentVisibility.PRIVATE && !codeClean.isNullOrBlank()) {
            "\"${escapeJson(codeClean)}\""
        } else "null"
        val payload = """{"p_tournament_id": "$tournamentId", "p_visibility": "${visibility.name.lowercase()}", "p_access_code": $codeParam}"""
        val result = SupabaseClient.rpc("update_tournament_visibility", payload)

        return result.fold(
            onSuccess = {
                if (visibility == TournamentVisibility.PRIVATE && !codeClean.isNullOrBlank()) {
                    hostAccessCodes[tournamentId] = codeClean
                } else if (visibility == TournamentVisibility.PUBLIC) {
                    hostAccessCodes.remove(tournamentId)
                }
                val current = tournamentsState.value[tournamentId]
                if (current != null) {
                    val updated = current.copy(visibility = visibility)
                    val map = tournamentsState.value.toMutableMap()
                    map[tournamentId] = updated
                    tournamentsState.value = map
                    Result.success(updated)
                } else {
                    Result.success(Tournament(id = tournamentId, hostId = "", name = "", game = GameType.FC_MOBILE, format = TournamentFormat.SINGLE_ELIMINATION, visibility = visibility))
                }
            },
            onFailure = { err -> Result.failure(err) }
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

            // Insert groups if groups format
            if (structure.groups.isNotEmpty()) {
                val groupArray = structure.groups.joinToString(",") { g ->
                    """{"id": "${g.id}", "tournament_id": "$tournamentId", "name": "${escapeJson(g.name)}", "group_order": ${g.order}}"""
                }
                SupabaseClient.post("/rest/v1/groups", "[$groupArray]")

                // Insert group members
                val membersList = mutableListOf<String>()
                structure.groups.forEach { g ->
                    g.participantIds.forEach { pId ->
                        membersList.add("""{"id": "${UUID.randomUUID()}", "group_id": "${g.id}", "participant_id": "$pId"}""")
                    }
                }
                if (membersList.isNotEmpty()) {
                    SupabaseClient.post("/rest/v1/group_members", "[${membersList.joinToString(",")}]")
                }
            }

            // Insert standings
            if (structure.standings.isNotEmpty()) {
                val standingsArray = structure.standings.joinToString(",") { s ->
                    """{
                        "id": "${s.id}",
                        "tournament_id": "$tournamentId",
                        "group_id": "${s.groupId}",
                        "participant_id": "${s.participantId}",
                        "played": ${s.played},
                        "wins": ${s.wins},
                        "draws": ${s.draws},
                        "losses": ${s.losses},
                        "goals_for": ${s.goalsFor},
                        "goals_against": ${s.goalsAgainst},
                        "goal_difference": ${s.goalDifference},
                        "points": ${s.points}
                    }"""
                }
                SupabaseClient.post("/rest/v1/standings", "[$standingsArray]")
            }

            // Insert matches
            if (structure.matches.isNotEmpty()) {
                val matchesArray = structure.matches.joinToString(",") { m ->
                    val stageStr = when(m.stage) {
                        MatchStage.GROUP -> "group"
                        MatchStage.ROUND_OF_64 -> "round_of_64"
                        MatchStage.ROUND_OF_32 -> "round_of_32"
                        MatchStage.ROUND_OF_16 -> "round_of_16"
                        MatchStage.QUARTER_FINAL -> "quarter_final"
                        MatchStage.SEMI_FINAL -> "semi_final"
                        MatchStage.FINAL -> "final"
                    }
                    val nextSlot = when(m.nextMatchSlot) {
                        MatchSlot.SLOT_A -> "\"slot_a\""
                        MatchSlot.SLOT_B -> "\"slot_b\""
                        null -> "null"
                    }
                    val pA = if (m.participantAId != null) "\"${m.participantAId}\"" else "null"
                    val pB = if (m.participantBId != null) "\"${m.participantBId}\"" else "null"
                    val gId = if (m.groupId != null) "\"${m.groupId}\"" else "null"
                    val nextMId = if (m.nextMatchId != null) "\"${m.nextMatchId}\"" else "null"

                    """{
                        "id": "${m.id}",
                        "tournament_id": "$tournamentId",
                        "stage": "$stageStr",
                        "round_number": ${m.roundNumber},
                        "match_number": ${m.matchNumber},
                        "group_id": $gId,
                        "participant_a": $pA,
                        "participant_b": $pB,
                        "score_a": null,
                        "score_b": null,
                        "winner_id": null,
                        "status": "scheduled",
                        "next_match_id": $nextMId,
                        "next_match_slot": $nextSlot,
                        "is_bye": ${m.isBye}
                    }"""
                }
                SupabaseClient.post("/rest/v1/matches", "[$matchesArray]")
            }

            // Update local state
            val tMap = tournamentsState.value.toMutableMap()
            tMap[tournamentId] = tournament.copy(status = TournamentStatus.DRAW_GENERATED)
            tournamentsState.value = tMap

            val pMap = participantsState.value.toMutableMap()
            pMap[tournamentId] = structure.participants
            participantsState.value = pMap

            val mMap = matchesState.value.toMutableMap()
            mMap[tournamentId] = structure.matches
            matchesState.value = mMap

            val gMap = groupsState.value.toMutableMap()
            gMap[tournamentId] = structure.groups
            groupsState.value = gMap

            val sMap = standingsState.value.toMutableMap()
            sMap[tournamentId] = structure.standings
            standingsState.value = sMap

            Result.success(structure)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockDraw(tournamentId: String): Result<Tournament> {
        val result = SupabaseClient.rpc("lock_tournament_draw", """{"p_tournament_id": "$tournamentId"}""")

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

        // Local state update
        val currentMatches = matchesState.value[tournamentId] ?: emptyList()
        val tournament = tournamentsState.value[tournamentId]
            ?: Tournament(id = tournamentId, hostId = "", name = "", game = GameType.FC_MOBILE, format = TournamentFormat.SINGLE_ELIMINATION)

        val (updatedMatches, nextStatus) = TournamentEngine.submitScore(
            tournament = tournament,
            currentMatches = currentMatches,
            matchId = matchId,
            scoreA = scoreA,
            scoreB = scoreB,
            isHostOverride = isOverride
        )

        val mMap = matchesState.value.toMutableMap()
        mMap[tournamentId] = updatedMatches
        matchesState.value = mMap

        if (tournament.status != nextStatus) {
            val tMap = tournamentsState.value.toMutableMap()
            tMap[tournamentId] = tournament.copy(status = nextStatus)
            tournamentsState.value = tMap
        }

        // Recalculate standings locally
        val modifiedMatch = updatedMatches.firstOrNull { it.id == matchId }
        if (modifiedMatch?.groupId != null) {
            val group = groupsState.value[tournamentId]?.firstOrNull { it.id == modifiedMatch.groupId }
            if (group != null) {
                val updatedStandings = TournamentEngine.recalculateGroupStandings(
                    tournament = tournament,
                    groupId = group.id,
                    participantIds = group.participantIds,
                    matches = updatedMatches
                )
                val sMap = standingsState.value.toMutableMap()
                sMap[tournamentId] = updatedStandings
                standingsState.value = sMap
            }
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
                } else {
                    // Try looking up by public_id if not found by UUID
                    searchTournamentByPublicId(id)
                }
            } catch (_: Exception) {}
        }.onFailure {
            searchTournamentByPublicId(id)
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
        val publicId = obj["public_id"]?.jsonPrimitive?.content ?: ""
        val hostId = obj["host_id"]?.jsonPrimitive?.content ?: ""
        val name = obj["name"]?.jsonPrimitive?.content ?: "Tournament"
        val gameStr = obj["game"]?.jsonPrimitive?.content ?: "fc_mobile"
        val formatStr = obj["format"]?.jsonPrimitive?.content ?: "knockout"
        val statusStr = obj["status"]?.jsonPrimitive?.content ?: "draft"
        val visibilityStr = obj["visibility"]?.jsonPrimitive?.content ?: "public"
        val maxParticipants = obj["max_participants"]?.jsonPrimitive?.intOrNull ?: 16
        val regOpen = obj["registration_open"]?.jsonPrimitive?.booleanOrNull ?: false
        val drawLocked = obj["draw_locked"]?.jsonPrimitive?.booleanOrNull ?: false
        val rules = try {
            obj["rules"]?.jsonArray?.mapNotNull {
                try { it.jsonPrimitive.content } catch (_: Exception) { null }
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return Tournament(
            id = id,
            publicId = publicId,
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
            visibility = TournamentVisibility.fromString(visibilityStr),
            maxParticipants = maxParticipants,
            registrationOpen = regOpen,
            drawLocked = drawLocked,
            rules = rules
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

    private fun hashSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    override suspend fun deleteTournament(tournamentId: String, hostId: String): Result<Unit> {
        val payload = """{"p_tournament_id": "$tournamentId", "p_host_id": "$hostId"}"""
        val rpcResult = SupabaseClient.rpc("delete_tournament_as_host", payload)

        return rpcResult.fold(
            onSuccess = {
                val current = tournamentsState.value.toMutableMap()
                current.remove(tournamentId)
                tournamentsState.value = current
                Result.success(Unit)
            },
            onFailure = { err ->
                // Direct REST DELETE fallback
                val directResult = SupabaseClient.delete("/rest/v1/tournaments?id=eq.$tournamentId")
                if (directResult.isSuccess) {
                    val current = tournamentsState.value.toMutableMap()
                    current.remove(tournamentId)
                    tournamentsState.value = current
                    Result.success(Unit)
                } else {
                    Result.failure(err)
                }
            }
        )
    }

    override suspend fun updateTournamentRules(tournamentId: String, rules: List<String>): Result<Tournament> {
        val rulesJson = rules.joinToString(",") { "\"${escapeJson(it)}\"" }
        val payload = """{"rules": [$rulesJson]}"""
        val result = SupabaseClient.patch("/rest/v1/tournaments?id=eq.$tournamentId", payload)

        return result.fold(
            onSuccess = { body ->
                var updated: Tournament? = null
                try {
                    val arr = json.parseToJsonElement(body).jsonArray
                    val first = arr.firstOrNull()?.jsonObject
                    if (first != null) {
                        parseTournament(first)?.let { updated = it }
                    }
                } catch (_: Exception) {}

                val current = tournamentsState.value.toMutableMap()
                val finalTourney = updated ?: current[tournamentId]?.copy(rules = rules)
                if (finalTourney != null) {
                    current[tournamentId] = finalTourney
                    tournamentsState.value = current
                    Result.success(finalTourney)
                } else {
                    Result.failure(Exception("Failed to update tournament rules"))
                }
            },
            onFailure = { err ->
                val current = tournamentsState.value.toMutableMap()
                val existing = current[tournamentId]
                if (existing != null) {
                    val updated = existing.copy(rules = rules)
                    current[tournamentId] = updated
                    tournamentsState.value = current
                    Result.success(updated)
                } else {
                    Result.failure(err)
                }
            }
        )
    }
}
