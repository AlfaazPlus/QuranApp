package com.quranapp.android.utils.mediaplayer

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
import com.quranapp.android.utils.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class RecitationController private constructor(private val appContext: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<Runnable>()

    private val _fullState = MutableStateFlow(RecitationServiceState.EMPTY)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** Events from service (errors, messages). UI subscribes to show toasts/snackbars. */
    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    // Track last event timestamp to detect new events
    private var lastSeenEventTimestamp: Long = 0L

    // ==================== Convenience Getters ====================

    val isPlaying: Boolean get() = _fullState.value.isPlaying
    val isLoading: Boolean get() = _fullState.value.isLoading

    val state: StateFlow<RecitationServiceState> = _fullState.asStateFlow()

    // ==================== Connection ====================

    fun connect() {
        if (mediaController != null || controllerFuture != null) return

        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, RecitationService::class.java))
        controllerFuture = MediaController.Builder(appContext, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    mediaController = null
                    _isConnected.value = false
                }

                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    updateStateFromExtras(extras)
                }
            })
            .buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                _isConnected.value = true
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

    private fun executePendingCallbacks() {
        val callbacks: List<Runnable>
        synchronized(pendingCallbacks) {
            callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
        }
        callbacks.forEach { it.run() }
    }

    private fun updateStateFromExtras(extras: Bundle) {
        val newState = RecitationServiceState.fromBundle(extras)
        _fullState.value = newState

        // Check for new events
        if (newState.lastEventTimestamp > lastSeenEventTimestamp && newState.lastEvent != null) {
            lastSeenEventTimestamp = newState.lastEventTimestamp
            _events.tryEmit(newState.lastEvent)
        }

        handler.post { executePendingCallbacks() }
    }

    // ==================== Playback Commands ====================
    fun play(chapterNo: Int, verseNo: Int) {
        ensureConnectedAndSend(
            PlayCommand(
                chapterNo = chapterNo,
                verseNo = verseNo
            )
        )
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
        sendCommand(StopCommand)
    }

    fun seekTo(positionMs: Long) {
        sendCommand(
            SeekToPositionCommand(
                positionMs = positionMs
            )
        )
    }

    fun seekLeft() {
        sendCommand(
            SeekToPositionCommand(
                positionMs = RecitationService.ACTION_SEEK_LEFT
            )
        )
    }

    fun seekRight() {
        sendCommand(
            SeekToPositionCommand(
                positionMs = RecitationService.ACTION_SEEK_RIGHT
            )
        )
    }

    fun previousVerse() {
        ensureConnectedAndSend(PreviousVerseCommand)
    }

    fun nextVerse() {
        ensureConnectedAndSend(
            NextVerseCommand
        )
    }

    fun cancelLoading() {
        sendCommand(
            CancelLoadingCommand
        )
    }

    // ==================== Settings Commands ====================

    fun setSpeed(speed: Float) {
        ensureConnectedAndSend(
            SetPlaybackSpeedCommand(speed = speed)
        )
    }

    fun setRepeat(repeat: Boolean) {
        ensureConnectedAndSend(
            SetRepeatCommand(repeat = repeat)
        )
    }

    fun setContinue(continuePlay: Boolean) {
        ensureConnectedAndSend(
            SetContinuePlayingCommand(continuePlaying = continuePlay)
        )
    }

    fun setVerseSync(sync: Boolean, fromUser: Boolean = false) {
        ensureConnectedAndSend(
            SetVerseSyncCommand(verseSync = sync)
        )
    }

    fun setAudioOption(option: Int) {
        ensureConnectedAndSend(
            SetAudioOptionCommand(audioOption = option)
        )
    }

    /**
     * Ensures service is running and connected, then sends command.
     */
    private fun ensureConnectedAndSend(cmd: BasePlayerCommand) {
        if (mediaController != null) {
            sendCommand(cmd)
        } else {
            ensureServiceStarted { sendCommand(cmd) }
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
        return state.value.isCurrentVerse(chapterNo, verseNo) && state.value.isPlaying
    }

    // ==================== Internal ====================

    private fun sendCommand(
        cmd: BasePlayerCommand,
        onResult: ((SessionResult) -> Unit)? = null
    ) {
        val controller = mediaController ?: return
        val command = SessionCommand(cmd.ACTION, Bundle.EMPTY)
        val future = controller.sendCustomCommand(command, cmd.toBundle())

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