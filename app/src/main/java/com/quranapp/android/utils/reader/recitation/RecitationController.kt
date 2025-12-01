package com.quranapp.android.utils.reader.recitation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.recitation.player.CurrentVerse
import com.quranapp.android.utils.reader.recitation.player.PlaybackCommand
import com.quranapp.android.utils.reader.recitation.player.PlaybackProgress
import com.quranapp.android.utils.reader.recitation.player.PlaybackSettings
import com.quranapp.android.utils.reader.recitation.player.PlayerEvent
import com.quranapp.android.utils.reader.recitation.player.PlayerStatus
import com.quranapp.android.utils.reader.recitation.player.RecitationServiceState
import com.quranapp.android.utils.reader.recitation.player.ReciterInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.quranapp.android.utils.services.RecitationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RecitationController - Bridge between UI and RecitationService.
 * 
 * UI components subscribe to individual state flows:
 * - [currentVerse] - For highlighting current verse
 * - [playerStatus] - For play/pause button, loading indicator
 * - [playbackProgress] - For seekbar
 * - [playbackSettings] - For settings UI
 * 
 * Usage:
 * ```kotlin
 * // Subscribe to current verse (e.g., to highlight it)
 * controller.currentVerse.collect { verse ->
 *     highlightVerse(verse.chapter, verse.verse)
 * }
 * 
 * // Play a verse
 * controller.play(
 *     chapter = 1,
 *     verse = 5,
 *     rangeStart = ChapterVersePair(1, 1),
 *     rangeEnd = ChapterVersePair(1, 7)
 * )
 * ```
 */
class RecitationController private constructor(private val appContext: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<Runnable>()

    // ==================== State Flows ====================
    // UI components subscribe to these individually

    private val _fullState = MutableStateFlow(RecitationServiceState.EMPTY)
    
    /** Current verse being played. Subscribe to highlight verses. */
    private val _currentVerse = MutableStateFlow(CurrentVerse.NONE)
    val currentVerse: StateFlow<CurrentVerse> = _currentVerse.asStateFlow()

    /** Player status (playing, loading, etc). Subscribe for play button state. */
    private val _playerStatus = MutableStateFlow(PlayerStatus())
    val playerStatus: StateFlow<PlayerStatus> = _playerStatus.asStateFlow()

    /** Playback progress. Subscribe for seekbar. */
    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    /** Playback settings. Subscribe for settings UI. */
    private val _playbackSettings = MutableStateFlow(PlaybackSettings())
    val playbackSettings: StateFlow<PlaybackSettings> = _playbackSettings.asStateFlow()

    /** Reciter info. Subscribe to show reciter name. */
    private val _reciterInfo = MutableStateFlow(ReciterInfo())
    val reciterInfo: StateFlow<ReciterInfo> = _reciterInfo.asStateFlow()

    /** Connection status. */
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** Events from service (errors, messages). UI subscribes to show toasts/snackbars. */
    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()
    
    // Track last event timestamp to detect new events
    private var lastSeenEventTimestamp: Long = 0L

    // ==================== Convenience Getters ====================

    val isPlaying: Boolean get() = _playerStatus.value.isPlaying
    val isLoading: Boolean get() = _playerStatus.value.isLoading

    /** Full state for components that need everything. */
    val state: StateFlow<RecitationServiceState> = _fullState.asStateFlow()

    // ==================== Connection ====================

    fun connect() {
        if (mediaController != null || controllerFuture != null) return

        val sessionToken = SessionToken(appContext, ComponentName(appContext, RecitationService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken)
            .setListener(controllerListener)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                onControllerConnected()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationController.connect")
                _isConnected.value = false
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
        synchronized(pendingCallbacks) { pendingCallbacks.clear() }
    }

    private fun ensureServiceStarted(onReady: Runnable? = null) {
        if (mediaController != null && _isConnected.value) {
            onReady?.run()
            return
        }

        if (onReady != null) {
            synchronized(pendingCallbacks) { pendingCallbacks.add(onReady) }
        }

        if (controllerFuture == null) {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, RecitationService::class.java)
            )
            handler.postDelayed({ connect() }, 100)
        }
    }

    private fun onControllerConnected() {
        _isConnected.value = true
        sendCommand(RecitationService.ACTION_GET_STATE, Bundle.EMPTY) { result ->
            updateStateFromExtras(result.extras)
            handler.post { executePendingCallbacks() }
        }
    }

    private fun executePendingCallbacks() {
        val callbacks: List<Runnable>
        synchronized(pendingCallbacks) {
            callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
        }
        callbacks.forEach { it.run() }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            mediaController = null
            _isConnected.value = false
        }

        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            updateStateFromExtras(extras)
        }
    }

    private fun updateStateFromExtras(extras: Bundle) {
        val newState = RecitationServiceState.fromBundle(extras)
        _fullState.value = newState
        
        // Update individual flows
        _currentVerse.value = newState.currentVerse
        _playerStatus.value = newState.status
        _playbackProgress.value = newState.progress
        _playbackSettings.value = newState.settings
        _reciterInfo.value = newState.reciterInfo
        
        // Check for new events
        if (newState.lastEventTimestamp > lastSeenEventTimestamp && newState.lastEvent != null) {
            lastSeenEventTimestamp = newState.lastEventTimestamp
            _events.tryEmit(newState.lastEvent)
        }
    }

    // ==================== Playback Commands ====================

    /**
     * Play a verse with full control over range.
     * Service handles everything: starting, downloading, caching, playback.
     */
    fun play(command: PlaybackCommand) {
        ensureConnectedAndSend(RecitationService.ACTION_PLAY, command.toBundle())
    }

    /**
     * Play a specific verse. Uses current range if already set.
     */
    fun play(chapter: Int, verse: Int) {
        ensureConnectedAndSend(RecitationService.ACTION_RECITE_VERSE, Bundle().apply {
            putInt(RecitationService.EXTRA_CHAPTER, chapter)
            putInt(RecitationService.EXTRA_VERSE, verse)
        })
    }

    /**
     * Play/pause toggle.
     */
    fun playPause() {
        ensureConnectedAndRun {
            val controller = mediaController ?: return@ensureConnectedAndRun
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        ensureConnectedAndRun { mediaController?.play() }
    }

    fun stop() {
        sendCommand(RecitationService.ACTION_STOP, Bundle.EMPTY)
    }

    fun seekTo(positionMs: Long) {
        sendCommand(RecitationService.ACTION_SEEK, Bundle().apply {
            putLong(RecitationService.EXTRA_SEEK_AMOUNT, positionMs)
        })
    }

    fun seekLeft() {
        sendCommand(RecitationService.ACTION_SEEK, Bundle().apply {
            putLong(RecitationService.EXTRA_SEEK_AMOUNT, RecitationService.ACTION_SEEK_LEFT)
        })
    }

    fun seekRight() {
        sendCommand(RecitationService.ACTION_SEEK, Bundle().apply {
            putLong(RecitationService.EXTRA_SEEK_AMOUNT, RecitationService.ACTION_SEEK_RIGHT)
        })
    }

    fun previousVerse() {
        ensureConnectedAndSend(RecitationService.ACTION_PREVIOUS_VERSE, Bundle.EMPTY)
    }

    fun nextVerse() {
        ensureConnectedAndSend(RecitationService.ACTION_NEXT_VERSE, Bundle.EMPTY)
    }

    fun cancelLoading() {
        sendCommand(RecitationService.ACTION_CANCEL_LOADING, Bundle.EMPTY)
    }

    // ==================== Settings Commands ====================

    fun setSpeed(speed: Float) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_PLAYBACK_SPEED, Bundle().apply {
            putFloat(RecitationService.EXTRA_SPEED, speed)
        })
    }

    fun setRepeat(repeat: Boolean) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_REPEAT, Bundle().apply {
            putBoolean(RecitationService.EXTRA_REPEAT, repeat)
        })
    }

    fun setContinue(continuePlay: Boolean) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_CONTINUE_PLAYING, Bundle().apply {
            putBoolean(RecitationService.EXTRA_CONTINUE, continuePlay)
        })
    }

    fun setVerseSync(sync: Boolean, fromUser: Boolean = false) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_VERSE_SYNC, Bundle().apply {
            putBoolean(RecitationService.EXTRA_SYNC, sync)
            putBoolean(RecitationService.EXTRA_FROM_USER, fromUser)
        })
    }

    fun setAudioOption(option: Int) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_AUDIO_OPTION, Bundle().apply {
            putInt(RecitationService.EXTRA_AUDIO_OPTION, option)
        })
    }

    // ==================== Range Setup ====================

    /**
     * Set up playback range for a chapter.
     */
    fun setChapter(chapterNo: Int, fromVerse: Int, toVerse: Int, currentVerse: Int = fromVerse) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_CHAPTER, Bundle().apply {
            putInt(RecitationService.EXTRA_CHAPTER, chapterNo)
            putInt(RecitationService.EXTRA_FROM_VERSE, fromVerse)
            putInt(RecitationService.EXTRA_TO_VERSE, toVerse)
            putInt(RecitationService.EXTRA_VERSE, currentVerse)
        })
    }

    fun setJuz(juzNo: Int) {
        ensureConnectedAndSend(RecitationService.ACTION_SET_JUZ, Bundle().apply {
            putInt(RecitationService.EXTRA_JUZ, juzNo)
        })
    }

    // ==================== Auto-connect Helpers ====================

    /**
     * Ensures service is running and connected, then sends command.
     * UI never needs to worry about service state.
     */
    private fun ensureConnectedAndSend(action: String, args: Bundle) {
        if (mediaController != null) {
            sendCommand(action, args)
        } else {
            ensureServiceStarted { sendCommand(action, args) }
        }
    }

    /**
     * Ensures service is running and connected, then runs action.
     */
    private fun ensureConnectedAndRun(action: () -> Unit) {
        if (mediaController != null) {
            action()
        } else {
            ensureServiceStarted { action() }
        }
    }

    // ==================== Query ====================

    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        val verse = _currentVerse.value
        return verse.chapter == chapterNo && verse.verse == verseNo && isPlaying
    }

    // ==================== Internal ====================

    private fun sendCommand(
        action: String,
        args: Bundle,
        onResult: ((SessionResult) -> Unit)? = null
    ) {
        val controller = mediaController ?: return
        val command = SessionCommand(action, Bundle.EMPTY)
        val future = controller.sendCustomCommand(command, args)

        if (onResult != null) {
            future.addListener({
                try {
                    onResult(future.get())
                } catch (e: Exception) {
                    Log.saveError(e, "RecitationController.sendCommand")
                }
            }, MoreExecutors.directExecutor())
        }
    }

    companion object {
        @Volatile
        private var instance: RecitationController? = null

        @JvmStatic
        fun getInstance(context: Context): RecitationController {
            return instance ?: synchronized(this) {
                instance ?: RecitationController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
