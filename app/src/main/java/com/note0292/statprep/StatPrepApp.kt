package com.note0292.statprep

import android.app.Application
import com.note0292.statprep.data.ProgressStore
import com.note0292.statprep.data.QuestionRepository

class StatPrepApp : Application() {
    val questionRepository by lazy { QuestionRepository(this) }
    val progressStore by lazy { ProgressStore(this) }
}
