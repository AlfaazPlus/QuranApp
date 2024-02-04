package com.quranapp.android.views.recitation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.databinding.LytRecitationPlayerBinding
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams
import com.quranapp.android.utils.services.RecitationService
import java.util.*
import java.util.concurrent.TimeUnit


@SuppressLint("ViewConstructor")
class RecitationPlayer(
    val activity: ActivityReader,
    var service: RecitationService?
) : FrameLayout(activity) {

    val binding = LytRecitationPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    private val playerMenu = RecitationPlayerMenu(this)

    var readerChanging = false

    init {
        id = R.id.recitationPlayer
        isSaveEnabled = true

        // intercept touch events
        setOnTouchListener { _, _ -> true }

        updateProgressBar(0)
        updateTimelineText(0, 0)

        initControls()
    }

    fun onChapterChanged(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        currentVerse: Int,
        preventStop: Boolean
    ) {
        readerChanging = true
        Log.d(currentVerse)
        service?.onChapterChanged(chapterNo, fromVerse, toVerse, currentVerse)

        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
        binding.verseSync.isEnabled = !activity.mReaderParams.isSingleVerse
    }

    fun onJuzChanged(juzNo: Int, preventStop: Boolean) {
        readerChanging = true

        service?.onJuzChanged(juzNo, activity.mQuranMetaRef.get()!!)

        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = 255
        binding.verseSync.isEnabled = true
    }

    private fun onReaderChanged(preventStop: Boolean) {
        if (!preventStop) {
            service?.stopMedia()

            playerMenu.close()
            service?.cancelLoading()
            updatePlayControlBtn(false)
        }

        updateProgressBar(0)
        updateTimelineText(0, 0)
        reveal()
    }

    private fun initControls() {
        binding.apply {
            progress.isSaveEnabled = false
            progress.isSaveFromParentEnabled = false
            progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                private var previouslyPlaying = false
                private var lastProgress = 0

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        lastProgress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    previouslyPlaying = service?.p?.previouslyPlaying ?: false

                    service?.pauseMedia()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    service?.seek(lastProgress * RecitationService.MILLIS_MULTIPLIER)

                    if (previouslyPlaying) {
                        service?.playMedia()
                    }
                }
            })

            playControl.setOnClickListener {
                if (service == null) {
                    startRecitationService()
                } else if (!service!!.isLoadingInProgress) {
                    service!!.playControl()
                }
            }

            seekLeft.setOnClickListener {
                if (service?.isLoadingInProgress != true) {
                    service?.seek(RecitationService.ACTION_SEEK_LEFT)
                }
            }

            seekRight.setOnClickListener {
                if (service?.isLoadingInProgress != true) {
                    service?.seek(RecitationService.ACTION_SEEK_RIGHT)
                }
            }

            prevVerse.setOnClickListener {
                service?.recitePreviousVerse()
            }
            nextVerse.setOnClickListener {
                service?.reciteNextVerse()
            }

            verseSync.imageAlpha = 0
            verseSync.isEnabled = false
            verseSync.setOnClickListener {
                if (!activity.mReaderParams.isSingleVerse) {
                    service?.updateVerseSync(!(service?.p?.syncWithVerse ?: false), true)
                }
            }

            menu.setOnClickListener {
                playerMenu.open(it)
            }
        }
    }

    private fun startRecitationService() {
        ContextCompat.startForegroundService(activity, Intent(activity, RecitationService::class.java))
        activity.bindPlayerService()
    }

    fun updateVerseSync(params: RecitationPlayerParams, isPlaying: Boolean) {
        binding.verseSync.let {
            it.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
            it.isSelected = params.syncWithVerse
            it.setImageResource(if (params.syncWithVerse) R.drawable.dr_icon_locked else R.drawable.dr_icon_unlocked)
        }

        if (params.syncWithVerse && isPlaying) {
            activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, true)
        }
    }

    fun onPlayMedia(params: RecitationPlayerParams) {
        if (params.syncWithVerse && !activity.mReaderParams.isSingleVerse) {
            reveal()
        }
        activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, true)
        updatePlayControlBtn(true)
    }

    fun onPauseMedia(params: RecitationPlayerParams) {
        activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, false)
        updatePlayControlBtn(false)
    }

    fun onStopMedia(params: RecitationPlayerParams) {
        activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, false)
        updatePlayControlBtn(false)
        updateProgressBar(0)
        updateTimelineText(0, 0)
    }

    fun reciteControl(pair: ChapterVersePair) {
        service?.reciteControl(pair)
    }

    private fun updatePlayControlBtn(playing: Boolean) {
        binding.playControl.setImageResource(if (playing) R.drawable.dr_icon_pause2 else R.drawable.dr_icon_play2)
    }

    fun updateMaxProgress(max: Int) {
        binding.progress.max = max
    }

    fun updateProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.progress.setProgress(progress, true)
        } else {
            binding.progress.progress = progress
        }
    }

    fun updateTimelineText(progress: Long, duration: Long) {
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
                TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(millis)
                )
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

    fun setPlaybackSpeed(speed: Float) {
        service?.updatePlaybackSpeed(speed)
    }

    fun setRepeat(repeat: Boolean) {
        service?.updateRepeatMode(repeat)
    }

    fun setContinueChapter(continueChapter: Boolean) {
        service?.updateContinuePlaying(continueChapter)
    }

    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return service?.isReciting(chapterNo, verseNo) ?: false
    }

    fun setAudioOption(newOption: Int) {
        service?.onAudioOptionChanged(newOption)
    }
}