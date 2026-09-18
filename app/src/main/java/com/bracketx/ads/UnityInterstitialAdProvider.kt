package com.bracketx.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.stackbyroy.bracketx.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import java.util.concurrent.atomic.AtomicBoolean

class UnityInterstitialAdProvider : InterstitialAdProvider,
    IUnityAdsInitializationListener,
    IUnityAdsLoadListener,
    IUnityAdsShowListener {

    companion object {
        private const val TAG = "UnityAds"
    }

    private var applicationContext: Context? = null
    private var prefs: SharedPreferences? = null

    @Volatile
    override var state: AdState = AdState.UNINITIALIZED
        private set

    @Volatile
    override var isInitialized: Boolean = false
        private set

    @Volatile
    override var isReady: Boolean = false
        private set

    // One-shot guard for the single launch evaluation per startup
    private val hasEvaluatedLaunchAd = AtomicBoolean(false)

    // Concurrency guard to prevent duplicate simultaneous show calls
    private val isShowing = AtomicBoolean(false)

    // Current dismissal/completion callback
    private var pendingShowDismissed: (() -> Unit)? = null

    override fun initialize(context: Context) {
        val appContext = context.applicationContext
        this.applicationContext = appContext
        this.prefs = appContext.getSharedPreferences(AdsConfig.PREFS_NAME, Context.MODE_PRIVATE)

        if (isInitialized || state == AdState.INITIALIZING) {
            logDebug("SDK already initialized or initializing")
            return
        }

        val gameId = AdsConfig.gameId
        if (gameId.isBlank() || gameId == "<UNITY_GAME_ID>") {
            logDebug("Unity Game ID not configured. Skipping initialization gracefully.")
            state = AdState.INITIALIZATION_FAILED
            return
        }

        state = AdState.INITIALIZING
        logDebug("SDK initialization started with Game ID: $gameId, TestMode: ${AdsConfig.isTestMode}")

        try {
            UnityAds.initialize(
                appContext,
                gameId,
                AdsConfig.isTestMode,
                this
            )
        } catch (e: Throwable) {
            logError("SDK initialization threw an exception", e)
            state = AdState.INITIALIZATION_FAILED
        }
    }

    override fun preload() {
        if (!isInitialized) {
            logDebug("Cannot preload interstitial: SDK not initialized")
            return
        }

        if (state == AdState.LOADING) {
            logDebug("Interstitial is already loading")
            return
        }

        if (isReady && state == AdState.READY) {
            logDebug("Interstitial already loaded and ready")
            return
        }

        val adUnitId = AdsConfig.interstitialAdUnitId
        state = AdState.LOADING
        logDebug("Interstitial load started for ad unit: $adUnitId")

        try {
            UnityAds.load(adUnitId, this)
        } catch (e: Throwable) {
            logError("UnityAds.load threw an exception", e)
            state = AdState.LOAD_FAILED
            isReady = false
        }
    }

    override fun isCooldownActive(): Boolean {
        val lastShownAt = prefs?.getLong(AdsConfig.KEY_LAST_INTERSTITIAL_SHOWN_AT, 0L) ?: 0L
        if (lastShownAt <= 0L) return false

        val elapsed = System.currentTimeMillis() - lastShownAt
        return elapsed < AdsConfig.COOLDOWN_DURATION_MS
    }

    override fun getRemainingCooldownMs(): Long {
        val lastShownAt = prefs?.getLong(AdsConfig.KEY_LAST_INTERSTITIAL_SHOWN_AT, 0L) ?: 0L
        if (lastShownAt <= 0L) return 0L

        val elapsed = System.currentTimeMillis() - lastShownAt
        val remaining = AdsConfig.COOLDOWN_DURATION_MS - elapsed
        return if (remaining > 0L) remaining else 0L
    }

    override fun shouldShowLaunchAd(): Boolean {
        // Enforce strict one-shot evaluation per app startup cycle
        if (!hasEvaluatedLaunchAd.compareAndSet(false, true)) {
            logDebug("Launch ad already evaluated for this startup cycle. Skipping.")
            return false
        }

        if (isCooldownActive()) {
            val minutesRemaining = (getRemainingCooldownMs() / (60 * 1000L)) + 1
            logDebug("Launch ad cooldown active: ~${minutesRemaining}m remaining. Interstitial skipped: cooldown")
            return false
        }

        if (!isInitialized) {
            logDebug("Interstitial skipped: SDK not initialized")
            return false
        }

        if (!isReady || state != AdState.READY) {
            logDebug("Interstitial skipped: not ready (current state: $state)")
            return false
        }

        logDebug("Launch ad eligible")
        return true
    }

    override fun show(activity: Activity, onShowDismissed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            logDebug("Interstitial show skipped: Activity is invalid/finishing")
            onShowDismissed()
            return
        }

        if (!isShowing.compareAndSet(false, true)) {
            logDebug("Interstitial already showing or show in progress")
            onShowDismissed()
            return
        }

        if (!isReady) {
            logDebug("Interstitial show failed: ad was not marked ready")
            isShowing.set(false)
            onShowDismissed()
            return
        }

        pendingShowDismissed = onShowDismissed
        state = AdState.SHOWING
        logDebug("Interstitial showing")

        try {
            val adUnitId = AdsConfig.interstitialAdUnitId
            UnityAds.show(
                activity,
                adUnitId,
                UnityAdsShowOptions(),
                this
            )
        } catch (e: Throwable) {
            logError("UnityAds.show threw an exception", e)
            state = AdState.SHOW_FAILED
            isShowing.set(false)
            isReady = false
            val callback = pendingShowDismissed
            pendingShowDismissed = null
            callback?.invoke()
            // Opportunistic reload
            preload()
        }
    }

    override fun recordSuccessfulDisplay() {
        val now = System.currentTimeMillis()
        try {
            prefs?.edit()?.putLong(AdsConfig.KEY_LAST_INTERSTITIAL_SHOWN_AT, now)?.apply()
            logDebug("Interstitial cooldown timestamp updated to $now")
        } catch (e: Throwable) {
            logError("Failed to persist cooldown timestamp", e)
        }
    }

    // --- IUnityAdsInitializationListener ---

    override fun onInitializationComplete() {
        isInitialized = true
        state = AdState.INITIALIZED
        logDebug("SDK initialization succeeded")
        // Immediately start preloading the interstitial
        preload()
    }

    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
        isInitialized = false
        state = AdState.INITIALIZATION_FAILED
        logDebug("SDK initialization failed: $error - $message")
    }

    // --- IUnityAdsLoadListener ---

    override fun onUnityAdsAdLoaded(placementId: String?) {
        isReady = true
        state = AdState.READY
        logDebug("Interstitial loaded for placement: $placementId")
    }

    override fun onUnityAdsFailedToLoad(
        placementId: String?,
        error: UnityAds.UnityAdsLoadError?,
        message: String?
    ) {
        isReady = false
        state = AdState.LOAD_FAILED
        logDebug("Interstitial load failed for placement: $placementId, error: $error - $message")
    }

    // --- IUnityAdsShowListener ---

    override fun onUnityAdsShowStart(placementId: String?) {
        logDebug("Interstitial displayed (show started): $placementId")
        // IMPORTANT: Only update cooldown when display starts successfully!
        recordSuccessfulDisplay()
    }

    override fun onUnityAdsShowClick(placementId: String?) {
        logDebug("Interstitial clicked: $placementId")
    }

    override fun onUnityAdsShowComplete(
        placementId: String?,
        state: UnityAds.UnityAdsShowCompletionState?
    ) {
        logDebug("Interstitial closed (completion state: $state)")
        this.state = AdState.CONSUMED
        this.isReady = false
        isShowing.set(false)

        val callback = pendingShowDismissed
        pendingShowDismissed = null
        callback?.invoke()

        // Preload next interstitial for subsequent eligible sessions
        preload()
    }

    override fun onUnityAdsShowFailure(
        placementId: String?,
        error: UnityAds.UnityAdsShowError?,
        message: String?
    ) {
        logDebug("Interstitial show failed: $placementId, error: $error - $message")
        this.state = AdState.SHOW_FAILED
        this.isReady = false
        isShowing.set(false)

        // Note: Do NOT update cooldown on show failure!
        val callback = pendingShowDismissed
        pendingShowDismissed = null
        callback?.invoke()

        // Attempt reload
        preload()
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[$TAG] $message")
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "[$TAG] $message", throwable)
        }
    }
}
