package com.note0292.pmlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.note0292.pmlearn.ui.AppNavigation
import com.note0292.pmlearn.ui.theme.PmLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PmLearnApp
        setContent {
            PmLearnTheme {
                AppNavigation(app.curriculumRepository.curriculum, app.progressStore)
            }
        }
    }
}
