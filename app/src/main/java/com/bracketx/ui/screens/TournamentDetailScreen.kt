package com.bracketx.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.domain.model.FairnessScore
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.Registration
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentVisibility
import com.bracketx.ui.components.BandBadge
import com.bracketx.ui.components.BracketView
import com.bracketx.ui.components.DrawPreviewDialog
import com.bracketx.ui.components.GameBadge
import com.bracketx.ui.components.RegistrationDialog
import com.bracketx.ui.components.ScoreEntryDialog
import com.bracketx.ui.components.StandingsTable
import com.bracketx.ui.components.StatusBadge
import com.bracketx.ui.components.VisibilityBadge
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import com.bracketx.util.TournamentShareHelper
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onNavigateBack: () -> Unit,
    onRequireAuth: () -> Unit = {}
) {
    val context = LocalContext.current
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

    val hasPrivateAccessFlow = remember(tournamentId) { RepositoryProvider.tournamentRepository.hasPrivateAccess(tournamentId) }
    val hasPrivateAccess by hasPrivateAccessFlow.collectAsState(initial = false)

    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showPrivateAccessDialog by remember { mutableStateOf(false) }
    var showDrawPreviewDialog by remember { mutableStateOf(false) }
    var showChangeVisibilityDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showManageRulesDialog by remember { mutableStateOf(false) }
    var isDeletingTournament by remember { mutableStateOf(false) }
    var selectedMatchForScoring by remember { mutableStateOf<Match?>(null) }
    var previewFairnessScore by remember { mutableStateOf<FairnessScore?>(null) }

    var accessCodeError by remember { mutableStateOf<String?>(null) }
    var isVerifyingCode by remember { mutableStateOf(false) }

    if (tournament == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            Text("Tournament not found", color = SecondaryText)
        }
        return
    }

    val t = tournament!!
    val isHost = currentUser?.id == t.hostId
    val isRegistered = participants.any { it.userId == currentUser?.id }
    val isPrivate = t.visibility == TournamentVisibility.PRIVATE
    val hostSavedCode = if (isHost) RepositoryProvider.tournamentRepository.getHostAccessCode(t.id) else null

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
                actions = {
                    IconButton(onClick = {
                        TournamentShareHelper.shareTournament(context, t)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Tournament",
                            tint = AccentBlue
                        )
                    }
                    if (isHost) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Tournament",
                                tint = ErrorRed
                            )
                        }
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameBadge(game = t.game)
                            VisibilityBadge(visibility = t.visibility)
                        }
                        StatusBadge(status = t.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${participants.size} / ${t.maxParticipants} Registered Players",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )

                        if (t.publicId.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tournament ID", t.publicId))
                                    Toast.makeText(context, "Tournament ID copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = t.publicId,
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Private Tournament Notice for Viewers
                    if (isPrivate && !isHost && !isRegistered) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevatedDark)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFFFFB74D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "This is a private tournament. Enter the access code to register.",
                                    color = androidx.compose.ui.graphics.Color(0xFFFFB74D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

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
                                val requiresCode = isPrivate && !hasPrivateAccess

                                Button(
                                    onClick = {
                                        if (currentUser == null) {
                                            onRequireAuth()
                                        } else if (requiresCode) {
                                            accessCodeError = null
                                            showPrivateAccessDialog = true
                                        } else {
                                            showRegistrationDialog = true
                                        }
                                    },
                                    enabled = !isFull,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (requiresCode) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enter Access Code to Join", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(
                                            if (currentUser == null) "Sign In to Join"
                                            else if (isFull) "Registration Full (Max ${t.maxParticipants})"
                                            else "Join Tournament",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { TournamentShareHelper.shareTournament(context, t) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Tournament", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                "Overview" -> OverviewTabContent(
                    tournament = t,
                    participants = participants,
                    isHost = isHost,
                    hostAccessCode = hostSavedCode,
                    onChangeVisibility = { showChangeVisibilityDialog = true },
                    onDeleteTournament = { showDeleteConfirmDialog = true },
                    onManageRules = { showManageRulesDialog = true }
                )
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

    // Private Access Code Prompt Dialog
    if (showPrivateAccessDialog) {
        var enteredCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!isVerifyingCode) showPrivateAccessDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("Private Tournament Access", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "This tournament is private. Please enter the access code provided by the organizer to proceed with registration.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = {
                            enteredCode = it
                            accessCodeError = null
                        },
                        label = { Text("Access Code") },
                        placeholder = { Text("e.g. 4X9P7M") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (accessCodeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(accessCodeError!!, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = enteredCode.trim()
                        if (clean.isBlank()) {
                            accessCodeError = "Please enter an access code"
                            return@Button
                        }
                        isVerifyingCode = true
                        scope.launch {
                            val result = RepositoryProvider.tournamentRepository.verifyPrivateAccess(t.id, clean)
                            isVerifyingCode = false
                            result.fold(
                                onSuccess = {
                                    showPrivateAccessDialog = false
                                    showRegistrationDialog = true
                                },
                                onFailure = {
                                    accessCodeError = "Invalid access code"
                                }
                            )
                        }
                    },
                    enabled = !isVerifyingCode && enteredCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (isVerifyingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryText, strokeWidth = 2.dp)
                    } else {
                        Text("Verify & Continue")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPrivateAccessDialog = false },
                    enabled = !isVerifyingCode
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Delete Tournament Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingTournament) showDeleteConfirmDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Delete Tournament?",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${t.name}\"? All brackets, matches, registered participants, and standings will be permanently deleted. This action cannot be undone.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingTournament = true
                        scope.launch {
                            val res = RepositoryProvider.tournamentRepository.deleteTournament(t.id, currentUser?.id ?: "")
                            isDeletingTournament = false
                            showDeleteConfirmDialog = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Tournament deleted successfully", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, err.message ?: "Failed to delete tournament", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    enabled = !isDeletingTournament
                ) {
                    if (isDeletingTournament) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryText, strokeWidth = 2.dp)
                    } else {
                        Text("Delete Permanently", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeletingTournament
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
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

    // Host Change Visibility Dialog
    if (showChangeVisibilityDialog) {
        var newVisibility by remember { mutableStateOf(if (t.visibility == TournamentVisibility.PUBLIC) TournamentVisibility.PRIVATE else TournamentVisibility.PUBLIC) }
        var newCode by remember { mutableStateOf("4X9P7M") }

        AlertDialog(
            onDismissRequest = { showChangeVisibilityDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("Change Visibility", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Change tournament visibility from ${t.visibility.displayName} to ${newVisibility.displayName}.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                    if (newVisibility == TournamentVisibility.PRIVATE) {
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            label = { Text("Set Private Access Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            RepositoryProvider.tournamentRepository.updateTournamentVisibility(
                                tournamentId = t.id,
                                visibility = newVisibility,
                                accessCode = if (newVisibility == TournamentVisibility.PRIVATE) newCode else null
                            )
                            showChangeVisibilityDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangeVisibilityDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Host Manage Rules Dialog
    if (showManageRulesDialog) {
        var rulesList by remember { mutableStateOf(t.rules) }
        var currentRuleText by remember { mutableStateOf("") }
        var isSavingRules by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSavingRules) showManageRulesDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("Tournament Rules", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Manually specify custom rules. Each rule will be displayed as a distinct bullet point to participants.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentRuleText,
                            onValueChange = { currentRuleText = it },
                            placeholder = { Text("Type rule here...", color = SecondaryText, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = currentRuleText.trim()
                                if (trimmed.isNotBlank()) {
                                    rulesList = rulesList + trimmed
                                    currentRuleText = ""
                                }
                            },
                            enabled = currentRuleText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    if (rulesList.isNotEmpty()) {
                        Text(
                            text = "CUSTOM RULES (${rulesList.size})",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(rulesList) { idx, rule ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceCard, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("•", color = AccentBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule,
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            rulesList = rulesList.filterIndexed { i, _ -> i != idx }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove rule",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingRules = true
                        scope.launch {
                            RepositoryProvider.tournamentRepository.updateTournamentRules(t.id, rulesList)
                            isSavingRules = false
                            showManageRulesDialog = false
                            Toast.makeText(context, "Tournament rules updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSavingRules,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (isSavingRules) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryText, strokeWidth = 2.dp)
                    } else {
                        Text("Save Rules")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showManageRulesDialog = false },
                    enabled = !isSavingRules
                ) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun OverviewTabContent(
    tournament: Tournament,
    participants: List<TournamentParticipant>,
    isHost: Boolean,
    hostAccessCode: String?,
    onChangeVisibility: () -> Unit,
    onDeleteTournament: () -> Unit,
    onManageRules: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val shareUrl = TournamentShareHelper.getTournamentUrl(tournament)

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
                    Text("TOURNAMENT SETUP", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OverviewDetailRow("Format", tournament.format.displayName)
                    OverviewDetailRow("Game", tournament.game.displayName)
                    OverviewDetailRow("Metric", tournament.game.metricLabel)
                    OverviewDetailRow("Registered", "${participants.size} / ${tournament.maxParticipants}")
                    OverviewDetailRow("Draw Status", if (tournament.drawLocked) "Locked & Official" else "Unlocked")
                    OverviewDetailRow("Visibility", tournament.visibility.displayName)
                    if (tournament.publicId.isNotBlank()) {
                        OverviewDetailRow("Tournament ID", tournament.publicId)
                    }
                }
            }
        }

        // TOURNAMENT RULES SECTION (Displayed as bullets to all users)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOURNAMENT RULES",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (isHost) {
                            TextButton(
                                onClick = onManageRules,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (tournament.rules.isEmpty()) "Add Rules" else "Edit Rules", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val displayRules = if (tournament.rules.isNotEmpty()) {
                        tournament.rules
                    } else {
                        listOf(
                            "Standard fair play rules apply to all matches.",
                            "Both players must take screenshots of final match scores.",
                            "Disconnections during active play must be reported to the host immediately.",
                            "Toxic behavior, cheating, or manipulation leads to instant disqualification."
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayRules.forEach { rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = if (tournament.rules.isNotEmpty()) AccentBlue else SecondaryText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = rule,
                                    color = if (tournament.rules.isNotEmpty()) PrimaryText else SecondaryText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // HOST MANAGEMENT CARD (Section 11)
        if (isHost) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "HOST TOURNAMENT MANAGEMENT",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // 1. Tournament ID with Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tournament ID", color = SecondaryText, fontSize = 11.sp)
                                Text(
                                    text = tournament.publicId.ifBlank { tournament.id.take(8).uppercase() },
                                    color = PrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val idToCopy = tournament.publicId.ifBlank { tournament.id }
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tournament ID", idToCopy))
                                    Toast.makeText(context, "Tournament ID copied", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy ID", fontSize = 11.sp)
                            }
                        }

                        // 2. Share Link with Copy & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Share Link", color = SecondaryText, fontSize = 11.sp)
                                Text(
                                    text = shareUrl,
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tournament Link", shareUrl))
                                    Toast.makeText(context, "Tournament link copied", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Link", fontSize = 11.sp)
                            }
                        }

                        // 3. Private Access Code (if Private)
                        if (tournament.visibility == TournamentVisibility.PRIVATE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Private Access Code", color = androidx.compose.ui.graphics.Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = hostAccessCode ?: "Required for registration",
                                        color = PrimaryText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (hostAccessCode != null) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Access Code", hostAccessCode))
                                            Toast.makeText(context, "Access code copied", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Code", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // 4. Visibility Lifecycle Management
                        val canChangeVisibility = tournament.status == TournamentStatus.DRAFT || tournament.status == TournamentStatus.REGISTRATION_OPEN
                        if (canChangeVisibility) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Change Visibility", color = SecondaryText, fontSize = 12.sp)
                                OutlinedButton(
                                    onClick = onChangeVisibility,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Switch to ${if (tournament.visibility == TournamentVisibility.PUBLIC) "Private" else "Public"}", fontSize = 11.sp)
                                }
                            }
                        }

                        // 5. Danger Zone: Delete Tournament
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onDeleteTournament,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Tournament", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
