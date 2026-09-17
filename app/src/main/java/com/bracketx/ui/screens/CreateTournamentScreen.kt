package com.bracketx.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentFormat
import com.bracketx.domain.model.TournamentSettings
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onNavigateBack: () -> Unit,
    onTournamentCreated: (String) -> Unit
) {
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }
    var tournamentName by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf(GameType.FC_MOBILE) }
    var selectedFormat by remember { mutableStateOf(TournamentFormat.SINGLE_ELIMINATION) }
    var maxParticipants by remember { mutableIntStateOf(32) }

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
                    text = "STEP $currentStep OF 4",
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

                if (currentStep < 4) {
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
                                maxParticipants = maxParticipants.coerceIn(2, 32),
                                registrationOpen = false,
                                settings = TournamentSettings()
                            )

                            scope.launch {
                                RepositoryProvider.tournamentRepository.createTournament(newTournament)
                                onTournamentCreated(newTournament.id)
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
