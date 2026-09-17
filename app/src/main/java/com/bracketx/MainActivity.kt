package com.bracketx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.ui.BracketXApp
import com.bracketx.ui.theme.BracketXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryProvider.init(applicationContext)
        setContent {
            BracketXTheme {
                BracketXApp()
            }
        }
    }
}
