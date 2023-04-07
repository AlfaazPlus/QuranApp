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
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.databinding.LytRecitationPlayerBinding
import com.quranapp.android.utils.services.RecitationPlayerService
import java.util.*
import java.util.concurrent.TimeUnit


@SuppressLint("ViewConstructor")
class RecitationPlayer(
    internal val activity: ActivityReader,
    var service: RecitationPlayerService?
) : FrameLayout(activity) {

    internal val binding = LytRecitationPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    private val playerMenu = RecitationPlayerMenu(this)


    internal var readerChanging = false

    init {
        id = R.id.recitationPlayer
        isSaveEnabled = true

        // intercept touch events
        setOnTouchListener { _, _ -> true }

        updateProgressBar()
        updateTimelineText()

        initControls()
    }

    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int, preventStop: Boolean) {
        readerChanging = true

        service?.onChapterChanged(chapterNo, fromVerse, toVerse)

        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
        binding.verseSync.isEnabled = !activity.mReaderParams.isSingleVerse
    }

    fun onJuzChanged(juzNo: Int, preventStop: Boolean) {
        readerChanging = true

        service?.onJuzChanged(juzNo, activity.mQuranMetaRef.get())

        onReaderChanged(preventStop)

        binding.verseSync.imageAlpha = 255
        binding.verseSync.isEnabled = true
    }

    private fun onReaderChanged(preventStop: Boolean) {
        if (!preventStop) {
            service?.let {
                it.recParams.currentRangeCompleted = true
                it.recParams.currentVerseCompleted = true
                it.release()
            }

            playerMenu.close()
            service?.cancelLoading()
            updatePlayControlBtn(false)
        }

        updateProgressBar()
        updateTimelineText()
        reveal()
    }

    private fun initControls() {
        binding.apply {
            progress.isSaveEnabled = false
            progress.isSaveFromParentEnabled = false
            progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                private var previouslyPlaying = false
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        service?.seek(progress * RecitationPlayerService.MILLIS_MULTIPLIER)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    previouslyPlaying = service?.recParams?.previouslyPlaying ?: false

                    service?.pauseMedia()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
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
                    service?.seek(RecitationPlayerService.ACTION_SEEK_LEFT)
                }
            }

            seekRight.setOnClickListener {
                if (service?.isLoadingInProgress != true) {
                    service?.seek(RecitationPlayerService.ACTION_SEEK_RIGHT)
                }
            }

            prevVerse.setOnClickListener {
                service?.recitePreviousVerse(activity.mReaderParams.isSingleVerse)
            }
            nextVerse.setOnClickListener {
                service?.reciteNextVerse(activity.mReaderParams.isSingleVerse)
            }

            verseSync.imageAlpha = 0
            verseSync.isEnabled = false
            verseSync.setOnClickListener {
                if (!activity.mReaderParams.isSingleVerse) {
                    service?.updateVerseSync(!(service?.recParams?.syncWithVerse ?: false), true)
                }
            }

            menu.setOnClickListener {
                playerMenu.open(it)
            }
        }
    }

    private fun startRecitationService() {
        activity.startService(Intent(activity, RecitationPlayerService::class.java))
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
        updateProgressBar()
        updateTimelineText()
    }

    fun onVerseReciteOrJump(chapterNo: Int, verseNo: Int) {
        activity.onVerseReciteOrJump(chapterNo, verseNo, true)
    }

    fun reciteControl(chapterNo: Int, verseNo: Int) {
        service?.reciteControl(chapterNo, verseNo)
    }


    private fun updatePlayControlBtn(playing: Boolean) {
        binding.playControl.setImageResource(if (playing) R.drawable.dr_icon_pause2 else R.drawable.dr_icon_play2)
    }

    fun updateProgressBar() {
        val progress = if (service != null) service!!.currentPosition / RecitationPlayerService.MILLIS_MULTIPLIER
        else 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.progress.setProgress(progress, true)
        } else {
            binding.progress.progress = progress
        }
    }

    fun updateTimelineText() {
        binding.progressText.text = String.format(
            Locale.getDefault(),
            "%s / %s",
            formatTime(service?.currentPosition?.toLong() ?: 0),
            formatTime(service?.duration?.toLong() ?: 0)
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

    fun setRepeat(repeat: Boolean) {
        service?.setRepeat(repeat)
    }

    fun setContinueChapter(continueChapter: Boolean) {
        service?.setContinueChapter(continueChapter)
    }

    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return service?.isReciting(chapterNo, verseNo) ?: false
    }

}