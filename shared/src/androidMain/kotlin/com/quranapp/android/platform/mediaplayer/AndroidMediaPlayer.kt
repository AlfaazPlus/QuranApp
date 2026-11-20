package com.quranapp.android.platform.mediaplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.mediaplayer.AndroidRecitationController
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationExoPlayers
import com.quranapp.android.utils.mediaplayer.RecitationUtils
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.FileSystem
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(UnstableApi::class)
class AndroidMediaPlayer(
    private val appContext: Context,
) : MediaPlayer {

    override val controller: RecitationController =
        AndroidRecitationController.getInstance(appContext)

    override val cache: MediaCacheManager = AndroidMediaCacheManager(appContext)

    override fun createPlayer(config: AudioPlayerConfig): AudioPlayer {
        return AndroidAudioPlayer(appContext, config)
    }
}

@OptIn(UnstableApi::class)
class AndroidAudioPlayer(
    private val appContext: Context,
    private val config: AudioPlayerConfig,
) : AudioPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<PlayerListener>()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentClips: List<AudioClip> = emptyList()

    private var exoPlayer: ExoPlayer? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                config.bufferConfig.minBufferMs,
                config.bufferConfig.maxBufferMs,
                config.bufferConfig.bufferForPlaybackMs,
                config.bufferConfig.bufferForPlaybackAfterRebufferMs,
            )
            .setTargetBufferBytes(config.bufferConfig.targetBufferBytes)
            .build()

        val builder = ExoPlayer.Builder(appContext)
            .setLoadControl(loadControl)

        if (config.useCache) {
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(AndroidMediaCacheManager.getSimpleCache(appContext))
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val dataSourceFactory = DefaultDataSource.Factory(appContext, cacheDataSourceFactory)
            builder.setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        }

        val audioUsage = when (config.audioUsage) {
            AudioUsage.SPEECH -> C.USAGE_MEDIA
            AudioUsage.MEDIA -> C.USAGE_MEDIA
        }
        val contentType = when (config.audioUsage) {
            AudioUsage.SPEECH -> C.AUDIO_CONTENT_TYPE_SPEECH
            AudioUsage.MEDIA -> C.AUDIO_CONTENT_TYPE_MUSIC
        }

        val player = builder.build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(audioUsage)
                    .setContentType(contentType)
                    .build(),
                true,
            )
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(playerEventListener)
        }

        exoPlayer = player
    }

    override val currentPositionMs: Long
        get() = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L

    override val durationMs: Long
        get() {
            val d = exoPlayer?.duration ?: 0L
            return if (d > 0L && d != C.TIME_UNSET) d else 0L
        }

    override val bufferedPositionMs: Long
        get() = exoPlayer?.bufferedPosition?.coerceAtLeast(0L) ?: 0L

    override val currentItemIndex: Int
        get() = exoPlayer?.currentMediaItemIndex?.coerceAtLeast(0) ?: 0

    override val playbackSpeed: Float
        get() = exoPlayer?.playbackParameters?.speed ?: 1.0f

    override fun setSource(clip: AudioClip) {
        setPlaylist(listOf(clip), 0, 0L)
    }

    override fun setPlaylist(clips: List<AudioClip>, startIndex: Int, startPositionMs: Long) {
        val player = exoPlayer ?: return
        currentClips = clips

        val mediaItems = clips.map { it.toMediaItem() }
        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun clearPlaylist() {
        currentClips = emptyList()
        exoPlayer?.clearMediaItems()
    }

    override fun prepare() {
        exoPlayer?.prepare()
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun stop() {
        exoPlayer?.stop()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun seekTo(itemIndex: Int, positionMs: Long) {
        exoPlayer?.seekTo(itemIndex, positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed.coerceAtLeast(0.1f))
    }

    override fun schedulePositionTrigger(
        itemIndex: Int,
        positionMs: Long,
        onTrigger: () -> Unit,
    ): CancellableTrigger? {
        val player = exoPlayer ?: return null
        val message = player.createMessage { _, _ -> onTrigger() }
            .setPosition(itemIndex, positionMs)
            .setLooper(Looper.getMainLooper())
            .setDeleteAfterDelivery(true)
            .send()

        return CancellableTrigger {
            message.cancel()
        }
    }

    override fun addListener(listener: PlayerListener) {
        listeners.addIfAbsent(listener)
    }

    override fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    override fun release() {
        val player = exoPlayer ?: return
        exoPlayer = null
        player.removeListener(playerEventListener)
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        player.release()
    }

    private val playerEventListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val mapped = when (state) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> PlaybackState.ENDED
                else -> PlaybackState.IDLE
            }
            _playbackState.value = mapped
            for (listener in listeners) {
                listener.onPlaybackStateChanged(mapped)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            for (listener in listeners) {
                listener.onIsPlayingChanged(isPlaying)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = currentItemIndex
            val clip = currentClips.getOrNull(index)
            for (listener in listeners) {
                listener.onMediaItemTransition(index, clip)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            for (listener in listeners) {
                listener.onPositionDiscontinuity(oldPosition.positionMs, newPosition.positionMs)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Logger.saveError(error, "AndroidAudioPlayer")
            if (RecitationExoPlayers.isOutOfMemoryError(error)) {
                mainHandler.post {
                    release()
                    initPlayer()
                }
            }
            for (listener in listeners) {
                listener.onError(error)
            }
        }
    }

    private fun AudioClip.toMediaItem(): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri.toUri())

        metadata?.let { meta ->
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(meta.title)
                    .setArtist(meta.artist)
                    .build(),
            )
        }

        if (isClipped) {
            val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
            if (endMs != AudioClip.UNSET_TIME_MS && endMs > startMs) {
                clipBuilder.setEndPositionMs(endMs)
            }
            builder.setClippingConfiguration(clipBuilder.build())
        }

        return builder.build()
    }
}

@OptIn(UnstableApi::class)
class AndroidMediaCacheManager(private val appContext: Context) : MediaCacheManager {
    override val maxSizeBytes: Long
        get() = CACHE_MAX_BYTES

    override fun getCacheSizeBytes(): Long {
        return runCatching { getSimpleCache(appContext).cacheSpace }.getOrDefault(0L)
    }

    override fun clearForReciter(urlTemplate: String) {
        val cache = getSimpleCache(appContext)
        for (chapterNo in QuranMeta.chapterRange) {
            val url = RecitationUtils.prepareAudioUrl(urlTemplate, chapterNo) ?: continue
            runCatching { cache.removeResource(url) }
        }
    }

    override fun clearAll() {
        val cache = getSimpleCache(appContext)
        runCatching {
            for (key in cache.keys) {
                cache.removeResource(key)
            }
        }
    }

    override fun evictResource(uri: String) {
        runCatching { getSimpleCache(appContext).removeResource(uri) }
    }

    companion object {
        private const val CACHE_DIR_NAME = "exo_recitation_cache"
        private const val CACHE_MAX_BYTES = 512L * 1024 * 1024

        @Volatile
        private var simpleCache: SimpleCache? = null

        private val lock = Any()

        fun getSimpleCache(context: Context): SimpleCache {
            synchronized(lock) {
                simpleCache?.let { return it }

                val dir = Platform.files.cacheDirectory / CACHE_DIR_NAME
                if (!AppFiles.exists(dir)) {
                    FileSystem.SYSTEM.createDirectories(dir)
                }

                val evictor = LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES)
                val dbProvider = StandaloneDatabaseProvider(context)
                return SimpleCache(dir.toFile(), evictor, dbProvider).also { simpleCache = it }
            }
        }
    }
}
