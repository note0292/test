package com.note0292.statprep.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 定理・命題とその証明。 */
@Serializable
data class Theorem(
    val title: String,
    val statement: String,
    val proof: String,
)

/** 解答を隠して表示する例題。 */
@Serializable
data class Example(
    val question: String,
    val solution: String,
)

/** 章末の公式まとめの 1 項目。 */
@Serializable
data class Formula(
    val title: String,
    val body: String,
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val body: String,
    val theorems: List<Theorem> = emptyList(),
    val examples: List<Example> = emptyList(),
)

/**
 * 1 分野 = 1 章。assets/textbook/<category>.json に対応する。
 * 文字列中の数式は TeX 記法で、インラインは `$...$`、別行立ては `$$...$$` で囲む。
 */
@Serializable
data class Chapter(
    val category: String,
    val intro: String,
    val lessons: List<Lesson>,
    val formulas: List<Formula> = emptyList(),
) {
    val categoryEnum: Category get() = Category.fromId(category) ?: error("unknown category: $category")
}

object TextbookParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Chapter = json.decodeFromString<Chapter>(text)
}
