package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.platform.PlatformServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

@OptIn(UnstableApi::class)
object WbwAudioPlayer {
    private val mutex = Mutex()
    private val playerLock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val idleReleaseRunnable = Runnable { stop() }
    private var player: ExoPlayer? = null
    private const val ONE_OFF_CACHE_MAX_BYTES = 16L * 1024 * 1024
    private const val IDLE_RELEASE_DELAY_MS = 2_000L

    @Volatile
    private var oneOffSimpleCache: SimpleCache? = null

    private val oneOffCacheLock = Any()

    private fun oneOffCache(context: Context): SimpleCache {
        synchronized(oneOffCacheLock) {
            oneOffSimpleCache?.let { return it }

            val app = context.applicationContext
            val dir = File(app.cacheDir, "exo_wbw_one_off_cache")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val evictor = LeastRecentlyUsedCacheEvictor(ONE_OFF_CACHE_MAX_BYTES)
            val dbProvider = StandaloneDatabaseProvider(app)

            return SimpleCache(dir, evictor, dbProvider).also {
                oneOffSimpleCache = it
            }
        }
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        synchronized(playerLock) {
            player?.let { return it }

            val app = context.applicationContext
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(oneOffCache(app))
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val dataSourceFactory = DefaultDataSource.Factory(app, cacheDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            return ExoPlayer.Builder(app)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(RecitationExoPlayers.createAudioLoadControl())
                .build()
                .apply {
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
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    scheduleIdleRelease()
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                Logger.saveError(error, "WbwWordAudioPlayer")
                                if (RecitationExoPlayers.isOutOfMemoryError(error)) {
                                    mainHandler.post { stop() }
                                }
                            }
                        },
                    )
                }.also { player = it }
        }
    }

    /**
     * Stops word playback and releases the ExoPlayer so it does not keep a second
     * decoder/sink alive alongside [RecitationService].
     */
    fun stop() {
        mainHandler.removeCallbacks(idleReleaseRunnable)

        val exo = synchronized(playerLock) {
            val current = player
            player = null
            current
        } ?: return

        try {
            exo.playWhenReady = false
            exo.stop()
            exo.clearMediaItems()
            exo.release()
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioPlayer.stop")
        }
    }

    private fun scheduleIdleRelease() {
        mainHandler.removeCallbacks(idleReleaseRunnable)
        mainHandler.postDelayed(idleReleaseRunnable, IDLE_RELEASE_DELAY_MS)
    }

    private fun cancelIdleRelease() {
        mainHandler.removeCallbacks(idleReleaseRunnable)
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

        val source = WbwAudioRepository.resolveWordPlaybackSource(
            chapterNo = chapterNo,
            verseNo = verseNo,
            appWordIndex = appWordIndex,
        )

        if (source == null) {
            return if (!PlatformServices.networkMonitor.isConnected) {
                WbwAudioPlayResult.NoInternet
            } else {
                WbwAudioPlayResult.NoChapterAudio
            }
        }

        Logger.d("Wbw Audio Source", source)

        when (source) {
            is WbwWordPlaybackSource.OneOff -> {
                mutex.withLock {
                    try {
                        cancelIdleRelease()
                        RecitationController.getInstance(app).pause()

                        val exo = getOrCreatePlayer(context).apply {
                            stop()
                            clearMediaItems()
                            setMediaItem(
                                MediaItem.Builder()
                                    .setUri(source.uri)
                                    .build(),
                            )
                        }

                        exo.prepare()
                        exo.playWhenReady = true
                    } catch (oom: OutOfMemoryError) {
                        Logger.saveError(oom, "WbwAudioPlayer.play.OOM")
                        stop()
                        return WbwAudioPlayResult.Unknown
                    }
                }
            }

            is WbwWordPlaybackSource.Chapter -> {
                if (
                    WbwAudioRepository.getTimingCount(context) == 0 &&
                    !PlatformServices.networkMonitor.isConnected
                ) {
                    return WbwAudioPlayResult.NoInternet
                }

                val timing =
                    WbwAudioRepository.getWordTiming(context, chapterNo, verseNo, appWordIndex)

                if (timing == null) {
                    val count = WbwAudioRepository.getTimingCount(context)

                    return when {
                        count == 0 && !PlatformServices.networkMonitor.isConnected ->
                            WbwAudioPlayResult.NoInternet

                        else -> WbwAudioPlayResult.TimingsNotLoaded
                    }
                }

                if (!isValidTimingWindow(timing.startMillis, timing.endMillis)) {
                    Logger.saveError(
                        Exception("Invalid WBW timing window ${timing.startMillis}–${timing.endMillis}"),
                        "WbwAudioPlayer.play",
                    )
                    return WbwAudioPlayResult.InvalidTiming
                }

                mutex.withLock {
                    try {
                        cancelIdleRelease()
                        RecitationController.getInstance(app).pause()

                        val exo = getOrCreatePlayer(context).apply {
                            stop()
                            clearMediaItems()
                            setMediaItem(
                                MediaItem.Builder()
                                    .setUri(source.uri)
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
                    } catch (oom: OutOfMemoryError) {
                        Logger.saveError(oom, "WbwAudioPlayer.play.OOM")
                        stop()
                        return WbwAudioPlayResult.Unknown
                    }
                }
            }
        }

        return WbwAudioPlayResult.Success
    }

    suspend fun warmUp(context: Context) {
        try {
            WbwAudioRepository.ensureTimingsAvailable(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioPlayer.warmUp")
        }
    }
}

sealed class WbwAudioPlayResult {
    data object Success : WbwAudioPlayResult()
    data object NoInternet : WbwAudioPlayResult()
    data object TimingsNotLoaded : WbwAudioPlayResult()
    data object InvalidTiming : WbwAudioPlayResult()
    data object NoChapterAudio : WbwAudioPlayResult()
    data object Unknown : WbwAudioPlayResult()
}
