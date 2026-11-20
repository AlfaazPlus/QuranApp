package com.quranapp.android.platform.mediaplayer

import com.quranapp.android.utils.mediaplayer.RecitationController
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform media gateway contract exposed via [com.quranapp.android.platform.Platform.audio].
 * Handles playback, caching, and recitation controller access.
 */
interface MediaPlayer {
    /** Global recitation controller for chapter, range, and continuous playback. */
    val controller: RecitationController

    /** Cache manager handling disk stream caching, size limits, and reciter eviction. */
    val cache: MediaCacheManager

    /**
     * Creates an [AudioPlayer] instance configured for the specified workload.
     * Speech playback uses a low-memory load control (8-16s buffer, 2MB target) by default.
     */
    fun createPlayer(config: AudioPlayerConfig = AudioPlayerConfig.Default): AudioPlayer
}

/**
 * Platform-agnostic low-level playback engine.
 * Supports single media files, streams, or multi-item clipped playlists.
 */
interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: Long
    val durationMs: Long
    val bufferedPositionMs: Long
    val currentItemIndex: Int
    val playbackSpeed: Float

    fun setSource(clip: AudioClip)
    fun setPlaylist(clips: List<AudioClip>, startIndex: Int = 0, startPositionMs: Long = 0L)
    fun clearPlaylist()

    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekTo(itemIndex: Int, positionMs: Long)
    fun setPlaybackSpeed(speed: Float)

    /**
     * Schedules a zero-polling one-shot trigger at [positionMs] in item [itemIndex].
     * Maps natively to ExoPlayer createMessage (Android) and boundary time observers (iOS).
     */
    fun schedulePositionTrigger(
        itemIndex: Int,
        positionMs: Long,
        onTrigger: () -> Unit,
    ): CancellableTrigger?

    fun addListener(listener: PlayerListener)
    fun removeListener(listener: PlayerListener)

    /** Releases native decoders, sinks, and audio tracks immediately. */
    fun release()
}

/**
 * Handle to cancel a scheduled position message.
 */
fun interface CancellableTrigger {
    fun cancel()
}

/**
 * Platform-independent descriptor for a media item or clipped segment.
 */
data class AudioClip(
    val id: String,
    val uri: String,
    val startMs: Long = 0L,
    val endMs: Long = UNSET_TIME_MS,
    val metadata: ClipMetadata? = null,
) {
    val isClipped: Boolean get() = startMs > 0L || (endMs != UNSET_TIME_MS && endMs > startMs)
    val clipDurationMs: Long get() = if (isClipped && endMs > startMs) endMs - startMs else 0L

    companion object {
        const val UNSET_TIME_MS = Long.MIN_VALUE + 1L
    }
}

data class ClipMetadata(
    val title: String? = null,
    val artist: String? = null,
    val chapterNo: Int = -1,
    val verseNo: Int = -1,
    val artworkUri: String? = null,
)

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

enum class AudioUsage {
    SPEECH,
    MEDIA
}

data class AudioPlayerConfig(
    val audioUsage: AudioUsage = AudioUsage.SPEECH,
    val bufferConfig: BufferConfig = BufferConfig.LowRamSpeech,
    val useCache: Boolean = true,
) {
    companion object {
        val Default = AudioPlayerConfig()
    }
}

/**
 * Buffer configuration tuned for speech playback on low-RAM devices.
 */
data class BufferConfig(
    val minBufferMs: Int = 8_000,
    val maxBufferMs: Int = 16_000,
    val bufferForPlaybackMs: Int = 1_000,
    val bufferForPlaybackAfterRebufferMs: Int = 2_000,
    val targetBufferBytes: Int = 2 * 1024 * 1024, // 2MB speech allocation cap
) {
    companion object {
        val LowRamSpeech = BufferConfig()
        val Standard = BufferConfig(
            minBufferMs = 15_000,
            maxBufferMs = 50_000,
            bufferForPlaybackMs = 2_500,
            bufferForPlaybackAfterRebufferMs = 5_000,
            targetBufferBytes = 16 * 1024 * 1024,
        )
    }
}

interface PlayerListener {
    fun onPlaybackStateChanged(state: PlaybackState) {}
    fun onIsPlayingChanged(isPlaying: Boolean) {}
    fun onMediaItemTransition(itemIndex: Int, clip: AudioClip?) {}
    fun onPositionDiscontinuity(oldPositionMs: Long, newPositionMs: Long) {}
    fun onError(error: Throwable) {}
}

/**
 * Manages HTTP audio stream caching on disk.
 */
interface MediaCacheManager {
    val maxSizeBytes: Long
    fun getCacheSizeBytes(): Long
    fun clearForReciter(urlTemplate: String)
    fun clearAll()
    fun evictResource(uri: String)
}
