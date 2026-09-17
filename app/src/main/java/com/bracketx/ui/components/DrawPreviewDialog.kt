package com.bracketx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.FairnessScore
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.PrimaryText
import androidx.compose.ui.graphics.Color
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import com.bracketx.ui.theme.WarningYellow

@Composable
fun DrawPreviewDialog(
    fairnessScore: FairnessScore,
    participantCount: Int,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onConfirmLock: () -> Unit
) {
    var showLockConfirmation by remember { mutableStateOf(false) }

    if (showLockConfirmation) {
        AlertDialog(
            onDismissRequest = { showLockConfirmation = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Lock this draw?",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Once locked, participant placement and seeds cannot be changed normally. Tournament matches will officially start.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmLock()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Lock Draw", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLockConfirmation = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceDark,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fair Draw Preview",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$participantCount Players",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            },
            text = {
                Column {
                    // Overall Fairness Score Card
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
                                Text("Fairness Score", color = SecondaryText, fontSize = 13.sp)
                                val scoreColor = when {
                                    fairnessScore.total >= 80.0 -> SuccessGreen
                                    fairnessScore.total >= 70.0 -> WarningYellow
                                    else -> AccentBlue
                                }
                                Text(
                                    text = "${String.format("%.1f", fairnessScore.total)} / 100",
                                    color = scoreColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 5 Component Breakdown per DRAW_ALGORITHM_SPEC.md Section 17
                            FairnessBarRow("Strength Distribution (30%)", fairnessScore.strengthDistribution)
                            Spacer(modifier = Modifier.height(8.dp))
                            FairnessBarRow("Bracket Balance (25%)", fairnessScore.bracketBalance)
                            Spacer(modifier = Modifier.height(8.dp))
                            FairnessBarRow("Competitive Diversity (20%)", fairnessScore.competitiveDiversity)
                            Spacer(modifier = Modifier.height(8.dp))
                            FairnessBarRow("Opportunity Score (15%)", fairnessScore.opportunity)
                            Spacer(modifier = Modifier.height(8.dp))
                            FairnessBarRow("Controlled Randomness (10%)", fairnessScore.randomness)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Review the generated structure. You can regenerate with a new random seed or lock the draw.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLockConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Lock Draw", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(onClick = onRegenerate) {
                        Text("Regenerate", color = AccentBlue)
                    }
                }
            }
        )
    }
}

@Composable
private fun FairnessBarRow(label: String, score: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = PrimaryText, fontSize = 11.sp)
            Text(text = "${String.format("%.0f", score)}%", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AccentBlue,
            trackColor = SurfaceElevatedDark,
        )
    }
}
