package com.quranapp.android.utils.mediaplayer

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.player.ChapterTimingMetadata
import com.quranapp.android.api.models.player.ReciterAudioType
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * RecitationService - Media playback service for Quran recitation.
 *
 * This service is completely decoupled from the UI:
 * - Extends MediaSessionService for automatic notification handling
 * - Uses MediaSession for system media controls integration
 * - Communicates state via session extras (controllers observe changes)
 * - UI connects via MediaController and sends commands
 * - Can run independently without any UI binding
 */
@OptIn(UnstableApi::class)
class RecitationService : MediaSessionService() {

    companion object {
        const val MILLIS_MULTIPLIER = 100L
        const val ACTION_SEEK_LEFT = -2L
        const val ACTION_SEEK_RIGHT = -1L

        // Extras keys
        const val EXTRA_CHAPTER = "chapter"
        const val EXTRA_VERSE = "verse"
        const val EXTRA_FROM_VERSE = "from_verse"
        const val EXTRA_TO_VERSE = "to_verse"
        const val EXTRA_JUZ = "juz"
        const val EXTRA_AUDIO_OPTION = "audio_option"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_REPEAT = "repeat"
        const val EXTRA_CONTINUE = "continue"
        const val EXTRA_SYNC = "sync"
        const val EXTRA_SEEK_AMOUNT = "seek_amount"
        const val EXTRA_FROM_USER = "from_user"
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var audioRepository: RecitationAudioRepository
    private lateinit var fileUtils: FileUtils

    private val headsetReceiver = RecitationHeadsetReceiver(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var forceManifestFetch = false
    var forceTranslationManifestFetch = false

    @Volatile
    private var _quranMeta = serviceScope.async() {
        QuranMeta2.prepareInstance(applicationContext)
    }

    // Full chapter playback state
    private var chapterTimingMetadata: ChapterTimingMetadata? = null
    private var verseTrackingJob: Job? = null
    private var progressBroadcastJob: Job? = null

    val _state = MutableStateFlow(
        RecitationServiceState.EMPTY
    )
    val state: StateFlow<RecitationServiceState> = _state.asStateFlow()

    private suspend fun requestQuranMeta(): QuranMeta {
        return _quranMeta.await()
    }

    override fun onCreate() {
        super.onCreate()

        fileUtils = FileUtils.newInstance(this)
        audioRepository = RecitationAudioRepository.getInstance(this)

        initializePlayer()
        initializeMediaSession()
        registerHeadsetReceiver()
        // TODO: load preferences

        scoped {
            state.collectLatest { newState ->
                // TODO
                // update player
                // broadcast state to listeners
                mediaSession?.let { session ->
                    session.setSessionExtras(newState.toBundle())
                }
            }
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true
            )
            addListener(playerListener)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            handleMediaItemTransition(mediaItem)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || reason == Player.DISCONTINUITY_REASON_SEEK) {
                handleMediaItemTransition(newPosition.mediaItem)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    setLoadingState(true)
                }

                Player.STATE_READY -> {
                    setLoadingState(false)
                }

                Player.STATE_ENDED -> {
                    handlePlaybackEnded()
                }

                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = state.value.copy(
                isPlaying = isPlaying,
                pausedByHeadset = false,
            )

            // Start/stop progress broadcasting based on playback state
            if (isPlaying) {
                startProgressTracking()
                startVerseTracking()
            } else {
                stopProgressTracking()
                stopVerseTracking()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.saveError(error, "RecitationService.ExoPlayer")
            error.printStackTrace()
        }
    }

    private fun initializeMediaSession() {
        val intent = Intent(this, ActivityReader::class.java).apply {
            putExtra(Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val sessionActivityIntent = PendingIntent.getActivity(this, 0, intent, flags)

        mediaSession = MediaSession.Builder(this, player).setId("RecitationService")
            .setSessionActivity(sessionActivityIntent).setCallback(mediaSessionCallback).build()
    }


    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession, controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder().apply {
                ALL_PLAYER_ACTIONS.forEach { action ->
                    add(SessionCommand(action, Bundle.EMPTY))
                }
            }.build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands).build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (command.customAction) {
                PlayCommand.ACTION -> {
                    PlayCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SetAudioOptionCommand.ACTION -> {
                    SetAudioOptionCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SetPlaybackSpeedCommand.ACTION -> {
                    SetPlaybackSpeedCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SetRepeatCommand.ACTION -> {
                    SetRepeatCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SetContinuePlayingCommand.ACTION -> {
                    SetContinuePlayingCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SetVerseSyncCommand.ACTION -> {
                    SetVerseSyncCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                SeekToPositionCommand.ACTION -> {
                    SeekToPositionCommand.fromBundle(args)?.let {
                        reduce(it)
                    }
                }

                StopCommand.ACTION -> {
                    reduce(StopCommand)
                }

                CancelLoadingCommand.ACTION -> {
                    reduce(CancelLoadingCommand)
                }

                PreviousVerseCommand.ACTION -> {
                    reduce(PreviousVerseCommand)
                }

                NextVerseCommand.ACTION -> {
                    reduce(NextVerseCommand)
                }
            }

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

    }

    private fun registerHeadsetReceiver() {
        ContextCompat.registerReceiver(
            this@RecitationService,
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG).apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            },
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)
        audioRepository.cancelAll()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setLoadingState(loading: Boolean) {
        _state.value = state.value.copy(isLoading = loading)
    }

    private fun emitEvent(event: PlayerEvent) {
        _state.value = state.value.copy(
            lastEvent = event, lastEventTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== Configuration ====================

    private fun findVerseInPlaylist(chapter: Int, verse: Int): Int {
        for (i in 0 until player.mediaItemCount) {
            val tag = player.getMediaItemAt(i).localConfiguration?.tag as? RecitationMediaItem
            if (tag?.chapterNo == chapter && tag.verseNo == verse) {
                return i
            }
        }
        return -1
    }


    // ==================== Playback Control ====================

    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val tag = mediaItem?.localConfiguration?.tag as? RecitationMediaItem ?: return

        _state.value = state.value.copy(
            currentVerse = CurrentVerse(
                chapterNo = tag.chapterNo, verseNo = tag.verseNo
            ), settings = state.value.settings.copy(
                reciter = tag.reciter, translationReciter = tag.translReciter
            )
        )

        // Update media metadata for notification
        updateMediaMetadata()
    }

    private fun updateMediaMetadata() {
        // TODO: player mode mayve full chapter
        val currentVerse = state.value.currentVerse

        scoped {
            val meta = requestQuranMeta()

            val chapterName = meta.getChapterName(this@RecitationService, currentVerse.chapterNo)
            val title = getString(
                R.string.strLabelVerseSerialWithChapter,
                chapterName,
                state.value.currentVerse.chapterNo,
                currentVerse.verseNo
            )
            val artist = resolveReciterDisplayName()

            val metadata = MediaMetadata.Builder().setTitle(title).setArtist(artist).build()

            val currentItem = player.currentMediaItem
            if (currentItem != null) {
                val updatedItem = currentItem.buildUpon().setMediaMetadata(metadata).build()
                // We can't update the current item's metadata directly,
                // but the session extras will provide the info
            }
        }
    }

    private fun resolveReciterDisplayName(): String {
        val settings = state.value.settings

        return when (settings.audioOption) {
            RecitationUtils.AUDIO_OPTION_BOTH -> {
                val reciterName = RecitationManager.getReciterName(settings.reciter)
                val translReciterName =
                    RecitationManager.getTranslationReciterName(settings.translationReciter)
                "${reciterName ?: ""} & ${translReciterName ?: ""}"
            }

            RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION -> {
                RecitationManager.getTranslationReciterName(settings.translationReciter) ?: ""
            }

            else -> {
                RecitationManager.getReciterName(settings.reciter) ?: ""
            }
        }
    }

    private fun handlePlaybackEnded() {
        if (!state.value.settings.continueRange) {
            return
        }

        scoped {
            val meta = requestQuranMeta()

            // TODO: consider playback mode

            val nextVerse = recParams.getNextVerse(meta)

            if (nextVerse == null) {
                stopMedia()
                return@scoped
            }

            // Check if next verse is already in playlist
            val playlistIndex = findVerseInPlaylist(nextVerse.chapterNo, nextVerse.verseNo)
            if (playlistIndex >= 0) {
                // ExoPlayer will auto-transition, just update state
                return@scoped
            }

            // Next verse not in playlist, need to load it
            // Show buffering while we wait/download
            setLoadingState(true)

            // Wait for verse if it's being downloaded
            if (audioRepository.isPreloading(
                    recParams.currentReciter,
                    recParams.currentTranslationReciter,
                    nextVerse.chapterNo,
                    nextVerse.verseNo
                )
            ) {
                audioRepository.awaitVerseReady(
                    nextVerse.chapterNo, nextVerse.verseNo, 30000
                )
            }

            setLoadingState(false)
            reciteVerse(
                chapterNo = nextVerse.chapterNo,
                verseNo = nextVerse.verseNo
            )
        }
    }

    fun playMedia() {
        player.play()

        _state.value = state.value.copy(
            pausedByHeadset = false
        )
    }

    fun pauseMedia(
        source: PlayerInterationSource = PlayerInterationSource.USER
    ) {
        player.pause()
        _state.value = state.value.copy(
            pausedByHeadset = source == PlayerInterationSource.HEADSET
        )
    }

    fun playControl() {
        if (player.duration > 0 && player.currentPosition < player.duration) {
            if (state.value.isPlaying) pauseMedia()
            else playMedia()
        } else {
            restartVerse()
        }
    }

    fun stopMedia() {
        player.pause()
        player.stop()
        player.clearMediaItems()

        stopVerseTracking()
        chapterTimingMetadata = null
    }

    fun seek(amountOrDirection: Long) {
        if (state.value.isLoading) return

        val seekAmount = when (amountOrDirection) {
            ACTION_SEEK_RIGHT -> 5000L
            ACTION_SEEK_LEFT -> -5000L
            else -> amountOrDirection
        }

        val fromBtnClick =
            amountOrDirection == ACTION_SEEK_LEFT || amountOrDirection == ACTION_SEEK_RIGHT
        var seekFinal: Long = seekAmount

        if (fromBtnClick) {
            seekFinal += player.currentPosition
            seekFinal = seekFinal.coerceAtLeast(0)
            seekFinal = seekFinal.coerceAtMost(player.duration)
        }

        player.seekTo(seekFinal)
    }

    fun restartVerse() {
        val currentVerse = state.value.currentVerse
        reciteVerse(
            chapterNo = currentVerse.chapterNo,
            verseNo = currentVerse.verseNo
        )
    }

    fun reciteVerse(
        chapterNo: Int,
        verseNo: Int
    ) {
        // TODO
    }

    fun recitePreviousVerse() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            scoped {
                val meta = requestQuranMeta()
                val previousVerse = recParams.getPreviousVerse(meta) ?: return@scoped

                reciteVerse(
                    chapterNo = previousVerse.chapterNo,
                    verseNo = previousVerse.verseNo
                )
            }
        }
    }

    fun reciteNextVerse() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            scoped {
                val meta = requestQuranMeta()
                val nextVerse = recParams.getNextVerse(meta) ?: return@scoped

                reciteVerse(
                    chapterNo = nextVerse.chapterNo,
                    verseNo = nextVerse.verseNo
                )
            }
        }
    }


    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return state.value.isCurrentVerse(chapterNo, verseNo) && state.value.isPlaying
    }


    private fun <T : BasePlayerCommand> reduce(cmd: T) = scoped {
        when (cmd) {
            is PlayCommand -> {
                reciteVerse(
                    chapterNo = cmd.chapterNo,
                    verseNo = cmd.verseNo
                )
            }

            is SetAudioOptionCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        audioOption = cmd.audioOption
                    )
                )

                // TODO
            }

            is SetPlaybackSpeedCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        speed = cmd.speed
                    )
                )

                player.setPlaybackSpeed(cmd.speed)
            }

            is SetRepeatCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        repeatVerse = cmd.repeat
                    )
                )

                player.repeatMode = if (cmd.repeat) {
                    Player.REPEAT_MODE_ONE
                } else {
                    Player.REPEAT_MODE_OFF
                }
            }

            is SetContinuePlayingCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        continueRange = cmd.continuePlaying
                    )
                )

                // TODO
            }

            is SetVerseSyncCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        verseSync = cmd.verseSync
                    )
                )

                // TODO
            }

            is SeekToPositionCommand -> {
                seek(cmd.positionMs)
            }

            is StopCommand -> {
                stopMedia()
            }

            is CancelLoadingCommand -> {
                cancelLoading()
            }

            is PreviousVerseCommand -> {
                recitePreviousVerse()
            }

            is NextVerseCommand -> {
                reciteNextVerse()
            }

            is SetRecitorCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        reciter = cmd.reciter
                    )
                )

                // TODO
            }

            is SetTranslationRecitorCommand -> {
                _state.value = state.value.copy(
                    settings = state.value.settings.copy(
                        translationReciter = cmd.translationReciter
                    )
                )

                // TODO
            }
        }
    }

    private fun scoped(action: suspend CoroutineScope.() -> Unit) {
        serviceScope.launch {
            action()
        }
    }


    private fun startProgressTracking() {
        progressBroadcastJob?.cancel()

        progressBroadcastJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                _state.value = state.value.copy(
                    positionMs = player.currentPosition, durationMs = player.duration
                )
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressBroadcastJob?.cancel()
        progressBroadcastJob = null
    }

    private fun startVerseTracking() {
        // Handle verse tracking for full chapter mode
        if (state.value.playbackMode != PlaybackMode.FULL_CHAPTER) {
            return
        }

        verseTrackingJob?.cancel()

        val timing = chapterTimingMetadata
        if (timing == null || !timing.hasVerseTiming) {
            return
        }

        verseTrackingJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                val position = player.currentPosition
                val verseTiming = timing.getVerseAtPosition(position)
                val currentVerse = state.value.currentVerse

                if (verseTiming != null && verseTiming.verseNo != currentVerse.verseNo) {
                    _state.value = state.value.copy(
                        currentVerse = ChapterVersePair(
                            chapterNo = timing.chapterNo,
                            verseNo = verseTiming.verseNo,
                        )
                    )
                }

                delay(200) // Check every 200ms
            }
        }
    }

    private fun stopVerseTracking() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null
    }

    fun seekToVerseInFullChapterMode(verseNo: Int) {
        if (state.value.playbackMode != PlaybackMode.FULL_CHAPTER) return

        val timing = chapterTimingMetadata?.getVerseTiming(verseNo) ?: return

        player.seekTo(timing.startMs)
        _state.value = state.value.copy(
            currentVerse = ChapterVersePair(
                chapterNo = state.value.currentVerse.chapterNo,
                verseNo = timing.verseNo,
            )
        )
    }

    private fun resolveReciterAudioType(reciterSlug: String?): ReciterAudioType? {
        if (reciterSlug == null) return ReciterAudioType.VERSE_BY_VERSE

        val model = RecitationManager.getModel(reciterSlug)
        return model?.audioType
    }


    fun cancelLoading() {
        audioRepository.cancelAll()
        setLoadingState(false)
    }
}