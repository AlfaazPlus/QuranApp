package com.quranapp.android.utils.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.FileDataSource
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.extensions.runOnInterval
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.reader.recitation.player.RecitationMediaItem
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerVerseLoadCallback
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerVerseLoader
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.recitation.RecitationPlayer
import java.io.File


class RecitationService : Service() {
    companion object {
        const val NOTIF_ID = 55
        const val MILLIS_MULTIPLIER = 100L
        const val ACTION_SEEK_LEFT = -1L
        const val ACTION_SEEK_RIGHT = 1L

        const val COMMAND_RECITE_VERSE = 313
    }

    private val binder = LocalBinder()
    private lateinit var player: ExoPlayer
    private val playlist = ConcatenatingMediaSource()
    private val dataSourceFactory = FileDataSource.Factory()
    private var mediaSession: MediaSessionCompat? = null
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val verseLoader = RecitationPlayerVerseLoader()
    private val verseLoadCallback = RecitationPlayerVerseLoadCallback(this)
    private val recParams = RecitationPlayerParams()
    lateinit var fileUtils: FileUtils

    private var progressHandler: Handler? = null
    private var activity: ActivityReader? = null
    private var quranMeta: QuranMeta? = null
    var recPlayer: RecitationPlayer? = null
    var isLoadingInProgress = false
    var forceManifestFetch = false

    inner class LocalBinder : Binder() {
        fun getService(): RecitationService = this@RecitationService
    }

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()

        fileUtils = FileUtils.newInstance(this)

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: PositionInfo,
                    newPosition: PositionInfo,
                    reason: Int
                ) {
                    if (reason != DISCONTINUITY_REASON_AUTO_TRANSITION) return

                    runAudioProgress()

                    val item = newPosition.mediaItem!!.localConfiguration?.tag as? RecitationMediaItem ?: return

                    recParams.currentVerse = ChapterVersePair(item.chapterNo, item.verseNo)
                    recParams.currentReciter = item.reciter

                    recPlayer?.onPlayMedia(recParams)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        STATE_BUFFERING -> {
                            recPlayer?.setupOnLoadingInProgress(true)
                        }
                        STATE_READY -> {
                            recPlayer?.setupOnLoadingInProgress(false)
                            runAudioProgress()
                        }
                        STATE_ENDED -> {
                            if (quranMeta == null || !recParams.continueRange) {
                                recPlayer?.onStopMedia(recParams)
                                return
                            }

                            val nextVerse = recParams.getNextVerse(quranMeta!!)

                            if (nextVerse != null) {
                                reciteVerse(nextVerse)
                            } else {
                                stopMedia()
                            }
                        }
                        else -> {
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    recParams.previouslyPlaying = isPlaying

                    if (isPlaying) {
                        recPlayer?.onPlayMedia(recParams)
                    } else {
                        recPlayer?.onPauseMedia(recParams)
                    }
                }
            })
        }
        player.setMediaSource(playlist)

        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIF_ID,
            getString(R.string.strNotifChannelIdRecitation)
        ).setMediaDescriptionAdapter(object : MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): String {
                val chapterName = quranMeta?.getChapterName(this@RecitationService, recParams.currentChapterNo) ?: ""

                return getString(
                    R.string.strLabelVerseWithChapNameAndNo,
                    chapterName,
                    recParams.currentChapterNo,
                    recParams.currentVerseNo
                )
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return null
            }

            override fun getCurrentContentText(player: Player): String? {
                return RecitationUtils.getReciterName(recParams.currentReciter)
            }

            override fun getCurrentLargeIcon(player: Player, callback: BitmapCallback): Bitmap {
                return drawable(R.drawable.dr_quran_wallpaper).toBitmap()
            }
        })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopMedia()
                    stopSelf()
                }

                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    startForeground(notificationId, notification)
                }
            })
            .setSmallIconResourceId(R.drawable.dr_logo)
            .setPreviousActionIconResourceId(R.drawable.dr_icon_player_seek_left)
            .setPlayActionIconResourceId(R.drawable.dr_icon_play_verse)
            .setPauseActionIconResourceId(R.drawable.dr_icon_pause_verse)
            .setStopActionIconResourceId(R.drawable.icon_verse_stop)
            .setNextActionIconResourceId(R.drawable.dr_icon_player_seek_right)
            .build()


        playerNotificationManager!!.let {
            it.setColor(color(R.color.colorPrimaryDark))
            it.setColorized(true)

            it.setUseRewindAction(false)
            it.setUseFastForwardAction(false)
            it.setUseChronometer(true)
            it.setUseStopAction(true)
            it.setUseNextAction(true)
            it.setUseNextActionInCompactView(true)
            it.setUsePreviousAction(true)
            it.setUsePreviousActionInCompactView(true)

            it.setPlayer(player)
        }

        mediaSession = MediaSessionCompat(this, "RecitationService").apply {
            isActive = true
            playerNotificationManager!!.setMediaSessionToken(sessionToken)
        }
    }

    override fun onDestroy() {
        Log.d("DESTROY")

        playerNotificationManager?.setPlayer(null)
        destroy()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(intent, flags, startId)
        return super.onStartCommand(intent, flags, startId)
    }

    fun destroy() {
        player.release()
        mediaSession?.release()
        playerNotificationManager = null
        mediaSession = null

        playlist.clear()
    }

    fun setRecitationPlayer(recPlayer: RecitationPlayer?, activity: ActivityReader?) {
        this.recPlayer = recPlayer
        this.activity = activity

        quranMeta = activity?.mQuranMetaRef?.get()

        sync()
    }

    fun updateRepeatMode(repeat: Boolean) {
        recParams.repeatVerse = repeat
        player.repeatMode = if (repeat) REPEAT_MODE_ONE else REPEAT_MODE_OFF
        SPReader.setRecitationRepeatVerse(this, repeat)
    }

    fun updateContinuePlaying(continuePlaying: Boolean) {
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

    fun setupOnLoadingInProgress(inProgress: Boolean) {
        isLoadingInProgress = inProgress

        recPlayer?.setupOnLoadingInProgress(inProgress)
    }

    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        recParams.currentVerse = ChapterVersePair(chapterNo, fromVerse)
        recParams.firstVerse = ChapterVersePair(chapterNo, fromVerse)
        recParams.lastVerse = ChapterVersePair(chapterNo, toVerse)
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
    }

    fun onJuzChanged(juzNo: Int, quranMeta: QuranMeta) {
        val (firstChapter, lastChapter) = quranMeta.getChaptersInJuz(juzNo)
        val firstVerse = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter).first
        val (_, lastVerse) = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter)
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)

        recParams.firstVerse = ChapterVersePair(firstChapter, firstVerse)
        recParams.lastVerse = ChapterVersePair(lastChapter, lastVerse)
        recParams.currentVerse = recParams.firstVerse
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
    }

    private fun sync() {
        runAudioProgress()

        recPlayer?.let {
            it.updateMaxProgress((player.duration / MILLIS_MULTIPLIER).coerceAtLeast(0).toInt())
            updatePlayerProgress()
            it.updateVerseSync(recParams, isPlaying)
            it.setupOnLoadingInProgress(isLoadingInProgress)

            if (isPlaying) {
                it.onPlayMedia(recParams)
            } else {
                it.onPauseMedia(recParams)
            }
        }
    }

    private fun runAudioProgress() {
        progressHandler?.removeCallbacksAndMessages(null)
        recPlayer?.updateMaxProgress((player.duration / MILLIS_MULTIPLIER).coerceAtLeast(0).toInt())

        if (recPlayer == null || !isPlaying) return

        progressHandler = runOnInterval({
            if (!isPlaying) {
                progressHandler?.removeCallbacksAndMessages(null)
                return@runOnInterval
            }

            updatePlayerProgress(player.currentPosition)
        }, 100L, true)
    }

    fun updatePlayerProgress(curMillis: Long = -1) {
        val millis = if (curMillis == -1L) player.currentPosition else curMillis

        recPlayer?.let {
            it.updateProgressBar((millis.toInt() / MILLIS_MULTIPLIER).toInt())
            it.updateTimelineText(millis, player.duration)
        }
    }

    val isPlaying get() = player.isPlaying

    fun isReciting(chapterNo: Int, verseNo: Int) = recParams.isCurrentVerse(chapterNo, verseNo) && isPlaying

    val p get() = recParams

    fun prepareMediaPlayer(audioURI: Uri, reciter: String, chapterNo: Int, verseNo: Int) {
        verseLoadCallback.postLoad()

        recParams.lastMediaURI = audioURI
        recParams.currentVerse = ChapterVersePair(chapterNo, verseNo)
        recParams.currentReciter = reciter

        playlist.clear()
        addToQueue(audioURI, reciter, chapterNo, verseNo)

        player.setMediaSource(playlist)
        player.prepare()
        playMedia()

        prepareNextVerses(reciter)
    }

    private fun prepareNextVerses(currentReciter: String) {
        if (!recParams.continueRange || quranMeta == null) return

        var currentVerse = recParams.currentVerse
        val lastVerse = recParams.lastVerse

        if (currentVerse == lastVerse) return

        val count = 10
        var i = 1

        while (i <= count) {
            val nextVerse = recParams.getNextVerse(quranMeta!!, currentVerse) ?: break

            val audioFile =
                fileUtils.getRecitationAudioFile(recParams.currentReciter, nextVerse.chapterNo, nextVerse.verseNo)
            if (audioFile.length() > 0) {
                addToQueue(audioFile.toUri(), currentReciter, nextVerse.chapterNo, nextVerse.verseNo)
            }

            currentVerse = nextVerse
            i++
        }
    }

    private fun addToQueue(audioURI: Uri, reciter: String, chapterNo: Int, verseNo: Int) {
        playlist.addMediaSource(
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                createMediaItem(audioURI, reciter, chapterNo, verseNo)
            )
        )
    }

    private fun createMediaItem(audioURI: Uri, reciter: String, chapterNo: Int, verseNo: Int): MediaItem {
        return MediaItem.Builder()
            .setUri(audioURI)
            .setTag(RecitationMediaItem(reciter, chapterNo, verseNo))
            .build()
    }

    fun playMedia() {
        runAudioProgress()
        player.play()
    }

    fun pauseMedia() {
        player.pause()
        runAudioProgress()
    }

    fun playControl() {
        if (quranMeta == null) return

        Log.d(player.duration > 0, player.currentPosition < player.duration, recParams.hasNextVerse(quranMeta!!))
        if (player.duration > 0 && player.currentPosition < player.duration) {
            if (isPlaying) pauseMedia()
            else playMedia()
        } else if (!recParams.hasNextVerse(quranMeta!!)) {
            restartRange()
        } else {
            restartVerse()
        }
    }

    fun stopMedia() {
        player.stop()
        player.clearMediaItems()
        playlist.clear()

        recParams.currentVerse = recParams.firstVerse
        recPlayer?.onStopMedia(recParams)

        runAudioProgress()
    }

    fun seek(amountOrDirection: Long) {
        if (isLoadingInProgress) {
            return
        }

        val p = player

        val seekAmount = when (amountOrDirection) {
            ACTION_SEEK_RIGHT -> 5000L
            ACTION_SEEK_LEFT -> -5000L
            else -> amountOrDirection
        }

        val fromBtnClick = amountOrDirection == ACTION_SEEK_LEFT || amountOrDirection == ACTION_SEEK_RIGHT
        var seekFinal: Long = seekAmount

        if (fromBtnClick) {
            seekFinal += p.currentPosition
            seekFinal = seekFinal.coerceAtLeast(0)
            seekFinal = seekFinal.coerceAtMost(p.duration)
        }

        p.seekTo(seekFinal)
    }

    fun recitePreviousVerse() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            val previousVerse = recParams.getPreviousVerse(quranMeta!!) ?: return
            reciteVerse(previousVerse)
        }
    }

    fun reciteNextVerse() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            val nextVerse = recParams.getNextVerse(quranMeta!!) ?: return
            reciteVerse(nextVerse)
        }
    }

    private fun restartRange() {
        if (isLoadingInProgress) return

        if (playlist.size == 0) {
            reciteVerse(recParams.firstVerse)
            return
        }

        player.seekTo(0, 0)
    }

    fun restartVerse(force: Boolean = false) {
        if (isLoadingInProgress) {
            return
        }

        if (playlist.size == 0 || force) {
            reciteVerse(recParams.currentVerse)
            return
        }

        player.seekTo(0)
    }

    fun onReciterChanged() {
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
    }

    fun onTranslationReciterChanged() {
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)
    }

    fun onAudioOptionChanged(newOption: String) {
        recParams.currentAudioOption = newOption
    }

    fun reciteControl(pair: ChapterVersePair) {
        if (isLoadingInProgress) return

        if (!recParams.isCurrentVerse(pair.chapterNo, pair.verseNo)
            || player.duration <= 0
            || player.currentPosition >= player.duration
        ) {
            reciteVerse(pair)
        }

        if (isPlaying) pauseMedia()
        else playMedia()
    }

    fun reciteVerse(pair: ChapterVersePair) {
        if (
            isLoadingInProgress ||
            !QuranMeta.isChapterValid(pair.chapterNo) ||
            quranMeta == null ||
            !quranMeta!!.isVerseValid4Chapter(pair.chapterNo, pair.verseNo)
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
                        reciteVerseOnSlugAvailable(quranMeta!!, r, pair.chapterNo, pair.verseNo)
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
        if (audioFile.length() > 0) {
            if (verseLoader.isPending(model.slug, chapterNo, verseNo)) {
                pauseMedia()
                verseLoadCallback.preLoad()
                verseLoader.addCallback(model.slug, chapterNo, verseNo, verseLoadCallback)
            } else {
                prepareMediaPlayer(audioFile.toUri(), model.slug, chapterNo, verseNo)
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

        val preLoadCount = 20
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

    fun cancelLoading() {
        verseLoader.cancelAll()
    }

    fun popMiniMsg(msg: String, duration: Int) {
        MessageUtils.showRemovableToast(this, msg, duration)
    }
}