package com.note0292.pmlearn.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class QuestionStat(
    val correct: Int = 0,
    val wrong: Int = 0,
    val lastCorrect: Boolean? = null,
) {
    val attempts: Int get() = correct + wrong
}

@Serializable
data class Progress(
    /** 理解度チェックに合格したレッスンの id。 */
    val completedLessons: Set<String> = emptySet(),
    /** レッスンごとの理解度チェックの最高正答率（0.0〜1.0）。 */
    val bestScores: Map<String, Double> = emptyMap(),
    /** 問題 id（"lessonId#番号"）ごとの解答履歴。 */
    val stats: Map<String, QuestionStat> = emptyMap(),
    /** 「やってみよう」に書いた自分の回答。 */
    val notes: Map<String, String> = emptyMap(),
    /** 修了テストの最高正答率。未受験なら null。 */
    val finalExamBest: Double? = null,
    /** 学習した日付 (ISO-8601, yyyy-MM-dd)。 */
    val studyDays: Set<String> = emptySet(),
) {
    fun stat(id: String): QuestionStat = stats[id] ?: QuestionStat()

    fun recordAnswer(id: String, correct: Boolean, today: LocalDate): Progress {
        val old = stat(id)
        val updated = old.copy(
            correct = old.correct + if (correct) 1 else 0,
            wrong = old.wrong + if (correct) 0 else 1,
            lastCorrect = correct,
        )
        return copy(stats = stats + (id to updated), studyDays = studyDays + today.toString())
    }

    /** 理解度チェックの結果を記録し、合格ラインに届いていればレッスンを修了にする。 */
    fun recordLessonQuiz(lessonId: String, score: Double, today: LocalDate): Progress = copy(
        bestScores = bestScores + (lessonId to maxOf(score, bestScores[lessonId] ?: 0.0)),
        completedLessons = if (score >= PASS_RATE) completedLessons + lessonId else completedLessons,
        studyDays = studyDays + today.toString(),
    )

    fun recordFinalExam(score: Double, today: LocalDate): Progress = copy(
        finalExamBest = maxOf(score, finalExamBest ?: 0.0),
        studyDays = studyDays + today.toString(),
    )

    fun saveNote(lessonId: String, text: String): Progress =
        copy(notes = if (text.isBlank()) notes - lessonId else notes + (lessonId to text))

    fun completedCount(stage: Stage): Int = stage.lessons.count { it.id in completedLessons }

    /** 今日(または昨日)から遡って途切れずに学習した日数。 */
    fun streak(today: LocalDate): Int {
        var day = if (today.toString() in studyDays) today else today.minusDays(1)
        var count = 0
        while (day.toString() in studyDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    companion object {
        /** 理解度チェックの合格ライン。 */
        const val PASS_RATE = 0.6
        /** 修了テストの合格ライン。 */
        const val FINAL_PASS_RATE = 0.8
    }
}
