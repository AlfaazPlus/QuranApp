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
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioResult
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

    private var chapterTimingMetadata: ChapterTimingMetadata? = null
    private var verseClipPlan: VerseClipPlan? = null
    private var verseTrackingJob: Job? = null
    private var progressBroadcastJob: Job? = null

    val _state = MutableStateFlow(RecitationServiceState.EMPTY)
    val state: StateFlow<RecitationServiceState> = _state.asStateFlow()

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

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
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

    private fun setLoadingState(loading: Boolean) {
        _state.value = state.value.copy(isLoading = loading)
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
            if (state.value.isPlaying) pauseMedia() else playMedia()
        } else {
            restartVerse()
        }
    }

    fun stopMedia() {
        player.pause()
        player.stop()
        player.clearMediaItems()
        stopVerseTracking()
        stopProgressTracking()
        chapterTimingMetadata = null
        verseClipPlan = null
    }

    fun seek(amountOrDirection: Long) {
        if (state.value.isLoading) return

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
        if (state.value.playbackMode != PlaybackMode.FULL_CHAPTER) return

        val plan = verseClipPlan
        if (plan != null && !plan.isEmpty) {
            player.seekTo(plan.firstIndexForVerse(verseNo), 0L)
        } else {
            val timing = chapterTimingMetadata?.getVerseTiming(verseNo) ?: return
            player.seekTo(timing.startMs)
        }
        updateState {
            copy(currentVerse = ChapterVersePair(currentVerse.chapterNo, verseNo))
        }
    }

    fun restartVerse() {
        val cv = state.value.currentVerse
        reciteVerse(cv.chapterNo, cv.verseNo)
    }

    fun recitePreviousVerse() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            scoped {
                val prev = state.value.getPreviousVerse(requestQuranMeta()) ?: return@scoped
                reciteVerse(prev.chapterNo, prev.verseNo)
            }
        }
    }

    fun reciteNextVerse() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            scoped {
                val next = state.value.getNextVerse(requestQuranMeta()) ?: return@scoped
                reciteVerse(next.chapterNo, next.verseNo)
            }
        }
    }

    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return state.value.isCurrentVerse(chapterNo, verseNo) && state.value.isPlaying
    }

    fun cancelLoading() {
        audioRepository.cancelAll()
        setLoadingState(false)
    }

    // ==================== Chapter playback ====================

    fun reciteVerse(chapterNo: Int, verseNo: Int) {
        scoped {
            setLoadingState(true)
            try {
                audioRepository.resolveAudioUris(chapterNo).collect { result ->
                    when (result) {
                        is RecitationAudioRepository.AudioResult.Downloading -> {
                            setLoadingState(true)
                        }

                        is RecitationAudioRepository.AudioResult.Error -> {
                            setLoadingState(false)
                            emitEvent(PlayerEvent.Error(result.error.message))
                        }

                        is RecitationAudioRepository.AudioResult.Success -> {
                            setLoadingState(false)
                            startChapterPlayback(chapterNo, verseNo, result)
                        }
                    }
                }
            } catch (e: Exception) {
                setLoadingState(false)
                Log.saveError(e, "RecitationService.reciteVerse")
                emitEvent(PlayerEvent.Error(e.message))
            }
        }
    }

    private suspend fun startChapterPlayback(
        chapterNo: Int,
        startVerse: Int,
        result: RecitationAudioRepository.AudioResult.Success,
    ) {
        val meta = requestQuranMeta()
        val settings = state.value.settings

        chapterTimingMetadata = result.quran?.timingMetadata ?: result.translation?.timingMetadata
        val plan = buildVerseClipPlan(meta, chapterNo, result, settings)
        verseClipPlan = plan

        val updatedSettings = settings.copy(
            reciter = result.quran?.reciterId,
            translationReciter = result.translation?.reciterId,
        )

        player.stop()
        player.clearMediaItems()

        if (plan != null) {
            player.setMediaItems(plan.items)
            player.seekTo(plan.firstIndexForVerse(startVerse), 0L)
        } else {
            val primary = resolvePrimaryAudio(result, settings) ?: run {
                emitEvent(PlayerEvent.Error("No audio for this chapter"))
                return
            }

            player.setMediaItem(
                buildFullChapterMediaItem(meta, updatedSettings, chapterNo, startVerse, primary),
            )

            val seekMs = chapterTimingMetadata?.getVerseTiming(startVerse)?.startMs ?: 0L
            player.seekTo(seekMs)
        }

        player.repeatMode =
            if (settings.repeatVerse) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.prepare()
        player.play()

        updateState {
            copy(
                playbackMode = PlaybackMode.FULL_CHAPTER,
                timingMetadata = chapterTimingMetadata,
                settings = updatedSettings,
                currentVerse = ChapterVersePair(chapterNo, startVerse),
                isPlaying = true,
                pausedByHeadset = false,
            )
        }
    }

    private fun resolvePrimaryAudio(
        result: RecitationAudioRepository.AudioResult.Success,
        settings: PlayerSettings,
    ): RecitationAudioResult? {
        val preferred = when (settings.audioOption) {
            RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION -> result.translation
            else -> result.quran
        }
        return preferred ?: result.translation ?: result.quran
    }

    // ==================== Playlist building ====================

    private data class TrackSource(
        val audio: RecitationAudioResult,
        val kind: RecitationClipKind,
    )

    /**
     * Determines which audio sources to include as separate tracks in the clipped playlist.
     * Result already contains resolved URIs and timing metadata based on current settings,
     * so this just maps to internal TrackSource objects.
     */
    private fun resolveTrackSources(
        result: RecitationAudioRepository.AudioResult.Success,
    ): List<TrackSource>? {
        val sources = listOf<TrackSource>()

        if (
            result.quran != null && result.quran.hasVerseTiming
        ) {
            sources.plus(TrackSource(result.quran, RecitationClipKind.QURAN))
        }

        if (
            result.translation != null && result.translation.hasVerseTiming
        ) {
            sources.plus(TrackSource(result.translation, RecitationClipKind.TRANSLATION))
        }

        return if (sources.isEmpty()) null else sources
    }

    /**
     * Builds a clipped playlist for verse-level playback.
     * Returns null when timing is unavailable; caller falls back to single unclipped file.
     */
    private fun buildVerseClipPlan(
        meta: QuranMeta,
        chapterNo: Int,
        result: RecitationAudioRepository.AudioResult.Success,
        settings: PlayerSettings,
    ): VerseClipPlan? {
        val tracks = resolveTrackSources(result) ?: return null

        val items = mutableListOf<MediaItem>()

        for (v in 1..meta.getChapterVerseCount(chapterNo)) {
            for (track in tracks) {
                val vt = track.audio.timingMetadata!!.getVerseTiming(v) ?: return null
                if (vt.durationMs <= 0L) continue

                val verseMetadata = createVerseMetadata(
                    meta,
                    settings,
                    chapterNo,
                    v,
                    track.kind,
                )

                items.add(
                    MediaItem.Builder()
                        .setUri(track.audio.audioUri)
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
        startVerse: Int,
        audio: RecitationAudioResult,
    ): MediaItem {
        val kind = if (settings.audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION) {
            RecitationClipKind.TRANSLATION
        } else {
            RecitationClipKind.QURAN
        }
        return MediaItem.Builder()
            .setUri(audio.audioUri)
            .setMediaMetadata(createVerseMetadata(meta, settings, chapterNo, startVerse, kind))
            .setTag(
                RecitationMediaItem(
                    slug = audio.reciterId,
                    translationSlug = null,
                    chapterNo = chapterNo,
                    verseNo = startVerse,
                    clipKind = kind,
                ),
            )
            .build()
    }

    // ==================== Metadata ====================

    private fun createVerseMetadata(
        meta: QuranMeta,
        settings: PlayerSettings,
        chapterNo: Int,
        verseNo: Int,
        kind: RecitationClipKind = RecitationClipKind.QURAN,
    ): MediaMetadata {
        val chapterName = meta.getChapterName(this, chapterNo) ?: ""
        val baseTitle = getString(
            R.string.strLabelVerseSerialWithChapter,
            chapterName,
            chapterNo,
            verseNo,
        )
        val title = when (kind) {
            RecitationClipKind.TRANSLATION ->
                "$baseTitle · ${getString(R.string.strLabelIncludeTranslation)}"

            RecitationClipKind.QURAN -> baseTitle
        }

        val artist = when (settings.audioOption) {
            RecitationUtils.AUDIO_OPTION_BOTH -> {
                val r = RecitationManager.getReciterName(settings.reciter) ?: ""
                val t =
                    RecitationManager.getTranslationReciterName(settings.translationReciter) ?: ""
                "$r & $t"
            }

            RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION ->
                RecitationManager.getTranslationReciterName(settings.translationReciter)

            else ->
                RecitationManager.getReciterName(settings.reciter)
        }

        return MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
    }

    // ==================== Progress & verse tracking ====================

    private fun startProgressTracking() {
        progressBroadcastJob?.cancel()
        progressBroadcastJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                updateState { copy(positionMs = currentPosition(), durationMs = currentDuration()) }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressBroadcastJob?.cancel()
        progressBroadcastJob = null
    }

    /**
     * Polls player position to update current verse when playing a single unclipped chapter file.
     * Not needed for clipped playlists (verse transitions fire [onMediaItemTransition]).
     */
    private fun startVerseTracking() {
        if (state.value.playbackMode != PlaybackMode.FULL_CHAPTER) return
        if (verseClipPlan != null) return

        verseTrackingJob?.cancel()
        val timing = chapterTimingMetadata
        if (timing == null || !timing.hasVerseTiming) return

        verseTrackingJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                val vt = timing.getVerseAtPosition(player.currentPosition)
                val current = state.value.currentVerse
                if (vt != null && vt.verseNo != current.verseNo) {
                    updateState {
                        copy(currentVerse = ChapterVersePair(timing.chapterNo, vt.verseNo))
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopVerseTracking() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null
    }

    // ==================== Player listener ====================

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            handleMediaItemTransition(mediaItem)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION ||
                reason == Player.DISCONTINUITY_REASON_SEEK
            ) {
                handleMediaItemTransition(newPosition.mediaItem)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> setLoadingState(true)
                Player.STATE_READY -> setLoadingState(false)
                Player.STATE_ENDED -> handlePlaybackEnded()
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { copy(isPlaying = isPlaying, pausedByHeadset = false) }
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

    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val tag = RecitationMediaItem.fromTag(mediaItem?.localConfiguration?.tag) ?: return
        updateState {
            copy(
                currentVerse = ChapterVersePair(tag.chapterNo, tag.verseNo),
                settings = settings.copy(
                    reciter = tag.slug,
                    translationReciter = tag.translationSlug,
                ),
            )
        }
    }

    private fun handlePlaybackEnded() {
        if (!state.value.settings.continueRange) return

        scoped {
            val meta = requestQuranMeta()
            if (state.value.playbackMode == PlaybackMode.FULL_CHAPTER) {
                val next = state.value.getNextChapter(meta)
                if (next != null) reciteVerse(next.chapterNo, next.verseNo)
                else stopMedia()
            } else {
                val next = state.value.getNextVerse(meta) ?: run { stopMedia(); return@scoped }
                val idx = findVerseInPlaylist(next.chapterNo, next.verseNo)
                if (idx < 0) reciteVerse(next.chapterNo, next.verseNo)
            }
        }
    }

    private fun findVerseInPlaylist(chapter: Int, verse: Int): Int {
        for (i in 0 until player.mediaItemCount) {
            val tag = RecitationMediaItem.fromTag(player.getMediaItemAt(i).localConfiguration?.tag)
            if (tag?.chapterNo == chapter && tag.verseNo == verse) return i
        }
        return -1
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

    // ==================== Utility ====================

    private fun scoped(action: suspend CoroutineScope.() -> Unit) {
        serviceScope.launch { action() }
    }
}
