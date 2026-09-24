package com.note0292.statprep.data.game

import com.note0292.statprep.data.CategoryKind
import com.note0292.statprep.data.QuizBuilder
import java.time.LocalDate
import kotlin.random.Random

/** ステージ内の 1 枚。[isNew] なら初めて学ぶカード（内容を読んで覚える）。 */
data class StageItem(val card: StudyCard, val isNew: Boolean)

/** ステージを終えたときの報酬と、更新後の状態。 */
data class StageOutcome(
    val state: GameState,
    val cleared: Boolean,
    val stars: Int,
    val correct: Int,
    val graded: Int,
    val xpGained: Int,
    val xpBreakdown: List<Pair<String, Int>>,
    val levelBefore: Int,
    val levelAfter: Int,
    val newBadges: List<Badge>,
    val readinessBefore: Double,
    val readinessAfter: Double,
)

enum class Badge(val title: String, val description: String) {
    FIRST_STEP("はじめの一歩", "最初のステージをクリア"),
    STREAK_7("1週間皆勤", "7日連続でクリア"),
    STREAK_30("1か月皆勤", "30日連続でクリア"),
    STREAK_100("100日皆勤", "100日連続でクリア"),
    PERFECT("パーフェクト", "全問正解で星3つ"),
    SPEEDY("3分の達人", "3分以内にクリア"),
    WORLD_1("森の踏破者", "ワールド1のボスを倒す"),
    WORLD_2("平原の踏破者", "ワールド2のボスを倒す"),
    WORLD_3("山脈の踏破者", "ワールド3のボスを倒す"),
    WORLD_4("海の踏破者", "ワールド4のボスを倒す"),
    WORLD_5("塔の踏破者", "ワールド5のボスを倒す"),
    READY("合格圏", "合格力 80% に到達"),
    GRADUATE("受験準備完了", "最終試験に合格"),
}

object GameEngine {
    /** 1 ステージの目安時間（秒）。 */
    const val TARGET_SECONDS = 180

    /** 1 ステージの最大枚数（約 3 分に収まる量）。 */
    const val MAX_ITEMS = 8

    /** 復習カードが少ないときでも、この枚数まではおさらいで補う。 */
    const val MIN_ITEMS = 5
    const val BOSS_QUESTIONS = 6
    const val FINAL_QUESTIONS = 10

    /** ボス戦・最終試験の合格ライン（本番と同じく 6 割）。 */
    const val PASS_RATE = 0.6

    /** 遅れを取り戻すときに 1 日に遊べるステージ数の上限。 */
    const val MAX_STAGES_PER_DAY = 2

    /** 合格力の目標値。 */
    const val READY_THRESHOLD = 0.8

    /** box ごとの次回復習までの日数。 */
    val INTERVALS = listOf(1, 4, 10, 25, 60)

    /** この box まで上がったカードを「定着した」とみなす（3 回続けて思い出せた状態）。 */
    const val MASTER_BOX = 3

    // ---- 進行 ----

    fun canPlay(state: GameState, today: LocalDate): Boolean {
        if (!state.started || state.completed) return false
        val clears = state.clearsOn(today)
        return clears == 0 || (clears < MAX_STAGES_PER_DAY && state.daysBehind(today) > 0)
    }

    /** 今日のステージを終えた後、復習期限の来たカードが残っていれば、おかわり復習ができる。 */
    fun dueCount(curriculum: Curriculum, state: GameState, today: LocalDate): Int =
        curriculum.introducedBy(state.clearedDays).count { c -> state.cards[c.id]?.let { isDue(it, today) } == true }

    // ---- 間隔反復 ----

    fun review(card: CardState, correct: Boolean, today: LocalDate): CardState {
        // 忘れていたら 2 段階戻す（最初からやり直しにはしない）
        val box = if (correct) (card.box + 1).coerceAtMost(INTERVALS.lastIndex) else (card.box - 2).coerceAtLeast(0)
        return CardState(box = box, due = today.plusDays(INTERVALS[box].toLong()).toString())
    }

    /** 初めて学んだカードは翌日に最初の復習を迎える。 */
    fun learn(today: LocalDate): CardState = CardState(box = 0, due = today.plusDays(1).toString())

    private fun isDue(card: CardState, today: LocalDate): Boolean =
        card.due.isNotEmpty() && LocalDate.parse(card.due) <= today

    // ---- ステージの組み立て ----

    fun buildStage(
        curriculum: Curriculum,
        state: GameState,
        day: Int,
        today: LocalDate,
        random: Random = Random.Default,
    ): List<StageItem> {
        val plan = curriculum.day(day)
        val upTo = if (plan.type == StageType.BONUS) state.clearedDays else day - 1
        val learned = curriculum.introducedBy(upTo).filter { it.id in state.cards }
        return when (plan.type) {
            StageType.LEARN -> {
                val fresh = plan.newCardIds.map { StageItem(curriculum.cards.getValue(it), isNew = true) }
                fresh + reviews(learned, state, today, (MAX_ITEMS - fresh.size).coerceAtLeast(2), MIN_ITEMS - fresh.size, random)
            }
            StageType.REVIEW -> reviews(learned, state, today, MAX_ITEMS, MAX_ITEMS, random)
            StageType.BONUS -> reviews(learned, state, today, MAX_ITEMS, 0, random)
            StageType.BOSS -> {
                val world = curriculum.world(plan.world)
                val questions = learned.filterIsInstance<StudyCard.QuestionCard>()
                val inWorld = questions.filter { curriculum.introDay[it.id] in world.days }
                val others = questions - inWorld.toSet()
                (inWorld.shuffled(random) + others.shuffled(random)).take(BOSS_QUESTIONS).map { StageItem(it, false) }
            }
            StageType.FINAL -> {
                val cards = curriculum.cards.values.filterIsInstance<StudyCard.QuestionCard>()
                    .filter { it.category.kind == CategoryKind.CORE }
                val picked = QuizBuilder.mockExam(cards.map { it.question }, FINAL_QUESTIONS, random).map { it.id }.toSet()
                cards.filter { it.question.id in picked }.shuffled(random).map { StageItem(it, false) }
            }
        }
    }

    /**
     * 期限の来たカードを、期限切れが古い順・覚えの浅い順に最大 [max] 枚選ぶ。
     * [min] 枚に満たなければ、覚えの浅いカードでおさらいを補う。
     */
    private fun reviews(
        learned: List<StudyCard>,
        state: GameState,
        today: LocalDate,
        max: Int,
        min: Int,
        random: Random,
    ): List<StageItem> {
        val shuffled = learned.shuffled(random)
        val due = shuffled.filter { isDue(state.card(it.id), today) }
            .sortedWith(compareBy({ state.card(it.id).due }, { state.card(it.id).box }))
            .take(max)
        val extra = if (due.size >= min) emptyList() else (shuffled - due.toSet())
            .sortedBy { state.card(it.id).box }
            .take(min - due.size)
        return (due + extra).shuffled(random).map { StageItem(it, isNew = false) }
    }

    // ---- 採点・報酬 ----

    fun stars(correct: Int, graded: Int): Int {
        if (graded == 0) return 3
        val rate = correct.toDouble() / graded
        return when {
            rate >= 0.9 -> 3
            rate >= 0.7 -> 2
            else -> 1
        }
    }

    /**
     * ステージの結果を反映する。[results] は各カードの正誤（新しい定理・例題は読めば true）。
     * ボス戦・最終試験は合格ラインに届かなければクリアにならない（何度でも挑戦できる）。
     */
    fun complete(
        curriculum: Curriculum,
        state: GameState,
        day: Int,
        items: List<StageItem>,
        results: List<Boolean>,
        seconds: Int,
        today: LocalDate,
    ): StageOutcome {
        val plan = curriculum.day(day)
        val readinessBefore = readiness(curriculum, state)
        val levelBefore = level(state.xp)

        var cards = state.cards
        items.forEachIndexed { i, item ->
            val ok = results.getOrElse(i) { false }
            val next = when {
                // 初見で正解できた問題は、1 回目の復習を済ませたものとして扱う
                item.isNew && item.card is StudyCard.QuestionCard && ok -> review(CardState(), true, today)
                item.isNew -> learn(today)
                else -> review(state.card(item.card.id), ok, today)
            }
            cards = cards + (item.card.id to next)
        }

        // 新しく読んだ定理・例題は採点しない。新しい問題は採点する
        val gradedIdx = items.indices.filter { !items[it].isNew || items[it].card is StudyCard.QuestionCard }
        val graded = gradedIdx.size
        val correct = gradedIdx.count { results.getOrElse(it) { false } }
        val exam = plan.type == StageType.BOSS || plan.type == StageType.FINAL
        val cleared = !exam || (graded > 0 && correct >= Math.ceil(graded * PASS_RATE).toInt())
        val stars = if (cleared) stars(correct, graded) else 0

        val breakdown = mutableListOf<Pair<String, Int>>()
        val newCount = items.count { it.isNew }
        if (newCount > 0) breakdown += "新しいカード ×$newCount" to newCount * 10
        if (correct > 0) breakdown += "正解 ×$correct" to correct * 5
        if (cleared) {
            if (exam) breakdown += (if (plan.type == StageType.FINAL) "最終試験合格" else "ボス撃破") to 100
            if (seconds <= TARGET_SECONDS) breakdown += "3分以内ボーナス" to 20
            if (graded > 0 && correct == graded) breakdown += "全問正解ボーナス" to 20
        }

        var next = state.copy(cards = cards)
        if (cleared && plan.type != StageType.BONUS) {
            next = next.copy(
                clearedDays = maxOf(state.clearedDays, day),
                records = state.records + (day to StageRecord(today.toString(), stars, correct, graded, seconds)),
            )
            val streak = next.streak(today)
            if (streak > 1) breakdown += "連続${streak}日ボーナス" to minOf(streak, 10) * 2
        }
        val xpGained = breakdown.sumOf { it.second }
        next = next.copy(xp = state.xp + xpGained)

        val readinessAfter = readiness(curriculum, next)
        val earned = buildList {
            if (cleared) add(Badge.FIRST_STEP)
            val streak = next.streak(today)
            if (streak >= 7) add(Badge.STREAK_7)
            if (streak >= 30) add(Badge.STREAK_30)
            if (streak >= 100) add(Badge.STREAK_100)
            if (cleared && graded > 0 && correct == graded) add(Badge.PERFECT)
            if (cleared && seconds <= TARGET_SECONDS) add(Badge.SPEEDY)
            if (cleared && plan.type == StageType.BOSS) add(Badge.entries[Badge.WORLD_1.ordinal + plan.world - 1])
            if (readinessAfter >= READY_THRESHOLD) add(Badge.READY)
            if (cleared && plan.type == StageType.FINAL) add(Badge.GRADUATE)
        }.filter { it.name !in state.badges }
        next = next.copy(badges = next.badges + earned.map { it.name })

        return StageOutcome(
            state = next,
            cleared = cleared,
            stars = stars,
            correct = correct,
            graded = graded,
            xpGained = xpGained,
            xpBreakdown = breakdown,
            levelBefore = levelBefore,
            levelAfter = level(next.xp),
            newBadges = earned,
            readinessBefore = readinessBefore,
            readinessAfter = readinessAfter,
        )
    }

    // ---- 合格力 ----

    /** カード 1 枚の定着度（0〜1）。未学習は 0、box が [MASTER_BOX] 以上で 1。 */
    fun mastery(card: CardState?): Double =
        if (card == null) 0.0 else (card.box + 1).coerceAtMost(MASTER_BOX + 1).toDouble() / (MASTER_BOX + 1)

    /** 準1級の出題範囲（基礎数学・発展を除く）のカード全体の平均定着度。 */
    fun readiness(curriculum: Curriculum, state: GameState): Double {
        val core = curriculum.cards.values.filter { it.category.kind == CategoryKind.CORE }
        if (core.isEmpty()) return 0.0
        return core.sumOf { mastery(state.cards[it.id]) } / core.size
    }

    /** 分野ごとの定着度。 */
    fun masteryByCategory(curriculum: Curriculum, state: GameState): Map<com.note0292.statprep.data.Category, Double> =
        curriculum.cards.values.groupBy { it.category }
            .mapValues { (_, cs) -> cs.sumOf { mastery(state.cards[it.id]) } / cs.size }

    // ---- レベル ----

    /** レベル L に必要な累計 XP は 50·L·(L−1)。 */
    fun xpForLevel(level: Int): Int = 50 * level * (level - 1)

    fun level(xp: Int): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    fun title(level: Int): String = when {
        level >= 18 -> "準1級マスター"
        level >= 15 -> "統計の賢者"
        level >= 12 -> "モデル職人"
        level >= 9 -> "検定の達人"
        level >= 6 -> "推定の使い手"
        level >= 3 -> "データ見習い"
        else -> "統計ビギナー"
    }
}
