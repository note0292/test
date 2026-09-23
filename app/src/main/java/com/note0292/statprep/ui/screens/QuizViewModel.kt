package com.note0292.statprep.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.note0292.statprep.data.QuizItem

/**
 * 1 回分の出題セッション。
 * [immediateFeedback] が false（模擬試験）のときは解答ごとの正誤表示をせず、最後にまとめて採点する。
 */
class QuizViewModel : ViewModel() {
    var items by mutableStateOf<List<QuizItem>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var revealed by mutableStateOf(false)
        private set
    var immediateFeedback by mutableStateOf(true)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    var finishedAt by mutableLongStateOf(0L)
        private set

    /** 各問題でユーザーが選んだ選択肢の番号。 */
    val answers = mutableStateListOf<Int>()

    private var started = false

    val current: QuizItem? get() = items.getOrNull(index)
    val finished: Boolean get() = started && index >= items.size
    val correctCount: Int get() = items.indices.count { answers.getOrNull(it) == items[it].answer }

    fun startIfNeeded(immediateFeedback: Boolean, build: () -> List<QuizItem>) {
        if (started) return
        start(build(), immediateFeedback)
    }

    fun start(newItems: List<QuizItem>, immediateFeedback: Boolean = this.immediateFeedback) {
        started = true
        items = newItems
        index = 0
        selected = null
        revealed = false
        answers.clear()
        this.immediateFeedback = immediateFeedback
        startedAt = System.currentTimeMillis()
        finishedAt = 0L
    }

    fun select(choice: Int) {
        if (!revealed) selected = choice
    }

    /** 解答を確定する。戻り値は正解かどうか（未選択なら null）。 */
    fun submit(): Boolean? {
        val item = current ?: return null
        val choice = selected ?: return null
        if (revealed) return null
        answers.add(choice)
        if (immediateFeedback) revealed = true else advance()
        return choice == item.answer
    }

    fun next() {
        if (revealed) advance()
    }

    private fun advance() {
        index++
        selected = null
        revealed = false
        if (index >= items.size) finishedAt = System.currentTimeMillis()
    }
}
