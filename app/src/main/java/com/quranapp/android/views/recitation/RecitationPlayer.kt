package com.quranapp.android.views.recitation

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.LytRecitationPlayerBinding
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("ViewConstructor")
class RecitationPlayer(internal val activity: ActivityReader) : FrameLayout(activity) {
    private val MILLIS_MULTIPLIER = 100
    private val SEEK_LEFT = -1
    private val SEEK_RIGHT = 1

    internal val binding = LytRecitationPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    private val menu = RecitationPlayerMenu(this)

    internal var params = RecitationPlayerParams()
    private val verseLoadCallback = RecitationPlayerVerseLoadCallback(this)


    internal val fileUtils = FileUtils.newInstance(context)
    private val progressHandler = Handler(Looper.getMainLooper())
    private var mPlayerProgressRunner: Runnable? = null
    private val verseLoader = RecitationPlayerVerseLoader()

    private val player = ExoPlayer.Builder(context).build().apply {
        addListener(RecitationPlayerEventListener(this@RecitationPlayer, activity))
    }

    private var isLoadingInProgress = false
    internal var forceManifestFetch = false
    internal var readerChanging = false

    init {
        id = R.id.recitationPlayer
        isSaveEnabled = true

        // intercept touch events
        setOnTouchListener { _, _ -> true }

        updateRepeatMode(SPReader.getRecitationRepeatVerse(context))
        setupContinuePlaying(SPReader.getRecitationContinueChapter(context))
        updateVerseSync(SPReader.getRecitationScrollSync(context), false)
        updateProgressBar()
        updateTimelineText()

        initControls()
    }

    override fun onSaveInstanceState(): Parcelable {
        return RecitationPlayerSavedState(super.onSaveInstanceState()).apply {
            this.params = this@RecitationPlayer.params
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is RecitationPlayerSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        params = state.params

        if (params.lastMediaURI != null && params.currentReciter != null) {
            prepareMediaPlayer(
                params.lastMediaURI!!,
                params.currentReciter!!,
                params.currentChapterNo,
                params.currentVerseNo,
                params.previouslyPlaying
            )
        }
    }

    fun destroy() {
        player.release()
        menu.close()
        verseLoader.cancelAll()
        // showNotification(RecitationPlayerReceiver.ACTION_STOP)
    }


    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        readerChanging = true
        params.currentVerse = Pair(chapterNo, fromVerse)
        params.firstVerse = Pair(chapterNo, fromVerse)
        params.lastVerse = Pair(chapterNo, toVerse)

        onReaderChanged()

        binding.verseSync.imageAlpha = if (activity.mReaderParams.isSingleVerse) 0 else 255
        binding.verseSync.isEnabled = !activity.mReaderParams.isSingleVerse
    }

    fun onJuzChanged(juzNo: Int) {
        readerChanging = true

        val quranMeta = activity.mQuranMetaRef.get()
        val (firstChapter, lastChapter) = quranMeta.getChaptersInJuz(juzNo)
        val firstVerse = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter).first
        val (_, lastVerse) = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter)

        params.firstVerse = Pair(firstChapter, firstVerse)
        params.lastVerse = Pair(lastChapter, lastVerse)
        params.currentVerse = params.firstVerse

        onReaderChanged()

        binding.verseSync.imageAlpha = 255
        binding.verseSync.isEnabled = true
    }

    private fun onReaderChanged() {
        params.currentRangeCompleted = true
        params.currentVerseCompleted = true

        // release()

        menu.close()
        verseLoader.cancelAll()
        updatePlayControlBtn(false)

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
                    if (fromUser && player.playbackState != ExoPlayer.STATE_ENDED) {
                        seek(progress * MILLIS_MULTIPLIER)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    previouslyPlaying = params.previouslyPlaying
                    pauseMedia()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (previouslyPlaying) {
                        playMedia()
                    }
                }
            })

            playControl.setOnClickListener {
                if (!isLoadingInProgress) {
                    playControl()
                }
            }

            seekLeft.setOnClickListener {
                if (!isLoadingInProgress) {
                    seek(SEEK_LEFT)
                }
            }

            seekRight.setOnClickListener {
                if (!isLoadingInProgress) {
                    seek(SEEK_RIGHT)
                }
            }

            prevVerse.setOnClickListener { recitePreviousVerse() }
            nextVerse.setOnClickListener { reciteNextVerse() }

            verseSync.imageAlpha = 0
            verseSync.isEnabled = false
            verseSync.setOnClickListener {
                if (!activity.mReaderParams.isSingleVerse) {
                    updateVerseSync(!params.syncWithVerse, true)
                }
            }

            menu.setOnClickListener(::openPlayerMenu)
        }
    }

    private fun updateVerseSync(sync: Boolean, fromUser: Boolean) {
        params.syncWithVerse = sync

        binding.verseSync.isSelected = params.syncWithVerse
        binding.verseSync.setImageResource(if (params.syncWithVerse) R.drawable.dr_icon_locked else R.drawable.dr_icon_unlocked)

        if (params.syncWithVerse && isPlaying()) {
            activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, true)
        }

        SPReader.setRecitationVerseSync(context, params.syncWithVerse)

        if (fromUser) {
            popMiniMsg(
                context.getString(if (params.syncWithVerse) R.string.verseSyncOn else R.string.verseSyncOff),
                Toast.LENGTH_SHORT
            )
        }
    }

    private fun updateRepeatMode(repeat: Boolean) {
        params.repeatVerse = repeat
        player.repeatMode = if (repeat) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
        SPReader.setRecitationRepeatVerse(context, repeat)
    }

    private fun setupContinuePlaying(continuePlaying: Boolean) {
        params.continueRange = continuePlaying
        SPReader.setRecitationContinueChapter(context, continuePlaying)
    }

    private fun openPlayerMenu(anchorView: View) {
        menu.open(anchorView)
    }


    fun prepareMediaPlayer(
        audioURI: Uri,
        reciter: String,
        chapterNo: Int,
        verseNo: Int,
        play: Boolean
    ) {
        if (!activity.mReaderParams.isVerseInValidRange(chapterNo, verseNo)) {
            return
        }

        verseLoadCallback.postLoad()

        params.lastMediaURI = audioURI
        params.currentReciter = reciter
        params.currentVerse = Pair(chapterNo, verseNo)

        // release()

        player.setMediaItem(MediaItem.Builder().setMediaMetadata(prepareMetadata()).setUri(audioURI).build())
        player.prepare()

        updateRepeatMode(params.repeatVerse)

        binding.progress.max = (player.duration / MILLIS_MULTIPLIER).toInt()
        updateProgressBar()
        updateTimelineText()

        if (play) {
            playMedia()
        }
    }

    private fun prepareMetadata(): MediaMetadata {
        val quranMeta = activity.mQuranMetaRef.get()

        val chapterName = quranMeta.getChapterName(context, params.currentChapterNo)
        val title = context.getString(
            R.string.strLabelVerseWithChapNameAndNo,
            chapterName,
            params.currentChapterNo,
            params.currentVerseNo
        )

        val reciter = RecitationUtils.getReciterName(params.currentReciter)

        return MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(reciter)
            .build()
    }


    fun playControl() {
        if (params.currentRangeCompleted && params.currentVerseCompleted) {
            restartRange()
        } else if (player.playbackState != ExoPlayer.STATE_ENDED) {
            if (isPlaying()) {
                pauseMedia()
            } else {
                playMedia()
            }
        } else {
            restartVerse()
        }
    }

    fun playMedia() {
        if (params.syncWithVerse && !activity.mReaderParams.isSingleVerse) {
            reveal()
        }

        player.play()
        runAudioProgress()

        params.currentRangeCompleted = false
        params.currentVerseCompleted = false

        activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, true)

        // showNotification(RecitationPlayerReceiver.ACTION_PLAY)
        Log.d(
            "Curr - " + params.currentVerse.first + ":" + params.currentVerse.second,
            params.firstVerse.first
                .toString() + ":" + params.firstVerse.second + " - " + params.lastVerse.first + ":" + params.lastVerse.second
        )

        updatePlayControlBtn(true)

        params.previouslyPlaying = true
    }

    fun pauseMedia() {
        if (isPlaying()) {
            player.pause()
            progressHandler.removeCallbacksAndMessages(null)
        }

        activity.onVerseRecite(params.currentChapterNo, params.currentVerseNo, false)

        // showNotification(RecitationPlayerReceiver.ACTION_PAUSE)
        updatePlayControlBtn(false)

        params.previouslyPlaying = false
    }

    fun seek(amountOrDirection: Int) {
        if (player.playbackState == ExoPlayer.STATE_ENDED || isLoadingInProgress) {
            return
        }

        params.currentVerseCompleted = false

        val seekAmount = when (amountOrDirection) {
            SEEK_RIGHT -> 5000
            SEEK_LEFT -> -5000
            else -> amountOrDirection
        }

        val fromBtnClick = amountOrDirection == SEEK_LEFT || amountOrDirection == SEEK_RIGHT
        var seekFinal = seekAmount.toLong()

        if (fromBtnClick) {
            seekFinal += player.currentPosition
            seekFinal = seekFinal.coerceAtLeast(0)
            seekFinal = seekFinal.coerceAtMost(player.duration)
        }

        player.seekTo(seekFinal)

        updateTimelineText()

        if (fromBtnClick) {
            updateProgressBar()
        }
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun recitePreviousVerse() {
        if (isLoadingInProgress) return

        val chapterNo: Int
        val verseNo: Int

        if (activity.mReaderParams.isSingleVerse) {
            chapterNo = params.currentChapterNo
            verseNo = params.currentVerseNo - 1
            activity.onVerseReciteOrJump(chapterNo, verseNo, true)
        } else {
            val previousVerse = params.getPreviousVerse(activity.mQuranMetaRef.get())
            chapterNo = previousVerse.first
            verseNo = previousVerse.second
        }

        Log.d("PREV VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    fun reciteNextVerse() {
        if (isLoadingInProgress) return

        val chapterNo: Int
        val verseNo: Int

        if (activity.mReaderParams.isSingleVerse) {
            chapterNo = params.currentChapterNo
            verseNo = params.currentVerseNo + 1
            activity.onVerseReciteOrJump(chapterNo, verseNo, true)
        } else {
            val nextVerse = params.getNextVerse(activity.mQuranMetaRef.get())
            chapterNo = nextVerse.first
            verseNo = nextVerse.second
        }

        Log.d("NEXT VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    fun restartRange() {
        if (isLoadingInProgress) return

        val firstVerse = params.firstVerse
        reciteVerse(firstVerse.first, firstVerse.second)
    }

    fun restartVerse() {
        if (isLoadingInProgress) {
            return
        }

        reciteVerse(params.currentChapterNo, params.currentVerseNo)
    }

    fun reciteControl(chapterNo: Int, verseNo: Int) {
        if (isLoadingInProgress) return

        if (params.isCurrentVerse(chapterNo, verseNo) && player.playbackState != ExoPlayer.STATE_ENDED) {
            playControl()
        } else {
            reciteVerse(chapterNo, verseNo)
        }
    }


    private fun updatePlayControlBtn(playing: Boolean) {
        binding.playControl.setImageResource(if (playing) R.drawable.dr_icon_pause2 else R.drawable.dr_icon_play2)
    }

    fun updateProgressBar() {
        val progress = (player.currentPosition / MILLIS_MULTIPLIER).toInt()
        Log.d(progress)

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
            formatTime(player.currentPosition),
            formatTime(if (player.duration == C.TIME_UNSET) 0 else player.duration)
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

    private fun runAudioProgress() {
        if (player.playbackState == ExoPlayer.STATE_ENDED) return

        mPlayerProgressRunner?.let { progressHandler.removeCallbacks(it) }

        mPlayerProgressRunner = object : Runnable {
            override fun run() {
                updateProgressBar()
                updateTimelineText()
                if (isPlaying()) {
                    progressHandler.postDelayed(this, MILLIS_MULTIPLIER.toLong())
                }
            }
        }

        mPlayerProgressRunner!!.run()
    }

    fun setupOnLoadingInProgress(inProgress: Boolean) {
        isLoadingInProgress = inProgress

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
        updateRepeatMode(repeat)
    }

    fun setContinueChapter(continueChapter: Boolean) {
        setupContinuePlaying(continueChapter)
    }


    fun isReciting(chapterNo: Int, verseNo: Int): Boolean {
        return params.isCurrentVerse(chapterNo, verseNo) && isPlaying()
    }

    fun p() = params

    fun reciteVerse(chapterNo: Int, verseNo: Int) {
        if (
            isLoadingInProgress ||
            !QuranMeta.isChapterValid(chapterNo) ||
            !activity.mReaderParams.isVerseInValidRange(chapterNo, verseNo) ||
            !activity.mQuranMetaRef.get().isVerseValid4Chapter(chapterNo, verseNo)
        ) {
            return
        }

        verseLoadCallback.preLoad()

        RecitationUtils.obtainRecitationModel(
            context,
            forceManifestFetch,
            object : OnResultReadyCallback<RecitationInfoModel?> {
                override fun onReady(r: RecitationInfoModel?) {
                    // Saved recitation slug successfully fetched, now proceed.
                    forceManifestFetch = false

                    if (r != null) {
                        reciteVerseOnSlugAvailable(r, chapterNo, verseNo)
                    } else {
                        verseLoadCallback.onFailed(null, null)
                        verseLoadCallback.postLoad()

                        destroy()
                        pauseMedia()
                    }
                }
            })
    }

    private fun reciteVerseOnSlugAvailable(model: RecitationInfoModel, chapterNo: Int, verseNo: Int) {
        val audioFile = fileUtils.getRecitationAudioFile(model.slug, chapterNo, verseNo)

        // Check If the audio file exists.
        if (audioFile.exists() && audioFile.length() > 0) {
            if (verseLoader.isPending(model.slug, chapterNo, verseNo)) {
                pauseMedia()
                verseLoadCallback.preLoad()
                verseLoader.addCallback(model.slug, chapterNo, verseNo, verseLoadCallback)
            } else {
                val audioURI: Uri = fileUtils.getFileURI(audioFile)
                prepareMediaPlayer(audioURI, model.slug, chapterNo, verseNo, true)
            }
        } else {
            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                popMiniMsg(context.getString(R.string.strMsgNoInternet), Toast.LENGTH_LONG)
                destroy()
                pauseMedia()
                return
            }
            pauseMedia()
            loadVerse(model, chapterNo, verseNo, verseLoadCallback)
        }

        preLoadVerses(activity.mQuranMetaRef.get(), model, chapterNo, verseNo)
    }

    private fun loadVerse(
        model: RecitationInfoModel,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        callback?.preLoad()

        val verseFile = fileUtils.getRecitationAudioFile(model.slug, chapterNo, verseNo)

        if (verseFile.exists() && verseFile.length() > 0) {
            verseLoader.publishVerseLoadStatus(verseFile, callback, true, null)
            return
        }

        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            verseLoader.publishVerseLoadStatus(verseFile, callback, false, NoInternetException())
            return
        }

        if (!fileUtils.createFile(verseFile)) {
            verseLoader.publishVerseLoadStatus(verseFile, callback, false, null)
            return
        }

        val url = RecitationUtils.prepareAudioUrl(model, chapterNo, verseNo) ?: return
        verseLoader.addTask(verseFile, url, model.slug, chapterNo, verseNo, callback)
    }

    private fun preLoadVerses(
        quranMeta: QuranMeta,
        model: RecitationInfoModel,
        chapterNo: Int,
        firstVerseToLoad: Int
    ) {
        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            return
        }

        val preLoadCount = 2
        val length = (activity.mQuranMetaRef.get().getChapterVerseCount(chapterNo) - firstVerseToLoad)
            .coerceAtMost(preLoadCount - 1)

        // Start from 1 so that the current verse is skipped.
        for (i in 1..length) {
            val verseToLoad = firstVerseToLoad + i

            if (!quranMeta.isVerseValid4Chapter(chapterNo, verseToLoad)) {
                continue
            }

            val verseFile = fileUtils.getRecitationAudioFile(model.slug, chapterNo, verseToLoad)

            if (verseLoader.isPending(model.slug, chapterNo, verseToLoad)) {
                continue
            }

            if (verseFile.exists() && verseFile.length() > 0) {
                continue
            }

            Log.d("Loading ahead verse - $chapterNo:$verseToLoad")

            if (!fileUtils.createFile(verseFile)) {
                continue
            }

            loadVerse(model, chapterNo, verseToLoad, object : RecitationPlayerVerseLoadCallback(null) {
                override fun onFailed(e: Throwable?, file: File?) {
                    e?.printStackTrace()
                    verseFile?.delete()
                }
            })
        }
    }

    fun popMiniMsg(msg: String, duration: Int) {
        MessageUtils.showRemovableToast(context, msg, duration)
    }
}