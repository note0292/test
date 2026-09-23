package com.note0292.statprep.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class QuestionStat(
    val correct: Int = 0,
    val wrong: Int = 0,
    val lastCorrect: Boolean? = null,
    val lastAnsweredAt: Long = 0L,
) {
    val attempts: Int get() = correct + wrong
    val accuracy: Double get() = if (attempts == 0) 0.0 else correct.toDouble() / attempts
}

@Serializable
data class Progress(
    val stats: Map<String, QuestionStat> = emptyMap(),
    val bookmarks: Set<String> = emptySet(),
    /** 学習した日付 (ISO-8601, yyyy-MM-dd)。連続学習日数の計算に使う。 */
    val studyDays: Set<String> = emptySet(),
) {
    fun stat(id: String): QuestionStat = stats[id] ?: QuestionStat()

    fun recordAnswer(id: String, correct: Boolean, now: Long, today: LocalDate): Progress {
        val old = stat(id)
        val updated = old.copy(
            correct = old.correct + if (correct) 1 else 0,
            wrong = old.wrong + if (correct) 0 else 1,
            lastCorrect = correct,
            lastAnsweredAt = now,
        )
        return copy(stats = stats + (id to updated), studyDays = studyDays + today.toString())
    }

    fun toggleBookmark(id: String): Progress =
        copy(bookmarks = if (id in bookmarks) bookmarks - id else bookmarks + id)

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
}
