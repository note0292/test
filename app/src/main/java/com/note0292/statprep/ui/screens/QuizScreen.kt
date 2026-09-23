package com.note0292.statprep.ui.screens

import androidx.compose.foundation.BorderStroke
import com.note0292.statprep.ui.math.MathText
import androidx.compose.runtime.key
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.ProgressStore
import com.note0292.statprep.data.Question
import com.note0292.statprep.data.QuizBuilder
import com.note0292.statprep.data.QuizItem
import com.note0292.statprep.data.QuizMode
import com.note0292.statprep.ui.theme.CorrectColor
import com.note0292.statprep.ui.theme.WrongColor
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 準1級（CBT）の試験時間。模擬試験の目安表示に使う。 */
private const val EXAM_MINUTES = 90

private fun modeTitle(mode: QuizMode): String = when (mode) {
    is QuizMode.Random -> "ランダム出題"
    is QuizMode.ByCategory -> mode.category.label
    QuizMode.Weak -> "苦手問題の復習"
    QuizMode.Bookmarked -> "ブックマーク"
    is QuizMode.Mock -> "模擬試験"
}

private fun formatElapsed(millis: Long): String {
    val sec = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}

@Composable
fun QuizScreen(
    mode: QuizMode,
    questions: List<Question>,
    progress: Progress,
    store: ProgressStore,
    onOpenChapter: (Category) -> Unit,
    onExit: () -> Unit,
    vm: QuizViewModel = viewModel(),
) {
    vm.startIfNeeded(immediateFeedback = mode !is QuizMode.Mock) {
        QuizBuilder.build(mode, questions, progress)
    }
    val title = modeTitle(mode)

    when {
        vm.items.isEmpty() -> EmptyQuiz(title, mode, onExit)
        vm.finished -> QuizResult(title, vm, progress, store, onOpenChapter, onExit)
        else -> QuizQuestion(title, mode is QuizMode.Mock, vm, progress, store, onOpenChapter, onExit)
    }
}

@Composable
private fun EmptyQuiz(title: String, mode: QuizMode, onExit: () -> Unit) {
    val message = when (mode) {
        QuizMode.Weak -> "苦手問題はまだありません。\n問題を解くと、間違えた問題がここに集まります。"
        QuizMode.Bookmarked -> "ブックマークした問題はありません。\n問題画面右上のアイコンで保存できます。"
        else -> "出題できる問題がありません。"
    }
    Scaffold(topBar = { BackTopBar(title, onExit) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QuizQuestion(
    title: String,
    isMock: Boolean,
    vm: QuizViewModel,
    progress: Progress,
    store: ProgressStore,
    onOpenChapter: (Category) -> Unit,
    onExit: () -> Unit,
) {
    val item = vm.current ?: return
    val question = item.question
    val bookmarked = question.id in progress.bookmarks

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (isMock) {
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    Scaffold(
        topBar = {
            BackTopBar(title, onExit) {
                if (isMock) {
                    Text(
                        "${formatElapsed(now - vm.startedAt)} / ${EXAM_MINUTES}:00",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                IconButton(onClick = { store.toggleBookmark(question.id) }) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (bookmarked) "ブックマークを外す" else "ブックマークする",
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { (vm.index + 1).toFloat() / vm.items.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("問 ${vm.index + 1} / ${vm.items.size}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = {}, label = { Text(question.categoryEnum.label) })
                }
                // 選択肢を切り替えても再描画しないよう、問題ごとに key を分ける
                key(question.id) { MathText(question.question, fontSizePx = 17) }

                item.choices.forEachIndexed { i, choice ->
                    ChoiceCard(
                        text = choice,
                        selected = vm.selected == i,
                        revealed = vm.revealed,
                        isAnswer = item.answer == i,
                        onClick = { vm.select(i) },
                    )
                }

                if (vm.revealed) {
                    ExplanationCard(
                        correct = vm.selected == item.answer,
                        explanation = question.explanation,
                        category = question.categoryEnum,
                        onOpenChapter = onOpenChapter,
                    )
                }
            }
            Box(Modifier.padding(16.dp)) {
                val isLast = vm.index == vm.items.size - 1
                if (!vm.revealed) {
                    Button(
                        onClick = { vm.submit()?.let { store.recordAnswer(question.id, it) } },
                        enabled = vm.selected != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (isMock) (if (isLast) "解答して採点する" else "解答して次へ") else "解答する")
                    }
                } else {
                    Button(onClick = vm::next, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(if (isLast) "結果を見る" else "次の問題へ")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(text: String, selected: Boolean, revealed: Boolean, isAnswer: Boolean, onClick: () -> Unit) {
    val borderColor = when {
        revealed && isAnswer -> CorrectColor
        revealed && selected -> WrongColor
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    OutlinedCard(
        onClick = onClick,
        enabled = !revealed,
        border = BorderStroke(if (selected || (revealed && isAnswer)) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                revealed && isAnswer -> ResultIcon(true)
                revealed && selected -> ResultIcon(false)
                else -> RadioButton(selected = selected, onClick = onClick, enabled = !revealed)
            }
            Spacer(Modifier.width(8.dp))
            // WebView はタップを横取りするので、上に透明な層を重ねてカード全体で選択できるようにする
            Box(Modifier.weight(1f)) {
                MathText(text)
                if (!revealed) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClick,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultIcon(correct: Boolean) {
    Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        Icon(
            if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (correct) "正解" else "不正解",
            tint = if (correct) CorrectColor else WrongColor,
        )
    }
}

@Composable
private fun ExplanationCard(
    correct: Boolean,
    explanation: String,
    category: Category,
    onOpenChapter: (Category) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (correct) "正解！" else "不正解",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (correct) CorrectColor else WrongColor,
            )
            Text("解説", style = MaterialTheme.typography.labelLarge)
            MathText(explanation, fontSizePx = 15)
            TextButton(onClick = { onOpenChapter(category) }) { Text("テキストで復習する（${category.label}）") }
        }
    }
}

@Composable
private fun QuizResult(
    title: String,
    vm: QuizViewModel,
    progress: Progress,
    store: ProgressStore,
    onOpenChapter: (Category) -> Unit,
    onExit: () -> Unit,
) {
    val total = vm.items.size
    val correct = vm.correctCount
    val wrongItems = vm.items.filterIndexed { i, item -> vm.answers.getOrNull(i) != item.answer }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackTopBar("$title の結果", onExit) }) { padding ->
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
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("$correct / $total 問正解", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("正答率 ${percent(correct.toDouble() / total)}　所要時間 ${formatElapsed(vm.finishedAt - vm.startedAt)}")
                        Text(
                            // 準1級の合格基準はおおむね 60 点（100 点満点）
                            if (correct.toDouble() / total >= 0.6) "合格ライン（60%）に到達しています" else "合格ライン（60%）まであと少し",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (wrongItems.isNotEmpty()) {
                        Button(
                            onClick = { vm.start(wrongItems.map { QuizBuilder.shuffleChoices(it.question, Random.Default) }) },
                            modifier = Modifier.weight(1f),
                        ) { Text("間違えた${wrongItems.size}問を解き直す") }
                    }
                    OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text("ホームへ") }
                }
            }
            item { Text("問題ごとの結果（タップで解説）", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(vm.items) { i, item ->
                ReviewRow(
                    number = i + 1,
                    item = item,
                    chosen = vm.answers.getOrNull(i),
                    expanded = expandedId == item.question.id,
                    bookmarked = item.question.id in progress.bookmarks,
                    onToggle = { expandedId = if (expandedId == item.question.id) null else item.question.id },
                    onBookmark = { store.toggleBookmark(item.question.id) },
                    onOpenChapter = { onOpenChapter(item.question.categoryEnum) },
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(
    number: Int,
    item: QuizItem,
    chosen: Int?,
    expanded: Boolean,
    bookmarked: Boolean,
    onToggle: () -> Unit,
    onBookmark: () -> Unit,
    onOpenChapter: () -> Unit,
) {
    val correct = chosen == item.answer
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = if (correct) "正解" else "不正解",
                    tint = if (correct) CorrectColor else WrongColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "問$number　${item.question.categoryEnum.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onBookmark) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (bookmarked) "ブックマークを外す" else "ブックマークする",
                    )
                }
            }
            if (expanded) {
                MathText(item.question.question, fontSizePx = 15)
                if (!correct && chosen != null) {
                    Text("あなたの解答", color = WrongColor, style = MaterialTheme.typography.labelLarge)
                    MathText(item.choices[chosen], fontSizePx = 15)
                }
                Text("正解", color = CorrectColor, style = MaterialTheme.typography.labelLarge)
                MathText(item.choices[item.answer], fontSizePx = 15)
                Text("解説", style = MaterialTheme.typography.labelLarge)
                MathText(item.question.explanation, fontSizePx = 15)
                TextButton(onClick = onOpenChapter) { Text("テキストで復習する") }
            }
        }
    }
}
