package com.bracketx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceElevatedDark
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val scope = rememberCoroutineScope()
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
    ) {
        Text("Profile & Settings", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevatedDark)
                        .border(1.dp, AccentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Avatar", tint = AccentBlue, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = currentUser?.displayName ?: "User",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unified Host / Participant Account",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column {
                Text("ACCOUNT MODEL", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Single unified account system. Roles (Host vs Participant) are determined per tournament.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = {
                scope.launch {
                    RepositoryProvider.authRepository.signOut()
                }
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = ErrorRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "BracketX v1.0.0 · Fair Tournament Operating System",
            color = SecondaryText,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
