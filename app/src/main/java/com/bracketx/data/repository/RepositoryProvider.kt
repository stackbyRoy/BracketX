package com.bracketx.data.repository

import android.content.Context

object RepositoryProvider {

    val authRepository: AuthRepository by lazy { SupabaseAuthRepository() }
    val tournamentRepository: TournamentRepository by lazy { SupabaseTournamentRepository() }

    fun init(context: Context) {
        (authRepository as? SupabaseAuthRepository)?.initPreferences(context)
    }
}
