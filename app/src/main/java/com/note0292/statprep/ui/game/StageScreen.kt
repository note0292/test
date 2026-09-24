package com.note0292.statprep.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import com.note0292.statprep.data.ProgressStore
import com.note0292.statprep.data.game.Curriculum
import com.note0292.statprep.data.game.GameEngine
import com.note0292.statprep.data.game.StageOutcome
import com.note0292.statprep.data.game.StageType
import com.note0292.statprep.data.game.StudyCard
import com.note0292.statprep.ui.math.MathText
import com.note0292.statprep.ui.screens.ChoiceCard
import com.note0292.statprep.ui.screens.ExplanationCard
import com.note0292.statprep.ui.screens.percent
import com.note0292.statprep.ui.theme.CorrectColor
import com.note0292.statprep.ui.theme.WrongColor
import kotlinx.coroutines.delay
import java.time.LocalDate

private fun clock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

fun stageLabel(curriculum: Curriculum, day: Int): String {
    if (day == Curriculum.BONUS_DAY) return "おかわり復習"
    val plan = curriculum.day(day)
    return when (plan.type) {
        StageType.BOSS -> "Day $day　ボス戦"
        StageType.FINAL -> "Day $day　最終試験"
        else -> "Day $day"
    }
}

@Composable
fun StageScreen(
    day: Int,
    curriculum: Curriculum,
    store: ProgressStore,
    onExit: () -> Unit,
    vm: StageViewModel = viewModel(),
) {
    vm.startIfNeeded { GameEngine.buildStage(curriculum, store.game.value, day, LocalDate.now()) }
    val title = stageLabel(curriculum, day)

    // ステージが終わったら 1 度だけ結果を確定して保存する
    if (vm.finished && vm.outcome == null && vm.items.isNotEmpty()) {
        LaunchedEffect(vm.finishedAt) {
            val outcome = GameEngine.complete(
                curriculum, store.game.value, day, vm.items, vm.results.toList(), vm.elapsedSeconds, LocalDate.now(),
            )
            store.setGame(outcome.state)
            vm.finish(outcome)
        }
    }

    val outcome = vm.outcome
    when {
        vm.items.isEmpty() -> EmptyStage(title, onExit)
        outcome != null -> StageResult(title, day, curriculum, outcome, vm, store, onExit)
        vm.finished -> Box(Modifier.fillMaxSize())
        else -> StagePlay(title, vm, store, onExit)
    }
}

@Composable
private fun EmptyStage(title: String, onExit: () -> Unit) {
    Scaffold(topBar = { StageTopBar(title, onExit) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text("今は復習するカードがありません。\nまた明日挑戦しましょう。", textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageTopBar(title: String, onClose: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "やめる") }
        },
        actions = { actions() },
    )
}

@Composable
private fun StagePlay(title: String, vm: StageViewModel, store: ProgressStore, onExit: () -> Unit) {
    val item = vm.current ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmExit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    BackHandler { confirmExit = true }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("ステージをやめますか？") },
            text = { Text("途中でやめると、このステージはクリアになりません。") },
            confirmButton = { TextButton(onClick = onExit) { Text("やめる") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("続ける") } },
        )
    }

    val elapsed = (now - vm.startedAt) / 1000
    val over = elapsed > GameEngine.TARGET_SECONDS
    Scaffold(
        topBar = {
            StageTopBar(title, { confirmExit = true }) {
                Text(
                    "⏱ ${clock(elapsed)} / ${clock(GameEngine.TARGET_SECONDS.toLong())}",
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val progress by animateFloatAsState(vm.index.toFloat() / vm.items.size, tween(400), label = "progress")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${vm.index + 1} / ${vm.items.size}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(12.dp))
                    AssistChip(onClick = {}, label = { Text(kindLabel(item.card, item.isNew)) })
                    Spacer(Modifier.weight(1f))
                    if (vm.combo >= 2) {
                        Text("🔥 ${vm.combo}コンボ", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    }
                }
                Text(item.card.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                key(vm.index, item.card.id) {
                    when (val card = item.card) {
                        is StudyCard.QuestionCard -> QuestionBody(vm, card)
                        is StudyCard.TheoremCard -> TheoremBody(vm, card, item.isNew)
                        is StudyCard.ExampleCard -> ExampleBody(vm, card)
                    }
                }
            }
            Box(Modifier.padding(16.dp)) {
                StageButtons(vm, item.card, item.isNew, store)
            }
        }
    }
}

private fun kindLabel(card: StudyCard, isNew: Boolean): String {
    val kind = when (card) {
        is StudyCard.QuestionCard -> "問題"
        is StudyCard.TheoremCard -> "定理"
        is StudyCard.ExampleCard -> "例題"
    }
    return if (isNew) "NEW $kind" else "復習 $kind"
}

@Composable
private fun QuestionBody(vm: StageViewModel, card: StudyCard.QuestionCard) {
    val quiz = vm.currentQuiz ?: return
    MathText(card.question.question, fontSizePx = 17)
    quiz.choices.forEachIndexed { i, choice ->
        ChoiceCard(
            text = choice,
            selected = vm.selected == i,
            revealed = vm.revealed,
            isAnswer = quiz.answer == i,
            onClick = { vm.select(i) },
        )
    }
    if (vm.revealed) {
        ExplanationCard(
            correct = vm.selected == quiz.answer,
            explanation = card.question.explanation,
            category = card.category,
            onOpenChapter = null,
        )
    }
}

@Composable
private fun TheoremBody(vm: StageViewModel, card: StudyCard.TheoremCard, isNew: Boolean) {
    Text(card.lessonTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MathText("【定理】" + card.theorem.title, fontSizePx = 17)
            if (isNew || vm.revealed) {
                MathText(card.theorem.statement, fontSizePx = 16)
            } else {
                Text("内容を思い出してから「答えを見る」をタップ", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (isNew || vm.revealed) {
        TextButton(onClick = vm::toggleProof) { Text(if (vm.showProof) "証明を閉じる" else "証明を見る") }
        if (vm.showProof) MathText(card.theorem.proof, fontSizePx = 15)
    }
}

@Composable
private fun ExampleBody(vm: StageViewModel, card: StudyCard.ExampleCard) {
    Text(card.lessonTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) { MathText("【例題】" + card.example.question, fontSizePx = 16) }
    }
    if (vm.revealed) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("解答", style = MaterialTheme.typography.labelLarge)
                MathText(card.example.solution, fontSizePx = 15)
            }
        }
    } else {
        Text("まず自分で解き方を考えてみよう", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StageButtons(vm: StageViewModel, card: StudyCard, isNew: Boolean, store: ProgressStore) {
    val full = Modifier.fillMaxWidth().height(52.dp)
    when {
        card is StudyCard.QuestionCard && !vm.revealed ->
            Button(
                onClick = { vm.submitChoice()?.let { store.recordAnswer(card.question.id, it) } },
                enabled = vm.selected != null,
                modifier = full,
            ) { Text("解答する") }

        card is StudyCard.QuestionCard -> Button(onClick = vm::next, modifier = full) { Text("次へ") }

        isNew && card is StudyCard.TheoremCard -> Button(onClick = { vm.grade(true) }, modifier = full) { Text("覚えた！") }

        !vm.revealed -> Button(onClick = vm::reveal, modifier = full) { Text("答えを見る") }

        isNew -> Button(onClick = { vm.grade(true) }, modifier = full) { Text("理解した！") }

        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.grade(false) }, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(if (card is StudyCard.ExampleCard) "解けなかった" else "あやしい")
            }
            Button(onClick = { vm.grade(true) }, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(if (card is StudyCard.ExampleCard) "解けた" else "覚えていた")
            }
        }
    }
}

@Composable
private fun StageResult(
    title: String,
    day: Int,
    curriculum: Curriculum,
    outcome: StageOutcome,
    vm: StageViewModel,
    store: ProgressStore,
    onExit: () -> Unit,
) {
    val exam = day != Curriculum.BONUS_DAY && curriculum.day(day).type.let { it == StageType.BOSS || it == StageType.FINAL }
    Scaffold(topBar = { StageTopBar(title, onExit) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when {
                    !outcome.cleared -> "あと一歩…"
                    day == Curriculum.BONUS_DAY -> "復習完了！"
                    exam -> "撃破！"
                    else -> "ステージクリア！"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (outcome.cleared) {
                Text(
                    "★".repeat(outcome.stars) + "☆".repeat(3 - outcome.stars),
                    fontSize = 44.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (outcome.graded > 0) {
                Text("${outcome.correct} / ${outcome.graded} 正解　⏱ ${clock(vm.elapsedSeconds.toLong())}")
            }
            if (!outcome.cleared) {
                val need = Math.ceil(outcome.graded * GameEngine.PASS_RATE).toInt()
                Text(
                    "クリアには ${need} 問以上の正解が必要です。解説を確認して、もう一度挑戦しよう。",
                    textAlign = TextAlign.Center,
                    color = WrongColor,
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("獲得 XP　+${outcome.xpGained}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    outcome.xpBreakdown.forEach { (label, xp) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text("+$xp", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (outcome.levelAfter > outcome.levelBefore) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("レベルアップ！ Lv ${outcome.levelAfter}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("称号：${GameEngine.title(outcome.levelAfter)}")
                    }
                }
            }
            outcome.newBadges.forEach { badge ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🏅 バッジ獲得「${badge.title}」", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(badge.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "合格力　${percent(outcome.readinessBefore)} → ${percent(outcome.readinessAfter)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { outcome.readinessAfter.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (outcome.readinessAfter >= GameEngine.READY_THRESHOLD) CorrectColor else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "出題範囲のカードがどれだけ定着したかの目安です（目標 ${percent(GameEngine.READY_THRESHOLD)}）。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (!outcome.cleared) {
                Button(
                    onClick = { vm.start(GameEngine.buildStage(curriculum, store.game.value, day, LocalDate.now())) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("もう一度挑戦する") }
            }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("ホームへ") }
        }
    }
}
