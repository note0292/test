package com.note0292.statprep.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5CAA),
    secondary = Color(0xFF4F6F52),
    tertiary = Color(0xFFB2542B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABC7FF),
    secondary = Color(0xFFB5D0B6),
    tertiary = Color(0xFFFFB597),
)

val CorrectColor = Color(0xFF2E7D32)
val WrongColor = Color(0xFFC62828)

@Composable
fun StatPrepTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
