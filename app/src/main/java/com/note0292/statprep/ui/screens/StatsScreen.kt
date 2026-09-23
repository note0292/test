package com.note0292.statprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.Question
import com.note0292.statprep.ui.theme.CorrectColor
import com.note0292.statprep.ui.theme.WrongColor
import java.time.LocalDate

@Composable
fun StatsScreen(
    categories: List<Category>,
    questions: List<Question>,
    progress: Progress,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar("学習記録", onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("学習日数: ${progress.studyDays.size}日（連続 ${progress.streak(LocalDate.now())}日）")
                    Text("のべ解答数: ${progress.stats.values.sumOf { it.attempts }}回")
                    Text("読んだレッスン: ${progress.readLessons.size}")
                }
            }
            Text("分野別の正答率", style = MaterialTheme.typography.titleMedium)
            Text(
                "正答率の低い分野から「分野別に学習」や「苦手問題を復習」で取り組むのがおすすめです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            categories.forEach { category ->
                val stats = questions.filter { it.category == category.id }.map { progress.stat(it.id) }
                val attempts = stats.sumOf { it.attempts }
                val accuracy = if (attempts == 0) null else stats.sumOf { it.correct }.toDouble() / attempts
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(category.label, modifier = Modifier.weight(1f))
                        Text(accuracy?.let(::percent) ?: "未学習")
                    }
                    LinearProgressIndicator(
                        progress = { accuracy?.toFloat() ?: 0f },
                        color = if ((accuracy ?: 0.0) >= 0.6) CorrectColor else WrongColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("学習記録をリセット")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("学習記録をリセット") },
            text = { Text("解答履歴・ブックマーク・読んだレッスン・学習日数をすべて削除します。よろしいですか？") },
            confirmButton = {
                TextButton(onClick = { onReset(); confirmReset = false }) { Text("リセット") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("キャンセル") } },
        )
    }
}
