package com.bracketx.ads

import android.content.Context

/**
 * Service provider for advertisement managers.
 * Follows the existing RepositoryProvider pattern in BracketX.
 */
object AdsProvider {

    val interstitialProvider: InterstitialAdProvider by lazy {
        UnityInterstitialAdProvider()
    }

    /**
     * Initializes the advertising subsystem early at app startup.
     * Non-blocking and non-intrusive.
     */
    fun init(context: Context) {
        interstitialProvider.initialize(context)
    }
}
