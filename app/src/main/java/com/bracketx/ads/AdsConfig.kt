package com.bracketx.ads

import com.stackbyroy.bracketx.BuildConfig

/**
 * Centralized configuration for Unity Ads monetization.
 * Production credentials can be set via local.properties or BuildConfig fields.
 */
object AdsConfig {
    val gameId: String get() = BuildConfig.UNITY_GAME_ID
    val interstitialAdUnitId: String get() = BuildConfig.UNITY_INTERSTITIAL_AD_UNIT_ID
    val isTestMode: Boolean get() = BuildConfig.UNITY_TEST_MODE

    /**
     * 30-minute cooldown duration between launch interstitial presentations.
     */
    const val COOLDOWN_DURATION_MS: Long = 30 * 60 * 1000L

    /**
     * SharedPreferences file and key names for persisting the cooldown timestamp.
     */
    const val PREFS_NAME: String = "bracketx_ads_prefs"
    const val KEY_LAST_INTERSTITIAL_SHOWN_AT: String = "last_interstitial_shown_at"
}
