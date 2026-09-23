package com.note0292.statprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.Chapter
import com.note0292.statprep.data.Example
import com.note0292.statprep.data.FormulaNotes
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.TextbookRepository
import com.note0292.statprep.data.Theorem
import com.note0292.statprep.ui.theme.CorrectColor

private fun chapterLabel(category: Category): String =
    "第${category.ordinal + 1}章　${category.label}" + if (category.optional) "（発展）" else ""

/** 章の一覧。 */
@Composable
fun TextbookScreen(
    categories: List<Category>,
    textbook: TextbookRepository,
    progress: Progress,
    onSelect: (Category) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar("テキスト", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "各章は「解説 → 定理と証明 → 例題」の順に構成されています。読み終えたら章末の練習問題で理解を確認しましょう。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(categories) { category ->
                val lessons = textbook.chapter(category)?.lessons.orEmpty()
                val read = lessons.count { it.id in progress.readLessons }
                Card(onClick = { onSelect(category) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(chapterLabel(category), style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { if (lessons.isEmpty()) 0f else read.toFloat() / lessons.size },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "読了 $read / ${lessons.size} レッスン",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** 章の目次・公式まとめ・練習問題への導線。 */
@Composable
fun ChapterScreen(
    category: Category,
    chapter: Chapter?,
    questionCount: Int,
    progress: Progress,
    onLesson: (String) -> Unit,
    onPractice: () -> Unit,
    onBack: () -> Unit,
) {
    var formulasOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { BackTopBar(category.label, onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column {
                    Text(chapterLabel(category), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(chapter?.intro.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            itemsIndexed(chapter?.lessons.orEmpty()) { i, lesson ->
                val read = lesson.id in progress.readLessons
                Card(onClick = { onLesson(lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (read) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (read) "読了" else "未読",
                            tint = if (read) CorrectColor else MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${category.ordinal + 1}.${i + 1}　${lesson.title}", style = MaterialTheme.typography.titleSmall)
                            val extras = buildList {
                                if (lesson.theorems.isNotEmpty()) add("定理 ${lesson.theorems.size}")
                                if (lesson.examples.isNotEmpty()) add("例題 ${lesson.examples.size}")
                            }
                            if (extras.isNotEmpty()) {
                                Text(
                                    extras.joinToString("・"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            item {
                OutlinedCard(onClick = { formulasOpen = !formulasOpen }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("公式まとめ", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Icon(if (formulasOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        if (formulasOpen) {
                            FormulaNotes.notes[category].orEmpty().forEach { formula ->
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Text(formula.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(formula.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = onPractice, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = questionCount > 0) {
                    Text("この章の練習問題を解く（${questionCount}問）")
                }
            }
        }
    }
}

/** 1 レッスンの本文。開いた時点で読了として記録する。 */
@Composable
fun LessonScreen(
    chapter: Chapter,
    index: Int,
    onRead: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
    onPractice: () -> Unit,
    onBack: () -> Unit,
) {
    val lesson = chapter.lessons[index]
    val category = chapter.categoryEnum
    val number = "${category.ordinal + 1}.${index + 1}"
    LaunchedEffect(lesson.id) { onRead(lesson.id) }

    Scaffold(topBar = { BackTopBar(category.label, onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("$number　${lesson.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SelectionContainer { Text(lesson.body, style = MaterialTheme.typography.bodyLarge) }

            lesson.theorems.forEachIndexed { i, theorem -> TheoremCard("$number.${i + 1}", theorem) }
            lesson.examples.forEachIndexed { i, example -> ExampleCard(i + 1, example) }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val prev = chapter.lessons.getOrNull(index - 1)
                val next = chapter.lessons.getOrNull(index + 1)
                OutlinedButton(onClick = { prev?.let { onOpenLesson(it.id) } }, enabled = prev != null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    Text("前へ")
                }
                if (next != null) {
                    Button(onClick = { onOpenLesson(next.id) }, modifier = Modifier.weight(1f)) {
                        Text("次へ")
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                } else {
                    Button(onClick = onPractice, modifier = Modifier.weight(1f)) { Text("練習問題へ") }
                }
            }
        }
    }
}

@Composable
private fun TheoremCard(number: String, theorem: Theorem) {
    var showProof by rememberSaveable(number) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("定理 $number（${theorem.title}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SelectionContainer { Text(theorem.statement, style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = { showProof = !showProof }) {
                Text(if (showProof) "証明を閉じる" else "証明を見る")
            }
            if (showProof) {
                HorizontalDivider()
                Text("証明", style = MaterialTheme.typography.labelLarge)
                SelectionContainer { Text(theorem.proof + "　∎", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun ExampleCard(number: Int, example: Example) {
    var showSolution by rememberSaveable(example.question) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("例題 $number") })
            SelectionContainer { Text(example.question, style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = { showSolution = !showSolution }) {
                Text(if (showSolution) "解答を閉じる" else "解答を見る")
            }
            if (showSolution) {
                HorizontalDivider()
                SelectionContainer { Text(example.solution, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
