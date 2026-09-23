package com.note0292.pmlearn.ui.screens

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
import com.note0292.pmlearn.data.Curriculum
import com.note0292.pmlearn.data.Progress
import java.time.LocalDate

@Composable
fun StatsScreen(
    curriculum: Curriculum,
    progress: Progress,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val allStats = curriculum.questions.map { progress.stat(it.id) }
    val attempts = allStats.sumOf { it.attempts }

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
                    Text("修了レッスン: ${progress.completedLessons.count { curriculum.lesson(it) != null }} / ${curriculum.lessons.size}")
                    Text("のべ解答数: ${attempts}回" + if (attempts > 0) "（正答率 ${percent(allStats.sumOf { it.correct }.toDouble() / attempts)}）" else "")
                    Text("修了テスト: " + (progress.finalExamBest?.let { "最高 ${percent(it)}" + if (it >= Progress.FINAL_PASS_RATE) "（合格）" else "" } ?: "未受験"))
                    Text("「やってみよう」の記入: ${progress.notes.size}件")
                }
            }
            Text("ステージ別の進み具合", style = MaterialTheme.typography.titleMedium)
            curriculum.stages.forEach { stage ->
                val done = progress.completedCount(stage)
                val stats = curriculum.questions.filter { it.stage.id == stage.id }.map { progress.stat(it.id) }
                val stageAttempts = stats.sumOf { it.attempts }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text("STEP ${stage.number} ${stage.title}", modifier = Modifier.weight(1f))
                        Text("$done / ${stage.lessons.size}")
                    }
                    LinearProgressIndicator(
                        progress = { done.toFloat() / stage.lessons.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (stageAttempts == 0) "未学習" else "確認問題の正答率 ${percent(stats.sumOf { it.correct }.toDouble() / stageAttempts)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = { Text("修了状況・解答履歴・「やってみよう」のメモ・学習日数をすべて削除します。よろしいですか？") },
            confirmButton = {
                TextButton(onClick = { onReset(); confirmReset = false }) { Text("リセット") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("キャンセル") } },
        )
    }
}
