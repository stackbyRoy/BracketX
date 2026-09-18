package com.bracketx.ads

import android.app.Activity
import android.content.Context

/**
 * Provider interface abstracting interstitial ad operations.
 * Isolates the application from any direct vendor SDK calls.
 */
interface InterstitialAdProvider {
    val state: AdState
    val isInitialized: Boolean
    val isReady: Boolean

    /**
     * Initializes the advertising SDK asynchronously.
     * Non-blocking and fails open if any issue arises.
     */
    fun initialize(context: Context)

    /**
     * Preloads an interstitial ad in the background.
     */
    fun preload()

    /**
     * Evaluates if the 30-minute cooldown is currently active.
     */
    fun isCooldownActive(): Boolean

    /**
     * Returns the remaining cooldown in milliseconds (0 if expired).
     */
    fun getRemainingCooldownMs(): Long

    /**
     * Checks if a launch ad should be displayed at this instant.
     * Must return true ONLY if:
     * - Cooldown has passed
     * - SDK is initialized
     * - Interstitial is loaded and ready
     * - Launch evaluation hasn't already occurred for this app launch cycle
     */
    fun shouldShowLaunchAd(): Boolean

    /**
     * Displays the loaded interstitial using the foreground Activity.
     * @param activity Foreground activity to present the ad on
     * @param onShowDismissed Callback invoked when the ad is closed or fails to show
     */
    fun show(activity: Activity, onShowDismissed: () -> Unit = {})

    /**
     * Updates the persistent cooldown timestamp.
     * Must ONLY be invoked upon verified successful display start.
     */
    fun recordSuccessfulDisplay()
}
