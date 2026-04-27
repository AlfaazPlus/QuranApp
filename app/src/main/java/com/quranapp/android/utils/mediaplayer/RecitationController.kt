package com.quranapp.android.utils.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.utils.preferences.RecitationPreferences.RECITATION_MIN_REPEAT_COUNT
import com.quranapp.android.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecitationController private constructor(private val appContext: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<Runnable>()

    /** Only show buffering UI if it lasts this long, to avoid flicker on short stalls. */
    private val bufferingShowRunnable = Runnable { _isBuffering.value = true }
    private val connectionLock = Any()
    private var activeConnectionOwners = 0

    private val _isConnected = MutableStateFlow(false)
    val isConnectedState: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)

    val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBufferingState: StateFlow<Boolean> = _isBuffering.asStateFlow()

    val isPlaying: Boolean get() = _isPlaying.value
    val isLoading: Boolean get() = state.value.resolvingChapterNo != null || _isBuffering.value

    val state: StateFlow<RecitationServiceState> = RecitationService.sharedState

    val currentPositionMs: Long
        get() {
            val c = mediaController ?: return 0L
            val plan = state.value.clipPlan ?: return c.currentPosition
            return plan.virtualPositionAt(c.currentMediaItemIndex, c.currentPosition)
        }

    val durationMs: Long
        get() {
            val c = mediaController ?: return 0L
            val plan = state.value.clipPlan
            return plan?.virtualDurationMs ?: c.duration
        }

    // ==================== Connection ====================

    fun connect() {
        synchronized(connectionLock) {
            activeConnectionOwners += 1

            // Shared controller already active/connecting, only increase owner count.
            if (mediaController != null || controllerFuture != null) return
        }

        establishConnection()
    }

    @OptIn(UnstableApi::class)
    private fun establishConnection() {
        synchronized(connectionLock) {
            if (mediaController != null || controllerFuture != null) return
        }

        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, RecitationService::class.java)
        )

        controllerFuture = MediaController.Builder(appContext, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    mediaController = null
                    _isConnected.value = false
                    controllerFuture = null

                    // If there are still active owners, reconnect automatically.
                    val shouldReconnect = synchronized(connectionLock) {
                        activeConnectionOwners > 0
                    }

                    if (shouldReconnect) {
                        establishConnection()
                    }
                }

                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    checkForNewEvents(RecitationServiceState.fromBundle(extras))
                }
            })
            .buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                controller?.addListener(playerListener)

                _isPlaying.value = controller?.isPlaying ?: false

                applyPlaybackStateForBuffering(controller?.playbackState ?: Player.STATE_IDLE)

                _isConnected.value = true
            } catch (e: Exception) {
                Log.saveError(e, "RecitationController.connect")
                _isConnected.value = false
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        val shouldRelease = synchronized(connectionLock) {
            activeConnectionOwners == (--activeConnectionOwners).coerceAtLeast(0)
        }

        // Another component is still using the shared controller.
        if (!shouldRelease) return

        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
        _isPlaying.value = false
        _isBuffering.value = false
        handler.removeCallbacks(bufferingShowRunnable)

        synchronized(pendingCallbacks) { pendingCallbacks.clear() }
    }

    private fun applyPlaybackStateForBuffering(playbackState: Int) {
        handler.removeCallbacks(bufferingShowRunnable)

        if (playbackState == Player.STATE_BUFFERING) {
            handler.postDelayed(bufferingShowRunnable, 500)
        } else {
            _isBuffering.value = false
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            applyPlaybackStateForBuffering(playbackState)
        }
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
            handler.postDelayed({ establishConnection() }, 100)
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

    private fun checkForNewEvents(newState: RecitationServiceState) {
        handler.post { executePendingCallbacks() }
    }

    // ==================== Playback Commands ====================
    fun playControl(verse: ChapterVersePair) {
        val controller = mediaController

        if (
            controller == null ||
            controller.playbackState == Player.STATE_IDLE ||
            !state.value.currentVerse.doesEqual(verse.chapterNo, verse.verseNo)
        ) {
            start(verse)
            return
        }

        playPause()
    }

    fun start(verse: ChapterVersePair? = null) {
        ensureConnectedAndSend(
            StartCommand(verse)
        )
    }

    /**
     * Play/pause toggle.
     */
    fun playPause() {
        if (mediaController == null || mediaController!!.playbackState == Player.STATE_IDLE) {
            start()
            return
        }

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
        ensureConnectedAndSend(
            SeekToPositionCommand(positionMs = positionMs)
        )
    }

    fun seekLeft() {
        ensureConnectedAndSend(
            SeekToPositionCommand(positionMs = RecitationService.ACTION_SEEK_LEFT)
        )
    }

    fun seekRight() {
        ensureConnectedAndSend(
            SeekToPositionCommand(positionMs = RecitationService.ACTION_SEEK_RIGHT)
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

    // ==================== Settings Commands ====================

    fun setSpeed(speed: Float) {
        ensureConnectedAndSend(
            SetPlaybackSpeedCommand(speed = speed)
        )
    }

    fun setRepeatCount(repeatCount: Int) {
        ensureConnectedAndSend(
            SetRepeatCommand(repeatCount = repeatCount.coerceAtLeast(RECITATION_MIN_REPEAT_COUNT))
        )
    }

    fun setContinue(continuePlay: Boolean) {
        ensureConnectedAndSend(
            SetContinuePlayingCommand(continuePlaying = continuePlay)
        )
    }

    fun setAudioOption(option: AudioOption) {
        ensureConnectedAndSend(
            SetAudioOptionCommand(audioOption = option)
        )
    }

    fun setVerseGroupSize(size: Int) {
        ensureConnectedAndSend(
            SetVerseGroupSizeCommand(verseGroupSize = size.coerceAtLeast(1))
        )
    }

    fun setReciter(id: String, kind: RecitationAudioKind) {
        ensureConnectedAndSend(
            SetReciterCommand(reciter = id, kind = kind)
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


        // composable
        @Composable
        fun remember(): RecitationController? {
            val context = LocalContext.current

            val state = produceState<RecitationController?>(initialValue = null, context) {
                value = getInstance(context)
            }

            return state.value
        }
    }
}