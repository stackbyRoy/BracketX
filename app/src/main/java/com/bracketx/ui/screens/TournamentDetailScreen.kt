package com.bracketx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.domain.model.FairnessScore
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.Registration
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.ui.components.BandBadge
import com.bracketx.ui.components.BracketView
import com.bracketx.ui.components.DrawPreviewDialog
import com.bracketx.ui.components.GameBadge
import com.bracketx.ui.components.RegistrationDialog
import com.bracketx.ui.components.ScoreEntryDialog
import com.bracketx.ui.components.StandingsTable
import com.bracketx.ui.components.StatusBadge
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onNavigateBack: () -> Unit
) {
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()
    val tournamentFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.getTournament(tournamentId) }
    val tournament by tournamentFlow.collectAsState(initial = null)

    val participantsFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.getParticipants(tournamentId) }
    val participants by participantsFlow.collectAsState(initial = emptyList())

    val matchesFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.getMatches(tournamentId) }
    val matches by matchesFlow.collectAsState(initial = emptyList())

    val groupsFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.getGroups(tournamentId) }
    val groups by groupsFlow.collectAsState(initial = emptyList())

    val standingsFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.getStandings(tournamentId) }
    val standings by standingsFlow.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showDrawPreviewDialog by remember { mutableStateOf(false) }
    var selectedMatchForScoring by remember { mutableStateOf<Match?>(null) }
    var previewFairnessScore by remember { mutableStateOf<FairnessScore?>(null) }

    if (tournament == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            Text("Tournament not found", color = SecondaryText)
        }
        return
    }

    val t = tournament!!
    val isHost = currentUser?.id == t.hostId
    val isRegistered = participants.any { it.userId == currentUser?.id }

    val tabs = when (t.format) {
        TournamentFormat.SINGLE_ELIMINATION -> listOf("Overview", "Bracket", "Matches", "Players")
        TournamentFormat.LEAGUE_GROUPS -> listOf("Overview", "Standings", "Matches", "Players")
        TournamentFormat.GROUPS_KNOCKOUT -> listOf("Overview", "Standings", "Bracket", "Matches", "Players")
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = t.name,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameBadge(game = t.game)
                        StatusBadge(status = t.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${participants.size} / ${t.maxParticipants} Registered Players",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contextual Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isHost) {
                            when (t.status) {
                                TournamentStatus.DRAFT -> {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                RepositoryProvider.tournamentRepository.openRegistration(t.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Open Registration")
                                    }
                                }

                                TournamentStatus.REGISTRATION_OPEN -> {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                RepositoryProvider.tournamentRepository.closeRegistration(t.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Close Registration")
                                    }
                                }

                                TournamentStatus.REGISTRATION_CLOSED, TournamentStatus.DRAW_PENDING -> {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val result = RepositoryProvider.tournamentRepository.generateDraw(t.id)
                                                result.onSuccess { structure ->
                                                    previewFairnessScore = structure.fairnessScore
                                                    showDrawPreviewDialog = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Generate Fair Draw")
                                    }
                                }

                                TournamentStatus.DRAW_GENERATED -> {
                                    Button(
                                        onClick = {
                                            if (previewFairnessScore == null) {
                                                previewFairnessScore = FairnessScore(88.0, 90.0, 88.0, 85.0, 90.0, 85.0)
                                            }
                                            showDrawPreviewDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Preview / Lock Draw")
                                    }
                                }

                                TournamentStatus.DRAW_LOCKED, TournamentStatus.IN_PROGRESS -> {
                                    Text(
                                        text = "Competition Active · Tap any scheduled match in bracket to enter scores",
                                        color = SuccessGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TournamentStatus.COMPLETED -> {
                                    Text(
                                        text = "Tournament Completed",
                                        color = SecondaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Participant view
                            if (t.registrationOpen && !isRegistered) {
                                val isFull = participants.size >= t.maxParticipants
                                Button(
                                    onClick = { showRegistrationDialog = true },
                                    enabled = !isFull,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (isFull) "Registration Full (Max ${t.maxParticipants})" else "Join Tournament",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isRegistered) {
                                Text(
                                    text = "✓ You are registered for this competition",
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Tab navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceDark,
                contentColor = PrimaryText,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) AccentBlue else SecondaryText,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            val currentTab = tabs[selectedTabIndex]
            when (currentTab) {
                "Overview" -> OverviewTabContent(t, participants)
                "Bracket" -> {
                    BracketView(
                        matches = matches,
                        participants = participants,
                        isHost = isHost,
                        onMatchClick = { match -> selectedMatchForScoring = match },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "Standings" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groups) { group ->
                            val groupStandings = standings.filter { it.groupId == group.id }
                            StandingsTable(
                                groupName = group.name,
                                standings = groupStandings,
                                participants = participants,
                                currentUserId = currentUser?.id
                            )
                        }
                    }
                }
                "Matches" -> MatchesTabContent(matches, participants, isHost) { match ->
                    selectedMatchForScoring = match
                }
                "Players" -> PlayersTabContent(participants, t.game.metricLabel)
            }
        }
    }

    // Score Entry Dialog
    if (selectedMatchForScoring != null) {
        val match = selectedMatchForScoring!!
        val participantMap = participants.associateBy { it.id }
        ScoreEntryDialog(
            match = match,
            participantA = match.participantAId?.let { participantMap[it] },
            participantB = match.participantBId?.let { participantMap[it] },
            onDismiss = { selectedMatchForScoring = null },
            onSubmitScore = { scoreA, scoreB, isOverride ->
                scope.launch {
                    RepositoryProvider.tournamentRepository.submitScore(
                        tournamentId = t.id,
                        matchId = match.id,
                        scoreA = scoreA,
                        scoreB = scoreB,
                        isOverride = isOverride
                    )
                }
            }
        )
    }

    // Registration Dialog
    if (showRegistrationDialog) {
        RegistrationDialog(
            tournament = t,
            onDismiss = { showRegistrationDialog = false },
            onRegister = { name, ign, metric ->
                scope.launch {
                    RepositoryProvider.tournamentRepository.registerParticipant(
                        Registration(
                            id = UUID.randomUUID().toString(),
                            tournamentId = t.id,
                            userId = currentUser?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            inGameId = ign,
                            gameMetricType = t.game.metricLabel,
                            gameMetricValue = metric
                        )
                    )
                }
            }
        )
    }

    // Draw Preview & Lock Dialog
    if (showDrawPreviewDialog && previewFairnessScore != null) {
        DrawPreviewDialog(
            fairnessScore = previewFairnessScore!!,
            participantCount = participants.size,
            onDismiss = { showDrawPreviewDialog = false },
            onRegenerate = {
                scope.launch {
                    val result = RepositoryProvider.tournamentRepository.generateDraw(t.id, System.currentTimeMillis())
                    result.onSuccess { previewFairnessScore = it.fairnessScore }
                }
            },
            onConfirmLock = {
                scope.launch {
                    RepositoryProvider.tournamentRepository.lockDraw(t.id)
                }
            }
        )
    }
}

@Composable
private fun OverviewTabContent(tournament: com.bracketx.domain.model.Tournament, participants: List<TournamentParticipant>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("TOURNAMENT RULES & SETUP", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OverviewDetailRow("Format", tournament.format.displayName)
                    OverviewDetailRow("Game", tournament.game.displayName)
                    OverviewDetailRow("Metric", tournament.game.metricLabel)
                    OverviewDetailRow("Registered", "${participants.size} / ${tournament.maxParticipants}")
                    OverviewDetailRow("Draw Status", if (tournament.drawLocked) "Locked & Official" else "Unlocked")
                }
            }
        }
    }
}

@Composable
private fun MatchesTabContent(
    matches: List<Match>,
    participants: List<TournamentParticipant>,
    isHost: Boolean,
    onMatchClick: (Match) -> Unit
) {
    val participantMap = participants.associateBy { it.id }

    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No matches generated yet. Complete draw first.", color = SecondaryText)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(matches) { match ->
            val pA = match.participantAId?.let { participantMap[it] }
            val pB = match.participantBId?.let { participantMap[it] }

            com.bracketx.ui.components.MatchCard(
                match = match,
                participantA = pA,
                participantB = pB,
                isHost = isHost,
                onClick = { onMatchClick(match) }
            )
        }
    }
}

@Composable
private fun PlayersTabContent(participants: List<TournamentParticipant>, metricLabel: String) {
    if (participants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No participants registered yet.", color = SecondaryText)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(participants) { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (p.seed != null) {
                        Text("#${p.seed}", color = SecondaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    }
                    Column {
                        Text(p.displayName, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(p.inGameId, color = SecondaryText, fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BandBadge(band = p.competitiveBand)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$metricLabel ${p.metricValue.toInt()}",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SecondaryText, fontSize = 13.sp)
        Text(value, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
