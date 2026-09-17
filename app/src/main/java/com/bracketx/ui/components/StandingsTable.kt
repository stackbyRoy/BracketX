package com.bracketx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.Standing
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceElevatedDark

@Composable
fun StandingsTable(
    groupName: String,
    standings: List<Standing>,
    participants: List<TournamentParticipant>,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    val participantMap = participants.associateBy { it.id }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = groupName.uppercase(),
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceElevatedDark)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            Text("PLAYER", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("P", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            Text("W", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            Text("D", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            Text("L", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
            Text("GD", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
            Text("PTS", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Table Rows
        standings.forEachIndexed { index, standing ->
            val p = participantMap[standing.participantId]
            val isCurrentUser = p?.userId == currentUserId

            val rowBg = if (isCurrentUser) AccentBlue.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent
            val textCol = if (isCurrentUser) AccentBlue else PrimaryText

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(rowBg)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(24.dp)
                )

                Text(
                    text = p?.displayName ?: "Unknown",
                    color = textCol,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(standing.played.toString(), color = SecondaryText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                Text(standing.wins.toString(), color = SecondaryText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                Text(standing.draws.toString(), color = SecondaryText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
                Text(standing.losses.toString(), color = SecondaryText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))

                val gdPrefix = if (standing.goalDifference > 0) "+" else ""
                Text(
                    text = "$gdPrefix${standing.goalDifference}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )

                Text(
                    text = standing.points.toString(),
                    color = textCol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}
