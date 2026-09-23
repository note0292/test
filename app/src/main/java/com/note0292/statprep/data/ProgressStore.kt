package com.note0292.statprep.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.time.LocalDate

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _progress = MutableStateFlow(load())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private fun load(): Progress {
        val raw = prefs.getString(KEY, null) ?: return Progress()
        return runCatching { json.decodeFromString<Progress>(raw) }.getOrDefault(Progress())
    }

    private fun update(transform: (Progress) -> Progress) {
        val next = transform(_progress.value)
        _progress.value = next
        prefs.edit().putString(KEY, json.encodeToString(Progress.serializer(), next)).apply()
    }

    fun recordAnswer(id: String, correct: Boolean) =
        update { it.recordAnswer(id, correct, System.currentTimeMillis(), LocalDate.now()) }

    fun toggleBookmark(id: String) = update { it.toggleBookmark(id) }

    fun reset() = update { Progress() }

    private companion object {
        const val KEY = "progress_v1"
    }
}
