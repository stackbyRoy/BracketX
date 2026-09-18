package com.bracketx.util

import android.content.Context
import android.content.Intent
import com.bracketx.domain.model.GameType
import com.bracketx.domain.model.Tournament
import com.bracketx.domain.model.TournamentVisibility
import com.stackbyroy.bracketx.BuildConfig

object TournamentShareHelper {

    val webBaseUrl: String
        get() = BuildConfig.WEB_BASE_URL.trimEnd('/')

    /**
     * Generates the canonical tournament share URL:
     * https://<web-base-url>/t/{public_id}
     */
    fun getTournamentUrl(tournament: Tournament): String {
        val identifier = tournament.publicId.ifBlank { tournament.id }
        return "$webBaseUrl/t/$identifier"
    }

    fun getTournamentUrlByPublicId(publicId: String): String {
        return "$webBaseUrl/t/$publicId"
    }

    /**
     * Opens native Android share sheet with tournament details and direct link.
     * Note: As per Section 21, the private access code is NEVER included in this share text.
     */
    fun shareTournament(context: Context, tournament: Tournament) {
        val url = getTournamentUrl(tournament)
        val gameName = when (tournament.game) {
            GameType.FC_MOBILE -> "EA SPORTS FC Mobile"
            GameType.EFOOTBALL -> "eFootball"
        }
        val formatName = tournament.format.name.replace('_', ' ')
        val visibilityNotice = if (tournament.visibility == TournamentVisibility.PRIVATE) {
            "\n🔒 Note: This is a private tournament. An access code is required to register."
        } else ""

        val shareText = """
            🏆 Join my tournament on BracketX!
            
            🎮 Tournament: ${tournament.name}
            ⚽ Game: $gameName
            ⚔️ Format: $formatName (${tournament.maxParticipants} players)$visibilityNotice
            
            Tap the link below to view or register:
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
