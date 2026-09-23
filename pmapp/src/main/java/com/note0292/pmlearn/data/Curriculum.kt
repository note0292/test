package com.note0292.pmlearn.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 解説の 1 まとまり。[body] は段落を空行で区切り、「・」で始まる行は箇条書きとして表示する。 */
@Serializable
data class Section(
    val heading: String,
    val body: String,
)

/** 手を動かして考える課題。[sampleAnswer] は自分で書いてから確認する模範解答。 */
@Serializable
data class Exercise(
    val prompt: String,
    val hint: String = "",
    val sampleAnswer: String,
)

@Serializable
data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val minutes: Int,
    /** このレッスンを終えるとできるようになること。 */
    val goal: String,
    val sections: List<Section>,
    val keyPoints: List<String>,
    /** 実務でどう使われるかの具体例。 */
    val example: String = "",
    val exercise: Exercise? = null,
    val quiz: List<QuizQuestion>,
) {
    /** 理解度チェックの各問題の id（学習記録のキーに使う）。 */
    fun questionId(index: Int): String = "$id#$index"
}

@Serializable
data class Stage(
    val id: String,
    val number: Int,
    val title: String,
    val level: String,
    val description: String,
    val lessons: List<Lesson>,
)

@Serializable
data class GlossaryTerm(
    val term: String,
    val reading: String = "",
    val description: String,
    val stageId: String,
)

@Serializable
data class GlossaryFile(val terms: List<GlossaryTerm>)

/** 出題用に、元のレッスンへの参照を持たせた問題。 */
data class LessonQuestion(
    val id: String,
    val lesson: Lesson,
    val stage: Stage,
    val question: QuizQuestion,
)

class Curriculum(val stages: List<Stage>, val glossary: List<GlossaryTerm>) {
    val lessons: List<Lesson> = stages.flatMap { it.lessons }

    val questions: List<LessonQuestion> = stages.flatMap { stage ->
        stage.lessons.flatMap { lesson ->
            lesson.quiz.mapIndexed { i, q -> LessonQuestion(lesson.questionId(i), lesson, stage, q) }
        }
    }

    fun stage(id: String): Stage? = stages.firstOrNull { it.id == id }
    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }
    fun stageOf(lessonId: String): Stage? = stages.firstOrNull { s -> s.lessons.any { it.id == lessonId } }

    /** 学習順で次のレッスン（最後なら null）。 */
    fun nextLesson(lessonId: String): Lesson? {
        val i = lessons.indexOfFirst { it.id == lessonId }
        return if (i < 0) null else lessons.getOrNull(i + 1)
    }

    /** まだ修了していない最初のレッスン。すべて修了していれば null。 */
    fun firstIncomplete(progress: Progress): Lesson? = lessons.firstOrNull { it.id !in progress.completedLessons }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseStage(text: String): Stage = json.decodeFromString<Stage>(text)
        fun parseGlossary(text: String): List<GlossaryTerm> = json.decodeFromString<GlossaryFile>(text).terms
    }
}
