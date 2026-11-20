package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.serialization.Serializable

@Serializable
enum class PlayerInterationSource {
    HEADSET,
    USER
}

@Serializable
data class RecitationServiceState(
    val currentVerse: ChapterVersePair = ChapterVersePair(-1, -1),
    /** Helps in indiacating if the player is resolving and also for which chapter no */
    val resolvingChapterNo: Int? = null,
    /** 0–100 while chapter audio is downloading from the network; null otherwise. */
    val downloadProgress: Int? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val pausedByHeadset: Boolean = false,
    /** False when single-file chapter audio has no timing and no verse clip playlist is in use. */
    val isVerseSyncAvailable: Boolean = true,
    val playbackScope: PlaybackScopeType = PlaybackScopeType.CONTINUOUS,
    val settings: PlayerSettings = PlayerSettings(),
) {
    suspend fun getPreviousVerse(repository: QuranRepository): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !repository.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var previousChapterNo = currentChapterNo
        var previousVerseNo = currentVerseNo - 1

        if (previousVerseNo < 1) {
            if (QuranMeta.isChapterValid(previousChapterNo - 1)) {
                previousChapterNo--
                previousVerseNo = repository.getChapterVerseCount(previousChapterNo)
            } else {
                previousVerseNo = -1
            }
        }

        if (previousChapterNo == -1 || previousVerseNo == -1) {
            return null
        }

        return ChapterVersePair(previousChapterNo, previousVerseNo)
    }

    suspend fun getNextVerse(repository: QuranRepository): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !repository.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = currentVerseNo + 1

        if (nextVerseNo > repository.getChapterVerseCount(nextChapterNo)) {
            if (QuranMeta.isChapterValid(nextChapterNo + 1)) {
                nextChapterNo++
                nextVerseNo = 1
            } else {
                nextVerseNo = -1
            }
        }

        if (nextChapterNo == -1 || nextVerseNo == -1) {
            return null
        }

        return ChapterVersePair(nextChapterNo, nextVerseNo)
    }

    companion object {
        val EMPTY = RecitationServiceState()

    }
}
