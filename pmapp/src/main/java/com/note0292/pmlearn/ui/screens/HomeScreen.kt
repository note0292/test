package com.note0292.pmlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.note0292.pmlearn.data.Curriculum
import com.note0292.pmlearn.data.Progress
import com.note0292.pmlearn.data.QuizBuilder
import com.note0292.pmlearn.data.Stage
import com.note0292.pmlearn.ui.FINAL_EXAM_COUNT
import com.note0292.pmlearn.ui.theme.CorrectColor
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    curriculum: Curriculum,
    progress: Progress,
    onStage: (String) -> Unit,
    onLesson: (String) -> Unit,
    onReview: () -> Unit,
    onFinalExam: () -> Unit,
    onGlossary: () -> Unit,
    onStats: () -> Unit,
) {
    val total = curriculum.lessons.size
    val completed = curriculum.lessons.count { it.id in progress.completedLessons }
    val next = curriculum.firstIncomplete(progress)
    val reviewCount = QuizBuilder.reviewQuestions(curriculum.questions, progress).size

    Scaffold(topBar = { TopAppBar(title = { Text("PM学習ロードマップ") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("未経験からプロダクトマネージャーへ", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else completed.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        SummaryValue("修了レッスン", "$completed / $total")
                        SummaryValue("学習日数", "${progress.studyDays.size}日")
                        SummaryValue("連続学習", "${progress.streak(LocalDate.now())}日")
                    }
                }
            }

            if (next != null) {
                val stage = curriculum.stageOf(next.id)
                Card(onClick = { onLesson(next.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (completed == 0) "まずはここから" else "次に学ぶレッスン",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(next.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "STEP ${stage?.number} ${stage?.title}　約${next.minutes}分",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { onLesson(next.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (next.id in progress.bestScores) "もう一度挑戦する" else "学習をはじめる")
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("全レッスン修了！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("修了テストで総仕上げをしましょう。80%以上で合格です。")
                    }
                }
            }

            Text("ロードマップ", style = MaterialTheme.typography.titleMedium)
            curriculum.stages.forEach { stage ->
                StageCard(stage, progress.completedCount(stage), onClick = { onStage(stage.id) })
            }

            Text("復習とまとめ", style = MaterialTheme.typography.titleMedium)
            MenuItem(Icons.Filled.Replay, "間違えた問題を復習", "前回まちがえた問題（${reviewCount}問）", onReview)
            MenuItem(
                Icons.Filled.EmojiEvents,
                "修了テスト（${FINAL_EXAM_COUNT}問）",
                progress.finalExamBest?.let { "全ステージから出題・最高 ${percent(it)}" } ?: "全ステージから出題・80%以上で合格",
                onFinalExam,
            )
            MenuItem(Icons.AutoMirrored.Filled.MenuBook, "用語集", "現場でよく使う ${curriculum.glossary.size} 語", onGlossary)
            MenuItem(Icons.Filled.BarChart, "学習記録", "ステージ別の進み具合と正答率", onStats)
        }
    }
}

@Composable
private fun StageCard(stage: Stage, completed: Int, onClick: () -> Unit) {
    val total = stage.lessons.size
    val done = completed == total
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (done) CorrectColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (done) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "修了", tint = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            "${stage.number}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "STEP ${stage.number} · ${stage.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stage.title, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else completed.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$completed / $total レッスン修了", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
