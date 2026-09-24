package com.note0292.statprep.data.game

import com.note0292.statprep.data.CategoryKind
import com.note0292.statprep.data.QuestionParser
import com.note0292.statprep.data.TextbookParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

class GameEngineTest {
    private val chapters = File("src/main/assets/textbook").listFiles { f -> f.extension == "json" }.orEmpty()
        .map { TextbookParser.parse(it.readText(Charsets.UTF_8)) }
    private val questions = QuestionParser.parse(File("src/main/assets/questions.json").readText(Charsets.UTF_8))
    private val curriculum = Curriculum.build(chapters, questions)
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun curriculumCoversEveryCardOnceWithinLearnDays() {
        assertEquals(Curriculum.TOTAL_DAYS, curriculum.days.size)
        val ids = curriculum.days.flatMap { it.newCardIds }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(curriculum.cards.keys, ids.toSet())
        assertTrue(curriculum.cards.values.none { it.category.kind == CategoryKind.ADVANCED })
        curriculum.days.forEach { d ->
            when {
                d.day == Curriculum.TOTAL_DAYS -> assertEquals(StageType.FINAL, d.type)
                d.day > Curriculum.LAST_LEARN_DAY -> assertEquals(StageType.REVIEW, d.type)
                d.day % 30 == 0 -> assertEquals(StageType.BOSS, d.type)
                else -> {
                    assertEquals(StageType.LEARN, d.type)
                    assertTrue("day ${d.day} has no new cards", d.newCardIds.isNotEmpty())
                    assertTrue("day ${d.day} too many new cards", d.newCardIds.size <= 4)
                }
            }
        }
        // 基礎数学から始まり、準1級の章へ進む
        assertEquals(CategoryKind.MATH, curriculum.day(1).categories.first().kind)
    }

    @Test
    fun stageFitsInThreeMinutes() {
        var state = GameState(startDate = start.toString())
        var today = start
        repeat(Curriculum.TOTAL_DAYS) { i ->
            val day = i + 1
            assertTrue("day $day playable", GameEngine.canPlay(state, today))
            val items = GameEngine.buildStage(curriculum, state, day, today, Random(day))
            assertTrue("day $day size ${items.size}", items.size in 1..GameEngine.FINAL_QUESTIONS)
            // 8 割正解するプレイヤー
            val results = items.indices.map { it % 5 != 4 }
            val outcome = GameEngine.complete(curriculum, state, day, items, results, 170, today)
            assertTrue("day $day cleared", outcome.cleared)
            state = outcome.state
            assertFalse(GameEngine.canPlay(state, today))
            today = today.plusDays(1)
        }
        assertTrue(state.completed)
        assertEquals(Curriculum.TOTAL_DAYS, state.streak(today.minusDays(1)))
        assertTrue(Badge.GRADUATE.name in state.badges)
        val readiness = GameEngine.readiness(curriculum, state)
        assertTrue("readiness $readiness", readiness > 0.6)
    }

    @Test
    fun bossRequiresPassMarkAndCanBeRetried() {
        var state = GameState(startDate = start.toString(), clearedDays = 29)
        curriculum.introducedBy(29).forEach { state = state.copy(cards = state.cards + (it.id to CardState(0, start.toString()))) }
        val items = GameEngine.buildStage(curriculum, state, 30, start, Random(1))
        assertEquals(GameEngine.BOSS_QUESTIONS, items.size)
        assertTrue(items.all { it.card is StudyCard.QuestionCard })
        val failed = GameEngine.complete(curriculum, state, 30, items, items.map { false }, 100, start)
        assertFalse(failed.cleared)
        assertEquals(29, failed.state.clearedDays)
        assertTrue(GameEngine.canPlay(failed.state, start))
        val passed = GameEngine.complete(curriculum, failed.state, 30, items, items.map { true }, 100, start)
        assertTrue(passed.cleared)
        assertTrue(Badge.WORLD_1 in passed.newBadges)
    }

    @Test
    fun catchUpAllowsTwoStagesPerDay() {
        val state = GameState(startDate = start.toString(), clearedDays = 3,
            records = mapOf(3 to StageRecord(start.plusDays(9).toString(), 3, 1, 1, 60)))
        val today = start.plusDays(9)
        assertTrue(state.daysBehind(today) > 0)
        assertTrue(GameEngine.canPlay(state, today))
        val twice = state.copy(clearedDays = 4, records = state.records + (4 to StageRecord(today.toString(), 3, 1, 1, 60)))
        assertFalse(GameEngine.canPlay(twice, today))
    }

    @Test
    fun spacedRepetitionIntervals() {
        val c0 = GameEngine.learn(start)
        assertEquals(start.plusDays(1).toString(), c0.due)
        val c1 = GameEngine.review(c0, true, start)
        assertEquals(1, c1.box)
        assertEquals(start.plusDays(4).toString(), c1.due)
        val c3 = GameEngine.review(GameEngine.review(c1, true, start), true, start)
        assertEquals(3, c3.box)
        assertEquals(1, GameEngine.review(c3, false, start).box)
        assertEquals(1.0, GameEngine.mastery(c3), 1e-9)
    }

    @Test
    fun bonusReviewDoesNotAdvanceDays() {
        var state = GameState(startDate = start.toString(), clearedDays = 10,
            records = mapOf(10 to StageRecord(start.toString(), 3, 1, 1, 60)))
        curriculum.introducedBy(10).forEach { state = state.copy(cards = state.cards + (it.id to CardState(0, start.toString()))) }
        assertTrue(GameEngine.dueCount(curriculum, state, start) > 0)
        val items = GameEngine.buildStage(curriculum, state, Curriculum.BONUS_DAY, start, Random(3))
        assertEquals(GameEngine.MAX_ITEMS, items.size)
        val out = GameEngine.complete(curriculum, state, Curriculum.BONUS_DAY, items, items.map { true }, 100, start)
        assertEquals(10, out.state.clearedDays)
        assertEquals(1, out.state.clearsOn(start))
        assertTrue(out.xpGained > 0)
    }

    @Test
    fun levels() {
        assertEquals(1, GameEngine.level(0))
        assertEquals(2, GameEngine.level(100))
        assertEquals(3, GameEngine.level(300))
        assertEquals("統計ビギナー", GameEngine.title(1))
    }
}
