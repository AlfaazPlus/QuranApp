package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.dialogs.AudioEndBehaviour
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.quran.QuranMeta

enum class PlayerInterationSource {
    HEADSET,
    USER
}

data class PlayerSettings(
    val speed: Float = 1.0f,
    val repeatCount: Int = 1,
    val audioEndBehaviour: AudioEndBehaviour = AudioEndBehaviour.DEFAULT,
    val audioOption: AudioOption = AudioOption.DEFAULT,
    val reciter: String? = null,
    val translationReciter: String? = null
)


data class AudioResolutionRequest(val chapterNo: Int, val settings: PlayerSettings)

data class RecitationServiceState(
    val currentVerse: ChapterVersePair = ChapterVersePair(1, 1),
    /** Helps in indiacating if the player is resolving and also for which chapter no */
    val resolvingChapterNo: Int? = null,
    /** 0–100 while chapter audio is downloading from the network; null otherwise. */
    val downloadProgress: Int? = null,
    val pausedByHeadset: Boolean = false,
    /** False when single-file chapter audio has no timing and no verse clip playlist is in use. */
    val isVerseSyncAvailable: Boolean = true,

    val clipPlan: VerseClipPlan? = null,
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

    fun toBundle(): Bundle = Bundle().apply {
        putInt(KEY_CURRENT_CHAPTER, currentVerse.chapterNo)
        putInt(KEY_CURRENT_VERSE, currentVerse.verseNo)
        putInt(KEY_RESOLVING_CHAPTER_NO, resolvingChapterNo ?: -1)
        putInt(KEY_AUDIO_DOWNLOAD_PROGRESS, downloadProgress ?: -1)
        putBoolean(KEY_IS_VERSE_SYNC_AVAILABLE, isVerseSyncAvailable)
        putBoolean(KEY_PAUSED_BY_HEADSET, pausedByHeadset)
        putString(KEY_CURRENT_RECITER, settings.reciter)
        putString(KEY_CURRENT_TRANSLATION_RECITER, settings.translationReciter)
        putFloat(KEY_PLAYBACK_SPEED, settings.speed)
        putInt(KEY_REPEAT_COUNT, settings.repeatCount)
        putString(KEY_AUDIO_END_BEHAVIOUR, settings.audioEndBehaviour.value)
        putString(KEY_AUDIO_OPTION, settings.audioOption.value)
    }

    companion object {
        private const val KEY_CURRENT_CHAPTER = "state_current_chapter"
        private const val KEY_CURRENT_VERSE = "state_current_verse"
        private const val KEY_CURRENT_RECITER = "state_current_reciter"
        private const val KEY_CURRENT_TRANSLATION_RECITER = "state_current_translation_reciter"
        private const val KEY_RESOLVING_CHAPTER_NO = "state_resolving_chapter_no"
        private const val KEY_AUDIO_DOWNLOAD_PROGRESS = "state_audio_download_progress"
        private const val KEY_IS_VERSE_SYNC_AVAILABLE = "state_is_verse_sync_available"
        private const val KEY_PAUSED_BY_HEADSET = "state_paused_by_headset"
        private const val KEY_PLAYBACK_SPEED = "state_playback_speed"
        private const val KEY_REPEAT_COUNT = "state_repeat_count"
        private const val KEY_AUDIO_END_BEHAVIOUR = "state_audio_end_behaviour"
        private const val KEY_AUDIO_OPTION = "state_audio_option"
        val EMPTY = RecitationServiceState()

        fun fromBundle(bundle: Bundle): RecitationServiceState {
            return RecitationServiceState(
                currentVerse = ChapterVersePair(
                    chapterNo = bundle.getInt(KEY_CURRENT_CHAPTER, -1),
                    verseNo = bundle.getInt(KEY_CURRENT_VERSE, -1),
                ),
                resolvingChapterNo = bundle.getInt(KEY_RESOLVING_CHAPTER_NO, -1)
                    .takeIf { it != -1 },
                downloadProgress = bundle.getInt(KEY_AUDIO_DOWNLOAD_PROGRESS, -1)
                    .takeIf { it >= 0 },
                isVerseSyncAvailable = bundle.getBoolean(KEY_IS_VERSE_SYNC_AVAILABLE, true),
                pausedByHeadset = bundle.getBoolean(KEY_PAUSED_BY_HEADSET, false),
                settings = PlayerSettings(
                    speed = bundle.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
                    repeatCount = bundle.getInt(KEY_REPEAT_COUNT, 1).coerceAtLeast(1),
                    audioEndBehaviour = bundle.getString(
                        KEY_AUDIO_END_BEHAVIOUR,
                    )?.let { AudioEndBehaviour.fromValue(it) } ?: AudioEndBehaviour.DEFAULT,
                    audioOption = bundle.getString(
                        KEY_AUDIO_OPTION
                    )?.let { AudioOption.fromValue(it) } ?: AudioOption.DEFAULT,
                    reciter = bundle.getString(KEY_CURRENT_RECITER),
                    translationReciter = bundle.getString(KEY_CURRENT_TRANSLATION_RECITER)
                ),
            )
        }
    }
}
