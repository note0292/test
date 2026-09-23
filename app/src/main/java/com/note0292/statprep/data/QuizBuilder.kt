package com.note0292.statprep.data

import kotlin.random.Random

/** 出題用に選択肢の順番を並べ替えた問題。 */
data class QuizItem(
    val question: Question,
    val choices: List<String>,
    val answer: Int,
)

sealed interface QuizMode {
    data class Random(val count: Int) : QuizMode
    data class ByCategory(val category: Category) : QuizMode
    data object Weak : QuizMode
    data object Bookmarked : QuizMode
    data class Mock(val count: Int) : QuizMode
}

object QuizBuilder {
    const val WEAK_ACCURACY_THRESHOLD = 0.6

    fun shuffleChoices(question: Question, random: Random): QuizItem {
        val order = question.choices.indices.shuffled(random)
        return QuizItem(
            question = question,
            choices = order.map { question.choices[it] },
            answer = order.indexOf(question.answer),
        )
    }

    /** 前回不正解、または正答率が閾値未満の問題を苦手問題とみなし、苦手度の高い順に返す。 */
    fun weakQuestions(questions: List<Question>, progress: Progress): List<Question> =
        questions
            .filter { q ->
                val s = progress.stat(q.id)
                s.attempts > 0 && (s.lastCorrect == false || s.accuracy < WEAK_ACCURACY_THRESHOLD)
            }
            .sortedWith(compareBy({ progress.stat(it.id).accuracy }, { -progress.stat(it.id).wrong }))

    /** 各分野から問題数に比例した数を選ぶ（各分野最低1問、最大剰余法で端数を配分）。 */
    fun mockExam(questions: List<Question>, count: Int, random: Random): List<Question> {
        val groups = questions.groupBy { it.category }.mapValues { it.value.shuffled(random) }
        if (groups.isEmpty()) return emptyList()
        val target = count.coerceAtMost(questions.size)
        val share = groups.mapValues { it.value.size.toDouble() * target / questions.size }
        val quota = share.mapValues { (cat, s) ->
            maxOf(1, s.toInt()).coerceAtMost(groups.getValue(cat).size)
        }.toMutableMap()
        // 分野数が target より多い場合などは、割当ての多い分野から減らす
        while (quota.values.sum() > target) {
            val cat = quota.filterValues { it > 1 }.maxByOrNull { it.value }?.key
                ?: quota.keys.random(random)
            quota[cat] = quota.getValue(cat) - 1
            if (quota.getValue(cat) == 0) quota.remove(cat)
        }
        // 端数（小数部分）の大きい分野から 1 問ずつ追加する
        while (quota.values.sum() < target) {
            val cat = groups.keys
                .filter { (quota[it] ?: 0) < groups.getValue(it).size }
                .maxBy { share.getValue(it) - (quota[it] ?: 0) }
            quota[cat] = (quota[cat] ?: 0) + 1
        }
        return quota.flatMap { (cat, n) -> groups.getValue(cat).take(n) }.shuffled(random)
    }

    /** ランダム出題。まだ解いていない問題を優先する。 */
    fun randomSet(questions: List<Question>, progress: Progress, count: Int, random: Random): List<Question> {
        val (unseen, seen) = questions.shuffled(random).partition { progress.stat(it.id).attempts == 0 }
        return (unseen + seen).take(count).shuffled(random)
    }

    fun build(
        mode: QuizMode,
        questions: List<Question>,
        progress: Progress,
        random: Random = Random.Default,
    ): List<QuizItem> {
        val selected = when (mode) {
            is QuizMode.Random -> randomSet(questions, progress, mode.count, random)
            is QuizMode.ByCategory -> questions.filter { it.category == mode.category.id }.shuffled(random)
            QuizMode.Weak -> weakQuestions(questions, progress)
            QuizMode.Bookmarked -> questions.filter { it.id in progress.bookmarks }.shuffled(random)
            // 模擬試験は本番の出題範囲に合わせ、基礎数学の問題を除く
            is QuizMode.Mock -> mockExam(questions.filter { it.categoryEnum.kind != CategoryKind.MATH }, mode.count, random)
        }
        return selected.map { shuffleChoices(it, random) }
    }
}
