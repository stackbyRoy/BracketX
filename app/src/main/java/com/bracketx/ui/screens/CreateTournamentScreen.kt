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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentSettings
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentVisibility
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

private fun generateSecureAccessCode(): String {
    val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    return (1..6).map { chars.random() }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onNavigateBack: () -> Unit,
    onTournamentCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }
    var tournamentName by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf(GameType.FC_MOBILE) }
    var selectedFormat by remember { mutableStateOf(TournamentFormat.SINGLE_ELIMINATION) }
    var maxParticipants by remember { mutableIntStateOf(32) }
    var selectedVisibility by remember { mutableStateOf(TournamentVisibility.PUBLIC) }
    var generatedAccessCode by remember { mutableStateOf(generateSecureAccessCode()) }
    var rulesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var newRuleText by remember { mutableStateOf("") }

    var createdTournamentResult by remember { mutableStateOf<Tournament?>(null) }
    var showCreatedSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Create Tournament", color = PrimaryText, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP $currentStep OF 6",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                1 -> {
                    Text("Tournament Details", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Give your tournament a recognizable name.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { tournamentName = it },
                        label = { Text("Tournament Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text("Select Game", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Determines the competitive metric and validation rules.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    SelectableCard(
                        title = GameType.FC_MOBILE.displayName,
                        subtitle = "Competitive Metric: OVR (60–120)",
                        isSelected = selectedGame == GameType.FC_MOBILE,
                        onClick = { selectedGame = GameType.FC_MOBILE }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectableCard(
                        title = GameType.EFOOTBALL.displayName,
                        subtitle = "Competitive Metric: Team Strength (1500–3500)",
                        isSelected = selectedGame == GameType.EFOOTBALL,
                        onClick = { selectedGame = GameType.EFOOTBALL }
                    )
                }

                3 -> {
                    Text("Tournament Format & Capacity", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Choose competition format and participant capacity.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    TournamentFormat.entries.forEach { format ->
                        SelectableCard(
                            title = format.displayName,
                            subtitle = when (format) {
                                TournamentFormat.SINGLE_ELIMINATION -> "Classic knockout bracket with byes support"
                                TournamentFormat.LEAGUE_GROUPS -> "Balanced groups with round-robin matches & standings"
                                TournamentFormat.GROUPS_KNOCKOUT -> "Group stage followed by qualified knockout phase"
                            },
                            isSelected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Player Capacity", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Maximum allowed participants (up to 32 players for ${selectedGame.displayName})", color = SecondaryText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 8, 16, 32).forEach { capacity ->
                            val isSelected = maxParticipants == capacity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentBlue else SurfaceCard)
                                    .border(1.dp, if (isSelected) AccentBlue else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { maxParticipants = capacity }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$capacity",
                                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (capacity == 32) {
                                        Text(
                                            text = "MAX",
                                            color = if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f) else SecondaryText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    Text("Tournament Visibility", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Control who can discover and register for your tournament.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    SelectableCard(
                        title = "Public Tournament",
                        subtitle = "Anyone can discover and register.",
                        isSelected = selectedVisibility == TournamentVisibility.PUBLIC,
                        onClick = { selectedVisibility = TournamentVisibility.PUBLIC }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectableCard(
                        title = "Private Tournament",
                        subtitle = "Tournament is not publicly listed and requires an access code.",
                        isSelected = selectedVisibility == TournamentVisibility.PRIVATE,
                        onClick = { selectedVisibility = TournamentVisibility.PRIVATE }
                    )

                    if (selectedVisibility == TournamentVisibility.PRIVATE) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevatedDark)
                                .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PRIVATE ACCESS CODE",
                                        color = AccentBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Regenerate",
                                        color = SecondaryText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { generatedAccessCode = generateSecureAccessCode() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = generatedAccessCode,
                                    color = PrimaryText,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Players will be required to enter this access code before registering. You will also be able to copy and share this code after creation.",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                5 -> {
                    Text("Tournament Rules", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Specify custom rules for participants by manually typing them below. Each rule will be displayed as a bullet to users.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newRuleText,
                            onValueChange = { newRuleText = it },
                            placeholder = { Text("e.g., Match duration 6 mins, No pause during active attack", color = SecondaryText, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newRuleText.trim()
                                if (trimmed.isNotBlank()) {
                                    rulesList = rulesList + trimmed
                                    newRuleText = ""
                                }
                            },
                            enabled = newRuleText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (rulesList.isNotEmpty()) {
                        Text(
                            text = "CUSTOM RULES (${rulesList.size})",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rulesList.forEachIndexed { idx, rule ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceCard)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("•", color = AccentBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = rule, color = PrimaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
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
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCard)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("No custom rules added (Optional)", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Standard fair-play rules will be applied automatically if no rules are typed.", color = SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                }

                6 -> {
                    Text("Review & Confirm", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Verify the tournament setup before launching.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            ReviewRow("Tournament", tournamentName.ifBlank { "Untitled Tournament" })
                            ReviewRow("Game", selectedGame.displayName)
                            ReviewRow("Metric", selectedGame.metricLabel)
                            ReviewRow("Format", selectedFormat.displayName)
                            ReviewRow("Max Players", "$maxParticipants players (Highest: 32)")
                            ReviewRow("Visibility", if (selectedVisibility == TournamentVisibility.PUBLIC) "Public" else "Private (Access Code Protected)")
                            if (selectedVisibility == TournamentVisibility.PRIVATE) {
                                ReviewRow("Access Code", generatedAccessCode)
                            }
                            ReviewRow("Custom Rules", if (rulesList.isEmpty()) "Standard fair-play defaults" else "${rulesList.size} custom rules configured")
                            if (rulesList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                rulesList.forEach { rule ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("•", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                                        Text(rule, color = SecondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    OutlinedButton(onClick = { currentStep-- }) {
                        Text("Back", color = SecondaryText)
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (currentStep < 6) {
                    Button(
                        onClick = { currentStep++ },
                        enabled = currentStep != 1 || tournamentName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Continue")
                    }
                } else {
                    Button(
                        onClick = {
                            val newTournament = Tournament(
                                id = UUID.randomUUID().toString(),
                                hostId = currentUser?.id ?: "",
                                name = tournamentName.trim(),
                                game = selectedGame,
                                format = selectedFormat,
                                status = TournamentStatus.DRAFT,
                                visibility = selectedVisibility,
                                maxParticipants = maxParticipants.coerceIn(2, 32),
                                registrationOpen = false,
                                rules = rulesList,
                                settings = TournamentSettings()
                            )

                            val codeToSave = if (selectedVisibility == TournamentVisibility.PRIVATE) generatedAccessCode else null

                            scope.launch {
                                val result = RepositoryProvider.tournamentRepository.createTournament(newTournament, codeToSave)
                                result.onSuccess { created ->
                                    createdTournamentResult = created
                                    showCreatedSuccessDialog = true
                                }.onFailure {
                                    createdTournamentResult = newTournament
                                    showCreatedSuccessDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Create Tournament", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Tournament Created Success Modal / Dialog
    if (showCreatedSuccessDialog && createdTournamentResult != null) {
        val tournament = createdTournamentResult!!
        val isPrivate = tournament.visibility == TournamentVisibility.PRIVATE
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        AlertDialog(
            onDismissRequest = { /* Require explicit user action */ },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Tournament Created! 🏆",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Your tournament is ready. Use the Tournament ID and share link to invite players.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    // Tournament ID Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOURNAMENT ID", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    text = tournament.publicId.ifBlank { "BRX-" + tournament.id.take(6).uppercase() },
                                    color = PrimaryText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    val idToCopy = tournament.publicId.ifBlank { tournament.id }
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tournament ID", idToCopy))
                                    Toast.makeText(context, "Tournament ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = AccentBlue)
                            }
                        }
                    }

                    // Private Access Code Card (if Private)
                    if (isPrivate) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevatedDark)
                                .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PRIVATE ACCESS CODE", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text(
                                        text = generatedAccessCode,
                                        color = PrimaryText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Private Access Code", generatedAccessCode))
                                        Toast.makeText(context, "Access Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Access Code", tint = AccentBlue)
                                }
                            }
                        }

                        Text(
                            text = "⚠️ The access code is required for private registration. Share it directly with participants.",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }

                    // Native Share Button
                    OutlinedButton(
                        onClick = {
                            TournamentShareHelper.shareTournament(context, tournament)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Tournament", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreatedSuccessDialog = false
                        onTournamentCreated(tournament.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Go to Tournament")
                }
            }
        )
    }
}

@Composable
private fun SelectableCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentBlue else BorderSubtle
    val bgColor = if (isSelected) AccentBlue.copy(alpha = 0.12f) else SurfaceCard

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = SecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SecondaryText, fontSize = 13.sp)
        Text(text = value, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
