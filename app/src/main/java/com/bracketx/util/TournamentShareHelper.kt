package com.bracketx.util

import android.content.Context
import android.content.Intent
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament

object TournamentShareHelper {
    const val BASE_WEB_URL = "https://bracketx.vercel.app/tournament"

    fun getTournamentUrl(tournamentId: String): String {
        return "$BASE_WEB_URL/$tournamentId"
    }

    fun shareTournament(context: Context, tournament: Tournament) {
        val url = getTournamentUrl(tournament.id)
        val gameName = when (tournament.game) {
            GameType.FC_MOBILE -> "EA SPORTS FC Mobile"
            GameType.EFOOTBALL -> "eFootball"
        }
        val formatName = tournament.format.name.replace('_', ' ')

        val shareText = """
            🏆 Join my tournament on BracketX!
            
            🎮 Tournament: ${tournament.name}
            ⚽ Game: $gameName
            ⚔️ Format: $formatName (${tournament.maxParticipants} players)
            
            Tap the link below to view the bracket or register:
            $url
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Tournament Invite: ${tournament.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareChooser = Intent.createChooser(sendIntent, "Share Tournament Link via")
        context.startActivity(shareChooser)
    }
}
