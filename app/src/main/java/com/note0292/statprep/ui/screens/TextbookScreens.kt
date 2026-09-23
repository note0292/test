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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.CategoryKind
import com.note0292.statprep.data.Chapter
import com.note0292.statprep.data.Formula
import com.note0292.statprep.data.Lesson
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.TextbookRepository
import com.note0292.statprep.ui.math.MathBlock
import com.note0292.statprep.ui.math.MathHtml
import com.note0292.statprep.ui.math.MathPage
import com.note0292.statprep.ui.theme.CorrectColor

private fun chapterLabel(category: Category): String =
    category.chapterTitle + when (category.kind) {
        CategoryKind.MATH -> "（準備）"
        CategoryKind.ADVANCED -> "（発展）"
        CategoryKind.CORE -> ""
    }

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
                    "各章は「解説 → 定理と証明 → 例題」の順に構成されています。読み終えたら章末の練習問題で理解を確認しましょう。\n基礎1・2（微分積分・線形代数）は、統計で使う数学を高校レベルから大学レベルまで必要な部分に絞って解説した準備の章です。",
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
                            Text("${category.number}.${i + 1}　${lesson.title}", style = MaterialTheme.typography.titleSmall)
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
                            Spacer(Modifier.height(4.dp))
                            MathBlock(formulasHtml(chapter?.formulas.orEmpty()), fontSizePx = 15)
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

private fun formulasHtml(formulas: List<Formula>): String = formulas.joinToString("") { f ->
    "<div class=\"formula-title\">${MathHtml.escape(f.title)}</div><div>${MathHtml.escape(f.body)}</div>"
}

/** レッスン本文・定理と証明・例題を 1 枚の HTML にまとめる。証明と解答は折りたたんでおく。 */
private fun lessonHtml(number: String, lesson: Lesson): String = buildString {
    append("<h1>").append(MathHtml.escape("$number　${lesson.title}")).append("</h1>")
    append("<div>").append(MathHtml.escape(lesson.body)).append("</div>")
    lesson.theorems.forEachIndexed { i, t ->
        append("<div class=\"card thm\"><div class=\"label\">")
        append(MathHtml.escape("定理 $number.${i + 1}（${t.title}）"))
        append("</div><div>").append(MathHtml.escape(t.statement)).append("</div>")
        append("<details><summary>証明を見る</summary><div>")
        append(MathHtml.escape(t.proof)).append("<span class=\"qed\">∎</span></div></details></div>")
    }
    lesson.examples.forEachIndexed { i, e ->
        append("<div class=\"card ex\"><div class=\"label\">例題 ${i + 1}</div><div>")
        append(MathHtml.escape(e.question)).append("</div>")
        append("<details><summary>解答を見る</summary><div>")
        append(MathHtml.escape(e.solution)).append("</div></details></div>")
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
    val number = "${category.number}.${index + 1}"
    LaunchedEffect(lesson.id) { onRead(lesson.id) }
    val html = remember(lesson.id) { lessonHtml(number, lesson) }

    Scaffold(topBar = { BackTopBar(category.label, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MathPage(html, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp))
            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
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
