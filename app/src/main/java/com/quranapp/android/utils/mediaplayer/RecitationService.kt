package com.quranapp.android.utils.mediaplayer

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.reader.recitation.player.RecitationMediaItem
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams
import com.quranapp.android.utils.receivers.RecitationHeadsetReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
        const val ACTION_SEEK_LEFT = -1L
        const val ACTION_SEEK_RIGHT = 1L

        const val ACTION_PLAY = "play"
        const val ACTION_RECITE_VERSE = "recite_control"
        const val ACTION_SET_VERSE_RANGE = "set_verse_range"
        const val ACTION_SET_AUDIO_OPTION = "set_audio_option"
        const val ACTION_SET_PLAYBACK_SPEED = "set_playback_speed"
        const val ACTION_SET_REPEAT = "set_repeat"
        const val ACTION_SET_CONTINUE_PLAYING = "set_continue_playing"
        const val ACTION_SET_VERSE_SYNC = "set_verse_sync"
        const val ACTION_SEEK = "seek"
        const val ACTION_STOP = "stop"
        const val ACTION_CANCEL_LOADING = "cancel_loading"
        const val ACTION_PREVIOUS_VERSE = "previous_verse"
        const val ACTION_NEXT_VERSE = "next_verse"
        const val ACTION_GET_STATE = "get_state"

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
    private val recParams = RecitationPlayerParams()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var isLoadingInProgress = false
        private set
    var forceManifestFetch = false
    var forceTranslationManifestFetch = false

    // QuranMeta is loaded on-demand when needed
    private var quranMeta: QuranMeta? = null

    // Full chapter playback state
    private var currentPlaybackMode: PlaybackMode = PlaybackMode.VERSE_BY_VERSE
    private var chapterTimingMetadata: ChapterTimingMetadata? = null
    private var verseTrackingJob: Job? = null
    private var progressBroadcastJob: Job? = null

    // Event tracking for state broadcast
    private var lastEvent: PlayerEvent? = null
    private var lastEventTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()

        recParams.init(this)
        fileUtils = FileUtils.newInstance(this)
        audioRepository = RecitationAudioRepository.getInstance(this)

        initializePlayer()
        initializeMediaSession()
        registerHeadsetReceiver()
        syncConfigurations()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            addListener(playerListener)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            handleMediaItemTransition(mediaItem)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION ||
                reason == Player.DISCONTINUITY_REASON_SEEK
            ) {
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
            recParams.previouslyPlaying = isPlaying
            recParams.pausedDueToHeadset = false

            // Start/stop progress broadcasting based on playback state
            if (isPlaying) {
                startProgressBroadcasting()
                // Handle verse tracking for full chapter mode
                if (currentPlaybackMode == PlaybackMode.FULL_CHAPTER) {
                    startVerseTracking()
                }
            } else {
                stopProgressBroadcasting()
                if (currentPlaybackMode == PlaybackMode.FULL_CHAPTER) {
                    stopVerseTracking()
                }
            }

            broadcastState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.saveError(error, "RecitationService.ExoPlayer")
            error.printStackTrace()
        }
    }

    private fun initializeMediaSession() {
        val sessionActivityIntent = createSessionActivityIntent()

        mediaSession = MediaSession.Builder(this, player)
            .setId("RecitationService")
            .setSessionActivity(sessionActivityIntent)
            .setCallback(mediaSessionCallback)
            .build()
    }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, ActivityReader::class.java).apply {
            putExtra(Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Accept all connections and provide custom commands
            val sessionCommands = SessionCommands.Builder()
                .add(SessionCommand(ACTION_PLAY, Bundle.EMPTY))
                .add(SessionCommand(ACTION_RECITE_VERSE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_VERSE_RANGE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_AUDIO_OPTION, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_PLAYBACK_SPEED, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_REPEAT, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_CONTINUE_PLAYING, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_VERSE_SYNC, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SEEK, Bundle.EMPTY))
                .add(SessionCommand(ACTION_STOP, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CANCEL_LOADING, Bundle.EMPTY))
                .add(SessionCommand(ACTION_PREVIOUS_VERSE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_NEXT_VERSE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_GET_STATE, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return handleCustomCommand(customCommand, args)
        }
    }

    private fun handleCustomCommand(
        command: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (command.customAction) {
            ACTION_PLAY -> {
                val playCommand = PlaybackCommand.fromBundle(args)
                if (playCommand != null) {
                    playWithCommand(playCommand)
                }
            }

            ACTION_RECITE_VERSE -> {
                val chapter = args.getInt(EXTRA_CHAPTER, -1)
                val verse = args.getInt(EXTRA_VERSE, -1)
                if (chapter > 0 && verse > 0) {
                    reciteControl(ChapterVersePair(chapter, verse))
                }
            }

            ACTION_SET_AUDIO_OPTION -> {
                val option = args.getInt(EXTRA_AUDIO_OPTION, RecitationUtils.AUDIO_OPTION_DEFAULT)
                onAudioOptionChanged(option)
            }

            ACTION_SET_PLAYBACK_SPEED -> {
                val speed = args.getFloat(EXTRA_SPEED, 1.0f)
                updatePlaybackSpeed(speed)
            }

            ACTION_SET_REPEAT -> {
                val repeat = args.getBoolean(EXTRA_REPEAT, false)
                updateRepeatMode(repeat)
            }

            ACTION_SET_CONTINUE_PLAYING -> {
                val continuePlay = args.getBoolean(EXTRA_CONTINUE, true)
                updateContinuePlaying(continuePlay)
            }

            ACTION_SET_VERSE_SYNC -> {
                val sync = args.getBoolean(EXTRA_SYNC, true)
                val fromUser = args.getBoolean(EXTRA_FROM_USER, false)
                updateVerseSync(sync, fromUser)
            }

            ACTION_SEEK -> {
                val amount = args.getLong(EXTRA_SEEK_AMOUNT, 0L)
                seek(amount)
            }

            ACTION_STOP -> {
                stopMedia()
            }

            ACTION_CANCEL_LOADING -> {
                cancelLoading()
            }

            ACTION_PREVIOUS_VERSE -> {
                recitePreviousVerse()
            }

            ACTION_NEXT_VERSE -> {
                reciteNextVerse()
            }

            ACTION_GET_STATE -> {
                return Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        createState().toBundle()
                    )
                )
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private fun registerHeadsetReceiver() {
        ContextCompat.registerReceiver(
            this,
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

    // ==================== State Management ====================

    private fun setLoadingState(loading: Boolean) {
        isLoadingInProgress = loading
        broadcastState()
    }

    private fun createState(): RecitationServiceState {
        return RecitationServiceState(
            currentVerse = CurrentVerse(
                chapter = recParams.currentChapterNo,
                verse = recParams.currentVerseNo
            ),
            reciterInfo = ReciterInfo(
                reciter = recParams.currentReciter,
                translationReciter = recParams.currentTranslationReciter
            ),
            status = PlayerStatus(
                isPlaying = player.isPlaying,
                isLoading = isLoadingInProgress,
                playbackMode = currentPlaybackMode,
                hasVerseTiming = currentPlaybackMode == PlaybackMode.VERSE_BY_VERSE ||
                        chapterTimingMetadata?.hasVerseTiming == true
            ),
            progress = PlaybackProgress(
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.coerceAtLeast(0)
            ),
            settings = PlaybackSettings(
                speed = recParams.playbackSpeed,
                repeatVerse = recParams.repeatVerse,
                continueRange = recParams.continueRange,
                verseSync = recParams.syncWithVerse,
                audioOption = recParams.currentAudioOption
            ),
            lastEvent = lastEvent,
            lastEventTimestamp = lastEventTimestamp
        )
    }

    private fun emitEvent(event: PlayerEvent) {
        lastEvent = event
        lastEventTimestamp = System.currentTimeMillis()
        broadcastState()
    }

    private fun broadcastState() {
        mediaSession?.let { session ->
            session.setSessionExtras(createState().toBundle())
        }
    }

    // ==================== Configuration ====================

    private fun syncConfigurations() {
        updatePlaybackSpeed(recParams.playbackSpeed)
        updateRepeatMode(recParams.repeatVerse)
        broadcastState()
    }

    private fun findVerseInPlaylist(chapter: Int, verse: Int): Int {
        for (i in 0 until player.mediaItemCount) {
            val tag = player.getMediaItemAt(i).localConfiguration?.tag as? RecitationMediaItem
            if (tag?.chapterNo == chapter && tag.verseNo == verse) {
                return i
            }
        }
        return -1
    }

    fun updatePlaybackSpeed(speed: Float) {
        recParams.playbackSpeed = speed
        player.setPlaybackSpeed(speed)
        SPReader.setRecitationSpeed(this, speed)
        broadcastState()
    }

    fun updateRepeatMode(repeat: Boolean) {
        recParams.repeatVerse = repeat
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        SPReader.setRecitationRepeatVerse(this, repeat)
        broadcastState()
    }

    fun updateContinuePlaying(continuePlaying: Boolean) {
        recParams.continueRange = continuePlaying
        SPReader.setRecitationContinueChapter(this, continuePlaying)
        broadcastState()

        if (!recParams.previouslyPlaying) return

        pauseMedia()
        val currentPos = player.currentPosition
        restartVerse(true)
        player.seekTo(currentPos)
    }

    fun updateVerseSync(sync: Boolean, fromUser: Boolean) {
        recParams.syncWithVerse = sync
        SPReader.setRecitationScrollSync(this, recParams.syncWithVerse)
        broadcastState()

        if (fromUser) {
            val msgResId =
                if (recParams.syncWithVerse) R.string.verseSyncOn else R.string.verseSyncOff
            emitEvent(PlayerEvent.Message(msgResId))
        }
    }

    // ==================== Chapter/Juz Management ====================

    fun onReciterChanged() {
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
        broadcastState()
    }

    fun onTranslationReciterChanged() {
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)
        broadcastState()
    }

    fun onAudioOptionChanged(newOption: Int) {
        recParams.currentAudioOption = newOption
        SPReader.setRecitationAudioOption(this, newOption)
        restartVerseOnConfigChange()
        broadcastState()
    }

    // ==================== Playback Control ====================

    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val tag = mediaItem?.localConfiguration?.tag as? RecitationMediaItem ?: return

        recParams.currentVerse = ChapterVersePair(tag.chapterNo, tag.verseNo)
        recParams.currentReciter = tag.reciter
        recParams.currentTranslationReciter = tag.translReciter

        // Update media metadata for notification
        updateMediaMetadata()
        broadcastState()
    }

    private fun updateMediaMetadata() {
        loadQuranMetaAndRun { meta ->
            val chapterName = meta.getChapterName(this, recParams.currentChapterNo)
            val title = getString(
                R.string.strLabelVerseSerialWithChapter,
                chapterName,
                recParams.currentChapterNo,
                recParams.currentVerseNo
            )
            val artist = getReciterDisplayName()

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()

            val currentItem = player.currentMediaItem
            if (currentItem != null) {
                val updatedItem = currentItem.buildUpon()
                    .setMediaMetadata(metadata)
                    .build()
                // We can't update the current item's metadata directly,
                // but the session extras will provide the info
            }
        }
    }

    private fun getReciterDisplayName(): String {
        return when (recParams.currentAudioOption) {
            RecitationUtils.AUDIO_OPTION_BOTH -> {
                val reciterName = RecitationManager.getReciterName(recParams.currentReciter)
                val translReciterName =
                    RecitationManager.getTranslationReciterName(recParams.currentTranslationReciter)
                "${reciterName ?: ""} & ${translReciterName ?: ""}"
            }

            RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION -> {
                RecitationManager.getTranslationReciterName(recParams.currentTranslationReciter)
                    ?: ""
            }

            else -> {
                RecitationManager.getReciterName(recParams.currentReciter) ?: ""
            }
        }
    }

    private fun handlePlaybackEnded() {
        if (!recParams.continueRange) {
            broadcastState()
            return
        }

        loadQuranMetaAndRun { meta ->
            val nextVerse = recParams.getNextVerse(meta)

            if (nextVerse == null) {
                stopMedia()
                return@loadQuranMetaAndRun
            }

            // Check if next verse is already in playlist
            val playlistIndex = findVerseInPlaylist(nextVerse.chapterNo, nextVerse.verseNo)
            if (playlistIndex >= 0) {
                // ExoPlayer will auto-transition, just update state
                return@loadQuranMetaAndRun
            }

            // Next verse not in playlist, need to load it
            // Show buffering while we wait/download
            setLoadingState(true)

            serviceScope.launch {
                // Wait for verse if it's being downloaded
                if (audioRepository.isPreloading(
                        recParams.currentReciter,
                        recParams.currentTranslationReciter,
                        nextVerse.chapterNo,
                        nextVerse.verseNo
                    )
                ) {
                    audioRepository.awaitVerseReady(nextVerse.chapterNo, nextVerse.verseNo, 30000)
                }

                setLoadingState(false)
                reciteVerse(nextVerse)
            }
        }
    }

    fun playMedia() {
        player.play()
    }

    fun pauseMedia() {
        player.pause()
    }

    fun playControl() {
        loadQuranMetaAndRun { meta ->
            if (player.duration > 0 && player.currentPosition < player.duration) {
                if (isPlaying) pauseMedia()
                else playMedia()
            } else if (!recParams.hasNextVerse(meta)) {
                restartRange()
            } else {
                restartVerse()
            }
        }
    }

    fun stopMedia() {
        player.pause()
        player.stop()
        player.clearMediaItems()

        stopVerseTracking()
        currentPlaybackMode = PlaybackMode.VERSE_BY_VERSE
        chapterTimingMetadata = null

        recParams.currentVerse = recParams.firstVerseOfRange
        broadcastState()
    }

    fun seek(amountOrDirection: Long) {
        if (isLoadingInProgress) return

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

    fun recitePreviousVerse() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            loadQuranMetaAndRun { meta ->
                val previousVerse = recParams.getPreviousVerse(meta) ?: return@loadQuranMetaAndRun
                reciteVerse(previousVerse)
            }
        }
    }

    fun reciteNextVerse() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            loadQuranMetaAndRun { meta ->
                val nextVerse = recParams.getNextVerse(meta) ?: return@loadQuranMetaAndRun
                reciteVerse(nextVerse)
            }
        }
    }

    private fun restartRange() {
        if (isLoadingInProgress) return

        if (player.mediaItemCount == 0) {
            reciteVerse(recParams.firstVerseOfRange)
            return
        }

        player.seekTo(0, 0)
    }

    private fun restartVerse(force: Boolean = false) {
        if (isLoadingInProgress) return

        if (player.mediaItemCount == 0 || force) {
            reciteVerse(recParams.currentVerse)
            return
        }

        player.seekTo(0)
    }

    private fun restartVerseOnConfigChange() {
        val previouslyPlaying = recParams.previouslyPlaying

        player.stop()
        player.clearMediaItems()

        if (previouslyPlaying) {
            restartVerse(true)
        }
    }

    val isPlaying: Boolean get() = player.isPlaying

    fun isReciting(chapterNo: Int, verseNo: Int) =
        recParams.isCurrentVerse(chapterNo, verseNo) && isPlaying

    val p: RecitationPlayerParams get() = recParams

    // ==================== Verse Recitation ====================

    /**
     * Play with a complete command containing verse, range, and options.
     * This is the primary way UI should start playback.
     */
    fun playWithCommand(command: PlaybackCommand) {
        // Set up the range
        recParams.firstVerseOfRange = command.rangeStart
        recParams.lastVerseOfRange = command.rangeEnd
        recParams.currentAudioOption = command.audioOption
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)

        // Play the verse
        reciteVerse(ChapterVersePair(command.chapter, command.verse))
    }

    fun reciteControl(pair: ChapterVersePair) {
        if (isLoadingInProgress) return

        // If it's the current verse and playing, toggle play/pause
        if (recParams.isCurrentVerse(pair.chapterNo, pair.verseNo)
            && player.duration > 0
            && player.currentPosition < player.duration
        ) {
            if (isPlaying) pauseMedia() else playMedia()
            return
        }

        // Check if verse is already in playlist - just seek to it
        val playlistIndex = findVerseInPlaylist(pair.chapterNo, pair.verseNo)
        if (playlistIndex >= 0) {
            player.seekTo(playlistIndex, 0)
            recParams.currentVerse = pair
            if (!isPlaying) playMedia()
            broadcastState()
            return
        }

        // Verse not in playlist, load fresh
        reciteVerse(pair)
    }

    fun reciteVerse(pair: ChapterVersePair) {
        if (isLoadingInProgress || !QuranMeta.isChapterValid(pair.chapterNo)) {
            return
        }

        loadQuranMetaAndRun { meta ->
            if (!meta.isVerseValid4Chapter(pair.chapterNo, pair.verseNo)) {
                return@loadQuranMetaAndRun
            }

            // Check if the current reciter uses chapter-based audio
            val reciterSlug = SPReader.getSavedRecitationSlug(this)
            val reciterModel = reciterSlug?.let { RecitationManager.getModel(it) }

            if (reciterModel?.audioType == ReciterAudioType.FULL_CHAPTER) {
                // Use full chapter playback
                reciteChapter(reciterModel, pair.chapterNo, pair.verseNo)
                return@loadQuranMetaAndRun
            }

            // Use verse-by-verse playback (default)
            currentPlaybackMode = PlaybackMode.VERSE_BY_VERSE
            setLoadingState(true)

            serviceScope.launch {
                audioRepository.resolveAudioUris(
                    chapter = pair.chapterNo,
                    verse = pair.verseNo,
                    forceManifestFetch = forceManifestFetch,
                    forceTranslationManifestFetch = forceTranslationManifestFetch
                ).collectLatest { result ->
                    when (result) {
                        is RecitationAudioRepository.AudioResult.Success -> {
                            forceManifestFetch = false
                            forceTranslationManifestFetch = false

                            prepareMediaPlayer(
                                audioUri = result.audioUri,
                                translAudioUri = result.translationUri,
                                reciter = result.reciter,
                                translReciter = result.translationReciter,
                                chapterNo = result.chapter,
                                verseNo = result.verse
                            )

                            // Preload next verses
                            audioRepository.preloadVerses(
                                chapter = pair.chapterNo,
                                fromVerse = pair.verseNo,
                                toVerse = recParams.lastVerseOfRange.verseNo
                            )
                        }

                        is RecitationAudioRepository.AudioResult.Downloading -> {
                            // Still loading
                        }

                        is RecitationAudioRepository.AudioResult.Error -> {
                            setLoadingState(false)
                            Log.saveError(result.error, "RecitationService.reciteVerse")

                            if (result.error is HttpNotFoundException) {
                                forceManifestFetch = true
                            }

                            emitEvent(PlayerEvent.Error(R.string.strMsgSomethingWrong))
                        }
                    }
                }
            }
        }
    }

    private fun prepareMediaPlayer(
        audioUri: Uri?,
        translAudioUri: Uri?,
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int
    ) {
        stopMedia()

        recParams.lastMediaURI = audioUri
        recParams.lastTranslMediaURI = translAudioUri
        recParams.currentVerse = ChapterVersePair(chapterNo, verseNo)
        recParams.currentReciter = reciter
        recParams.currentTranslationReciter = translReciter

        val mediaItems = mutableListOf<MediaItem>()

        if (audioUri != null) {
            mediaItems.add(createMediaItem(audioUri, reciter, translReciter, chapterNo, verseNo))
        }
        if (translAudioUri != null) {
            mediaItems.add(
                createMediaItem(
                    translAudioUri,
                    reciter,
                    translReciter,
                    chapterNo,
                    verseNo
                )
            )
        }

        if (mediaItems.isEmpty()) {
            setLoadingState(false)
            return
        }

        // Add cached next verses to playlist
        addCachedNextVersesToPlaylist(mediaItems, chapterNo, verseNo)

        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()

        setLoadingState(false)
        broadcastState()
    }

    private fun addCachedNextVersesToPlaylist(
        mediaItems: MutableList<MediaItem>,
        currentChapter: Int,
        currentVerse: Int
    ) {
        if (!recParams.continueRange) return

        val audioOption = recParams.currentAudioOption
        val isBoth = audioOption == RecitationUtils.AUDIO_OPTION_BOTH
        val isOnlyTranslation = audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

        loadQuranMetaAndRun { meta ->
            var verse = ChapterVersePair(currentChapter, currentVerse)
            var count = 10

            while (count > 0) {
                val nextVerse = recParams.getNextVerse(meta, verse) ?: break
                var wasArabicAdded = false

                if (!isOnlyTranslation) {
                    val audioFile = fileUtils.getRecitationAudioFile(
                        recParams.currentReciter,
                        nextVerse.chapterNo,
                        nextVerse.verseNo
                    )
                    if (audioFile.length() > 0) {
                        mediaItems.add(
                            createMediaItem(
                                audioFile.toUri(),
                                recParams.currentReciter,
                                recParams.currentTranslationReciter,
                                nextVerse.chapterNo,
                                nextVerse.verseNo
                            )
                        )
                        wasArabicAdded = true
                    } else {
                        break
                    }
                }

                if (isBoth || isOnlyTranslation) {
                    val translFile = fileUtils.getRecitationAudioFile(
                        recParams.currentTranslationReciter,
                        nextVerse.chapterNo,
                        nextVerse.verseNo
                    )
                    if (translFile.length() > 0) {
                        mediaItems.add(
                            createMediaItem(
                                translFile.toUri(),
                                recParams.currentReciter,
                                recParams.currentTranslationReciter,
                                nextVerse.chapterNo,
                                nextVerse.verseNo
                            )
                        )
                    } else {
                        if (wasArabicAdded) {
                            mediaItems.removeAt(mediaItems.size - 1)
                        }
                        break
                    }
                }

                verse = nextVerse
                count--
            }
        }
    }

    private fun createMediaItem(
        audioUri: Uri,
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle("$chapterNo:$verseNo")
            .setArtist(getReciterDisplayName())
            .build()

        return MediaItem.Builder()
            .setUri(audioUri)
            .setTag(RecitationMediaItem(reciter, translReciter, chapterNo, verseNo))
            .setMediaMetadata(metadata)
            .build()
    }

    // ==================== Full Chapter Playback ====================

    /**
     * Recite a full chapter using chapter-based audio.
     * This is called when the reciter provides FULL_CHAPTER audio type.
     */
    private fun reciteChapter(
        reciterModel: RecitationInfoModel,
        chapterNo: Int,
        startVerse: Int = 1
    ) {
        if (isLoadingInProgress) return

        setLoadingState(true)
        currentPlaybackMode = PlaybackMode.FULL_CHAPTER

        serviceScope.launch {
            audioRepository.resolveChapterAudio(reciterModel, chapterNo).collectLatest { result ->
                when (result) {
                    is RecitationAudioRepository.ChapterAudioDownloadResult.Success -> {
                        chapterTimingMetadata = result.result.timingMetadata

                        prepareChapterMediaPlayer(
                            audioUri = result.result.audioUri,
                            reciter = result.result.reciterSlug,
                            chapterNo = result.result.chapterNo,
                            startVerse = startVerse
                        )

                        // Notify user if no verse sync available
                        if (chapterTimingMetadata?.hasVerseTiming != true) {
                            emitEvent(PlayerEvent.Message(R.string.strMsgNoVerseTiming))
                        }
                    }

                    is RecitationAudioRepository.ChapterAudioDownloadResult.Downloading -> {
                        // Still loading
                    }

                    is RecitationAudioRepository.ChapterAudioDownloadResult.Error -> {
                        setLoadingState(false)
                        Log.saveError(result.error, "RecitationService.reciteChapter")
                        emitEvent(PlayerEvent.Error(R.string.strMsgSomethingWrong))
                    }
                }
            }
        }
    }

    /**
     * Prepares the media player for full chapter audio playback.
     */
    private fun prepareChapterMediaPlayer(
        audioUri: Uri,
        reciter: String,
        chapterNo: Int,
        startVerse: Int
    ) {
        stopMedia()

        recParams.currentVerse = ChapterVersePair(chapterNo, startVerse)
        recParams.currentReciter = reciter
        recParams.lastMediaURI = audioUri

        val metadata = MediaMetadata.Builder()
            .setTitle(chapterNo.toString())
            .setArtist(RecitationManager.getReciterName(reciter) ?: reciter)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(audioUri)
            .setTag(RecitationMediaItem(reciter, null, chapterNo, startVerse))
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()

        // Seek to start verse position if timing available
        chapterTimingMetadata?.getVerseTiming(startVerse)?.let { timing ->
            player.seekTo(timing.startMs)
        }

        player.play()

        // Start verse tracking
        startVerseTracking()

        setLoadingState(false)
        broadcastState()
    }

    /**
     * Starts broadcasting progress updates while playing.
     * Broadcasts state every 100ms for real-time progress updates.
     */
    private fun startProgressBroadcasting() {
        progressBroadcastJob?.cancel()

        progressBroadcastJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                broadcastState()
                delay(100)
            }
        }
    }

    private fun stopProgressBroadcasting() {
        progressBroadcastJob?.cancel()
        progressBroadcastJob = null
    }

    /**
     * Starts tracking current verse based on playback position and timing metadata.
     * Updates recParams.currentVerse as playback progresses.
     */
    private fun startVerseTracking() {
        verseTrackingJob?.cancel()

        val timing = chapterTimingMetadata
        if (timing == null || !timing.hasVerseTiming) {
            return
        }

        verseTrackingJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                val position = player.currentPosition
                val verseTiming = timing.getVerseAtPosition(position)

                if (verseTiming != null && verseTiming.verseNo != recParams.currentVerseNo) {
                    recParams.currentVerse = ChapterVersePair(timing.chapterNo, verseTiming.verseNo)
                    broadcastState()
                }

                delay(200) // Check every 200ms
            }
        }
    }

    /**
     * Stops verse tracking when playback stops or mode changes.
     */
    private fun stopVerseTracking() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null
    }

    /**
     * Seeks to a specific verse in full chapter mode.
     * Only works if timing metadata is available.
     */
    fun seekToVerse(verseNo: Int) {
        if (currentPlaybackMode != PlaybackMode.FULL_CHAPTER) return

        val timing = chapterTimingMetadata?.getVerseTiming(verseNo) ?: return
        player.seekTo(timing.startMs)
        recParams.currentVerse = ChapterVersePair(recParams.currentChapterNo, verseNo)
        broadcastState()
    }

    /**
     * Determines the appropriate playback mode based on reciter configuration.
     */
    private fun getReciterAudioType(reciterSlug: String?): ReciterAudioType {
        if (reciterSlug == null) return ReciterAudioType.VERSE_BY_VERSE

        val model = RecitationManager.getModel(reciterSlug)
        return model?.audioType ?: ReciterAudioType.VERSE_BY_VERSE
    }

    // ==================== Utility ====================

    private fun loadQuranMetaAndRun(action: (QuranMeta) -> Unit) {
        if (quranMeta != null) {
            action(quranMeta!!)
            return
        }

        QuranMeta.prepareInstance(
            this,
            object : OnResultReadyCallback<QuranMeta> {
                override fun onReady(r: QuranMeta) {
                    quranMeta = r
                    action(r)
                }
            })
    }

    fun cancelLoading() {
        audioRepository.cancelAll()
        setLoadingState(false)
    }
}