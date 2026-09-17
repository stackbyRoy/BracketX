package com.bracketx.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark

@Composable
fun RegistrationDialog(
    tournament: Tournament,
    onDismiss: () -> Unit,
    onRegister: (name: String, inGameId: String, metricValue: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var inGameId by remember { mutableStateOf("") }
    var metricString by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val metricLabel = tournament.game.metricLabel
    val minMetric = tournament.settings.minMetric ?: tournament.game.defaultMinMetric
    val maxMetric = tournament.settings.maxMetric ?: tournament.game.defaultMaxMetric

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Join ${tournament.name}",
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your participant details to register.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Field 1: Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2: In-game ID
                OutlinedTextField(
                    value = inGameId,
                    onValueChange = { inGameId = it; errorMessage = null },
                    label = { Text("In-Game ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 3: Dynamic Competitive Metric (OVR or Team Strength)
                OutlinedTextField(
                    value = metricString,
                    onValueChange = { metricString = it; errorMessage = null },
                    label = { Text("$metricLabel ($minMetric - $maxMetric)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ErrorRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "By registering, you agree to the tournament rules.",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter your name"
                        return@Button
                    }
                    if (inGameId.isBlank()) {
                        errorMessage = "Please enter your in-game ID"
                        return@Button
                    }
                    val metric = metricString.toDoubleOrNull()
                    if (metric == null) {
                        errorMessage = "Please enter a valid numeric $metricLabel"
                        return@Button
                    }
                    if (metric < minMetric || metric > maxMetric) {
                        errorMessage = "$metricLabel must be between $minMetric and $maxMetric"
                        return@Button
                    }

                    onRegister(name.trim(), inGameId.trim(), metric)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Register", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
