package com.bracketx.ads

/**
 * State of the interstitial advertising lifecycle.
 */
enum class AdState {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED,
    LOADING,
    READY,
    SHOWING,
    CONSUMED,
    INITIALIZATION_FAILED,
    LOAD_FAILED,
    SHOW_FAILED
}
