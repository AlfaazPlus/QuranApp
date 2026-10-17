package com.quranapp.android.api.models.mediaplayer

import android.net.Uri
import androidx.media3.common.C
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

data class VerseSegment(
    val index: Int,
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class VerseTiming(
    @SerialName("verse")
    val verseNo: Int,

    @SerialName("start_ms")
    val startMs: Long,

    @SerialName("end_ms")
    val endMs: Long,

    @SerialName("segments")
    private val segments: List<List<Long>>? = null
) {
    val hasWordSegments: Boolean
        get() = !segments.isNullOrEmpty()

    val durationMs: Long
        get() {
            if (
                startMs == C.TIME_UNSET ||
                endMs == C.TIME_UNSET ||
                startMs < 0L ||
                endMs < 0L ||
                endMs <= startMs
            ) return 0L

            return try {
                Math.subtractExact(endMs, startMs)
            } catch (_: ArithmeticException) {
                0L
            }
        }

    private val seg: List<VerseSegment> by lazy {
        segments?.map {
            VerseSegment(
                index = it[0].toInt(),
                startMs = it[1],
                endMs = it[2]
            )
        } ?: emptyList()
    }

    fun containsPosition(positionMs: Long): Boolean {
        return positionMs in startMs until endMs
    }

    fun getSegmentAtPosition(positionMs: Long): VerseSegment? {
        val list = seg
        var lo = 0
        var hi = list.lastIndex

        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val segment = list[mid]

            when {
                positionMs < segment.startMs -> hi = mid - 1
                positionMs >= segment.endMs -> lo = mid + 1
                else -> return segment
            }
        }

        return null
    }
}

@Serializable
data class ChapterTimingMetadata(
    @SerialName("chapter")
    val chapterNo: Int,

    @SerialName("duration_ms")
    val durationMs: Long,

    @SerialName("verses")
    val verses: List<VerseTiming>? = null
) {
    val hasVerseTiming: Boolean get() = !verses.isNullOrEmpty()
    val hasWordTiming: Boolean by lazy {
        verses?.any { it.hasWordSegments } == true
    }

    @Transient
    private val verseByNo: Map<Int, VerseTiming> =
        verses?.associateBy { it.verseNo } ?: emptyMap()

    fun getVerseAtPosition(positionMs: Long): VerseTiming? {
        val list = verses ?: return null
        var lo = 0
        var hi = list.size - 1

        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val v = list[mid]

            when {
                positionMs < v.startMs -> hi = mid - 1
                positionMs >= v.endMs -> lo = mid + 1
                else -> return v
            }
        }

        return null
    }

    fun getVerseTiming(verseNo: Int): VerseTiming? = verseByNo[verseNo]

    fun hasCompleteTimingFor(fromVerse: Int, toVerse: Int): Boolean {
        if (verseByNo.isEmpty()) return false
        return (fromVerse..toVerse).all { it in verseByNo }
    }
}

@Serializable
data class AudioTimingMetadata(
    @SerialName("reciter")
    val reciterId: String,
    @SerialName("chapters")
    val chapters: List<ChapterTimingMetadata> = emptyList()
) {}

enum class RecitationAudioKind(val type: String) {
    QURAN("quran"),
    TRANSLATION("transl"),
}

class RecitationAudioTrack(
    val kind: RecitationAudioKind,
    val chapterNo: Int,
    val reciterId: String,
    val audioUri: Uri,
    val timingMetadata: ChapterTimingMetadata?,
) {
    val hasVerseTiming: Boolean
        get() =
            timingMetadata?.hasVerseTiming ?: false

    suspend fun getReciterName(manager: RecitationModelManager): String {
        if (kind == RecitationAudioKind.QURAN) {
            return manager.getQuranModel(reciterId)?.getReciterName() ?: ""
        } else {
            return manager.getTranslationModel(reciterId)?.getReciterName() ?: ""
        }
    }

}


sealed class ResolvedAudioResult {
    data class Resoved(
        val chapter: Int,
        val quran: RecitationAudioTrack?,
        val translation: RecitationAudioTrack?,
    ) : ResolvedAudioResult()

    /** [progress] 0–100 from the download worker; negative values are internal signals (e.g. clear UI). */
    data class Downloading(val progress: Int) : ResolvedAudioResult()

    data class Error(val error: Throwable) : ResolvedAudioResult()
}
