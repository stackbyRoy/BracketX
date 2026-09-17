package com.bracketx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark

@Composable
fun ScoreEntryDialog(
    match: Match,
    participantA: TournamentParticipant?,
    participantB: TournamentParticipant?,
    onDismiss: () -> Unit,
    onSubmitScore: (scoreA: Int, scoreB: Int, isOverride: Boolean) -> Unit
) {
    var scoreA by remember { mutableStateOf(match.scoreA ?: 0) }
    var scoreB by remember { mutableStateOf(match.scoreB ?: 0) }
    var isReviewing by remember { mutableStateOf(false) }

    val nameA = participantA?.displayName ?: "Player A"
    val nameB = participantB?.displayName ?: "Player B"
    val isKnockout = match.stage != MatchStage.GROUP
    val isTie = scoreA == scoreB
    val isValidScore = !isKnockout || !isTie

    val winnerName = when {
        scoreA > scoreB -> nameA
        scoreB > scoreA -> nameB
        else -> "Draw"
    }

    if (isReviewing) {
        // Step 2: Confirmation Dialog
        AlertDialog(
            onDismissRequest = { isReviewing = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Confirm Match Result",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "$nameA: $scoreA  —  $scoreB: $nameB",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isKnockout) {
                        Text(
                            text = "Winner: $winnerName will advance automatically in the bracket.",
                            color = SuccessGreen,
                            fontSize = 14.sp
                        )
                    }
                    if (match.isCompleted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note: This is an administrative override on a completed match.",
                            color = ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitScore(scoreA, scoreB, match.isCompleted)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Confirm Official Result", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isReviewing = false }) {
                    Text("Back", color = SecondaryText)
                }
            }
        )
    } else {
        // Step 1: Score Entry with large touch targets
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Enter Match Result",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.stage.displayName,
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Player A Counter
                    ScoreRow(
                        name = nameA,
                        score = scoreA,
                        onDecrement = { if (scoreA > 0) scoreA-- },
                        onIncrement = { scoreA++ }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player B Counter
                    ScoreRow(
                        name = nameB,
                        score = scoreB,
                        onDecrement = { if (scoreB > 0) scoreB-- },
                        onIncrement = { scoreB++ }
                    )

                    if (isKnockout && isTie) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Knockout matches cannot end in a draw",
                            color = ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isReviewing = true },
                    enabled = isValidScore,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Review Result")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun ScoreRow(
    name: String,
    score: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevatedDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = PrimaryText,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Large touch target minus button (min 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BackgroundDark)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "−", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = score.toString(),
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )

            // Large touch target plus button (min 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BackgroundDark)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = AccentBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
