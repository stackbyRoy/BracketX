package com.bracketx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bracketx.domain.model.Match
import com.bracketx.domain.model.MatchStage
import com.bracketx.domain.model.TournamentParticipant
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BorderSubtle
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SuccessGreen
import com.bracketx.ui.theme.SurfaceCard
import com.bracketx.ui.theme.SurfaceDark
import com.bracketx.ui.theme.SurfaceElevatedDark

@Composable
fun BracketView(
    matches: List<Match>,
    participants: List<TournamentParticipant>,
    isHost: Boolean,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Bracket will appear once draw is locked.", color = SecondaryText)
        }
        return
    }

    val participantMap = participants.associateBy { it.id }
    val rounds = matches.groupBy { it.roundNumber }.toSortedMap()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        rounds.forEach { (roundNumber, roundMatches) ->
            val stageName = roundMatches.firstOrNull()?.stage?.displayName ?: "Round $roundNumber"

            Column(
                modifier = Modifier.width(230.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevatedDark)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stageName.uppercase(),
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Column of matches in this round, vertically spaced
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    roundMatches.sortedBy { it.matchNumber }.forEach { match ->
                        MatchCard(
                            match = match,
                            participantA = match.participantAId?.let { participantMap[it] },
                            participantB = match.participantBId?.let { participantMap[it] },
                            isHost = isHost,
                            onClick = { onMatchClick(match) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    participantA: TournamentParticipant?,
    participantB: TournamentParticipant?,
    isHost: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = match.isCompleted
    val isReady = match.isReady

    val borderColor = when {
        isCompleted -> SuccessGreen.copy(alpha = 0.5f)
        isReady -> AccentBlue.copy(alpha = 0.6f)
        else -> BorderSubtle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = isHost && (isReady || isCompleted), onClick = onClick)
            .padding(10.dp)
    ) {
        // Participant A row
        ParticipantRow(
            participant = participantA,
            score = match.scoreA,
            isWinner = isCompleted && match.winnerId != null && match.winnerId == match.participantAId,
            isBye = match.isBye && match.participantBId == null
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Participant B row
        ParticipantRow(
            participant = participantB,
            score = match.scoreB,
            isWinner = isCompleted && match.winnerId != null && match.winnerId == match.participantBId,
            isBye = match.isBye && match.participantAId == null
        )
    }
}

@Composable
private fun ParticipantRow(
    participant: TournamentParticipant?,
    score: Int?,
    isWinner: Boolean,
    isBye: Boolean
) {
    val textColor = when {
        isWinner -> SuccessGreen
        participant == null -> SecondaryText.copy(alpha = 0.6f)
        else -> PrimaryText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isWinner) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (participant?.seed != null) {
                Text(
                    text = "#${participant.seed}",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp)
                )
            }

            Text(
                text = when {
                    isBye -> "BYE"
                    participant != null -> participant.displayName
                    else -> "TBD"
                },
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (score != null) {
            Text(
                text = score.toString(),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
