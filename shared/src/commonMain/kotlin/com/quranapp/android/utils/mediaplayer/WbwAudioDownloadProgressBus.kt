package com.quranapp.android.utils.mediaplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory mirror of live download bytes. Restored via wordInfo progress. */
object WbwAudioDownloadProgressBus {
    private val _state = MutableStateFlow<Map<String, ChapterByteProgress>>(emptyMap())
    val state: StateFlow<Map<String, ChapterByteProgress>> = _state.asStateFlow()

    fun key(busId: String, chapterNo: Int): String = "$busId:$chapterNo"

    fun parseBusKey(key: String): Pair<String, Int>? {
        val i = key.lastIndexOf(':')

        if (i <= 0) return null

        val busId = key.substring(0, i)
        val chapterNo = key.substring(i + 1).toIntOrNull() ?: return null

        return busId to chapterNo
    }

    fun set(busId: String, chapterNo: Int, bytes: Long, total: Long) {
        val k = key(busId, chapterNo)

        _state.update { it + (k to ChapterByteProgress(bytes, total)) }
    }

    fun clear(busId: String, chapterNo: Int) {
        val k = key(busId, chapterNo)

        _state.update { it - k }
    }

    fun prune(shouldRemove: (String) -> Boolean) {
        _state.update { current ->
            current.filterKeys { !shouldRemove(it) }
        }
    }
}
