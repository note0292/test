package com.note0292.pmlearn.data

import android.content.Context

/** assets/curriculum/stageNN.json と glossary.json から教材を読み込む。 */
class CurriculumRepository(context: Context) {
    val curriculum: Curriculum by lazy {
        val assets = context.assets
        fun read(path: String) = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val stages = assets.list(DIR).orEmpty()
            .filter { it.startsWith("stage") && it.endsWith(".json") }
            .sorted()
            .map { Curriculum.parseStage(read("$DIR/$it")) }
            .sortedBy { it.number }
        Curriculum(stages, Curriculum.parseGlossary(read("$DIR/glossary.json")))
    }

    private companion object {
        const val DIR = "curriculum"
    }
}
