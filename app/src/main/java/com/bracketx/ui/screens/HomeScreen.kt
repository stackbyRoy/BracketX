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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.domain.model.TournamentVisibility
import com.bracketx.ui.components.GameBadge
import com.bracketx.ui.components.StatusBadge
import com.bracketx.ui.components.VisibilityBadge
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToTournament: (String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()
    val tournaments by RepositoryProvider.tournamentRepository.getTournaments().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }

    // Section 1: My Tournaments (Host or Member)
    val myTournaments = tournaments.filter { it.hostId == currentUser?.id }

    // Section 2: Public Discover Tournaments (Strictly PUBLIC tournaments not hosted by current user)
    val discoverTournaments = tournaments.filter {
        it.visibility == TournamentVisibility.PUBLIC && it.hostId != currentUser?.id
    }

    fun performSearch() {
        val cleanQuery = searchInput.trim().uppercase()
        if (cleanQuery.isBlank()) {
            searchErrorMessage = "Please enter a Tournament ID"
            return
        }

        focusManager.clearFocus()
        isSearching = true
        searchErrorMessage = null

        scope.launch {
            val result = RepositoryProvider.tournamentRepository.searchTournamentByPublicId(cleanQuery)
            isSearching = false
            result.fold(
                onSuccess = { tournament ->
                    if (tournament != null) {
                        searchErrorMessage = null
                        onNavigateToTournament(tournament.id)
                    } else {
                        searchErrorMessage = "Tournament not found"
                    }
                },
                onFailure = {
                    searchErrorMessage = "Tournament not found"
                }
            )
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = AccentBlue,
                contentColor = PrimaryText
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome back,",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
                Text(
                    text = currentUser?.displayName ?: "Competitor",
                    color = PrimaryText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // SEARCH BY TOURNAMENT ID
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "FIND TOURNAMENT",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = {
                                    searchInput = it
                                    if (searchErrorMessage != null) searchErrorMessage = null
                                },
                                placeholder = {
                                    Text("e.g. BRX-7F92KQ", color = SecondaryText.copy(alpha = 0.6f), fontSize = 13.sp)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = PrimaryText,
                                    unfocusedTextColor = PrimaryText
                                )
                            )

                            Button(
                                onClick = { performSearch() },
                                enabled = !isSearching && searchInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = PrimaryText,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Search", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (searchErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = searchErrorMessage!!,
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // SECTION 1: MY TOURNAMENTS
            item {
                Text(
                    text = "MY TOURNAMENTS",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (myTournaments.isEmpty()) {
                item {
                    EmptyTournamentCard(
                        text = "You haven't organized any tournaments yet.",
                        actionText = "Create your first tournament",
                        onClick = onNavigateToCreate
                    )
                }
            } else {
                items(myTournaments) { tournament ->
                    TournamentItemCard(
                        tournament = tournament,
                        onClick = { onNavigateToTournament(tournament.id) }
                    )
                }
            }

            // SECTION 2: DISCOVER TOURNAMENTS (Public Only)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DISCOVER TOURNAMENTS",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (discoverTournaments.isEmpty()) {
                item {
                    EmptyTournamentCard(
                        text = "No public tournaments discovered in your area.",
                        actionText = null,
                        onClick = null
                    )
                }
            } else {
                items(discoverTournaments) { tournament ->
                    TournamentItemCard(
                        tournament = tournament,
                        onClick = { onNavigateToTournament(tournament.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TournamentItemCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
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
                    GameBadge(game = tournament.game)
                    VisibilityBadge(visibility = tournament.visibility)
                }
                StatusBadge(status = tournament.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tournament.name,
                color = PrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tournament.format.displayName} · Max ${tournament.maxParticipants} players",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
                if (tournament.publicId.isNotBlank()) {
                    Text(
                        text = tournament.publicId,
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (tournament.status == TournamentStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevatedDark)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Active Competition · Bracket In Progress",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTournamentCard(
    text: String,
    actionText: String?,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, color = SecondaryText, fontSize = 14.sp)
            if (actionText != null && onClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actionText,
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onClick)
                )
            }
        }
    }
}
