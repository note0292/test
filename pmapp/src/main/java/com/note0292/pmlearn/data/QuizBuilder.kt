package com.note0292.pmlearn.data

import kotlin.random.Random

/** 出題用に選択肢の順番を並べ替えた問題。 */
data class QuizItem(
    val question: LessonQuestion,
    val choices: List<String>,
    val answer: Int,
)

sealed interface QuizMode {
    /** レッスンの理解度チェック。合格するとレッスン修了。 */
    data class Lesson(val lessonId: String) : QuizMode
    /** 間違えた問題の復習。 */
    data object Review : QuizMode
    /** 全ステージから出題する修了テスト。 */
    data class Final(val count: Int) : QuizMode
}

object QuizBuilder {
    fun shuffleChoices(question: LessonQuestion, random: Random): QuizItem {
        val q = question.question
        val order = q.choices.indices.shuffled(random)
        return QuizItem(
            question = question,
            choices = order.map { q.choices[it] },
            answer = order.indexOf(q.answer),
        )
    }

    /** 前回不正解だった問題。間違えた回数の多い順。 */
    fun reviewQuestions(questions: List<LessonQuestion>, progress: Progress): List<LessonQuestion> =
        questions
            .filter { progress.stat(it.id).lastCorrect == false }
            .sortedByDescending { progress.stat(it.id).wrong }

    /** 各ステージからできるだけ均等に選ぶ。 */
    fun finalExam(questions: List<LessonQuestion>, count: Int, random: Random): List<LessonQuestion> {
        val pools = questions.groupBy { it.stage.id }.values.map { it.shuffled(random).toMutableList() }
        val picked = mutableListOf<LessonQuestion>()
        while (picked.size < count && pools.any { it.isNotEmpty() }) {
            pools.shuffled(random).forEach { pool ->
                if (picked.size < count && pool.isNotEmpty()) picked += pool.removeAt(0)
            }
        }
        return picked.shuffled(random)
    }

    fun build(
        mode: QuizMode,
        curriculum: Curriculum,
        progress: Progress,
        random: Random = Random.Default,
    ): List<QuizItem> {
        val selected = when (mode) {
            // 理解度チェックは教材の流れに沿うよう、出題順は並べ替えない
            is QuizMode.Lesson -> curriculum.questions.filter { it.lesson.id == mode.lessonId }
            QuizMode.Review -> reviewQuestions(curriculum.questions, progress)
            is QuizMode.Final -> finalExam(curriculum.questions, mode.count, random)
        }
        return selected.map { shuffleChoices(it, random) }
    }
}
