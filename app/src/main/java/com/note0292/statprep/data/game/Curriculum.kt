package com.note0292.statprep.data.game

import com.note0292.statprep.data.Category
import com.note0292.statprep.data.CategoryKind
import com.note0292.statprep.data.Chapter
import com.note0292.statprep.data.Example
import com.note0292.statprep.data.Question
import com.note0292.statprep.data.Theorem

/** ゲームで出題・復習する学習カード。[id] は種類ごとの接頭辞つきで一意。 */
sealed interface StudyCard {
    val id: String
    val category: Category

    /** 選択式の問題。 */
    data class QuestionCard(val question: Question) : StudyCard {
        override val id: String get() = "q:${question.id}"
        override val category: Category get() = question.categoryEnum
    }

    /** 定理・公式。初回は内容を読み、復習では名前から内容を思い出す。 */
    data class TheoremCard(
        override val id: String,
        override val category: Category,
        val lessonId: String,
        val lessonTitle: String,
        val theorem: Theorem,
    ) : StudyCard

    /** 例題。初回は解答を読み、復習では自力で解けるか確かめる。 */
    data class ExampleCard(
        override val id: String,
        override val category: Category,
        val lessonId: String,
        val lessonTitle: String,
        val example: Example,
    ) : StudyCard
}

enum class StageType {
    /** 新しいカードを覚え、期限の来たカードを復習する通常ステージ。 */
    LEARN,

    /** ワールドの最後に、そのワールドで学んだ問題から出題する。 */
    BOSS,

    /** 新規カードのない総仕上げ（苦手・期限の来たカード中心）。 */
    REVIEW,

    /** 最終日の模擬試験。 */
    FINAL,

    /** その日のステージをクリアした後に遊べる、期限の来たカードだけの追加復習（日数は進まない）。 */
    BONUS,
}

data class DayPlan(
    val day: Int,
    val type: StageType,
    val title: String,
    /** この日に初めて学ぶカード。 */
    val newCardIds: List<String> = emptyList(),
    /** この日に学ぶレッスン（テキストへのリンク用）。 */
    val lessonIds: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    val world: Int get() = (day - 1) / Curriculum.DAYS_PER_WORLD + 1
}

data class World(
    val number: Int,
    val name: String,
    val days: IntRange,
    val categories: List<Category>,
)

/**
 * 180 日（6 ワールド × 30 日）の学習計画。
 * 基礎数学と準1級の出題範囲の章を順に並べ、定理・例題・問題を学習日に均等に割り振る。
 * 各ワールドの 30 日目はボス戦、最後のワールドは総仕上げと最終試験。
 */
class Curriculum private constructor(
    val days: List<DayPlan>,
    val cards: Map<String, StudyCard>,
    val worlds: List<World>,
) {
    /** カードを初めて学ぶ日。 */
    val introDay: Map<String, Int> = days.flatMap { d -> d.newCardIds.map { it to d.day } }.toMap()

    fun day(n: Int): DayPlan = if (n == BONUS_DAY) BONUS_PLAN else days[n - 1]

    fun world(n: Int): World = worlds[n - 1]

    /** [day] 日目までに学んだカード（その日を含む）。 */
    fun introducedBy(day: Int): List<StudyCard> = introDay.filterValues { it <= day }.keys.map { cards.getValue(it) }

    companion object {
        const val TOTAL_DAYS = 180

        /** おかわり復習を表す日番号。 */
        const val BONUS_DAY = 0
        private val BONUS_PLAN = DayPlan(BONUS_DAY, StageType.BONUS, "おかわり復習")
        const val DAYS_PER_WORLD = 30
        const val WORLD_COUNT = TOTAL_DAYS / DAYS_PER_WORLD

        /** 新しいカードを学ぶ最後の日（最終ワールドは総仕上げに充てる）。 */
        const val LAST_LEARN_DAY = TOTAL_DAYS - DAYS_PER_WORLD

        val WORLD_NAMES = listOf("はじまりの森", "確率の平原", "推測の山脈", "モデルの海", "応用の塔", "試験の城")

        fun isBossDay(day: Int) = day % DAYS_PER_WORLD == 0 && day <= LAST_LEARN_DAY

        /** 章の中のカードを、レッスン順に定理 → 例題と並べ、問題をレッスンの間に均等に差し込む。 */
        internal fun chapterCards(chapter: Chapter, questions: List<Question>): List<StudyCard> {
            val category = chapter.categoryEnum
            val qs = questions.filter { it.category == chapter.category }
            val lessons = chapter.lessons
            val result = mutableListOf<StudyCard>()
            lessons.forEachIndexed { li, lesson ->
                lesson.theorems.forEachIndexed { i, t ->
                    result += StudyCard.TheoremCard("t:${lesson.id}:$i", category, lesson.id, lesson.title, t)
                }
                lesson.examples.forEachIndexed { i, e ->
                    result += StudyCard.ExampleCard("e:${lesson.id}:$i", category, lesson.id, lesson.title, e)
                }
                val from = li * qs.size / lessons.size
                val to = (li + 1) * qs.size / lessons.size
                qs.subList(from, to).forEach { result += StudyCard.QuestionCard(it) }
            }
            if (lessons.isEmpty()) qs.forEach { result += StudyCard.QuestionCard(it) }
            return result
        }

        fun build(chapters: List<Chapter>, questions: List<Question>): Curriculum {
            val ordered = chapters
                .filter { it.categoryEnum.kind != CategoryKind.ADVANCED }
                .sortedBy { it.categoryEnum.ordinal }
            val sequence = ordered.flatMap { chapterCards(it, questions) }
            val cards = sequence.associateBy { it.id }

            val learnDays = (1..LAST_LEARN_DAY).filterNot(::isBossDay)
            val assigned = HashMap<Int, List<StudyCard>>()
            learnDays.forEachIndexed { i, day ->
                val from = i * sequence.size / learnDays.size
                val to = (i + 1) * sequence.size / learnDays.size
                assigned[day] = sequence.subList(from, to)
            }

            val days = (1..TOTAL_DAYS).map { day ->
                val world = (day - 1) / DAYS_PER_WORLD + 1
                when {
                    day == TOTAL_DAYS -> DayPlan(day, StageType.FINAL, "最終試験")
                    day > LAST_LEARN_DAY -> DayPlan(day, StageType.REVIEW, "総仕上げ")
                    isBossDay(day) -> DayPlan(day, StageType.BOSS, "${WORLD_NAMES[world - 1]}のボス")
                    else -> {
                        val newCards = assigned[day].orEmpty()
                        DayPlan(
                            day = day,
                            type = StageType.LEARN,
                            title = stageTitle(newCards),
                            newCardIds = newCards.map { it.id },
                            lessonIds = newCards.mapNotNull { it.lessonId() }.distinct(),
                            categories = newCards.map { it.category }.distinct(),
                        )
                    }
                }
            }

            val worlds = (1..WORLD_COUNT).map { n ->
                val range = ((n - 1) * DAYS_PER_WORLD + 1)..(n * DAYS_PER_WORLD)
                val cats = days.filter { it.day in range }.flatMap { it.categories }.distinct()
                World(n, WORLD_NAMES[n - 1], range, cats)
            }
            return Curriculum(days, cards, worlds)
        }

        private fun StudyCard.lessonId(): String? = when (this) {
            is StudyCard.TheoremCard -> lessonId
            is StudyCard.ExampleCard -> lessonId
            is StudyCard.QuestionCard -> null
        }

        private fun stageTitle(cards: List<StudyCard>): String {
            val first = cards.firstOrNull() ?: return "おさらい"
            return when (first) {
                is StudyCard.TheoremCard -> first.lessonTitle
                is StudyCard.ExampleCard -> first.lessonTitle
                is StudyCard.QuestionCard -> "${first.category.label}の演習"
            }
        }
    }
}
