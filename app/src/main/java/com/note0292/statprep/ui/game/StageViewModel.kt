package com.note0292.statprep.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.note0292.statprep.data.QuizBuilder
import com.note0292.statprep.data.QuizItem
import com.note0292.statprep.data.game.StageItem
import com.note0292.statprep.data.game.StageOutcome
import com.note0292.statprep.data.game.StudyCard
import kotlin.random.Random

/** 1 回分のステージ。カードを順に解き、最後に [outcome] を確定する。 */
class StageViewModel : ViewModel() {
    var items by mutableStateOf<List<StageItem>>(emptyList())
        private set

    /** 問題カードの選択肢の並び（問題以外は null）。 */
    var quizzes by mutableStateOf<List<QuizItem?>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set

    /** 問題なら選んだ選択肢、定理・例題なら使わない。 */
    var selected by mutableStateOf<Int?>(null)
        private set

    /** 正解・答えを表示中か。 */
    var revealed by mutableStateOf(false)
        private set
    var showProof by mutableStateOf(false)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    var finishedAt by mutableLongStateOf(0L)
        private set
    var outcome by mutableStateOf<StageOutcome?>(null)
        private set

    val results = mutableStateListOf<Boolean>()

    private var started = false

    val current: StageItem? get() = items.getOrNull(index)
    val currentQuiz: QuizItem? get() = quizzes.getOrNull(index)
    val finished: Boolean get() = started && index >= items.size
    val elapsedSeconds: Int get() = ((finishedAt - startedAt) / 1000).toInt()

    fun startIfNeeded(build: () -> List<StageItem>) {
        if (!started) start(build())
    }

    fun start(newItems: List<StageItem>) {
        started = true
        items = newItems
        quizzes = newItems.map { (it.card as? StudyCard.QuestionCard)?.let { q -> QuizBuilder.shuffleChoices(q.question, Random.Default) } }
        index = 0
        selected = null
        revealed = false
        showProof = false
        combo = 0
        results.clear()
        outcome = null
        startedAt = System.currentTimeMillis()
        finishedAt = 0L
    }

    fun select(choice: Int) {
        if (!revealed) selected = choice
    }

    fun toggleProof() {
        showProof = !showProof
    }

    /** 問題の解答を確定する。戻り値は正解かどうか。 */
    fun submitChoice(): Boolean? {
        val quiz = currentQuiz ?: return null
        val choice = selected ?: return null
        if (revealed) return null
        revealed = true
        return (choice == quiz.answer).also(::record)
    }

    /** 定理・例題の答えを表示する。 */
    fun reveal() {
        revealed = true
    }

    /** 定理・例題の自己採点（新規カードは読めば true）。 */
    fun grade(correct: Boolean) {
        record(correct)
        advance()
    }

    fun next() {
        if (revealed && results.size > index) advance()
    }

    private fun record(correct: Boolean) {
        results.add(correct)
        combo = if (correct) combo + 1 else 0
    }

    private fun advance() {
        index++
        selected = null
        revealed = false
        showProof = false
        if (index >= items.size) finishedAt = System.currentTimeMillis()
    }

    fun finish(result: StageOutcome) {
        outcome = result
    }
}
