package com.quranapp.android.utils.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.reader.recitation.RecitationNotificationHelper
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.recitation.RecitationPlayer
import com.quranapp.android.views.recitation.RecitationPlayerParams
import com.quranapp.android.views.recitation.RecitationPlayerVerseLoadCallback
import com.quranapp.android.views.recitation.RecitationPlayerVerseLoader
import java.io.File

class RecitationPlayerService : Service() {
    companion object {
        const val MILLIS_MULTIPLIER = 100
        const val SEEK_LEFT = -1
        const val SEEK_RIGHT = 1
        const val NOTIF_ID = 55
    }

    private val binder = LocalBinder()
    val recParams = RecitationPlayerParams()
    private var player: MediaPlayer? = null
    private var session: MediaSessionCompat? = null
    lateinit var fileUtils: FileUtils

    private val notifManager by lazy { NotificationManagerCompat.from(this) }
    private val notifActionPrev by lazy { RecitationNotificationHelper.createPreviousVerseAction(this) }
    private val notifActionNext by lazy { RecitationNotificationHelper.createNextVerseAction(this) }
    private var notifBuilder: NotificationCompat.Builder? = null
    private var notifTitle: String? = null
    private var notifDescription: String? = null

    private val progressHandler = Handler(Looper.getMainLooper())
    private var activity: ActivityReader? = null
    private var quranMeta: QuranMeta? = null
    var recPlayer: RecitationPlayer? = null
    private var playerProgressRunner: Runnable? = null
    private val verseLoader = RecitationPlayerVerseLoader()
    private val verseLoadCallback = RecitationPlayerVerseLoadCallback(this)

    var isLoadingInProgress = false
    internal var forceManifestFetch = false

    inner class LocalBinder : Binder() {
        fun getService(): RecitationPlayerService = this@RecitationPlayerService
    }

    override fun onBind(intent: Intent?) = binder

    fun setRecitationPlayer(recPlayer: RecitationPlayer?, activity: ActivityReader?) {
        this.recPlayer = recPlayer
        this.activity = activity

        quranMeta = activity?.mQuranMetaRef?.get()
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIF_ID,
            NotificationUtils.createEmptyNotif(this, getString(R.string.strNotifChannelIdRecitation))
        )

        fileUtils = FileUtils.newInstance(this)
        session = MediaSessionCompat(this, "RecitationPlayer").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playMedia()
                }

                override fun onPause() {
                    pauseMedia()
                }

                override fun onStop() {
                    this@RecitationPlayerService.release()
                }

                override fun onSkipToPrevious() {
                    recitePreviousVerse(activity?.mReaderParams?.isSingleVerse ?: false)
                }

                override fun onSkipToNext() {
                    reciteNextVerse(activity?.mReaderParams?.isSingleVerse ?: false)
                }
            })
        }

        updateRepeatMode(SPReader.getRecitationRepeatVerse(this))
        setupContinuePlaying(SPReader.getRecitationContinueChapter(this))
        updateVerseSync(SPReader.getRecitationScrollSync(this), false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(session, intent)

        return START_STICKY
    }

    fun destroy() {
        release()

        verseLoader.cancelAll()
        session?.release()
        notifManager.cancel(NOTIF_ID)
    }

    fun release() {
        player?.let {
            it.stop()
            it.reset()
            it.release()
            player = null
        }

        session?.isActive = false
    }

    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        recParams.currentVerse = Pair(chapterNo, fromVerse)
        recParams.firstVerse = Pair(chapterNo, fromVerse)
        recParams.lastVerse = Pair(chapterNo, toVerse)
    }

    fun onJuzChanged(juzNo: Int, quranMeta: QuranMeta) {
        val (firstChapter, lastChapter) = quranMeta.getChaptersInJuz(juzNo)
        val firstVerse = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter).first
        val (_, lastVerse) = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter)

        recParams.firstVerse = Pair(firstChapter, firstVerse)
        recParams.lastVerse = Pair(lastChapter, lastVerse)
        recParams.currentVerse = recParams.firstVerse
    }

    fun prepareMediaPlayer(
        audioURI: Uri,
        reciter: String,
        chapterNo: Int,
        verseNo: Int,
        play: Boolean
    ) {
        verseLoadCallback.postLoad()
        release()

        player = MediaPlayer.create(this, audioURI)

        if (player == null) {
            popMiniMsg("Something happened wrong while playing the audio", Toast.LENGTH_LONG)
            return
        }

        recParams.lastMediaURI = audioURI
        recParams.currentReciter = reciter
        recParams.currentVerse = Pair(chapterNo, verseNo)

        session!!.isActive = true
        session!!.setMetadata(prepareMetadata())

        val p = player!!

        p.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        p.setNextMediaPlayer(null)
        updateRepeatMode(recParams.repeatVerse)

        p.setOnPreparedListener { mp ->
            recPlayer?.let {
                it.binding.progress.max = mp.duration / MILLIS_MULTIPLIER
                it.updateProgressBar()
                it.updateTimelineText()
            }

            if (play) {
                session!!.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setActions(
                            PlaybackStateCompat.ACTION_PLAY or
                                    PlaybackStateCompat.ACTION_PAUSE or
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        ).setState(PlaybackStateCompat.STATE_PLAYING, mp.currentPosition.toLong(), 1.0f)
                        .build()
                )
                playMedia()
            }
        }

        p.setOnErrorListener { _, what, extra ->
            Log.d(what, extra)
            true
        }

        p.setOnInfoListener { _, what, extra ->
            Log.d(what, extra)
            true
        }

        p.setOnCompletionListener {
            recParams.currentVerseCompleted = true

            release()

            if (quranMeta == null) {
                pauseMedia()
                return@setOnCompletionListener
            }

            val continueNext = recParams.continueRange && recParams.hasNextVerse(quranMeta!!)

            val verseValid = quranMeta!!.isVerseValid4Chapter(recParams.currentChapterNo, recParams.currentVerseNo + 1)
            val isSingleVerseMode = activity?.mReaderParams?.isSingleVerse ?: false
            val continueNextSingle = recParams.continueRange && isSingleVerseMode && verseValid

            recParams.currentRangeCompleted = !continueNext && !continueNextSingle

            if (continueNext || continueNextSingle) {
                reciteNextVerse(isSingleVerseMode)
            } else {
                pauseMedia()
            }
        }
    }

    private fun prepareMetadata(): MediaMetadataCompat {
        val chapterName = quranMeta?.getChapterName(this, recParams.currentChapterNo) ?: ""
        notifTitle = this.getString(
            R.string.strLabelVerseWithChapNameAndNo,
            chapterName,
            recParams.currentChapterNo,
            recParams.currentVerseNo
        )

        notifDescription = RecitationUtils.getReciterName(recParams.currentReciter)

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, notifTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, notifDescription)
            .build()
    }

    private fun updateRepeatMode(repeat: Boolean) {
        recParams.repeatVerse = repeat
        player?.isLooping = repeat
        SPReader.setRecitationRepeatVerse(this, repeat)
    }

    private fun setupContinuePlaying(continuePlaying: Boolean) {
        recParams.continueRange = continuePlaying
        SPReader.setRecitationContinueChapter(this, continuePlaying)
    }

    fun updateVerseSync(sync: Boolean, fromUser: Boolean) {
        recParams.syncWithVerse = sync

        recPlayer?.updateVerseSync(recParams, isPlaying)

        SPReader.setRecitationVerseSync(this, recParams.syncWithVerse)

        if (fromUser) {
            popMiniMsg(
                getString(if (recParams.syncWithVerse) R.string.verseSyncOn else R.string.verseSyncOff),
                Toast.LENGTH_SHORT
            )
        }
    }

    fun playMedia() {
        player?.let {
            it.start()
            runAudioProgress()

            recParams.currentRangeCompleted = false
            recParams.currentVerseCompleted = false
        }

        recPlayer?.onPlayMedia(recParams)

        showNotification(true)
        ContextCompat.startForegroundService(this, Intent(this, RecitationPlayerService::class.java))

        Log.d(
            "Curr - " + recParams.currentVerse.first + ":" + recParams.currentVerse.second,
            recParams.firstVerse.first
                .toString() + ":" + recParams.firstVerse.second + " - " + recParams.lastVerse.first + ":" + recParams.lastVerse.second
        )

        recParams.previouslyPlaying = true
    }

    fun pauseMedia() {
        if (isPlaying) {
            player?.pause()
            progressHandler.removeCallbacksAndMessages(null)
        }

        showNotification(false)
        recParams.previouslyPlaying = false

        recPlayer?.onPauseMedia(recParams)
    }

    fun playControl() {
        if (recParams.currentRangeCompleted && recParams.currentVerseCompleted) {
            restartRange()
        } else if (player != null) {
            if (isPlaying) {
                pauseMedia()
            } else {
                playMedia()
            }
        } else {
            restartVerse()
        }
    }


    fun seek(amountOrDirection: Int) {
        if (player == null || isLoadingInProgress) {
            return
        }

        val p = player!!

        recParams.currentVerseCompleted = false

        val seekAmount = when (amountOrDirection) {
            SEEK_RIGHT -> 5000
            SEEK_LEFT -> -5000
            else -> amountOrDirection
        }

        val fromBtnClick = amountOrDirection == SEEK_LEFT || amountOrDirection == SEEK_RIGHT
        var seekFinal = seekAmount

        if (fromBtnClick) {
            seekFinal += p.currentPosition
            seekFinal = seekFinal.coerceAtLeast(0)
            seekFinal = seekFinal.coerceAtMost(p.duration)
        }

        p.seekTo(seekFinal)

        recPlayer?.let {
            it.updateTimelineText()

            if (fromBtnClick) {
                it.updateProgressBar()
            }
        }
    }

    private fun runAudioProgress() {
        if (player == null) return

        playerProgressRunner?.let { progressHandler.removeCallbacks(it) }

        playerProgressRunner = object : Runnable {
            override fun run() {
//                recPlayer?.post {
                recPlayer?.let {
                    it.updateProgressBar()
                    it.updateTimelineText()
                }
//                }

                if (isPlaying) {
                    progressHandler.postDelayed(this, MILLIS_MULTIPLIER.toLong())
                }
            }
        }

        playerProgressRunner!!.run()
    }

    val isPlaying get() = player?.isPlaying ?: false

    val currentPosition get() = player?.currentPosition ?: 0

    val duration get() = player?.duration ?: 0

    fun isReciting(chapterNo: Int, verseNo: Int) = recParams.isCurrentVerse(chapterNo, verseNo) && isPlaying


    fun recitePreviousVerse(isSingleVerseMode: Boolean) {
        if (isLoadingInProgress || quranMeta == null) return

        val chapterNo: Int
        val verseNo: Int

        if (isSingleVerseMode) {
            chapterNo = recParams.currentChapterNo
            verseNo = recParams.currentVerseNo - 1
            recPlayer?.onVerseReciteOrJump(chapterNo, verseNo)
        } else {
            val previousVerse = recParams.getPreviousVerse(quranMeta!!)
            chapterNo = previousVerse.first
            verseNo = previousVerse.second
        }

        Log.d("PREV VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    fun reciteNextVerse(isSingleVerseMode: Boolean) {
        if (isLoadingInProgress || quranMeta == null) return

        val chapterNo: Int
        val verseNo: Int

        if (isSingleVerseMode) {
            chapterNo = recParams.currentChapterNo
            verseNo = recParams.currentVerseNo + 1
            recPlayer?.onVerseReciteOrJump(chapterNo, verseNo)
        } else {
            val nextVerse = recParams.getNextVerse(quranMeta!!)
            chapterNo = nextVerse.first
            verseNo = nextVerse.second
        }

        Log.d("NEXT VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    fun restartRange() {
        if (isLoadingInProgress) return

        val firstVerse = recParams.firstVerse
        reciteVerse(firstVerse.first, firstVerse.second)
    }

    fun restartVerse() {
        if (isLoadingInProgress) {
            return
        }

        reciteVerse(recParams.currentChapterNo, recParams.currentVerseNo)
    }

    fun reciteControl(chapterNo: Int, verseNo: Int) {
        if (isLoadingInProgress) return

        if (recParams.isCurrentVerse(chapterNo, verseNo) && player != null) {
            playControl()
        } else {
            reciteVerse(chapterNo, verseNo)
        }
    }

    fun p() = recParams

    fun reciteVerse(chapterNo: Int, verseNo: Int) {
        if (
            isLoadingInProgress ||
            !QuranMeta.isChapterValid(chapterNo) ||
            quranMeta == null ||
            !quranMeta!!.isVerseValid4Chapter(chapterNo, verseNo)
        ) {
            return
        }

        verseLoadCallback.preLoad()

        RecitationUtils.obtainRecitationModel(
            this,
            forceManifestFetch,
            object : OnResultReadyCallback<RecitationInfoModel?> {
                override fun onReady(r: RecitationInfoModel?) {
                    // Saved recitation slug successfully fetched, now proceed.
                    forceManifestFetch = false

                    if (r != null) {
                        reciteVerseOnSlugAvailable(quranMeta!!, r, chapterNo, verseNo)
                    } else {
                        verseLoadCallback.onFailed(null, null)
                        verseLoadCallback.postLoad()

                        destroy()
                        pauseMedia()
                    }
                }
            })
    }

    private fun reciteVerseOnSlugAvailable(
        quranMeta: QuranMeta,
        model: RecitationInfoModel,
        chapterNo: Int,
        verseNo: Int
    ) {
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
            if (!NetworkStateReceiver.isNetworkConnected(this)) {
                popMiniMsg(getString(R.string.strMsgNoInternet), Toast.LENGTH_LONG)
                destroy()
                pauseMedia()
                return
            }
            pauseMedia()
            loadVerse(model, chapterNo, verseNo, verseLoadCallback)
        }

        preLoadVerses(quranMeta, model, chapterNo, verseNo)
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

        if (!NetworkStateReceiver.isNetworkConnected(this)) {
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
        if (!NetworkStateReceiver.isNetworkConnected(this)) {
            return
        }

        val preLoadCount = 2
        val length = (quranMeta.getChapterVerseCount(chapterNo) - firstVerseToLoad)
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

    fun setupOnLoadingInProgress(inProgress: Boolean) {
        isLoadingInProgress = inProgress

        recPlayer?.setupOnLoadingInProgress(inProgress)
    }

    fun setRepeat(repeat: Boolean) {
        updateRepeatMode(repeat)
    }

    fun setContinueChapter(continueChapter: Boolean) {
        setContinueChapter(continueChapter)
    }

    fun cancelLoading() {
        verseLoader.cancelAll()
    }

    private fun showNotification(isPlay: Boolean) {
        if (notifBuilder == null) {
            notifBuilder = RecitationNotificationHelper.createNotificationBuilder(this, session!!)
        }

        notifBuilder = notifBuilder!!.setContentTitle(notifTitle)
            .setContentText(notifDescription)
            .clearActions()
            .addAction(notifActionPrev)
            .setProgress(player?.duration ?: 0, player?.currentPosition ?: 0, false)
            .addAction(
                NotificationCompat.Action(
                    if (isPlay) R.drawable.dr_icon_pause_verse
                    else R.drawable.dr_icon_play_verse,
                    if (isPlay) "Pause"
                    else "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        if (isPlay) PlaybackStateCompat.ACTION_PAUSE
                        else PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
            .addAction(notifActionNext)

        notifManager.notify(NOTIF_ID, notifBuilder!!.build())
    }

    private fun updateNotificationProgress() {
        if (notifBuilder == null) return

        notifBuilder!!.setProgress(player?.duration ?: 0, player?.currentPosition ?: 0, false)
        notifManager.notify(NOTIF_ID, notifBuilder!!.build())
    }

    fun popMiniMsg(msg: String, duration: Int) {
        MessageUtils.showRemovableToast(this, msg, duration)
    }
}