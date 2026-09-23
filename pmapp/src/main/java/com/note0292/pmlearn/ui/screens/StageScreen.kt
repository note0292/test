package com.note0292.pmlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.note0292.pmlearn.data.Progress
import com.note0292.pmlearn.data.Stage
import com.note0292.pmlearn.ui.theme.CorrectColor

@Composable
fun StageScreen(
    stage: Stage,
    progress: Progress,
    onLesson: (String) -> Unit,
    onBack: () -> Unit,
) {
    val nextId = stage.lessons.firstOrNull { it.id !in progress.completedLessons }?.id

    Scaffold(topBar = { BackTopBar("STEP ${stage.number}", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stage.level, style = MaterialTheme.typography.labelLarge)
                        Text(stage.title, style = MaterialTheme.typography.titleLarge)
                        Text(stage.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${progress.completedCount(stage)} / ${stage.lessons.size} レッスン修了　合計 約${stage.lessons.sumOf { it.minutes }}分",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            itemsIndexed(stage.lessons) { i, lesson ->
                val done = lesson.id in progress.completedLessons
                val best = progress.bestScores[lesson.id]
                Card(onClick = { onLesson(lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        when {
                            done -> Icon(Icons.Filled.CheckCircle, contentDescription = "修了", tint = CorrectColor, modifier = Modifier.size(28.dp))
                            lesson.id == nextId -> Icon(Icons.Filled.PlayCircle, contentDescription = "次のレッスン", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            else -> Icon(Icons.Outlined.Circle, contentDescription = "未修了", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("レッスン ${stage.number}-${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                buildString {
                                    append("約${lesson.minutes}分・確認問題${lesson.quiz.size}問")
                                    if (best != null) append("・最高 ${percent(best)}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
