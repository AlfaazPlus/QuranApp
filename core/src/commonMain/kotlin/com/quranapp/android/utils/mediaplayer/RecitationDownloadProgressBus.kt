package com.quranapp.android.utils.mediaplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChapterByteProgress(val bytesDownloaded: Long, val totalBytes: Long)

/** In-memory mirror of live download bytes. Restored via wordInfo progress. */
object RecitationDownloadProgressBus {
    private val _state = MutableStateFlow<Map<String, ChapterByteProgress>>(emptyMap())
    val state: StateFlow<Map<String, ChapterByteProgress>> = _state.asStateFlow()

    fun key(reciterId: String, chapterNo: Int): String = "$reciterId:$chapterNo"

    fun parseBusKey(key: String): Pair<String, Int>? {
        val i = key.lastIndexOf(':')

        if (i <= 0) return null

        val reciterId = key.substring(0, i)
        val chapterNo = key.substring(i + 1).toIntOrNull() ?: return null

        return reciterId to chapterNo
    }

    fun snapshotKeys(): Set<String> = _state.value.keys.toSet()

    fun set(reciterId: String, chapterNo: Int, bytes: Long, total: Long) {
        val k = key(reciterId, chapterNo)
        _state.update { it + (k to ChapterByteProgress(bytes, total)) }
    }

    fun clear(reciterId: String, chapterNo: Int) {
        val k = key(reciterId, chapterNo)
        _state.update { it - k }
    }

    fun prune(shouldRemove: (String) -> Boolean) {
        _state.update { current ->
            current.filterKeys { !shouldRemove(it) }
        }
    }
}
