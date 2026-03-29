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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        private const val SEEK_STEP_MS = 5000L

        val sharedState = MutableStateFlow(RecitationServiceState.EMPTY)
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
    private var _quranMeta = serviceScope.async {
        QuranMeta2.prepareInstance(applicationContext)
    }

    private var singleTrackTimingMetadata: ChapterTimingMetadata? = null
    private var verseClipPlan: VerseClipPlan? = null
    private var verseTrackingJob: Job? = null
    private var latestPlaybackRequestId: Long = 0L
    private val chapterResolutionRequests = mutableMapOf<Int, Deferred<ResolvedAudioResult>>()

    val _state: MutableStateFlow<RecitationServiceState> get() = sharedState
    val state: StateFlow<RecitationServiceState> get() = sharedState

    // ==================== Lifecycle ====================

    override fun onCreate() {
        super.onCreate()
        fileUtils = FileUtils.newInstance(this)
        audioRepository = RecitationAudioRepository(this)
        initializePlayer()
        initializeMediaSession()
        registerHeadsetReceiver()

        scoped {
            state.collectLatest { newState ->
                mediaSession?.setSessionExtras(newState.toBundle())
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)
        audioRepository.cancelAll()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        sharedState.value = RecitationServiceState.EMPTY
        super.onDestroy()
    }

    private fun initializePlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 15_000,
                /* maxBufferMs */ 30_000,
                /* bufferForPlaybackMs */ 1_500,
                /* bufferForPlaybackAfterRebufferMs */ 3_000,
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    true,
                )
                addListener(playerListener)
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
        mediaSession = MediaSession.Builder(this, player)
            .setId("RecitationService")
            .setSessionActivity(sessionActivityIntent)
            .setCallback(mediaSessionCallback)
            .build()
    }

    private fun registerHeadsetReceiver() {
        ContextCompat.registerReceiver(
            this,
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG).apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private suspend fun requestQuranMeta(): QuranMeta = _quranMeta.await()

    // ==================== State helpers ====================

    private fun setResolving(resolving: Boolean) {
        _state.value = state.value.copy(isResolving = resolving)
    }

    private fun emitEvent(event: PlayerEvent) {
        _state.value = state.value.copy(
            lastEvent = event,
            lastEventTimestamp = System.currentTimeMillis(),
        )
    }

    private fun updateState(block: RecitationServiceState.() -> RecitationServiceState) {
        _state.value = state.value.block()
    }

    private fun currentPosition(): Long =
        verseClipPlan?.virtualPosition(player) ?: player.currentPosition

    private fun currentDuration(): Long =
        verseClipPlan?.virtualDurationMs ?: player.duration

    // ==================== Playback controls ====================

    fun playMedia() {
        player.play()
        updateState { copy(pausedByHeadset = false) }
    }

    fun pauseMedia(source: PlayerInterationSource = PlayerInterationSource.USER) {
        player.pause()
        updateState { copy(pausedByHeadset = source == PlayerInterationSource.HEADSET) }
    }

    fun playControl() {
        if (currentDuration() > 0 && currentPosition() < currentDuration()) {
            if (player.isPlaying) pauseMedia() else playMedia()
        } else {
            restartVerse()
        }
    }

    fun stopMedia() {
        player.pause()
        player.stop()
        player.clearMediaItems()
        stopVerseTracking()
        singleTrackTimingMetadata = null
        verseClipPlan = null
        chapterResolutionRequests.values.forEach { it.cancel() }
        chapterResolutionRequests.clear()
    }

    fun seek(amountOrDirection: Long) {
        if (state.value.isResolving) return

        val isRelative =
            amountOrDirection == ACTION_SEEK_LEFT || amountOrDirection == ACTION_SEEK_RIGHT

        val plan = verseClipPlan
        if (plan != null && !plan.isEmpty) {
            val target = if (isRelative) {
                val delta =
                    if (amountOrDirection == ACTION_SEEK_RIGHT) SEEK_STEP_MS else -SEEK_STEP_MS
                (plan.virtualPosition(player) + delta).coerceIn(0L, plan.virtualDurationMs)
            } else {
                amountOrDirection.coerceIn(0L, plan.virtualDurationMs)
            }
            plan.seekToVirtualPosition(player, target)
        } else {
            val target = if (isRelative) {
                val delta =
                    if (amountOrDirection == ACTION_SEEK_RIGHT) SEEK_STEP_MS else -SEEK_STEP_MS
                (player.currentPosition + delta).coerceIn(0L, player.duration)
            } else {
                amountOrDirection.coerceIn(0L, player.duration)
            }
            player.seekTo(target)
        }
    }

    fun seekToVerse(verseNo: Int) {
        val plan = verseClipPlan

        if (plan != null && !plan.isEmpty) {
            player.seekTo(plan.firstIndexForVerse(verseNo), 0L)
        } else {
            val timing = singleTrackTimingMetadata?.getVerseTiming(verseNo) ?: return
            player.seekTo(timing.startMs)
        }

        updateState {
            copy(currentVerse = ChapterVersePair(currentVerse.chapterNo, verseNo))
        }
    }

    fun restartVerse() {
        val cv = state.value.currentVerse
        if (!cv.isValid) return
        reciteVerse(cv.chapterNo, cv.verseNo)
    }

    fun recitePreviousVerse() {
        scoped {
            val meta = requestQuranMeta()
            val prev = state.value.getPreviousVerse(meta) ?: return@scoped
            loadChapterVerse(prev.chapterNo, prev.verseNo, meta)
        }
    }

    fun reciteNextVerse() {
        scoped {
            val meta = requestQuranMeta()
            val next = state.value.getNextVerse(meta) ?: return@scoped
            loadChapterVerse(next.chapterNo, next.verseNo, meta)
        }
    }

    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return state.value.isCurrentVerse(chapterNo, verseNo) && player.isPlaying
    }

    fun cancelLoading() {
        audioRepository.cancelAll()
        setResolving(false)
    }

    // ==================== Chapter playback ====================

    fun reciteVerse(chapterNo: Int, verseNo: Int) {
        scoped {
            val meta = requestQuranMeta()
            loadChapterVerse(chapterNo, verseNo, meta)
        }
    }

    /**
     * Loads and plays [verseNo] in [chapterNo], or seeks within the current chapter media
     * when the same chapter is already loaded and verse-level seeking is available.
     */
    private suspend fun loadChapterVerse(chapterNo: Int, verseNo: Int, meta: QuranMeta) {
        if (!meta.isVerseValid4Chapter(chapterNo, verseNo)) {
            return
        }

        val requestId = ++latestPlaybackRequestId

        if (trySeekToVerseInLoadedChapter(chapterNo, verseNo, meta)) {
            if (requestId == latestPlaybackRequestId) {
                setResolving(false)
            }
            return
        }

        setResolving(true)

        try {
            when (val result = awaitChapterResolution(chapterNo)) {
                is ResolvedAudioResult.Downloading -> {
                    // Terminal resolver output is always Error or Resolved.
                }

                is ResolvedAudioResult.Error -> {
                    if (requestId != latestPlaybackRequestId) return
                    setResolving(false)
                    emitEvent(PlayerEvent.Error(result.error.message))
                }

                is ResolvedAudioResult.Resoved -> {
                    if (requestId != latestPlaybackRequestId) return
                    setResolving(false)
                    startChapterPlayback(chapterNo, verseNo, result)
                }
            }
        } catch (e: Exception) {
            if (requestId != latestPlaybackRequestId) return
            setResolving(false)
            Log.saveError(e, "RecitationService.loadChapterVerse")
            emitEvent(PlayerEvent.Error(e.message))
        }
    }

    /**
     * Reuses an in-flight chapter resolution when available; otherwise starts a new one.
     * This keeps same-chapter requests efficient while allowing different chapters to
     * continue downloading in parallel.
     */
    private suspend fun awaitChapterResolution(chapterNo: Int): ResolvedAudioResult {
        val inFlight = chapterResolutionRequests[chapterNo]
        if (inFlight != null && inFlight.isActive) {
            return inFlight.await()
        }

        val deferred = serviceScope.async {
            try {
                var terminal: ResolvedAudioResult? = null
                audioRepository.resolveAudioUris(chapterNo).collect { result ->
                    when (result) {
                        is ResolvedAudioResult.Downloading -> Unit
                        is ResolvedAudioResult.Error -> terminal = result
                        is ResolvedAudioResult.Resoved -> terminal = result
                    }
                }

                terminal ?: ResolvedAudioResult.Error(
                    IllegalStateException("Audio resolution ended without terminal result")
                )
            } catch (e: Exception) {
                ResolvedAudioResult.Error(e)
            }
        }

        chapterResolutionRequests[chapterNo] = deferred

        return try {
            deferred.await()
        } finally {
            if (chapterResolutionRequests[chapterNo] == deferred && deferred.isCompleted) {
                chapterResolutionRequests.remove(chapterNo)
            }
        }
    }

    /**
     * Returns true if playback was adjusted in place (seek + play) without re-resolving audio.
     */
    private fun trySeekToVerseInLoadedChapter(
        chapterNo: Int,
        verseNo: Int,
        meta: QuranMeta
    ): Boolean {
        if (state.value.isResolving) return false
        if (state.value.currentVerse.chapterNo != chapterNo) return false
        if (player.mediaItemCount == 0) return false
        if (player.playbackState == Player.STATE_IDLE) return false

        val timingMeta = singleTrackTimingMetadata ?: return false
        if (timingMeta.chapterNo != chapterNo) return false

        val plan = verseClipPlan
        val canSeekWithPlan = plan != null && !plan.isEmpty
        val canSeekWithTiming = timingMeta.getVerseTiming(verseNo) != null
        if (!canSeekWithPlan && !canSeekWithTiming) return false

        seekToVerse(verseNo)
        playMedia()
        return true
    }

    private suspend fun startChapterPlayback(
        chapterNo: Int,
        startVerse: Int,
        result: ResolvedAudioResult.Resoved,
    ) {
        val meta = requestQuranMeta()
        val settings = state.value.settings

        val plan = buildMultiTrackVerseClipPlan(meta, chapterNo, result, settings)
        verseClipPlan = plan

        val updatedSettings = settings.copy(
            reciter = result.quran?.reciterId,
            translationReciter = result.translation?.reciterId,
        )

        player.stop()
        player.clearMediaItems()

        if (plan != null) {
            // Multi-track clipped playlist: verse boundaries are encoded in the clips themselves,
            // so per-track timing metadata is not needed at the service level.
            singleTrackTimingMetadata = null
            player.setMediaItems(plan.items)
            player.seekTo(plan.firstIndexForVerse(startVerse), 0L)
        } else {
            // Single file fallback — store the timing of the track we actually load,
            // so verse seeking / tracking works against its positions.
            val primary = result.quran ?: result.translation ?: run {
                emitEvent(PlayerEvent.Error("No audio for this chapter"))
                return
            }

            singleTrackTimingMetadata = primary.timingMetadata

            player.setMediaItem(
                buildFullChapterMediaItem(meta, updatedSettings, chapterNo, primary),
            )

            val seekMs = singleTrackTimingMetadata?.getVerseTiming(startVerse)?.startMs ?: 0L
            player.seekTo(seekMs)
        }

        player.repeatMode =
            if (settings.repeatVerse) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.prepare()
        player.play()

        updateState {
            copy(
                settings = updatedSettings,
                currentVerse = ChapterVersePair(chapterNo, startVerse),
                pausedByHeadset = false,
            )
        }
    }

    // ==================== Playlist building ====================
    /**
     * Determines which audio sources to include as separate tracks in the clipped playlist.
     * Result already contains resolved URIs and timing metadata based on current settings,
     * so this just maps to internal TrackSource objects.
     */
    private fun resolveTrackSources(
        result: ResolvedAudioResult.Resoved,
    ): List<RecitationAudioTrack>? {
        val sources = listOf<RecitationAudioTrack>()

        if (
            result.quran != null && result.quran.hasVerseTiming
        ) {
            sources.plus(result.quran)
        }

        if (
            result.translation != null && result.translation.hasVerseTiming
        ) {
            sources.plus(result.translation)
        }

        return if (sources.isEmpty()) null else sources
    }

    /**
     * Builds a clipped playlist for verse-level playback for multiple audio tracks (Quran + translation).
     * Returns null when timing is unavailable; caller falls back to single unclipped file.
     */
    private fun buildMultiTrackVerseClipPlan(
        meta: QuranMeta,
        chapterNo: Int,
        result: ResolvedAudioResult.Resoved,
        settings: PlayerSettings,
    ): VerseClipPlan? {
        val tracks = resolveTrackSources(result) ?: return null

        if (tracks.size < 2) {
            // not worth building a playlist for a single track.
            // Caller can just play the single file and seek to verse timing.
            return null
        }

        val verseCount = meta.getChapterVerseCount(chapterNo)
        val trackCount = tracks.size
        val items = ArrayList<MediaItem>(verseCount * trackCount)
        val artist = buildArtist(settings)

        for (verseNo in 1..verseCount) {
            val mediaId = "$chapterNo:$verseNo"
            val verseMetadata = MediaMetadata.Builder()
                .setTitle(buildTitle(meta, chapterNo, verseNo))
                .setArtist(artist)
                .build()

            for (track in tracks) {
                val vt = track.timingMetadata!!.getVerseTiming(verseNo) ?: return null
                if (vt.durationMs <= 0L) continue

                items.add(
                    MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(track.audioUri)
                        .setMediaMetadata(verseMetadata)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(vt.startMs)
                                .setEndPositionMs(vt.endMs)
                                .build(),
                        )
                        .build(),
                )
            }
        }

        return if (items.isEmpty()) null else VerseClipPlan.from(items)
    }

    /**
     * Single unclipped MediaItem used when no verse timing is available.
     */
    private fun buildFullChapterMediaItem(
        meta: QuranMeta,
        settings: PlayerSettings,
        chapterNo: Int,
        audio: RecitationAudioTrack,
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$chapterNo")
            .setUri(audio.audioUri)
            .setMediaMetadata(buildMediaMetadata(meta, settings, chapterNo, verseNo = null))
            .build()
    }

    // ==================== Metadata ====================

    private fun buildTitle(meta: QuranMeta, chapterNo: Int, verseNo: Int?): String {
        val chapterName = meta.getChapterName(this, chapterNo) ?: "Chapter $chapterNo"
        return if (verseNo != null) "$chapterName ($chapterNo:$verseNo)" else chapterName
    }

    private fun buildArtist(settings: PlayerSettings): String? {
        val quranReciter = settings.reciter?.let { RecitationManager.getReciterName(it) }
        val translationReciter =
            settings.translationReciter?.let { RecitationManager.getTranslationReciterName(it) }

        return when {
            quranReciter != null && translationReciter != null -> "$quranReciter & $translationReciter"
            quranReciter != null -> quranReciter
            translationReciter != null -> translationReciter
            else -> null
        }
    }

    private fun buildMediaMetadata(
        meta: QuranMeta,
        settings: PlayerSettings,
        chapterNo: Int,
        verseNo: Int?
    ): MediaMetadata {
        return MediaMetadata.Builder()
            .setTitle(buildTitle(meta, chapterNo, verseNo))
            .setArtist(buildArtist(settings))
            .build()
    }

    // ==================== Verse tracking ====================

    /**
     * Starts position-based verse tracking for single-track mode.
     * Clip-plan mode does not need polling — verse changes are detected
     * via [onVerseTransition] on media item transitions, and each clip
     * already has per-verse MediaMetadata baked in.
     */
    private fun startVerseTracking() {
        verseTrackingJob?.cancel()

        if (verseClipPlan != null) return

        val timing = singleTrackTimingMetadata
        if (timing == null || !timing.hasVerseTiming) {
            return
        }

        verseTrackingJob = serviceScope.launch {
            val meta = requestQuranMeta()
            while (isActive && player.isPlaying) {
                val vt = timing.getVerseAtPosition(player.currentPosition)
                val current = state.value.currentVerse

                if (vt != null && vt.verseNo != current.verseNo) {
                    val newVerse = ChapterVersePair(timing.chapterNo, vt.verseNo)
                    updateState { copy(currentVerse = newVerse) }
                    updateNotificationMetadata(meta, timing.chapterNo, vt.verseNo)
                }

                delay(200)
            }
        }
    }

    /**
     * Updates the current media item's metadata so the notification reflects the current verse.
     * Only needed for single-track mode where one media item spans the whole chapter.
     */
    private fun updateNotificationMetadata(meta: QuranMeta, chapterNo: Int, verseNo: Int) {
        if (player.mediaItemCount == 0) return

        val settings = state.value.settings

        val updated = player.currentMediaItem?.buildUpon()
            ?.setMediaMetadata(buildMediaMetadata(meta, settings, chapterNo, verseNo))
            ?.build() ?: return

        player.replaceMediaItem(player.currentMediaItemIndex, updated)
    }

    private fun stopVerseTracking() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null
    }

    // ==================== Player listener ====================

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onVerseTransition()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION ||
                reason == Player.DISCONTINUITY_REASON_SEEK
            ) {
                onVerseTransition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) handlePlaybackEnded()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                updateState { copy(pausedByHeadset = false) }
                startVerseTracking()
            } else {
                stopVerseTracking()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.saveError(error, "RecitationService.ExoPlayer")
            error.printStackTrace()
        }
    }

    /**
     * Called on media item transitions and seek discontinuities.
     * Parses the mediaId ("chapterNo:verseNo") to update the current verse.
     * Only meaningful for clip-plan playlists where each item is a verse.
     */
    private fun onVerseTransition() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val parts = mediaId.split(':')
        if (parts.size != 2) return

        val chapter = parts[0].toIntOrNull() ?: return
        val verse = parts[1].toIntOrNull() ?: return

        val resolved = ChapterVersePair(chapter, verse)
        if (resolved != state.value.currentVerse) {
            updateState { copy(currentVerse = resolved) }
        }
    }

    private fun handlePlaybackEnded() {
        /*fixme: temporarily disable
        if (!state.value.settings.continueRange) return

        scoped {
            val meta = requestQuranMeta()
            val next = state.value.getNextChapter(meta)
            if (next != null) reciteVerse(next.chapterNo, next.verseNo)
            else stopMedia()
        }*/
    }

    // ==================== Session callback & command dispatch ====================

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder().apply {
                ALL_PLAYER_ACTIONS.forEach { add(SessionCommand(it, Bundle.EMPTY)) }
            }.build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            dispatchCommand(command.customAction, args)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun dispatchCommand(action: String, args: Bundle) {
        when (action) {
            PlayCommand.ACTION ->
                PlayCommand.fromBundle(args)?.let { reduce(it) }

            SetAudioOptionCommand.ACTION ->
                SetAudioOptionCommand.fromBundle(args)?.let { reduce(it) }

            SetPlaybackSpeedCommand.ACTION ->
                SetPlaybackSpeedCommand.fromBundle(args)?.let { reduce(it) }

            SetRepeatCommand.ACTION ->
                SetRepeatCommand.fromBundle(args)?.let { reduce(it) }

            SetContinuePlayingCommand.ACTION ->
                SetContinuePlayingCommand.fromBundle(args)?.let { reduce(it) }

            SetVerseSyncCommand.ACTION ->
                SetVerseSyncCommand.fromBundle(args)?.let { reduce(it) }

            SeekToPositionCommand.ACTION ->
                SeekToPositionCommand.fromBundle(args)?.let { reduce(it) }

            StopCommand.ACTION -> reduce(StopCommand)
            CancelLoadingCommand.ACTION -> reduce(CancelLoadingCommand)
            PreviousVerseCommand.ACTION -> reduce(PreviousVerseCommand)
            NextVerseCommand.ACTION -> reduce(NextVerseCommand)
        }
    }

    private fun <T : BasePlayerCommand> reduce(cmd: T) = scoped {
        when (cmd) {
            is PlayCommand -> reciteVerse(cmd.chapterNo, cmd.verseNo)

            is SetAudioOptionCommand -> {
                updateState { copy(settings = settings.copy(audioOption = cmd.audioOption)) }
                // TODO: rebuild playlist when audio option changes mid-playback
            }

            is SetPlaybackSpeedCommand -> {
                updateState { copy(settings = settings.copy(speed = cmd.speed)) }
                player.setPlaybackSpeed(cmd.speed)
            }

            is SetRepeatCommand -> {
                updateState { copy(settings = settings.copy(repeatVerse = cmd.repeat)) }
                player.repeatMode =
                    if (cmd.repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }

            is SetContinuePlayingCommand -> {
                updateState { copy(settings = settings.copy(continueRange = cmd.continuePlaying)) }
                // TODO
            }

            is SetVerseSyncCommand -> {
                updateState { copy(settings = settings.copy(verseSync = cmd.verseSync)) }
                // TODO
            }

            is SeekToPositionCommand -> seek(cmd.positionMs)
            is StopCommand -> stopMedia()
            is CancelLoadingCommand -> cancelLoading()
            is PreviousVerseCommand -> recitePreviousVerse()
            is NextVerseCommand -> reciteNextVerse()

            is SetRecitorCommand -> {
                updateState { copy(settings = settings.copy(reciter = cmd.reciter)) }
                // TODO
            }

            is SetTranslationRecitorCommand -> {
                updateState {
                    copy(settings = settings.copy(translationReciter = cmd.translationReciter))
                }
                // TODO
            }
        }
    }

    private fun scoped(action: suspend CoroutineScope.() -> Unit) {
        serviceScope.launch { action() }
    }
}
