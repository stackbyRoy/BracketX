package com.bracketx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.CompetitiveBand
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.TournamentStatus
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.ErrorRed
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceElevatedDark
import com.bracketx.ui.theme.WarningYellow

@Composable
fun StatusBadge(status: TournamentStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (status) {
        TournamentStatus.DRAFT -> Triple(SurfaceElevatedDark, SecondaryText, "DRAFT")
        TournamentStatus.REGISTRATION_OPEN -> Triple(SuccessGreen.copy(alpha = 0.2f), SuccessGreen, "REGISTRATION OPEN")
        TournamentStatus.REGISTRATION_CLOSED -> Triple(WarningYellow.copy(alpha = 0.2f), WarningYellow, "REGISTRATION CLOSED")
        TournamentStatus.DRAW_PENDING -> Triple(WarningYellow.copy(alpha = 0.2f), WarningYellow, "DRAW PENDING")
        TournamentStatus.DRAW_GENERATED -> Triple(AccentBlue.copy(alpha = 0.2f), AccentBlue, "DRAW PREVIEW")
        TournamentStatus.DRAW_LOCKED -> Triple(AccentBlue.copy(alpha = 0.2f), AccentBlue, "DRAW LOCKED")
        TournamentStatus.IN_PROGRESS -> Triple(SuccessGreen.copy(alpha = 0.25f), SuccessGreen, "LIVE")
        TournamentStatus.COMPLETED -> Triple(SurfaceElevatedDark, SecondaryText, "COMPLETED")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, bgColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GameBadge(game: GameType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevatedDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = game.displayName,
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BandBadge(band: CompetitiveBand?, modifier: Modifier = Modifier) {
    if (band == null) return
    val (bgColor, textColor) = when (band) {
        CompetitiveBand.S -> Triple(Color(0xFFFFD700).copy(alpha = 0.2f), Color(0xFFFFD700), "S")
        CompetitiveBand.A -> Triple(AccentBlue.copy(alpha = 0.2f), AccentBlue, "A")
        CompetitiveBand.B -> Triple(SuccessGreen.copy(alpha = 0.2f), SuccessGreen, "B")
        CompetitiveBand.C -> Triple(WarningYellow.copy(alpha = 0.2f), WarningYellow, "C")
        CompetitiveBand.D -> Triple(SecondaryText.copy(alpha = 0.2f), SecondaryText, "D")
    }.let { it.first to it.second }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = band.code,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
