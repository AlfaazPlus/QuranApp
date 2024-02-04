package com.quranapp.android.utils.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.util.Pair
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.Player.PositionInfo
import com.google.android.exoplayer2.Player.REPEAT_MODE_OFF
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.upstream.FileDataSource
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.extensions.runOnInterval
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.reader.recitation.player.RecitationMediaItem
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerVerseLoadCallback
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerVerseLoader
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.receivers.RecitationHeadsetReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.views.recitation.RecitationPlayer
import java.io.File


class RecitationService : Service(), MediaDescriptionAdapter {
    companion object {
        const val NOTIF_ID = 55
        const val MILLIS_MULTIPLIER = 100L
        const val ACTION_SEEK_LEFT = -1L
        const val ACTION_SEEK_RIGHT = 1L
    }

    private val binder = LocalBinder()
    private lateinit var player: ExoPlayer
    private val playlist = ConcatenatingMediaSource()
    private val dataSourceFactory = FileDataSource.Factory()
    private val headsetReceiver = RecitationHeadsetReceiver(this)
    private var mediaSession: MediaSessionCompat? = null
    private var sessionConnector: MediaSessionConnector? = null
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
    var forceTranslationManifestFetch = false

    inner class LocalBinder : Binder() {
        fun getService(): RecitationService = this@RecitationService
    }

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()

        recParams.init(this)

        fileUtils = FileUtils.newInstance(this)
        player = ExoPlayer.Builder(this).build().apply {
            setMediaSource(playlist)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            addListener(object : Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    onMediaItemTransition(mediaItem)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: PositionInfo,
                    newPosition: PositionInfo,
                    reason: Int
                ) {
                    if (reason != DISCONTINUITY_REASON_AUTO_TRANSITION && reason != DISCONTINUITY_REASON_SEEK) return
                    onMediaItemTransition(newPosition.mediaItem)
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

                            Log.d(nextVerse)

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
                    recParams.pausedDueToHeadset = false

                    if (isPlaying) recPlayer?.onPlayMedia(recParams)
                    else recPlayer?.onPauseMedia(recParams)

                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.saveError(error, "RecitationService.ExoPlayer")
                    error.printStackTrace()
                }
            })
        }

        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIF_ID,
            NotificationUtils.CHANNEL_ID_RECITATION_PLAYER
        ).setMediaDescriptionAdapter(this)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopMedia()
                }

                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    ServiceCompat.startForeground(
                        this@RecitationService,
                        notificationId,
                        notification,
                        FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                }
            })
            .setSmallIconResourceId(R.drawable.dr_logo)
            .setPreviousActionIconResourceId(R.drawable.dr_icon_player_seek_left)
            .setPlayActionIconResourceId(R.drawable.dr_icon_play_verse)
            .setPauseActionIconResourceId(R.drawable.dr_icon_pause_verse)
//            .setStopActionIconResourceId(R.drawable.icon_verse_stop)
            .setNextActionIconResourceId(R.drawable.dr_icon_player_seek_right)
            .build()

        playerNotificationManager!!.let {
            it.setColor(color(R.color.colorPrimaryDark))
            it.setColorized(true)
            it.setUsePlayPauseActions(true)
            it.setUseRewindAction(false)
            it.setUseFastForwardAction(false)
            it.setUseChronometer(true)
//            it.setUseStopAction(true)
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

        sessionConnector = MediaSessionConnector(mediaSession!!).apply {
            setPlayer(player)
            setClearMediaItemsOnStop(true)
        }

        ContextCompat.registerReceiver(
            this,
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG).apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            },
            ContextCompat.RECEIVER_EXPORTED
        )

        syncConfigurations()
    }

    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)

        playerNotificationManager?.setPlayer(null)
        destroy()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    fun destroy() {
        player.release()
        mediaSession?.release()
        playerNotificationManager = null
        mediaSession = null

        playlist.clear()
    }

    @Synchronized
    fun setRecitationPlayer(recPlayer: RecitationPlayer?, activity: ActivityReader?) {
        this.recPlayer = recPlayer
        this.activity = activity

        quranMeta = activity?.mQuranMetaRef?.get()

        sync()
    }

    fun updatePlaybackSpeed(speed: Float) {
        recParams.playbackSpeed = speed
        player.setPlaybackSpeed(speed)
        SPReader.setRecitationSpeed(this, speed)
    }

    fun updateRepeatMode(repeat: Boolean) {
        recParams.repeatVerse = repeat
        player.repeatMode = if (repeat) REPEAT_MODE_ONE else REPEAT_MODE_OFF
        SPReader.setRecitationRepeatVerse(this, repeat)
    }

    fun updateContinuePlaying(continuePlaying: Boolean) {
        recParams.continueRange = continuePlaying
        SPReader.setRecitationContinueChapter(this, continuePlaying)

        if (!recParams.previouslyPlaying) {
            return
        }

        pauseMedia()
        val currentPos = player.currentPosition
        restartVerse(true)
        player.seekTo(currentPos)
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

    private fun onMediaItemTransition(mediaItem: MediaItem?) {
        runAudioProgress()

        val item = mediaItem?.localConfiguration?.tag as? RecitationMediaItem ?: return

        recParams.currentVerse = ChapterVersePair(item.chapterNo, item.verseNo)
        recParams.currentReciter = item.reciter
        recParams.currentTranslationReciter = item.translReciter

        recPlayer?.onPlayMedia(recParams)
    }

    @Synchronized
    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int, currentVerse: Int) {
        recParams.currentVerse = ChapterVersePair(chapterNo, currentVerse)
        recParams.firstVerseOfRange = ChapterVersePair(chapterNo, fromVerse)
        recParams.lastVerseOfRange = ChapterVersePair(chapterNo, toVerse)
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)
    }

    @Synchronized
    fun onJuzChanged(juzNo: Int, quranMeta: QuranMeta) {
        val (firstChapter, lastChapter) = quranMeta.getChaptersInJuz(juzNo)
        val firstVerse = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter).first
        val (_, lastVerse) = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter)

        recParams.firstVerseOfRange = ChapterVersePair(firstChapter, firstVerse)
        recParams.lastVerseOfRange = ChapterVersePair(lastChapter, lastVerse)
        recParams.currentVerse = recParams.firstVerseOfRange
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)
    }

    private fun sync() {
        runAudioProgress()

        recPlayer?.let {
            it.updateMaxProgress((player.duration / MILLIS_MULTIPLIER).coerceAtLeast(0).toInt())
            updatePlayerProgress()
            it.updateVerseSync(recParams, isPlaying)
            it.setupOnLoadingInProgress(isLoadingInProgress)

            if (player.playbackState != STATE_IDLE) {
                if (isPlaying) {
                    it.onPlayMedia(recParams)
                } else {
                    it.onPauseMedia(recParams)
                }
            }
        }
    }

    private fun syncConfigurations() {
        updatePlaybackSpeed(recParams.playbackSpeed)
        updateRepeatMode(recParams.repeatVerse)
        updateContinuePlaying(recParams.continueRange)
        updateVerseSync(recParams.syncWithVerse, false)
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

    fun prepareMediaPlayer(
        audioURI: Uri?,
        translAudioURI: Uri?,
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int
    ) {
        verseLoadCallback.postLoad()

        stopMedia()

        recParams.lastMediaURI = audioURI
        recParams.lastTranslMediaURI = translAudioURI
        recParams.currentVerse = ChapterVersePair(chapterNo, verseNo)
        recParams.currentReciter = reciter
        recParams.currentTranslationReciter = translReciter

        if (audioURI != null) addToQueue(audioURI, reciter, translReciter, chapterNo, verseNo)
        if (translAudioURI != null) addToQueue(translAudioURI, reciter, translReciter, chapterNo, verseNo)

        player.setMediaSource(playlist)
        player.prepare()
        playMedia()

        prepareNextVerses()
    }

    private fun prepareNextVerses() {
        if (!recParams.continueRange || quranMeta == null) return

        val audioOption = recParams.currentAudioOption
        val isBoth = audioOption == RecitationUtils.AUDIO_OPTION_BOTH
        val isOnlyTranslation = audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

        var currentVerse = recParams.currentVerse
        val lastVerse = recParams.lastVerseOfRange

        if (currentVerse == lastVerse) return

        val count = 10
        var i = 1

        // prepare next verses
        while (i <= count) {
            val nextVerse = recParams.getNextVerse(quranMeta!!, currentVerse) ?: break
            var wasArabicAddedToQueue = false

            if (!isOnlyTranslation) {
                val audioFile = fileUtils.getRecitationAudioFile(
                    recParams.currentReciter,
                    nextVerse.chapterNo,
                    nextVerse.verseNo
                )

                if (audioFile.length() > 0) {
                    addToQueue(
                        audioFile.toUri(),
                        recParams.currentReciter,
                        recParams.currentTranslationReciter,
                        nextVerse.chapterNo,
                        nextVerse.verseNo
                    )
                    wasArabicAddedToQueue = true
                } else {
                    break
                }
            }

            if (isBoth || isOnlyTranslation) {
                val translAudioFile = fileUtils.getRecitationAudioFile(
                    recParams.currentTranslationReciter,
                    nextVerse.chapterNo,
                    nextVerse.verseNo
                )

                if (translAudioFile.length() > 0) {
                    addToQueue(
                        translAudioFile.toUri(),
                        recParams.currentReciter,
                        recParams.currentTranslationReciter,
                        nextVerse.chapterNo,
                        nextVerse.verseNo
                    )
                } else {
                    // remove arabic audio if translation audio could not be added
                    // we'll add both arabic and translation audio together
                    // wasArabicAddedToQueue is true if audio option is BOTH
                    if (wasArabicAddedToQueue) {
                        playlist.removeMediaSource(playlist.size - 1)
                    }
                    break
                }
            }

            currentVerse = nextVerse
            i++
        }
    }

    private fun addToQueue(audioURI: Uri, reciter: String?, translReciter: String?, chapterNo: Int, verseNo: Int) {
        playlist.addMediaSource(
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                createMediaItem(audioURI, reciter, translReciter, chapterNo, verseNo)
            )
        )
    }

    private fun createMediaItem(
        audioURI: Uri,
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int
    ): MediaItem {
        return MediaItem.Builder()
            .setUri(audioURI)
            .setTag(RecitationMediaItem(reciter, translReciter, chapterNo, verseNo))
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
        player.pause()
        player.stop()
        player.clearMediaItems()
        playlist.clear()

        recParams.currentVerse = recParams.firstVerseOfRange
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
            reciteVerse(recParams.firstVerseOfRange)
            return
        }

        player.seekTo(0, 0)
    }

    private fun restartVerse(force: Boolean = false) {
        if (isLoadingInProgress) {
            return
        }

        if (playlist.size == 0 || force) {
            reciteVerse(recParams.currentVerse)
            return
        }

        player.seekTo(0)
    }

    fun restartVerseOnConfigChange() {
        val previouslyPlaying = recParams.previouslyPlaying

        player.stop()
        player.clearMediaItems()
        playlist.clear()

        if (previouslyPlaying) {
            restartVerse(true)
        }
    }

    fun onReciterChanged() {
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)
    }

    fun onTranslationReciterChanged() {
        recParams.currentTranslationReciter = SPReader.getSavedRecitationTranslationSlug(this)
    }

    fun onAudioOptionChanged(newOption: Int) {
        recParams.currentAudioOption = newOption

        restartVerseOnConfigChange()
    }

    fun reciteControl(pair: ChapterVersePair) {
        if (isLoadingInProgress) return

        if (!recParams.isCurrentVerse(pair.chapterNo, pair.verseNo)
            || player.duration <= 0
            || player.currentPosition >= player.duration
        ) {
            reciteVerse(pair)
            return
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

        RecitationUtils.obtainRecitationModels(
            this,
            forceManifestFetch,
            forceTranslationManifestFetch,
            object : OnResultReadyCallback<Pair<RecitationInfoModel?, RecitationTranslationInfoModel?>> {
                override fun onReady(r: Pair<RecitationInfoModel?, RecitationTranslationInfoModel?>) {
                    val isBoth = recParams.currentAudioOption == RecitationUtils.AUDIO_OPTION_BOTH
                    val isOnlyTransl = recParams.currentAudioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

                    forceManifestFetch = !isOnlyTransl && r.first == null
                    forceTranslationManifestFetch = (isBoth || isOnlyTransl) && r.second == null

                    val failed = if (isBoth) r.first == null || r.second == null
                    else if (isOnlyTransl) r.second == null
                    else r.first == null


                    if (!failed) {
                        reciteVerseOnSlugAvailable(
                            quranMeta!!,
                            r,
                            pair.chapterNo,
                            pair.verseNo
                        )
                    } else {
                        verseLoadCallback.onFailed(null, null, null, deleteVerseFile = false, deleteTranslFile = false)
                        verseLoadCallback.postLoad()

                        destroy()
                        pauseMedia()
                    }
                }
            })
    }

    private fun reciteVerseOnSlugAvailable(
        quranMeta: QuranMeta,
        models: Pair<RecitationInfoModel?, RecitationTranslationInfoModel?>,
        chapterNo: Int,
        verseNo: Int
    ) {
        val recModel = models.first
        val recTranslModel = models.second

        val verseFile = recModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }
        val verseTranslFile = recTranslModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }

        // verseFile is null when ONLY_TRANSLATION audio option is selected.
        val verseFileExists = verseFile == null || verseFile.length() > 0
        // verseTranslFile is null when ONLY_ARABIC audio option is selected.
        val verseTranslFileExists = verseTranslFile == null || verseTranslFile.length() > 0

        if (verseFileExists && verseTranslFileExists) {
            if (verseLoader.isPending(recModel?.slug, recTranslModel?.slug, chapterNo, verseNo)) {
                stopMedia()
                verseLoadCallback.preLoad()
                verseLoader.addCallback(recModel?.slug, recTranslModel?.slug, chapterNo, verseNo, verseLoadCallback)
            } else {
                prepareMediaPlayer(
                    verseFile?.toUri(),
                    verseTranslFile?.toUri(),
                    recModel?.slug,
                    recTranslModel?.slug,
                    chapterNo,
                    verseNo
                )
            }
        } else {
            stopMedia()

            if (!NetworkStateReceiver.isNetworkConnected(this)) {
                popMiniMsg(getString(R.string.strMsgNoInternet), Toast.LENGTH_LONG)
                return
            }

            loadVerse(recModel, recTranslModel, chapterNo, verseNo, verseLoadCallback)
        }

        preLoadVerses(quranMeta, recModel, recTranslModel, chapterNo, verseNo)
    }

    private fun loadVerse(
        model: RecitationInfoModel?,
        translModel: RecitationTranslationInfoModel?,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        callback?.preLoad()

        val verseFile = model?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }
        val verseTranslFile = translModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }

        // verseFile is null when ONLY_TRANSLATION audio option is selected.
        val verseFileExists = verseFile == null || verseFile.length() > 0
        // verseTranslFile is null when ONLY_ARABIC audio option is selected.
        val verseTranslFileExists = verseTranslFile == null || verseTranslFile.length() > 0

        if (verseFileExists && verseTranslFileExists) {
            verseLoader.publishVerseLoadStatus(verseFile, verseTranslFile, callback, true, null)
            return
        }

        if (!NetworkStateReceiver.isNetworkConnected(this)) {
            verseLoader.publishVerseLoadStatus(verseFile, verseTranslFile, callback, false, NoInternetException())
            return
        }

        // verseFile is null when ONLY_TRANSLATION audio option is selected.
        val verseFileCreated = verseFile == null || fileUtils.createFile(verseFile)
        // verseTranslFile is null when ONLY_ARABIC audio option is selected.
        val verseTranslFileCreated = verseTranslFile == null || fileUtils.createFile(verseTranslFile)

        if (!verseFileCreated || !verseTranslFileCreated) {
            verseLoader.publishVerseLoadStatus(verseFile, verseTranslFile, callback, false, null)
            return
        }

        verseLoader.addTask(verseFile, verseTranslFile, model, translModel, chapterNo, verseNo, callback)
    }

    private fun preLoadVerses(
        quranMeta: QuranMeta,
        model: RecitationInfoModel?,
        translModel: RecitationTranslationInfoModel?,
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

            if (verseLoader.isPending(model?.slug, translModel?.slug, chapterNo, verseToLoad)) {
                continue
            }
            val verseFile = model?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseToLoad) }
            val verseTranslFile = translModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseToLoad) }

            // verseFile is null when ONLY_TRANSLATION audio option is selected.
            val verseFileExists = verseFile == null || verseFile.length() > 0
            // verseTranslFile is null when ONLY_ARABIC audio option is selected.
            val verseTranslFileExists = verseTranslFile == null || verseTranslFile.length() > 0

            if (verseFileExists && verseTranslFileExists) {
                continue
            }

            if (
                verseFile != null && !fileUtils.createFile(verseFile) &&
                verseTranslFile != null && !fileUtils.createFile(verseTranslFile)
            ) {
                continue
            }

            loadVerse(model, translModel, chapterNo, verseToLoad, object : RecitationPlayerVerseLoadCallback(null) {
                override fun onFailed(
                    e: Throwable?,
                    verseFile: File?,
                    verseTranslFile: File?,
                    deleteVerseFile: Boolean,
                    deleteTranslFile: Boolean
                ) {
                    e?.printStackTrace()
                    if (deleteVerseFile) verseFile?.delete()
                    if (deleteTranslFile) verseTranslFile?.delete()
                }
            })
        }
    }

    override fun getCurrentContentTitle(player: Player): String {
        val chapterName = quranMeta?.getChapterName(this, recParams.currentChapterNo) ?: ""

        return getString(
            R.string.strLabelVerseSerialWithChapter,
            chapterName,
            recParams.currentChapterNo,
            recParams.currentVerseNo
        )
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        Intent(this, ActivityReader::class.java).apply {
            putExtra(Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, true)
            return PendingIntent.getActivity(this@RecitationService, 0, this, flag)
        }
    }

    override fun getCurrentContentText(player: Player): String? {

        return when (recParams.currentAudioOption) {
            RecitationUtils.AUDIO_OPTION_BOTH -> {
                val reciterName = RecitationManager.getReciterName(recParams.currentReciter)
                val translReciterName = RecitationManager.getTranslationReciterName(recParams.currentTranslationReciter)
                "${reciterName ?: ""} & ${translReciterName ?: ""}"
            }

            RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION -> {
                RecitationManager.getTranslationReciterName(recParams.currentTranslationReciter)
            }

            else -> {
                RecitationManager.getReciterName(recParams.currentReciter)
            }
        }
    }

    override fun getCurrentLargeIcon(player: Player, callback: BitmapCallback): Bitmap {
        return drawable(R.drawable.dr_quran_wallpaper).toBitmap()
    }

    fun cancelLoading() {
        verseLoader.cancelAll()
    }

    fun popMiniMsg(msg: String, duration: Int) {
        MessageUtils.showRemovableToast(this, msg, duration)
    }

}