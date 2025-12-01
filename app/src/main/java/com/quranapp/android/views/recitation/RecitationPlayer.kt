package com.quranapp.android.views.recitation

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.databinding.LytRecitationPlayerBinding
import com.quranapp.android.utils.Log
import android.widget.Toast
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.PlaybackMode
import com.quranapp.android.utils.mediaplayer.PlayerEvent
import com.quranapp.android.utils.mediaplayer.RecitationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * RecitationPlayer - UI component for controlling Quran recitation playback.
 *
 * Subscribes to individual state flows from RecitationController:
 * - playerStatus for play/pause button and loading
 * - playbackProgress for seekbar
 * - playbackSettings for verse sync
 * - currentVerse for verse highlighting
 */
@SuppressLint("ViewConstructor")
class RecitationPlayer(val activity: ActivityReader) : FrameLayout(activity) {

    val binding = LytRecitationPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    val controller = RecitationController.getInstance(context)
    private val playerMenu = RecitationPlayerMenu(this)

    private var uiScope: CoroutineScope? = null
    var readerChanging = false

    init {
        id = R.id.recitationPlayer
        isSaveEnabled = true

        setOnTouchListener { _, _ -> true }

        updateProgressBar(0)
        updateTimelineText(0, 0)

        initControls()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startObservingState()
        controller.connect()
    }

    override fun onDetachedFromWindow() {
        stopObservingState()
        super.onDetachedFromWindow()
    }

    // ==================== State Observation ====================

    private fun startObservingState() {
        uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Observe player status (play/pause, loading)
        uiScope?.launch {
            controller.playerStatus.collect { status ->
                setupOnLoadingInProgress(status.isLoading)
                updatePlayControlBtn(status.isPlaying)
                updatePlaybackModeUI(status.playbackMode, status.hasVerseTiming)
            }
        }

        // Observe playback progress (seekbar)
        uiScope?.launch {
            controller.playbackProgress.collect { progress ->
                updateProgressUI(progress.positionMs, progress.durationMs)
            }
        }

        // Observe playback settings (verse sync, etc.)
        uiScope?.launch {
            controller.playbackSettings.collect { settings ->
                updateVerseSyncUI(settings.verseSync)
            }
        }

        // Observe current verse (for highlighting and activity notification)
        uiScope?.launch {
            controller.currentVerse.collect { verse ->
                val status = controller.playerStatus.value
                activity.onVerseRecite(verse.chapter, verse.verse, status.isPlaying)
                
                // Auto-reveal when playing starts
                if (status.isPlaying && controller.playbackSettings.value.verseSync 
                    && !activity.mReaderParams.isSingleVerse) {
                    reveal()
                }
            }
        }

        // Observe events (errors, messages) - UI decides how to show them
        uiScope?.launch {
            controller.events.collect { event ->
                when (event) {
                    is PlayerEvent.Error -> {
                        val msg = event.message ?: context.getString(event.messageResId)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    is PlayerEvent.Message -> {
                        val msg = event.message ?: context.getString(event.messageResId)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun stopObservingState() {
        uiScope?.cancel()
        uiScope = null
    }

    // ==================== Controls ====================

    private fun initControls() {
        binding.apply {
            progress.isSaveEnabled = false
            progress.isSaveFromParentEnabled = false
            progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                private var wasPlaying = false
                private var lastProgress = 0

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        lastProgress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    wasPlaying = controller.isPlaying
                    controller.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val seekPosition = lastProgress * RecitationService.MILLIS_MULTIPLIER
                    controller.seekTo(seekPosition)

                    if (wasPlaying) {
                        controller.resume()
                    }
                }
            })

            playControl.setOnClickListener {
                if (!controller.isLoading) {
                    controller.playPause()
                }
            }

            seekLeft.setOnClickListener {
                if (!controller.isLoading) {
                    controller.seekLeft()
                }
            }

            seekRight.setOnClickListener {
                if (!controller.isLoading) {
                    controller.seekRight()
                }
            }

            prevVerse.setOnClickListener {
                controller.previousVerse()
            }

            nextVerse.setOnClickListener {
                controller.nextVerse()
            }

            verseSync.imageAlpha = 0
            verseSync.isEnabled = false
            verseSync.setOnClickListener {
                if (!activity.mReaderParams.isSingleVerse) {
                    val currentSync = controller.playbackSettings.value.verseSync
                    controller.setVerseSync(!currentSync, fromUser = true)
                }
            }

            menu.setOnClickListener {
                playerMenu.open(it)
            }
        }
    }

    // ==================== Chapter/Juz Change ====================

    fun onChapterChanged(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        currentVerse: Int,
        preventStop: Boolean
    ) {
        readerChanging = true
        Log.d(currentVerse)

        controller.setChapter(chapterNo, fromVerse, toVerse, currentVerse)
        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
        binding.verseSync.isEnabled = !activity.mReaderParams.isSingleVerse
    }

    fun onJuzChanged(juzNo: Int, preventStop: Boolean) {
        readerChanging = true

        controller.setJuz(juzNo)
        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = 255
        binding.verseSync.isEnabled = true
    }

    private fun onReaderChanged(preventStop: Boolean) {
        if (!preventStop) {
            controller.stop()
            playerMenu.close()
            controller.cancelLoading()
            updatePlayControlBtn(false)
        }

        updateProgressBar(0)
        updateTimelineText(0, 0)
        reveal()
    }

    // ==================== UI Updates ====================

    private fun updateVerseSyncUI(verseSync: Boolean) {
        binding.verseSync.let {
            it.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
            it.isSelected = verseSync
            it.setImageResource(if (verseSync) R.drawable.dr_icon_locked else R.drawable.dr_icon_unlocked)
        }
    }

    private fun updatePlaybackModeUI(playbackMode: PlaybackMode, hasVerseTiming: Boolean) {
        val isFullChapter = playbackMode == PlaybackMode.FULL_CHAPTER

        val canNavigateVerses = !isFullChapter || hasVerseTiming
        binding.prevVerse.apply {
            isEnabled = canNavigateVerses
            alpha = if (canNavigateVerses) 1f else 0.4f
        }
        binding.nextVerse.apply {
            isEnabled = canNavigateVerses
            alpha = if (canNavigateVerses) 1f else 0.4f
        }

        if (isFullChapter && !hasVerseTiming) {
            binding.verseSync.apply {
                isEnabled = false
                alpha = 0.4f
            }
        } else {
            binding.verseSync.apply {
                isEnabled = !activity.mReaderParams.isSingleVerse
                alpha = 1f
            }
        }
    }

    private fun updatePlayControlBtn(playing: Boolean) {
        binding.playControl.setImageResource(if (playing) R.drawable.dr_icon_pause2 else R.drawable.dr_icon_play2)
    }

    private fun updateProgressUI(positionMs: Long, durationMs: Long) {
        updateMaxProgress((durationMs / RecitationService.MILLIS_MULTIPLIER).coerceAtLeast(0).toInt())
        updateProgressBar((positionMs / RecitationService.MILLIS_MULTIPLIER).toInt())
        updateTimelineText(positionMs, durationMs)
    }

    private fun updateMaxProgress(max: Int) {
        binding.progress.max = max
    }

    private fun updateProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.progress.setProgress(progress, true)
        } else {
            binding.progress.progress = progress
        }
    }

    private fun updateTimelineText(progress: Long, duration: Long) {
        binding.progressText.text = String.format(
            Locale.getDefault(),
            "%s / %s",
            formatTime(progress),
            formatTime(duration)
        )
    }

    private fun formatTime(millis: Long): String {
        val m = TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
        val s = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    fun setupOnLoadingInProgress(inProgress: Boolean) {
        binding.let {
            if (inProgress) {
                it.loader.visibility = VISIBLE
                it.playControl.visibility = GONE
            } else {
                it.loader.visibility = GONE
                it.playControl.visibility = VISIBLE
            }
        }

        disableActions(inProgress)
    }

    private fun disableActions(disable: Boolean) {
        val alpha = if (disable) 0.6f else 1f

        binding.apply {
            for (view in arrayOf(progress, seekLeft, seekRight, prevVerse, nextVerse)) {
                view.alpha = alpha
                view.isEnabled = !disable
            }
        }
    }

    // ==================== Visibility ====================

    fun reveal() {
        if (parent !is View) return

        val parent = parent as View
        val params = parent.layoutParams

        if (params is CoordinatorLayout.LayoutParams) {
            val b = params.behavior
            if (b is HideBottomViewOnScrollBehavior) {
                b.slideUp(parent)
            }
        }

        activity.mBinding.readerHeader.setExpanded(true, true)
    }

    fun conceal() {
        val parent = parent as View
        val params = parent.layoutParams

        if (params is CoordinatorLayout.LayoutParams) {
            val b = params.behavior
            if (b is HideBottomViewOnScrollBehavior) {
                b.slideDown(parent)
            }
        }

        activity.mBinding.readerHeader.setExpanded(false)
    }
}
