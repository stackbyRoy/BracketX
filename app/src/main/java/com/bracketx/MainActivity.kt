package com.bracketx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bracketx.ads.AdsProvider
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.ui.BracketXApp
import com.bracketx.ui.screens.SplashScreen
import com.bracketx.ui.theme.BracketXTheme

class MainActivity : ComponentActivity() {

    private val incomingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryProvider.init(applicationContext)
        AdsProvider.init(applicationContext)
        incomingIntent.value = intent

        setContent {
            BracketXTheme {
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 400),
                    label = "SplashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onAdCheckpoint = {
                                if (AdsProvider.interstitialProvider.shouldShowLaunchAd()) {
                                    AdsProvider.interstitialProvider.show(this@MainActivity) {
                                        showSplash = false
                                    }
                                }
                            },
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        BracketXApp(incomingIntent = incomingIntent.value)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntent.value = intent
    }
}
