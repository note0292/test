package com.note0292.statprep.data

import android.content.Context

class TextbookRepository(context: Context) {
    private val chapters: Map<Category, Chapter> by lazy {
        val assets = context.assets
        assets.list(DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .map { name -> assets.open("$DIR/$name").bufferedReader(Charsets.UTF_8).use { TextbookParser.parse(it.readText()) } }
            .associateBy { it.categoryEnum }
    }

    private val lessonIndex: Map<String, Pair<Chapter, Int>> by lazy {
        chapters.values.flatMap { ch -> ch.lessons.mapIndexed { i, l -> l.id to (ch to i) } }.toMap()
    }

    fun chapter(category: Category): Chapter? = chapters[category]

    /** レッスンと、それを含む章・章内の位置。 */
    fun lesson(id: String): Pair<Chapter, Int>? = lessonIndex[id]

    private companion object {
        const val DIR = "textbook"
    }
}
