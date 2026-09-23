package com.note0292.statprep.data

import android.content.Context

class QuestionRepository(context: Context) {
    val questions: List<Question> by lazy {
        context.assets.open("questions.json").bufferedReader(Charsets.UTF_8).use {
            QuestionParser.parse(it.readText())
        }
    }

}
