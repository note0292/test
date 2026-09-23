package com.note0292.pmlearn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** assets/curriculum の教材データが正しい形式かを検証する。 */
class CurriculumTest {
    private val dir = File("src/main/assets/curriculum")
    private val curriculum: Curriculum = Curriculum(
        stages = dir.listFiles { f -> f.name.startsWith("stage") && f.name.endsWith(".json") }!!
            .sortedBy { it.name }
            .map { Curriculum.parseStage(it.readText(Charsets.UTF_8)) }
            .sortedBy { it.number },
        glossary = Curriculum.parseGlossary(File(dir, "glossary.json").readText(Charsets.UTF_8)),
    )

    @Test
    fun stagesAreNumberedInOrder() {
        assertTrue("ステージがない", curriculum.stages.isNotEmpty())
        assertEquals((1..curriculum.stages.size).toList(), curriculum.stages.map { it.number })
        assertEquals(curriculum.stages.size, curriculum.stages.map { it.id }.toSet().size)
    }

    @Test
    fun lessonIdsAreUniqueAndPrefixedByStage() {
        val ids = curriculum.lessons.map { it.id }
        assertEquals("重複したレッスン id", ids.size, ids.toSet().size)
        curriculum.stages.forEach { stage ->
            assertTrue("レッスンがない: ${stage.id}", stage.lessons.isNotEmpty())
            stage.lessons.forEach { assertTrue("id の接頭辞: ${it.id}", it.id.startsWith("${stage.id}-")) }
        }
    }

    @Test
    fun everyLessonIsWellFormed() {
        curriculum.lessons.forEach { l ->
            assertTrue("所要時間: ${l.id}", l.minutes > 0)
            assertTrue("ゴールが空: ${l.id}", l.goal.isNotBlank())
            assertTrue("解説が少ない: ${l.id}", l.sections.size >= 2)
            l.sections.forEach { assertTrue("見出しか本文が空: ${l.id}", it.heading.isNotBlank() && it.body.isNotBlank()) }
            assertTrue("要点が少ない: ${l.id}", l.keyPoints.size >= 3)
            assertNotNull("やってみようがない: ${l.id}", l.exercise)
            assertTrue("理解度チェックが少ない: ${l.id}", l.quiz.size >= 3)
        }
    }

    @Test
    fun everyQuestionIsWellFormed() {
        curriculum.questions.forEach { lq ->
            val q = lq.question
            assertTrue("選択肢が少ない: ${lq.id}", q.choices.size >= 2)
            assertTrue("answer が範囲外: ${lq.id}", q.answer in q.choices.indices)
            assertEquals("選択肢が重複: ${lq.id}", q.choices.size, q.choices.toSet().size)
            assertTrue("問題文が空: ${lq.id}", q.question.isNotBlank())
            assertTrue("解説が空: ${lq.id}", q.explanation.isNotBlank())
        }
    }

    @Test
    fun glossaryReferencesExistingStages() {
        assertTrue("用語が少ない", curriculum.glossary.size >= 50)
        val terms = curriculum.glossary.map { it.term }
        assertEquals("重複した用語", terms.size, terms.toSet().size)
        curriculum.glossary.forEach { assertNotNull("未知のステージ: ${it.term}", curriculum.stage(it.stageId)) }
    }

    @Test
    fun nextLessonFollowsCurriculumOrder() {
        val lessons = curriculum.lessons
        assertEquals(lessons[1], curriculum.nextLesson(lessons[0].id))
        assertEquals(null, curriculum.nextLesson(lessons.last().id))
        val done = Progress(completedLessons = setOf(lessons[0].id))
        assertEquals(lessons[1], curriculum.firstIncomplete(done))
    }
}
