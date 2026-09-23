package com.note0292.statprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.Question
import com.note0292.statprep.data.QuizBuilder
import com.note0292.statprep.ui.MOCK_COUNT
import com.note0292.statprep.ui.RANDOM_COUNT
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    questions: List<Question>,
    progress: Progress,
    onStartQuiz: (String) -> Unit,
    onCategories: () -> Unit,
    onNotes: () -> Unit,
    onStats: () -> Unit,
) {
    val answered = questions.count { progress.stat(it.id).attempts > 0 }
    val totalCorrect = questions.sumOf { progress.stat(it.id).correct }
    val totalAttempts = questions.sumOf { progress.stat(it.id).attempts }
    val weakCount = QuizBuilder.weakQuestions(questions, progress).size
    val bookmarkCount = questions.count { it.id in progress.bookmarks }
    val streak = progress.streak(LocalDate.now())

    Scaffold(topBar = { TopAppBar(title = { Text("統計検定準1級 対策") }) }) { padding ->
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
                    Text("学習の進み具合", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        SummaryValue("解いた問題", "$answered / ${questions.size}")
                        SummaryValue(
                            "正答率",
                            if (totalAttempts == 0) "−" else percent(totalCorrect.toDouble() / totalAttempts),
                        )
                        SummaryValue("連続学習", "${streak}日")
                    }
                }
            }

            MenuItem(Icons.Filled.Shuffle, "ランダム${RANDOM_COUNT}問", "未回答の問題を優先して出題") {
                onStartQuiz("random")
            }
            MenuItem(Icons.Filled.Category, "分野別に学習", "出題範囲の分野ごとに演習", onCategories)
            MenuItem(Icons.Filled.Replay, "苦手問題を復習", "前回不正解・正答率の低い問題（${weakCount}問）") {
                onStartQuiz("weak")
            }
            MenuItem(Icons.Filled.Bookmark, "ブックマーク", "保存した問題（${bookmarkCount}問）") {
                onStartQuiz("bookmarked")
            }
            MenuItem(Icons.Filled.Timer, "模擬試験（${MOCK_COUNT}問）", "全分野から出題・最後にまとめて採点") {
                onStartQuiz("mock")
            }
            MenuItem(Icons.AutoMirrored.Filled.MenuBook, "公式・要点ノート", "分野ごとの重要公式をまとめて確認", onNotes)
            MenuItem(Icons.Filled.BarChart, "学習記録", "分野別の正答率と進み具合", onStats)
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
