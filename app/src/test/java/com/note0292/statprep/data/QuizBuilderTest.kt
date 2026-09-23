package com.note0292.statprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class QuizBuilderTest {
    private fun q(id: String, category: Category = Category.PROBABILITY) = Question(
        id = id,
        category = category.id,
        question = "Q$id",
        choices = listOf("a", "b", "c", "d"),
        answer = 2,
        explanation = "",
    )

    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun shuffleChoicesKeepsCorrectAnswer() {
        val question = q("1")
        repeat(50) { seed ->
            val item = QuizBuilder.shuffleChoices(question, Random(seed))
            assertEquals("c", item.choices[item.answer])
            assertEquals(question.choices.toSet(), item.choices.toSet())
        }
    }

    @Test
    fun randomSetPrefersUnseenQuestions() {
        val questions = (1..20).map { q("$it") }
        var progress = Progress()
        (1..15).forEach { progress = progress.recordAnswer("$it", true, 0L, today) }
        val picked = QuizBuilder.randomSet(questions, progress, 5, Random(1))
        assertEquals(5, picked.size)
        assertTrue(picked.all { progress.stat(it.id).attempts == 0 })
    }

    @Test
    fun weakQuestionsAreSortedByAccuracy() {
        val questions = (1..4).map { q("$it") }
        val progress = Progress()
            .recordAnswer("1", true, 0L, today) // 正解のみ → 苦手ではない
            .recordAnswer("2", false, 0L, today) // 0%
            .recordAnswer("3", true, 0L, today).recordAnswer("3", false, 0L, today) // 50%, 前回不正解
        val weak = QuizBuilder.weakQuestions(questions, progress).map { it.id }
        assertEquals(listOf("2", "3"), weak)
    }

    @Test
    fun mockExamCoversAllCategories() {
        val questions = Category.entries.flatMap { c -> (1..8).map { q("${c.id}-$it", c) } }
        val picked = QuizBuilder.mockExam(questions, 30, Random(7))
        assertEquals(30, picked.size)
        assertEquals(30, picked.map { it.id }.toSet().size)
        assertEquals(Category.entries.map { it.id }.toSet(), picked.map { it.category }.toSet())
    }

    @Test
    fun mockExamWithFewQuestionsReturnsAll() {
        val questions = (1..5).map { q("$it") }
        assertEquals(5, QuizBuilder.mockExam(questions, 30, Random(0)).size)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        var progress = Progress()
        listOf(0L, 1L, 2L, 4L).forEach { back ->
            progress = progress.recordAnswer("1", true, 0L, today.minusDays(back))
        }
        assertEquals(3, progress.streak(today))
        // 今日まだ解いていなくても、昨日まで続いていれば途切れない
        assertEquals(3, progress.streak(today.plusDays(1)))
        assertEquals(0, progress.streak(today.plusDays(2)))
    }

    @Test
    fun toggleBookmark() {
        val p = Progress().toggleBookmark("1")
        assertTrue("1" in p.bookmarks)
        assertTrue("1" !in p.toggleBookmark("1").bookmarks)
    }
}
