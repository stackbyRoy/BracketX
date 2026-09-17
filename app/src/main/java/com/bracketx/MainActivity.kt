package com.bracketx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.ui.BracketXApp
import com.bracketx.ui.theme.BracketXTheme

class MainActivity : ComponentActivity() {

    private val incomingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryProvider.init(applicationContext)
        incomingIntent.value = intent

        setContent {
            BracketXTheme {
                BracketXApp(incomingIntent = incomingIntent.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntent.value = intent
    }
}
