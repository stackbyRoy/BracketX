package com.bracketx.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.PrimaryText
import com.stackbyroy.bracketx.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onAdCheckpoint: () -> Unit = {},
    onSplashFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.65f) }
    val logoAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.85f) }
    val glowAlpha = remember { Animatable(0f) }

    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }

    val progressAlpha = remember { Animatable(0f) }
    val overallAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Logo & ambient glow entrance (~420ms)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = LinearEasing)
            )
        }
        launch {
            glowAlpha.animateTo(
                targetValue = 0.45f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            glowScale.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // 2. Title and subtitle entrance with slight stagger (~200ms stagger)
        delay(180)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = LinearEasing)
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }

        // 3. Status indicator fade-in
        delay(120)
        launch {
            progressAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250)
            )
        }

        // 4. Hold presentation for optimal pacing (settles around 900-950ms)
        delay(420)

        // Ad readiness evaluation checkpoint (~80% through ~1.2s splash progression)
        onAdCheckpoint()

        // 5. Smooth exit scaling and fade out (~280ms, reaching ~1200ms total)
        launch {
            logoScale.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        overallAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )

        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .alpha(overallAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric radial neon glow behind logo
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(glowScale.value)
                .alpha(glowAlpha.value)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.35f),
                            AccentBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Icon with smooth rounded container
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF141820))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bracketx_logo),
                    contentDescription = "BracketX Logo",
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            ) {
                Text(
                    text = "BracketX",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryText,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "TOURNAMENT ENGINE & FAIR PLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Minimalist animated indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(progressAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AccentBlue, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(6.dp)
                        .background(AccentBlue, RoundedCornerShape(3.dp))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AccentBlue.copy(alpha = 0.35f), CircleShape)
                )
            }
        }
    }
}
