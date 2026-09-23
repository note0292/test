package com.note0292.statprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** assets/textbook/ のテキストデータが正しい形式かを検証する。 */
class TextbookTest {
    private val chapters: List<Chapter> =
        File("src/main/assets/textbook").listFiles { f -> f.extension == "json" }.orEmpty()
            .map { TextbookParser.parse(it.readText(Charsets.UTF_8)) }

    @Test
    fun everyCategoryHasExactlyOneChapter() {
        assertEquals(
            Category.entries.map { it.id }.sorted(),
            chapters.map { it.category }.sorted(),
        )
    }

    @Test
    fun lessonIdsAreUnique() {
        val ids = chapters.flatMap { ch -> ch.lessons.map { it.id } }
        val duplicated = ids.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue("重複したレッスン id: $duplicated", duplicated.isEmpty())
    }

    @Test
    fun chaptersAreComplete() {
        chapters.forEach { ch ->
            assertTrue("導入が空: ${ch.category}", ch.intro.isNotBlank())
            assertTrue("レッスンが少ない: ${ch.category}", ch.lessons.size >= 4)
            assertTrue("公式まとめがない: ${ch.category}", ch.formulas.isNotEmpty())
            ch.lessons.forEach { l ->
                assertTrue("本文が空: ${l.id}", l.body.isNotBlank())
                assertTrue("定理がない: ${l.id}", l.theorems.isNotEmpty())
                l.theorems.forEach { t ->
                    assertTrue("定理の記述が空: ${l.id} ${t.title}", t.statement.isNotBlank() && t.title.isNotBlank())
                    assertTrue("証明が空: ${l.id} ${t.title}", t.proof.isNotBlank())
                }
                l.examples.forEach { e ->
                    assertTrue("例題が空: ${l.id}", e.question.isNotBlank() && e.solution.isNotBlank())
                }
            }
        }
    }
}
