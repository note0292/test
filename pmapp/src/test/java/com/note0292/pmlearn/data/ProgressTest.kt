package com.note0292.pmlearn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun lessonIsCompletedOnlyWhenPassing() {
        val failed = Progress().recordLessonQuiz("s01-l1", 1.0 / 3, today)
        assertFalse("s01-l1" in failed.completedLessons)
        val passed = failed.recordLessonQuiz("s01-l1", 2.0 / 3, today)
        assertTrue("s01-l1" in passed.completedLessons)
    }

    @Test
    fun bestScoreKeepsMaximum() {
        val p = Progress()
            .recordLessonQuiz("s01-l1", 1.0, today)
            .recordLessonQuiz("s01-l1", 0.0, today)
        assertEquals(1.0, p.bestScores.getValue("s01-l1"), 1e-9)
        assertTrue("一度修了したら失敗しても修了のまま", "s01-l1" in p.completedLessons)
    }

    @Test
    fun recordAnswerCountsAttempts() {
        val p = Progress()
            .recordAnswer("q", correct = false, today = today)
            .recordAnswer("q", correct = true, today = today)
        val s = p.stat("q")
        assertEquals(2, s.attempts)
        assertEquals(true, s.lastCorrect)
    }

    @Test
    fun blankNoteIsRemoved() {
        val p = Progress().saveNote("s01-l1", "メモ")
        assertEquals("メモ", p.notes["s01-l1"])
        assertFalse("s01-l1" in p.saveNote("s01-l1", "  ").notes)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val p = Progress(studyDays = setOf("2026-09-21", "2026-09-22", "2026-09-20", "2026-09-18"))
        assertEquals("昨日まで続いていれば数える", 3, p.streak(today))
        assertEquals(0, Progress().streak(today))
    }
}
