package com.note0292.statprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** assets/questions.json の内容が正しい形式かを検証する。 */
class QuestionBankTest {
    private val questions: List<Question> =
        QuestionParser.parse(File("src/main/assets/questions.json").readText(Charsets.UTF_8))

    @Test
    fun idsAreUnique() {
        val duplicated = questions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("重複した id: $duplicated", duplicated.isEmpty())
    }

    @Test
    fun everyQuestionIsWellFormed() {
        questions.forEach { q ->
            assertNotNull("未知の分野: ${q.id} ${q.category}", Category.fromId(q.category))
            assertTrue("選択肢が少ない: ${q.id}", q.choices.size >= 2)
            assertTrue("answer が範囲外: ${q.id}", q.answer in q.choices.indices)
            assertEquals("選択肢が重複: ${q.id}", q.choices.size, q.choices.toSet().size)
            assertTrue("問題文が空: ${q.id}", q.question.isNotBlank())
            assertTrue("解説が空: ${q.id}", q.explanation.isNotBlank())
        }
    }

    @Test
    fun everyCategoryHasQuestionsAndNotes() {
        Category.entries.forEach { c ->
            assertTrue("問題がない分野: ${c.label}", questions.count { it.category == c.id } >= 5)
            assertTrue("ノートがない分野: ${c.label}", FormulaNotes.notes[c].orEmpty().isNotEmpty())
        }
    }
}
