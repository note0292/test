package com.note0292.statprep.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.note0292.statprep.data.game.Badge
import com.note0292.statprep.data.game.Curriculum
import com.note0292.statprep.data.game.GameEngine
import com.note0292.statprep.data.game.GameState
import com.note0292.statprep.data.game.StageType
import com.note0292.statprep.data.game.World
import com.note0292.statprep.ui.screens.BackTopBar
import com.note0292.statprep.ui.screens.percent
import com.note0292.statprep.ui.theme.CorrectColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHomeScreen(
    state: GameState,
    curriculum: Curriculum,
    today: LocalDate,
    onBegin: () -> Unit,
    onPlay: (day: Int) -> Unit,
    onMap: () -> Unit,
    onBadges: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("準1級クエスト") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "設定") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.started) {
                WelcomeCard(onBegin)
            } else {
                StatusCard(state, curriculum, today)
                TodayCard(state, curriculum, today, onPlay)
                if (!state.completed) WorldCard(curriculum.world(curriculum.day(state.nextDay).world), state, onMap)
            }
            MenuRow(Icons.Filled.Map, "冒険の地図", "6つのワールド・180日の道のり", onMap)
            MenuRow(Icons.Filled.EmojiEvents, "バッジ", "${state.badges.size} / ${Badge.entries.size} 獲得", onBadges)
            MenuRow(Icons.AutoMirrored.Filled.MenuBook, "自習モード", "テキスト・分野別演習・模擬試験など", onLibrary)
        }
    }
}

@Composable
private fun WelcomeCard(onBegin: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1日3分、180日で準1級へ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "毎日1ステージ（約3分）をクリアして、6つのワールドを進む冒険です。\n" +
                    "・微分積分・線形代数の準備から始めて、準1級の全分野を順番に学びます\n" +
                    "・覚えたカードは、忘れかけた頃に復習として出てきます\n" +
                    "・各ワールドの最後はボス戦（6割正解で撃破）\n" +
                    "・最後の30日は総仕上げと最終試験\n" +
                    "・休んでも大丈夫。遅れた日は1日2ステージまで遊べます",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("冒険をはじめる") }
        }
    }
}

@Composable
private fun StatusCard(state: GameState, curriculum: Curriculum, today: LocalDate) {
    val level = GameEngine.level(state.xp)
    val from = GameEngine.xpForLevel(level)
    val to = GameEngine.xpForLevel(level + 1)
    val readiness = GameEngine.readiness(curriculum, state)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lv $level", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text(GameEngine.title(level), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${state.xp} XP", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { (state.xp - from).toFloat() / (to - from) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Stat("🔥 ${state.streak(today)}日", "連続クリア")
                Stat("${minOf(state.clearedDays, Curriculum.TOTAL_DAYS)} / ${Curriculum.TOTAL_DAYS}", "クリア日数")
                Stat(percent(readiness), "合格力")
            }
            LinearProgressIndicator(
                progress = { readiness.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (readiness >= GameEngine.READY_THRESHOLD) CorrectColor else MaterialTheme.colorScheme.primary,
            )
            state.examDate()?.let {
                Text(
                    "受験の目安日：${it.format(DATE_FORMAT)}（合格力 ${percent(GameEngine.READY_THRESHOLD)} が目標）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TodayCard(state: GameState, curriculum: Curriculum, today: LocalDate, onPlay: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.completed) {
                val ready = GameEngine.readiness(curriculum, state) >= GameEngine.READY_THRESHOLD
                Text("🎉 全180ステージ制覇！", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (ready) "合格力が目標に届きました。いつでも試験を受けられる状態です。"
                    else "合格力を目標まで上げるため、おかわり復習と自習モードの模擬試験を続けましょう。",
                )
                BonusButton(state, curriculum, today, onPlay)
                return@Column
            }
            val plan = curriculum.day(state.nextDay)
            val canPlay = GameEngine.canPlay(state, today)
            val behind = state.daysBehind(today)
            Text(
                "ワールド${plan.world}　${curriculum.world(plan.world).name}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "Day ${plan.day}　" + when (plan.type) {
                    StageType.BOSS -> "👾 ボス戦"
                    StageType.FINAL -> "🏰 最終試験"
                    StageType.REVIEW -> "総仕上げ"
                    else -> plan.title
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when (plan.type) {
                    StageType.LEARN -> "新しいカード ${plan.newCardIds.size} 枚 ＋ 復習　" + plan.categories.joinToString("・") { it.label }
                    StageType.BOSS -> "このワールドで学んだ問題から ${GameEngine.BOSS_QUESTIONS} 問。6割正解で撃破"
                    StageType.REVIEW -> "忘れかけたカードと苦手を集中復習"
                    StageType.FINAL -> "準1級の全分野から ${GameEngine.FINAL_QUESTIONS} 問。6割正解で合格"
                    StageType.BONUS -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (canPlay) {
                if (behind > 0 && state.clearsOn(today) > 0) {
                    Text("予定より遅れているので、今日はもう1ステージ遊べます。", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { onPlay(plan.day) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("スタート（約3分）", fontSize = 18.sp)
                }
            } else {
                Text("✅ 今日のステージはクリア済み。また明日！", fontWeight = FontWeight.Bold)
                BonusButton(state, curriculum, today, onPlay)
            }
            if (behind > 1) {
                Text("予定より ${behind} 日遅れています（1日2ステージまで挽回できます）", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BonusButton(state: GameState, curriculum: Curriculum, today: LocalDate, onPlay: (Int) -> Unit) {
    val due = GameEngine.dueCount(curriculum, state, today)
    if (due > 0) {
        OutlinedButton(onClick = { onPlay(Curriculum.BONUS_DAY) }, modifier = Modifier.fillMaxWidth()) {
            Text("おかわり復習（復習待ち ${due} 枚）")
        }
        Text(
            "余裕のある日に遊ぶと、合格力が早く伸びます（日数は進みません）。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun WorldCard(world: World, state: GameState, onMap: () -> Unit) {
    Card(onClick = onMap, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ワールド${world.number}　${world.name}", style = MaterialTheme.typography.titleMedium)
            DayGrid(world, state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayGrid(world: World, state: GameState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        world.days.forEach { day -> DayDot(day, state) }
    }
}

@Composable
private fun DayDot(day: Int, state: GameState) {
    val record = state.records[day]
    val current = day == state.nextDay
    val special = Curriculum.isBossDay(day) || day == Curriculum.TOTAL_DAYS
    val bg = when {
        record != null -> MaterialTheme.colorScheme.primary
        current -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (record != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .size(34.dp)
            .background(bg, CircleShape)
            .then(if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when {
                special && record == null -> if (day == Curriculum.TOTAL_DAYS) "🏰" else "👾"
                record != null -> "★${record.stars}"
                else -> "$day"
            },
            color = fg,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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

@Composable
fun MapScreen(state: GameState, curriculum: Curriculum, onBack: () -> Unit) {
    val mastery = GameEngine.masteryByCategory(curriculum, state)
    Scaffold(topBar = { BackTopBar("冒険の地図", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(curriculum.worlds) { world ->
                val reached = state.nextDay >= world.days.first
                OutlinedCard(
                    border = BorderStroke(
                        if (state.nextDay in world.days) 2.dp else 1.dp,
                        if (state.nextDay in world.days) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "ワールド${world.number}　${world.name}" + if (reached) "" else "　🔒",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Day ${world.days.first}〜${world.days.last}　" +
                                if (world.categories.isEmpty()) "総仕上げ・最終試験" else world.categories.joinToString("・") { it.label },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DayGrid(world, state)
                        world.categories.forEach { cat ->
                            val m = mastery[cat] ?: 0.0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                LinearProgressIndicator(progress = { m.toFloat() }, modifier = Modifier.width(100.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(percent(m), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesScreen(state: GameState, onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar("バッジ", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Badge.entries) { badge ->
                val earned = badge.name in state.badges
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (earned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (earned) "🏅" else "🔒", fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                badge.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (earned) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(badge.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
