package com.quranapp.android.utils.mediaplayer

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WbwAudioPlayer {
    private val mutex = Mutex()
    private var player: ExoPlayer? = null

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        player?.let { return it }

        val app = context.applicationContext

        return ExoPlayer.Builder(app).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )

            repeatMode = Player.REPEAT_MODE_OFF

            addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.saveError(error, "WbwWordAudioPlayer")
                    }
                },
            )
        }.also { player = it }
    }

    private fun isValidTimingWindow(startMs: Long, endMs: Long): Boolean {
        if (
            startMs == C.TIME_UNSET ||
            endMs == C.TIME_UNSET ||
            startMs < 0L ||
            endMs < 0L ||
            endMs <= startMs
        ) {
            return false
        }

        return try {
            Math.subtractExact(endMs, startMs) > 0L
        } catch (_: ArithmeticException) {
            false
        }
    }

    suspend fun play(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): WbwAudioPlayResult {
        val app = context.applicationContext

        if (
            WbwAudioRepository.getTimingCount(context) == 0 &&
            !NetworkStateReceiver.isNetworkConnected(app)
        ) {
            return WbwAudioPlayResult.NoInternet
        }

        val timing = WbwAudioRepository.getWordTiming(context, chapterNo, verseNo, appWordIndex)

        if (timing == null) {
            val count = WbwAudioRepository.getTimingCount(context)
            return when {
                count == 0 && !NetworkStateReceiver.isNetworkConnected(app) ->
                    WbwAudioPlayResult.NoInternet

                else -> WbwAudioPlayResult.TimingsNotLoaded
            }
        }

        if (!isValidTimingWindow(timing.startMillis, timing.endMillis)) {
            Log.saveError(
                Exception("Invalid WBW timing window ${timing.startMillis}–${timing.endMillis}"),
                "WbwAudioPlayer.play",
            )
            return WbwAudioPlayResult.InvalidTiming
        }

        val uri = WbwAudioRepository.resolveChapterAudioUri(context, chapterNo)

        if (uri == null) {
            return if (!NetworkStateReceiver.isNetworkConnected(app)) {
                WbwAudioPlayResult.NoInternet
            } else {
                WbwAudioPlayResult.NoChapterAudio
            }
        }

        mutex.withLock {
            val exo = getOrCreatePlayer(context).apply {
                stop()
                clearMediaItems()
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(timing.startMillis)
                                .setEndPositionMs(timing.endMillis)
                                .build(),
                        )
                        .build(),
                )
            }

            exo.prepare()
            exo.playWhenReady = true
        }

        return WbwAudioPlayResult.Success
    }

    suspend fun warmUp(context: Context) {
        try {
            WbwAudioRepository.ensureTimingsAvailable(context)
        } catch (e: Exception) {
            Log.saveError(e, "WbwAudioPlayer.warmUp")
        }
    }
}

sealed class WbwAudioPlayResult {
    data object Success : WbwAudioPlayResult()
    data object NoInternet : WbwAudioPlayResult()
    data object TimingsNotLoaded : WbwAudioPlayResult()
    data object InvalidTiming : WbwAudioPlayResult()
    data object NoChapterAudio : WbwAudioPlayResult()
}
