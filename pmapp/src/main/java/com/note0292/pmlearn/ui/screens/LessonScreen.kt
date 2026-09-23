package com.note0292.pmlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.note0292.pmlearn.data.Exercise
import com.note0292.pmlearn.data.Lesson
import com.note0292.pmlearn.data.Progress
import com.note0292.pmlearn.data.Stage
import com.note0292.pmlearn.ui.theme.CorrectColor

@Composable
fun LessonScreen(
    lesson: Lesson,
    stage: Stage,
    progress: Progress,
    onSaveNote: (String) -> Unit,
    onStartQuiz: () -> Unit,
    onBack: () -> Unit,
) {
    val number = stage.lessons.indexOf(lesson) + 1
    val done = lesson.id in progress.completedLessons
    val best = progress.bestScores[lesson.id]

    Scaffold(topBar = { BackTopBar("レッスン ${stage.number}-$number", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "STEP ${stage.number} ${stage.title}　約${lesson.minutes}分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (done) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CorrectColor)
                        Spacer(Modifier.width(6.dp))
                        Text("修了済み（最高 ${percent(best ?: 1.0)}）", color = CorrectColor)
                    }
                }

                InfoCard(Icons.Filled.Flag, "このレッスンのゴール", MaterialTheme.colorScheme.primaryContainer) {
                    Text(lesson.goal, style = MaterialTheme.typography.bodyLarge)
                }

                lesson.sections.forEachIndexed { i, section ->
                    if (i > 0) HorizontalDivider()
                    Text(section.heading, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    RichText(section.body)
                }

                InfoCard(Icons.Filled.Lightbulb, "要点まとめ", MaterialTheme.colorScheme.secondaryContainer) {
                    lesson.keyPoints.forEach { Bullet("✓", it) }
                }

                if (lesson.example.isNotBlank()) {
                    InfoCard(Icons.Filled.Work, "現場ではこうなる", MaterialTheme.colorScheme.surfaceVariant) {
                        RichText(lesson.example)
                    }
                }

                lesson.exercise?.let { exercise ->
                    ExerciseCard(exercise, initial = progress.notes[lesson.id].orEmpty(), onSave = onSaveNote)
                }
            }
            Box(Modifier.padding(16.dp)) {
                Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (done) "理解度チェックをもう一度（${lesson.quiz.size}問）" else "理解度チェックに進む（${lesson.quiz.size}問）")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, color: Color, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, initial: String, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var showAnswer by rememberSaveable { mutableStateOf(false) }
    var showHint by rememberSaveable { mutableStateOf(false) }

    // 画面を離れるときに書いた内容を保存する
    val latestText by rememberUpdatedState(text)
    val latestSave by rememberUpdatedState(onSave)
    DisposableEffect(Unit) {
        onDispose { if (latestText != initial) latestSave(latestText) }
    }

    InfoCard(Icons.Filled.Edit, "やってみよう", MaterialTheme.colorScheme.tertiaryContainer) {
        RichText(exercise.prompt)
        if (exercise.hint.isNotBlank()) {
            if (showHint) {
                Text("ヒント：${exercise.hint}", style = MaterialTheme.typography.bodyMedium)
            } else {
                OutlinedButton(onClick = { showHint = true }) { Text("ヒントを見る") }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("自分の考えを書いてみる（端末に保存されます）") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        if (showAnswer) {
            HorizontalDivider()
            Text("回答例", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            RichText(exercise.sampleAnswer)
        } else {
            Button(onClick = { showAnswer = true; onSave(text) }) {
                Text(if (text.isBlank()) "回答例を見る" else "保存して回答例を見る")
            }
        }
    }
}
