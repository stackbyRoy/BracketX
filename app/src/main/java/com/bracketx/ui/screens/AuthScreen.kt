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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark
import kotlinx.coroutines.launch

@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()

    var isSignUpMode by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Branding Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceElevatedDark)
                .border(2.dp, AccentBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "BracketX Logo",
                tint = AccentBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BRACKETX",
            color = PrimaryText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )

        Text(
            text = "Fair Tournament Operating System",
            color = SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Mode Segment Switcher
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSignUpMode) AccentBlue else Color.Transparent)
                        .clickable {
                            isSignUpMode = true
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        color = if (isSignUpMode) Color.White else SecondaryText,
                        fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isSignUpMode) AccentBlue else Color.Transparent)
                        .clickable {
                            isSignUpMode = false
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (!isSignUpMode) Color.White else SecondaryText,
                        fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed.copy(alpha = 0.15f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form Fields
        if (isSignUpMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    errorMessage = null
                },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text(if (isSignUpMode) "Password (min 6 characters)" else "Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = SecondaryText
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Submit Button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (isSignUpMode && displayName.isBlank())) {
                    errorMessage = "Please fill in all required fields."
                    return@Button
                }

                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters long."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                scope.launch {
                    val result = if (isSignUpMode) {
                        RepositoryProvider.authRepository.signUp(
                            email = email.trim(),
                            password = password,
                            displayName = displayName.trim()
                        )
                    } else {
                        RepositoryProvider.authRepository.signIn(
                            email = email.trim(),
                            password = password
                        )
                    }

                    isLoading = false
                    result.onFailure { error ->
                        errorMessage = error.message ?: "Authentication failed. Please try again."
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = if (isSignUpMode) "Create Account" else "Sign In",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch Mode Link
        TextButton(
            onClick = {
                isSignUpMode = !isSignUpMode
                errorMessage = null
            }
        ) {
            Text(
                text = if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                color = AccentBlue,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
