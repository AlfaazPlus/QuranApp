package com.quranapp.android.utils.mediaplayer

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.ErrorEvent
import com.quranapp.android.utils.univ.EventBus
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

    private var repeatJob: Job? = null
    private var repeatRemainingPlaysForCurrentItem: Int = 0
    private var repeatScheduleGeneration: Long = 0L

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
            initializeStateFromPreferences()
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
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
            /* minBufferMs */ 15_000,
            /* maxBufferMs */ 30_000,
            /* bufferForPlaybackMs */ 1_500,
            /* bufferForPlaybackAfterRebufferMs */ 3_000,
        ).build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
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

    private suspend fun initializeStateFromPreferences() {
        val initialSettings = PlayerSettings(
            speed = RecitationPreferences.getSpeed(),
            repeatCount = RecitationPreferences.getRepeatCount(),
            continueRange = SPReader.getRecitationContinueChapter(this),
            audioOption = RecitationPreferences.getAudioOption(),
            reciter = RecitationPreferences.getReciterId(),
            translationReciter = RecitationPreferences.getTranslationReciterId(),
        )

        updateState { copy(settings = initialSettings) }
        player.setPlaybackSpeed(initialSettings.speed)
        invalidateRepeatSchedule()
    }

    // ==================== State helpers ====================

    private fun setResolving(resolving: Boolean) {
        _state.value = state.value.copy(isResolving = resolving)
    }

    private fun emitEvent(event: Any) {
        scoped { EventBus.send(event) }
    }

    private fun updateState(block: RecitationServiceState.() -> RecitationServiceState) {
        _state.value = state.value.block()
    }

    private fun currentPosition(): Long =
        verseClipPlan?.virtualPosition(player) ?: player.currentPosition

    private fun currentDuration(): Long = verseClipPlan?.virtualDurationMs ?: player.duration

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

        stopTrackings()

        singleTrackTimingMetadata = null
        verseClipPlan = null
        repeatRemainingPlaysForCurrentItem = 0
        chapterResolutionRequests.values.forEach { it.cancel() }
        chapterResolutionRequests.clear()
    }

    fun seek(amountOrDirection: Long) {
        if (state.value.isResolving) return

        invalidateRepeatSchedule()

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
        invalidateRepeatSchedule()

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

        updateNotificationMetadata(state.value.currentVerse.chapterNo, verseNo)
    }

    fun restartVerse() {
        val cv = state.value.currentVerse
        if (!cv.isValid) return
        playVerse(cv.chapterNo, cv.verseNo)
    }

    fun recitePreviousVerse() {
        scoped {
            val meta = requestQuranMeta()
            val prev = state.value.getPreviousVerse(meta) ?: return@scoped
            playChapter(prev.chapterNo, prev.verseNo)
        }
    }

    fun reciteNextVerse() {
        scoped {
            val meta = requestQuranMeta()
            val next = state.value.getNextVerse(meta) ?: return@scoped
            playChapter(next.chapterNo, next.verseNo)
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

    fun playVerse(chapterNo: Int, verseNo: Int) {
        scoped {
            playChapter(chapterNo, verseNo)
        }
    }

    /**
     * Loads and plays [fromVerse] in [chapterNo], or seeks within the current chapter media
     * when the same chapter is already loaded and verse-level seeking is available.
     */
    private suspend fun playChapter(chapterNo: Int, fromVerse: Int) {
        val meta = requestQuranMeta()

        if (!meta.isVerseValid4Chapter(chapterNo, fromVerse)) {
            return
        }

        val requestId = ++latestPlaybackRequestId

        if (trySeekToVerseInLoadedChapter(chapterNo, fromVerse, meta)) {
            if (requestId == latestPlaybackRequestId) {
                setResolving(false)
            }
            return
        }

        awaitChapterResolution(requestId, chapterNo) {
            startChapterPlayback(it, chapterNo, startVerse = fromVerse)
        }
    }

    private suspend fun awaitChapterResolution(
        requestId: Long, chapterNo: Int, action: suspend (ResolvedAudioResult.Resoved) -> Unit
    ) {
        setResolving(true)

        try {
            when (val result = resolveChapterAudio(chapterNo)) {
                is ResolvedAudioResult.Downloading -> {
                    // Terminal resolver output is always Error or Resolved.
                }

                is ResolvedAudioResult.Error -> {
                    emitEvent(ErrorEvent(result.error.message))

                    if (requestId != latestPlaybackRequestId) return
                    setResolving(false)
                }

                is ResolvedAudioResult.Resoved -> {
                    if (requestId != latestPlaybackRequestId) return
                    setResolving(false)

                    action(result)
                }
            }
        } catch (e: Exception) {
            if (requestId != latestPlaybackRequestId) return
            setResolving(false)
            Log.saveError(e, "RecitationService.loadChapterVerse")
            emitEvent(ErrorEvent(e.message))
        }
    }

    /**
     * Reuses an in-flight chapter resolution when available; otherwise starts a new one.
     * This keeps same-chapter requests efficient while allowing different chapters to
     * continue downloading in parallel.
     */
    private suspend fun resolveChapterAudio(chapterNo: Int): ResolvedAudioResult {
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
        chapterNo: Int, verseNo: Int, meta: QuranMeta
    ): Boolean {
        if (
            state.value.isResolving ||
            state.value.currentVerse.chapterNo != chapterNo ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_IDLE
        ) return false

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
        result: ResolvedAudioResult.Resoved,
        chapterNo: Int,
        startVerse: Int,
    ) {
        val settings = state.value.settings
        val plan = buildMultiTrackVerseClipPlan(
            chapterNo = chapterNo,
            result = result,
            settings = settings,
        )

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
                emitEvent(ErrorEvent("No audio for this chapter"))
                return
            }

            singleTrackTimingMetadata = primary.timingMetadata

            player.setMediaItem(
                buildFullChapterMediaItem(primary, chapterNo, startVerse),
            )

            val seekMs = singleTrackTimingMetadata?.getVerseTiming(startVerse)?.startMs ?: 0L
            player.seekTo(seekMs)
        }

        player.repeatMode = Player.REPEAT_MODE_OFF

        invalidateRepeatSchedule()

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
    private suspend fun resolveTrackSources(
        result: ResolvedAudioResult.Resoved,
    ): List<RecitationAudioTrack>? {
        val sources = mutableListOf<RecitationAudioTrack>()

        if (result.quran != null) {
            if (result.quran.hasVerseTiming) {
                sources.add(result.quran)
            } else {
                emitEvent(
                    ErrorEvent(
                        getString(
                            R.string.missingTimingData, result.quran.getReciterName(
                                RecitationModelManager.get(this)
                            )
                        )
                    )
                )
            }
        }

        if (result.translation != null) {
            if (result.translation.hasVerseTiming) {
                sources.add(result.translation)
            } else {
                emitEvent(
                    ErrorEvent(
                        getString(
                            R.string.missingTimingData, result.translation.getReciterName(
                                RecitationModelManager.get(this)
                            )
                        )
                    )
                )
            }
        }

        return if (sources.isEmpty()) {
            null
        } else {
            sources.sortedBy { track ->
                when (track.kind) {
                    RecitationAudioKind.QURAN -> 0
                    RecitationAudioKind.TRANSLATION -> 1
                }
            }
        }
    }

    /**
     * Builds a clipped playlist for verse-level playback for multiple audio tracks (Quran + translation).
     * Returns null when timing is unavailable; caller falls back to single unclipped file.
     */
    private suspend fun buildMultiTrackVerseClipPlan(
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

        val meta = requestQuranMeta()
        val verseCount = meta.getChapterVerseCount(chapterNo)
        val trackCount = tracks.size
        val items = ArrayList<MediaItem>(verseCount * trackCount)
        val metadataBuilder = buildMediaMetadata(chapterNo, 1)
        val chunkSize = RecitationPreferences.getVerseGroupSize()
        var startVerseNo = 1

        while (startVerseNo <= verseCount) {
            val endVerseNo = minOf(startVerseNo + chunkSize - 1, verseCount)
            for (track in tracks) {
                for (verseNo in startVerseNo..endVerseNo) {
                    val vt = track.timingMetadata!!.getVerseTiming(verseNo) ?: return null
                    if (vt.durationMs <= 0L) continue

                    items.add(
                        MediaItem.Builder()
                            .setMediaId("$chapterNo:$verseNo")
                            .setUri(track.audioUri)
                            .setMediaMetadata(
                                metadataBuilder
                                    .setTitle(buildTitle(chapterNo, verseNo))
                                    .build()
                            )
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(vt.startMs).setEndPositionMs(vt.endMs)
                                    .build(),
                            )
                            .build(),
                    )
                }
            }
            startVerseNo += chunkSize
        }

        return if (items.isEmpty()) null else VerseClipPlan.from(items)
    }

    /**
     * Single unclipped MediaItem used when no verse timing is available.
     */
    private suspend fun buildFullChapterMediaItem(
        audio: RecitationAudioTrack,
        chapterNo: Int,
        startVerse: Int
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$chapterNo")
            .setUri(audio.audioUri)
            .setMediaMetadata(buildMediaMetadata(chapterNo, startVerse).build())
            .build()
    }

    // ==================== Metadata ====================

    private suspend fun buildTitle(chapterNo: Int, verseNo: Int?): String {
        val meta = requestQuranMeta()
        val chapterName = meta.getChapterName(this, chapterNo)
        return if (verseNo != null) getString(
            R.string.strLabelVerseWithChapNameAndNo,
            chapterName,
            chapterNo,
            verseNo
        ) else chapterName
    }

    private suspend fun buildArtist(): String? {
        val manager = RecitationModelManager.get(this)
        return manager.getCurrentReciterNameForAudioOption()
    }

    private suspend fun buildMediaMetadata(
        chapterNo: Int,
        verseNo: Int
    ): MediaMetadata.Builder {
        val resId = R.drawable.dr_quran_wallpaper
        val uri = (
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                        resources.getResourcePackageName(resId) +
                        '/' +
                        resources.getResourceTypeName(resId) +
                        '/' +
                        resources.getResourceEntryName(resId)
                ).toUri();

        return MediaMetadata.Builder()
            .setAlbumTitle(getString(R.string.strTitleHolyQuran))
            .setTitle(buildTitle(chapterNo, verseNo))
            .setArtist(buildArtist())
            .setArtworkUri(uri)
    }

    // ==================== Verse tracking ====================

    /**
     * Starts position-based verse tracking for single-track mode.
     * Clip-plan mode does not need polling — verse changes are detected
     * via [checkVerseChanged] on media item transitions, and each clip
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
            while (isActive && player.isPlaying) {
                val vt = timing.getVerseAtPosition(player.currentPosition)
                val current = state.value.currentVerse

                if (vt != null) {
                    if (vt.verseNo != current.verseNo) {
                        val newVerse = ChapterVersePair(timing.chapterNo, vt.verseNo)
                        updateState { copy(currentVerse = newVerse) }
                        updateNotificationMetadata(timing.chapterNo, vt.verseNo)
                        invalidateRepeatSchedule()
                    }

                    scheduleRepeatForVerse(vt.startMs, vt.endMs)
                }

                delay(200)
            }
        }
    }

    private fun stopTrackings() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null

        // only cancel the job without resetting state of repeat schedule
        repeatJob?.cancel()
        repeatJob = null
    }


    /**
     * Rebuilds the current chapter queue after settings changes that impact resolved sources
     * or clip construction, restoring the user's virtual position afterwards.
     */
    private suspend fun invalidateChapterPlayback() {
        if (state.value.isResolving) return
        if (player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE) return

        val current = state.value.currentVerse

        val chapterNo = current.chapterNo
        val verseNo = current.verseNo

        val shouldResumePlaying = player.isPlaying
        val requestId = ++latestPlaybackRequestId

        awaitChapterResolution(requestId, chapterNo) {
            startChapterPlayback(it, chapterNo, startVerse = verseNo)

            if (!shouldResumePlaying) {
                pauseMedia()
            }
        }
    }

    private fun invalidateRepeatSchedule() {
        repeatScheduleGeneration++
        repeatJob?.cancel()
        repeatJob = null
        repeatRemainingPlaysForCurrentItem = state.value.settings.repeatCount.coerceAtLeast(0)
    }

    private fun scheduleRepeatForVerse(startMs: Long, endMs: Long) {
        repeatJob?.cancel()

        if (!isSingleTrackRepeatEligible()) return

        val myGeneration = repeatScheduleGeneration

        val playbackSpeed = player.playbackParameters.speed.coerceAtLeast(0.1f)
        val remainingMediaMs = endMs - player.currentPosition
        val adjustedDelayMs = (remainingMediaMs / playbackSpeed).toLong()
        val finalDelay = adjustedDelayMs.coerceAtLeast(0L)

        repeatJob = serviceScope.launch {
            delay((finalDelay - 100L).coerceAtLeast(0))

            // fine-grained wait for exact player position
            while (isActive &&
                myGeneration == repeatScheduleGeneration &&
                isSingleTrackRepeatEligible() &&
                player.currentPosition < endMs - 10
            ) {
                delay(2)
            }


            // double-check conditions
            if (!isActive || myGeneration != repeatScheduleGeneration || !isSingleTrackRepeatEligible()) return@launch

            repeatRemainingPlaysForCurrentItem--

            player.seekTo(startMs)

            // reschedule for next repeat
            scheduleRepeatForVerse(startMs, endMs)
        }
    }

    private fun rescheduleRepeatForCurrentPosition() {
        val timing = singleTrackTimingMetadata ?: return
        if (!timing.hasVerseTiming) return

        val vt = timing.getVerseAtPosition(player.currentPosition) ?: return

        scheduleRepeatForVerse(vt.startMs, vt.endMs)
    }

    private fun isSingleTrackRepeatEligible(): Boolean {
        return state.value.settings.audioOption == RecitationUtils.AUDIO_OPTION_ONLY_QURAN &&
                repeatRemainingPlaysForCurrentItem > 0 &&
                singleTrackTimingMetadata != null
    }

    /**
     * Updates the current media item's metadata so the notification reflects the current verse.
     * Only needed for single-track mode where one media item spans the whole chapter.
     */
    private fun updateNotificationMetadata(chapterNo: Int, verseNo: Int) {
        if (player.mediaItemCount == 0) return

        scoped {
            val updated = player.currentMediaItem?.buildUpon()
                ?.setMediaMetadata(buildMediaMetadata(chapterNo, verseNo).build())
                ?.build()
                ?: return@scoped

            player.replaceMediaItem(player.currentMediaItemIndex, updated)
        }
    }

    /**
     * Called on media item transitions and seek discontinuities.
     * Parses the mediaId ("chapterNo:verseNo") to update the current verse.
     * Only meaningful for clip-plan playlists where each item is a verse.
     */
    private fun checkVerseChanged() {
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

    private fun handlePlaybackEnded() {/*fixme: temporarily disable
        if (!state.value.settings.continueRange) return

        scoped {
            val meta = requestQuranMeta()
            val next = state.value.getNextChapter(meta)
            if (next != null) reciteVerse(next.chapterNo, next.verseNo)
            else stopMedia()
        }*/
    }

    // ==================== Session callback & command dispatch ====================
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            checkVerseChanged()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || reason == Player.DISCONTINUITY_REASON_SEEK) {
                checkVerseChanged()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                handlePlaybackEnded()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                updateState { copy(pausedByHeadset = false) }
                startVerseTracking()
            } else {
                stopTrackings()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.saveError(error, "RecitationService.ExoPlayer")
            error.printStackTrace()
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = SessionCommands.Builder()
                .apply {
                    ALL_PLAYER_ACTIONS.forEach { add(SessionCommand(it, Bundle.EMPTY)) }
                }.build()

            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)

            if (session.isMediaNotificationController(controller) ||
                session.isAutomotiveController(controller) ||
                session.isAutoCompanionController(controller)
            ) {
                val previousVerseButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                    .setDisplayName(getString(R.string.strLabelPreviousVerse))
                    .setSessionCommand(
                        SessionCommand(PreviousVerseCommand.ACTION, Bundle.EMPTY),
                    )
                    .build()

                val nextVerseButton = CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setDisplayName(getString(R.string.strLabelNextVerse))
                    .setSessionCommand(
                        SessionCommand(NextVerseCommand.ACTION, Bundle.EMPTY),
                    )
                    .build()

                resultBuilder.setCustomLayout(
                    ImmutableList.of(previousVerseButton, nextVerseButton),
                )
            }

            return resultBuilder.build()
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
            StartCommand.ACTION -> StartCommand.fromBundle(args)?.let { reduce(it) }

            SetAudioOptionCommand.ACTION -> SetAudioOptionCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetVerseGroupSizeCommand.ACTION -> SetVerseGroupSizeCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetPlaybackSpeedCommand.ACTION -> SetPlaybackSpeedCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetRepeatCommand.ACTION -> SetRepeatCommand.fromBundle(args)?.let { reduce(it) }

            SetContinuePlayingCommand.ACTION -> SetContinuePlayingCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetReciterCommand.ACTION -> SetReciterCommand.fromBundle(args)?.let { reduce(it) }

            SeekToPositionCommand.ACTION -> SeekToPositionCommand.fromBundle(args)
                ?.let { reduce(it) }

            StopCommand.ACTION -> reduce(StopCommand)
            PreviousVerseCommand.ACTION -> reduce(PreviousVerseCommand)
            NextVerseCommand.ACTION -> reduce(NextVerseCommand)
        }
    }

    private fun <T : BasePlayerCommand> reduce(cmd: T) = scoped {
        when (cmd) {
            is StartCommand -> {
                // TODO: restore any save state if incoming verse is null, for now using default
                playVerse(cmd.verse?.chapterNo ?: 1, cmd.verse?.verseNo ?: 1)
            }

            is SetAudioOptionCommand -> {
                updateState { copy(settings = settings.copy(audioOption = cmd.audioOption)) }
                invalidateChapterPlayback()
            }

            is SetVerseGroupSizeCommand -> {
                invalidateChapterPlayback()
            }

            is SetPlaybackSpeedCommand -> {
                updateState { copy(settings = settings.copy(speed = cmd.speed)) }
                player.setPlaybackSpeed(cmd.speed)

                invalidateRepeatSchedule()
                rescheduleRepeatForCurrentPosition()
            }

            is SetRepeatCommand -> {
                updateState {
                    copy(settings = settings.copy(repeatCount = cmd.repeatCount))
                }

                player.repeatMode = Player.REPEAT_MODE_OFF
                invalidateRepeatSchedule()
            }

            is SetContinuePlayingCommand -> {
                updateState { copy(settings = settings.copy(continueRange = cmd.continuePlaying)) }
            }

            is SeekToPositionCommand -> seek(cmd.positionMs)
            is StopCommand -> stopMedia()
            is PreviousVerseCommand -> recitePreviousVerse()
            is NextVerseCommand -> reciteNextVerse()

            is SetReciterCommand -> {
                updateState {
                    copy(
                        settings = settings.copy(
                            reciter = if (cmd.kind == RecitationAudioKind.QURAN) cmd.reciter else settings.reciter,
                            translationReciter = if (cmd.kind == RecitationAudioKind.TRANSLATION) cmd.reciter else settings.translationReciter,
                        )
                    )
                }
                invalidateChapterPlayback()
            }
        }
    }

    private fun scoped(action: suspend CoroutineScope.() -> Unit) {
        serviceScope.launch { action() }
    }
}
