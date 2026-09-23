package com.note0292.statprep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.note0292.statprep.ui.AppNavigation
import com.note0292.statprep.ui.theme.StatPrepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StatPrepApp
        setContent {
            StatPrepTheme {
                AppNavigation(app.questionRepository, app.textbookRepository, app.progressStore)
            }
        }
    }
}
