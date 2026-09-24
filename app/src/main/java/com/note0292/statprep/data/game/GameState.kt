package com.note0292.statprep.data.game

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** 間隔反復の状態。[box] が大きいほど次の復習までの間隔が長い。 */
@Serializable
data class CardState(
    val box: Int = 0,
    /** 次に復習する日 (yyyy-MM-dd)。 */
    val due: String = "",
)

/** クリアしたステージの記録。 */
@Serializable
data class StageRecord(
    val date: String,
    val stars: Int,
    val correct: Int,
    val total: Int,
    val seconds: Int,
)

@Serializable
data class GameState(
    /** 冒険を始めた日。null なら未開始。 */
    val startDate: String? = null,
    /** クリア済みの日数（= 次に遊ぶのは clearedDays + 1 日目）。 */
    val clearedDays: Int = 0,
    val records: Map<Int, StageRecord> = emptyMap(),
    val cards: Map<String, CardState> = emptyMap(),
    val xp: Int = 0,
    val badges: Set<String> = emptySet(),
) {
    val started: Boolean get() = startDate != null
    val nextDay: Int get() = clearedDays + 1
    val completed: Boolean get() = clearedDays >= Curriculum.TOTAL_DAYS

    fun card(id: String): CardState = cards[id] ?: CardState()

    fun clearsOn(date: LocalDate): Int = records.values.count { it.date == date.toString() }

    /** 開始日から数えた、今日が本来何日目か。 */
    fun scheduledDay(today: LocalDate): Int {
        val start = startDate?.let(LocalDate::parse) ?: return 1
        return (java.time.temporal.ChronoUnit.DAYS.between(start, today) + 1).toInt().coerceAtLeast(1)
    }

    /** 予定より何日遅れているか（今日の分は含めない）。 */
    fun daysBehind(today: LocalDate): Int = (scheduledDay(today) - nextDay).coerceAtLeast(0)

    /** 試験を受けられる予定日（開始から 180 日目の翌日）。 */
    fun examDate(): LocalDate? = startDate?.let { LocalDate.parse(it).plusDays(Curriculum.TOTAL_DAYS.toLong()) }

    /** 連続してステージをクリアした日数（今日または昨日から遡る）。 */
    fun streak(today: LocalDate): Int {
        val dates = records.values.map { it.date }.toSet()
        var day = if (today.toString() in dates) today else today.minusDays(1)
        var count = 0
        while (day.toString() in dates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
