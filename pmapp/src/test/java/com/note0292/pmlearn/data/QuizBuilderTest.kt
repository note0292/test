package com.note0292.pmlearn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizBuilderTest {
    private fun lesson(id: String, n: Int) = Lesson(
        id = id,
        title = id,
        minutes = 10,
        goal = "goal",
        sections = emptyList(),
        keyPoints = emptyList(),
        quiz = List(n) { QuizQuestion("$id q$it", listOf("正解", "誤り1", "誤り2", "誤り3"), 0, "解説") },
    )

    private val curriculum = Curriculum(
        stages = listOf(
            Stage("s01", 1, "A", "入門", "", listOf(lesson("s01-l1", 3), lesson("s01-l2", 3))),
            Stage("s02", 2, "B", "基礎", "", listOf(lesson("s02-l1", 3))),
            Stage("s03", 3, "C", "実践", "", listOf(lesson("s03-l1", 20))),
        ),
        glossary = emptyList(),
    )

    @Test
    fun shuffledAnswerStillPointsToCorrectChoice() {
        repeat(20) { seed ->
            val item = QuizBuilder.shuffleChoices(curriculum.questions.first(), Random(seed))
            assertEquals("正解", item.choices[item.answer])
        }
    }

    @Test
    fun lessonQuizKeepsQuestionOrder() {
        val items = QuizBuilder.build(QuizMode.Lesson("s01-l2"), curriculum, Progress(), Random(1))
        assertEquals(listOf("s01-l2#0", "s01-l2#1", "s01-l2#2"), items.map { it.question.id })
    }

    @Test
    fun reviewContainsOnlyLastWrongAnswers() {
        val today = java.time.LocalDate.of(2026, 1, 1)
        val progress = Progress()
            .recordAnswer("s01-l1#0", correct = false, today = today)
            .recordAnswer("s01-l1#1", correct = false, today = today)
            .recordAnswer("s01-l1#1", correct = true, today = today)
            .recordAnswer("s02-l1#2", correct = true, today = today)
        val ids = QuizBuilder.reviewQuestions(curriculum.questions, progress).map { it.id }
        assertEquals(listOf("s01-l1#0"), ids)
    }

    @Test
    fun finalExamDrawsFromEveryStage() {
        val picked = QuizBuilder.finalExam(curriculum.questions, 12, Random(3))
        assertEquals(12, picked.size)
        assertEquals(12, picked.map { it.id }.toSet().size)
        val perStage = picked.groupingBy { it.stage.id }.eachCount()
        assertEquals(setOf("s01", "s02", "s03"), perStage.keys)
        // 問題数の少ないステージは全問、残りは多いステージから補う
        assertEquals(3, perStage.getValue("s02"))
        assertTrue(perStage.getValue("s03") >= 4)
    }

    @Test
    fun finalExamIsCappedByAvailableQuestions() {
        val total = curriculum.questions.size
        assertEquals(total, QuizBuilder.finalExam(curriculum.questions, total + 10, Random(0)).size)
    }
}
