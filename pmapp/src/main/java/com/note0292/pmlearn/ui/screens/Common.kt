package com.note0292.pmlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
            }
        },
        actions = actions,
    )
}

/**
 * 教材の本文を表示する。空行で段落を分け、「・」で始まる行は箇条書き、
 * 「1. 」のように数字とピリオドで始まる行は番号付きの行として字下げする。
 */
@Composable
fun RichText(text: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        text.trim().split(Regex("\n\\s*\n")).forEach { paragraph ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                paragraph.lines().filter { it.isNotBlank() }.forEach { line ->
                    val trimmed = line.trim()
                    val numbered = Regex("^(\\d+\\.)\\s*(.*)").find(trimmed)
                    when {
                        trimmed.startsWith("・") -> Bullet("・", trimmed.removePrefix("・"))
                        numbered != null -> Bullet(numbered.groupValues[1], numbered.groupValues[2])
                        else -> Text(trimmed, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun Bullet(mark: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(mark, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

fun percent(value: Double): String = "${Math.round(value * 100)}%"
