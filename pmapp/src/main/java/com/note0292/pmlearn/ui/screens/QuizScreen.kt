package com.note0292.pmlearn.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.note0292.pmlearn.data.Curriculum
import com.note0292.pmlearn.data.Progress
import com.note0292.pmlearn.data.ProgressStore
import com.note0292.pmlearn.data.QuizBuilder
import com.note0292.pmlearn.data.QuizItem
import com.note0292.pmlearn.data.QuizMode
import com.note0292.pmlearn.ui.theme.CorrectColor
import com.note0292.pmlearn.ui.theme.WrongColor
import kotlin.random.Random

private fun modeTitle(mode: QuizMode, curriculum: Curriculum): String = when (mode) {
    is QuizMode.Lesson -> "理解度チェック：" + (curriculum.lesson(mode.lessonId)?.title ?: "")
    QuizMode.Review -> "間違えた問題を復習"
    is QuizMode.Final -> "修了テスト"
}

@Composable
fun QuizScreen(
    mode: QuizMode,
    curriculum: Curriculum,
    progress: Progress,
    store: ProgressStore,
    onExit: () -> Unit,
    onOpenLesson: (String) -> Unit,
    vm: QuizViewModel = viewModel(),
) {
    vm.startIfNeeded(immediateFeedback = mode !is QuizMode.Final) {
        QuizBuilder.build(mode, curriculum, progress)
    }
    val title = modeTitle(mode, curriculum)

    if (vm.finished) {
        LaunchedEffect(vm.startedAt) {
            if (!vm.resultRecorded && vm.items.isNotEmpty()) {
                vm.resultRecorded = true
                val score = vm.correctCount.toDouble() / vm.items.size
                when (mode) {
                    is QuizMode.Lesson -> store.recordLessonQuiz(mode.lessonId, score)
                    is QuizMode.Final -> store.recordFinalExam(score)
                    QuizMode.Review -> Unit
                }
            }
        }
    }

    when {
        vm.items.isEmpty() -> EmptyQuiz(title, onExit)
        vm.finished -> QuizResult(title, mode, curriculum, vm, onExit, onOpenLesson)
        else -> QuizQuestion(title, mode, vm, store, onExit)
    }
}

@Composable
private fun EmptyQuiz(title: String, onExit: () -> Unit) {
    Scaffold(topBar = { BackTopBar(title, onExit) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "復習する問題はありません。\nレッスンの理解度チェックで間違えた問題が、ここに集まります。",
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QuizQuestion(
    title: String,
    mode: QuizMode,
    vm: QuizViewModel,
    store: ProgressStore,
    onExit: () -> Unit,
) {
    val item = vm.current ?: return
    val question = item.question

    Scaffold(topBar = { BackTopBar(title, onExit) }) { padding ->
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
                Text("問 ${vm.index + 1} / ${vm.items.size}", style = MaterialTheme.typography.titleMedium)
                if (mode !is QuizMode.Lesson) {
                    AssistChip(onClick = {}, label = { Text("STEP ${question.stage.number}：${question.lesson.title}") })
                }
                Text(question.question.question, style = MaterialTheme.typography.bodyLarge)

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
                    ExplanationCard(correct = vm.selected == item.answer, explanation = question.question.explanation)
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
                        Text(
                            when {
                                vm.immediateFeedback -> "解答する"
                                isLast -> "解答して採点する"
                                else -> "解答して次へ"
                            },
                        )
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
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
private fun ExplanationCard(correct: Boolean, explanation: String) {
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
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuizResult(
    title: String,
    mode: QuizMode,
    curriculum: Curriculum,
    vm: QuizViewModel,
    onExit: () -> Unit,
    onOpenLesson: (String) -> Unit,
) {
    val total = vm.items.size
    val correct = vm.correctCount
    val score = correct.toDouble() / total
    val wrongItems = vm.items.filterIndexed { i, item -> vm.answers.getOrNull(i) != item.answer }
    var expandedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val passRate = if (mode is QuizMode.Final) Progress.FINAL_PASS_RATE else Progress.PASS_RATE
    val passed = score >= passRate
    val message = when (mode) {
        is QuizMode.Lesson ->
            if (passed) "合格です！このレッスンを修了しました。" else "合格ライン（${percent(passRate)}）まであと少し。解説を読んで、もう一度挑戦しましょう。"
        is QuizMode.Final ->
            if (passed) "修了テスト合格！PMとして働くための基礎知識が身についています。" else "合格ラインは${percent(passRate)}です。間違えた問題のレッスンを復習しましょう。"
        QuizMode.Review -> "正解した問題は復習リストから外れます。"
    }
    val nextLesson = (mode as? QuizMode.Lesson)?.let { curriculum.nextLesson(it.lessonId) }

    Scaffold(topBar = { BackTopBar("結果", onExit) }) { padding ->
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                        Text("$correct / $total 問正解", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("正答率 ${percent(score)}")
                        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (mode) {
                        is QuizMode.Lesson -> {
                            if (passed && nextLesson != null) {
                                Button(onClick = { onOpenLesson(nextLesson.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("次のレッスンへ：${nextLesson.title}")
                                }
                            }
                            if (!passed) {
                                Button(
                                    onClick = { vm.start(QuizBuilder.build(mode, curriculum, Progress())) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("もう一度挑戦する") }
                            }
                            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                                Text("レッスンに戻る")
                            }
                        }
                        else -> {
                            if (wrongItems.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        vm.start(wrongItems.map { QuizBuilder.shuffleChoices(it.question, Random.Default) })
                                        // 解き直しは成績として記録しない
                                        vm.resultRecorded = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("間違えた${wrongItems.size}問を解き直す") }
                            }
                            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("ホームへ") }
                        }
                    }
                }
            }
            item { Text("問題ごとの結果（タップで解説）", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(vm.items) { i, item ->
                ReviewRow(
                    number = i + 1,
                    item = item,
                    chosen = vm.answers.getOrNull(i),
                    expanded = expandedIndex == i,
                    onToggle = { expandedIndex = if (expandedIndex == i) null else i },
                    onOpenLesson = { onOpenLesson(item.question.lesson.id) }.takeIf { mode !is QuizMode.Lesson },
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
    onToggle: () -> Unit,
    onOpenLesson: (() -> Unit)?,
) {
    val correct = chosen == item.answer
    val question = item.question
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
                    "問$number　${question.question.question}",
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (expanded) {
                if (!correct && chosen != null) {
                    Text("あなたの解答: ${item.choices[chosen]}", color = WrongColor, style = MaterialTheme.typography.bodyMedium)
                }
                Text("正解: ${item.choices[item.answer]}", color = CorrectColor, style = MaterialTheme.typography.bodyMedium)
                Text(question.question.explanation, style = MaterialTheme.typography.bodyMedium)
                if (onOpenLesson != null) {
                    TextButton(onClick = onOpenLesson) {
                        Text("レッスン「${question.lesson.title}」を読み直す")
                    }
                }
            }
        }
    }
}
