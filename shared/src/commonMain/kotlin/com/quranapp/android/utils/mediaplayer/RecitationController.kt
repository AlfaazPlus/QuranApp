package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.platform.Platform
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-independent RecitationController interface.
 * Exposes reactive playback states and dispatches recitation playback/navigation commands.
 * Accessible platform-independently via [Platform.audio.controller].
 */
interface RecitationController {
    val isConnectedState: StateFlow<Boolean>
    val isPlayingState: StateFlow<Boolean>
    val isBufferingState: StateFlow<Boolean>

    val isPlaying: Boolean
    val isLoading: Boolean

    val state: StateFlow<RecitationServiceState>
    val currentVerseState: StateFlow<ChapterVersePair>
    val wordIndexState: StateFlow<Int>
    val playbackModeState: StateFlow<RecitationPlaybackMode>

    val currentPositionMs: Long
    val durationMs: Long

    fun connect()
    fun disconnect()

    fun playControl(verse: ChapterVersePair)
    fun start(verse: ChapterVersePair? = null)
    fun startRange(chapterNo: Int, fromVerse: Int, toVerse: Int, startVerse: Int = fromVerse)
    fun playControlRange(verse: ChapterVersePair, fromVerse: Int, toVerse: Int)
    fun clearMode()
    fun playPause()
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekLeft()
    fun seekRight()
    fun previousVerse()
    fun nextVerse()

    fun setSpeed(speed: Float)
    fun setRepeatCount(repeatCount: Int)
    fun setAudioEndBehaviour(behaviour: AudioEndBehaviour)
    fun setAudioOption(option: AudioOption)
    fun setVerseGroupSize(size: Int)
    fun setReciter(id: String, kind: RecitationAudioKind)

    companion object {
        fun get(): RecitationController = Platform.audio.controller
    }
}
