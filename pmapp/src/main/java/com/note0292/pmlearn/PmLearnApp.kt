package com.note0292.pmlearn

import android.app.Application
import com.note0292.pmlearn.data.CurriculumRepository
import com.note0292.pmlearn.data.ProgressStore

class PmLearnApp : Application() {
    val curriculumRepository by lazy { CurriculumRepository(this) }
    val progressStore by lazy { ProgressStore(this) }
}
