package com.quranapp.android.utils.mediaplayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Shared ExoPlayer helpers tuned for speech/recitation on low-RAM devices.
 */
@OptIn(UnstableApi::class)
object RecitationExoPlayers {
    private const val MIN_BUFFER_MS = 8_000
    private const val MAX_BUFFER_MS = 16_000
    private const val BUFFER_FOR_PLAYBACK_MS = 1_000
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000
    private const val TARGET_BUFFER_BYTES = 2 * 1024 * 1024

    fun createAudioLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .build()
    }

    fun isOutOfMemoryError(error: Throwable?): Boolean {
        var current = error

        while (current != null) {
            if (current is OutOfMemoryError) return true

            current = current.cause
        }

        return false
    }
}
