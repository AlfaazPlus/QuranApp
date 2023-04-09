package com.quranapp.android.utils.services

import android.app.Service

class RecitationPlayerService/* : Service()*/ {
    /*
    companion object {
        const val MILLIS_MULTIPLIER = 100
        const val ACTION_SEEK_LEFT = -1
        const val ACTION_SEEK_RIGHT = 1
        const val NOTIF_ID = 55

        fun startPlayerService(wrapper: ContextWrapper) {
            ContextCompat.startForegroundService(wrapper, Intent(wrapper, RecitationPlayerService::class.java))
        }
    }

    private val binder = LocalBinder()
    val recParams = RecitationPlayerParams()
    private var player: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    var session: MediaSessionCompat? = null
    private val playbackStateBuilder = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_SEEK_TO
    )
    lateinit var fileUtils: FileUtils

    private val notifHelper = RecitationNotificationHelper(this)

    private var progressHandler: Handler? = null
    private var activity: ActivityReader? = null
    private var quranMeta: QuranMeta? = null
    var recPlayer: RecitationPlayer? = null
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

        sync()
    }

    override fun onCreate() {
        super.onCreate()

        fileUtils = FileUtils.newInstance(this)
        session = MediaSessionCompat(this, "RecitationPlayer").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playControl()
                }

                override fun onPause() {
                    playControl()
                }

                override fun onSeekTo(pos: Long) {
                    seek(pos.toInt())
                }

                override fun onSkipToPrevious() {
                    recitePreviousVerse()
                }

                override fun onSkipToNext() {
                    reciteNextVerse()
                }
            })
        }

        startForeground(
            NOTIF_ID,
            NotificationUtils.createEmptyNotif(this, getString(R.string.strNotifChannelIdRecitation))
        )

        updateRepeatMode(SPReader.getRecitationRepeatVerse(this))
        updateContinuePlaying(SPReader.getRecitationContinueChapter(this))
        updateVerseSync(SPReader.getRecitationScrollSync(this), false)

        RecitationManager.prepare(this, false) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(session, intent)

        return START_STICKY
    }

    fun destroy() {
        release()

        verseLoader.cancelAll()
        session?.release()

        finish()
    }

    fun release() {
        setSessionState(PlaybackStateCompat.STATE_STOPPED)
        session?.isActive = false

        try {
            player?.let {
                it.stop()
                it.reset()
                it.release()
                player = null
            }

            nextPlayer?.let {
                it.stop()
                it.reset()
                it.release()
                nextPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun onChapterChanged(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        recParams.currentVerse = Pair(chapterNo, fromVerse)
        recParams.firstVerse = Pair(chapterNo, fromVerse)
        recParams.lastVerse = Pair(chapterNo, toVerse)
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)

        Log.d("onChapterChanged: ${recParams.firstVerse} to ${recParams.lastVerse}")
        session!!.setMetadata(notifHelper.prepareMetadata(quranMeta))
        notifHelper.showNotification(PlaybackStateCompat.ACTION_PAUSE)
    }

    fun onJuzChanged(juzNo: Int, quranMeta: QuranMeta) {
        val (firstChapter, lastChapter) = quranMeta.getChaptersInJuz(juzNo)
        val firstVerse = quranMeta.getVerseRangeOfChapterInJuz(juzNo, firstChapter).first
        val (_, lastVerse) = quranMeta.getVerseRangeOfChapterInJuz(juzNo, lastChapter)
        recParams.currentReciter = SPReader.getSavedRecitationSlug(this)

        recParams.firstVerse = Pair(firstChapter, firstVerse)
        recParams.lastVerse = Pair(lastChapter, lastVerse)
        recParams.currentVerse = recParams.firstVerse

        Log.d("onJuzChanged: ${recParams.firstVerse} to ${recParams.lastVerse}")
        session!!.setMetadata(notifHelper.prepareMetadata(quranMeta))
        notifHelper.showNotification(PlaybackStateCompat.ACTION_PAUSE)
    }

    private fun sync() {
        runAudioProgress()

        recPlayer?.let {
            it.binding.progress.max = ((player?.duration ?: 1) / MILLIS_MULTIPLIER).coerceAtLeast(0)
            it.updateProgressBar()
            it.updateTimelineText()
            it.updateVerseSync(recParams, isPlaying)
            it.setupOnLoadingInProgress(isLoadingInProgress)

            if (player != null && isPlaying) {
                it.onPlayMedia(recParams)
            } else if (player != null) {
                it.onPauseMedia(recParams)
            }
        }
    }

    fun prepareMediaPlayer(
        audioURI: Uri,
        reciter: String,
        chapterNo: Int,
        verseNo: Int
    ) {
        verseLoadCallback.postLoad()
        release()

        player = MediaPlayer.create(this, audioURI)

        if (player == null) {
            popMiniMsg("Something happened wrong while playing the audio", Toast.LENGTH_LONG)
            return
        }

        Log.d("PREPARING HARD")

        player!!.let {
            it.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            it.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            it.setOnInfoListener { _, what, extra ->
                Log.d(what, extra)
                true
            }
            it.setOnPreparedListener { mp ->
                onPreparePlayer(mp, audioURI, reciter, chapterNo, verseNo, true)
            }
        }
    }

    private fun onPreparePlayer(
        player: MediaPlayer,
        audioURI: Uri,
        reciter: String,
        chapterNo: Int,
        verseNo: Int,
        play: Boolean
    ) {
        Log.d("SETTING UP PLAYER ($chapterNo:$verseNo)")

        recParams.lastMediaURI = audioURI
        recParams.currentReciter = reciter
        recParams.currentVerse = Pair(chapterNo, verseNo)

        session!!.isActive = true
        session!!.setMetadata(notifHelper.prepareMetadata(quranMeta))

        prepareNextPlayer(player)

        updateRepeatMode(recParams.repeatVerse)

        recPlayer?.let {
            it.binding.progress.max = player.duration / MILLIS_MULTIPLIER
            it.updateProgressBar()
            it.updateTimelineText()
        }

        playMedia(play)
    }

    private fun onPlayingCompleted() {
        if (nextPlayer != null) {
            player?.release()
            player = nextPlayer
            nextPlayer = null
            return
        }

        Log.d("PLAYING COMPLETED, but next player is null")

        recParams.currentVerseCompleted = true

        release()

        if (quranMeta == null) {
            stopMedia()
            return
        }

        val canContinue = canContinuePlaying()
        recParams.currentRangeCompleted = !canContinue

        if (canContinue) {
            reciteNextVerse()
        } else {
            pauseMedia()
        }
    }

    private fun canContinuePlaying(): Boolean {
        val continueNext = recParams.continueRange && recParams.hasNextVerse(quranMeta!!)

        val verseValid = quranMeta!!.isVerseValid4Chapter(recParams.currentChapterNo, recParams.currentVerseNo + 1)
        val isSingleVerseMode = activity?.mReaderParams?.isSingleVerse ?: false
        val continueNextSingle = recParams.continueRange && isSingleVerseMode && verseValid

        return continueNext || continueNextSingle
    }

    private fun prepareNextPlayer(curPlayer: MediaPlayer) {
        curPlayer.setOnCompletionListener { onPlayingCompleted() }
        curPlayer.setOnErrorListener { _, what, extra ->
            Log.d("CURRENT PLAYER ERROR: $what, $extra")
            return@setOnErrorListener true
        }
        curPlayer.setOnInfoListener { _, what, extra ->
            Log.d("CURRENT PLAYER INFO: $what, $extra")
            return@setOnInfoListener true
        }

        val reciter = recParams.currentReciter

        if (reciter == null || quranMeta == null || !canContinuePlaying()) return

        val (nextChapterNo, nextVerseNo) = recParams.getNextVerse(quranMeta!!)

        val audioFile = fileUtils.getRecitationAudioFile(reciter, nextChapterNo, nextVerseNo)
        if (audioFile.length() == 0L) return

        val audioURI: Uri = fileUtils.getFileURI(audioFile)
        nextPlayer = MediaPlayer.create(this, audioURI)

        Log.d("NEXT VERSE: ($nextChapterNo, $nextVerseNo), $nextPlayer, URI: $audioURI)")

        if (nextPlayer == null) {
            return
        }

        curPlayer.setNextMediaPlayer(nextPlayer)

        nextPlayer!!.let {
            it.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            it.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            it.setOnErrorListener { _, what, extra ->
                Log.d("NEXT PLAYER ERROR: $what, $extra")
                return@setOnErrorListener true
            }
            it.setOnInfoListener { mp, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT) {
                    Log.d("NEXT PLAYER STARTED ($nextChapterNo, $nextVerseNo)")
                    onPreparePlayer(mp, audioURI, reciter, nextChapterNo, nextVerseNo, false)
                }

                return@setOnInfoListener true
            }
        }

    }

    private fun updateRepeatMode(repeat: Boolean) {
        recParams.repeatVerse = repeat
        player?.isLooping = repeat
        SPReader.setRecitationRepeatVerse(this, repeat)
    }

    private fun updateContinuePlaying(continuePlaying: Boolean) {
        nextPlayer = null

        try {
            player?.setNextMediaPlayer(null)
        } catch (e: Exception) {
            // Not initialized yet
        }

        recParams.continueRange = continuePlaying

        if (continuePlaying && player != null) {
            prepareNextPlayer(player!!)
        }

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

    fun playMedia(startPlayer: Boolean = true) {
        Log.d("PLAYING MEDIA: $startPlayer")

        try {
            if (startPlayer) player?.start()
        } catch (_: Exception) {
            // Not initialized yet
        }

        runAudioProgress()

        recParams.currentRangeCompleted = false
        recParams.currentVerseCompleted = false

        setSessionState(PlaybackStateCompat.STATE_PLAYING)

        recPlayer?.onPlayMedia(recParams)

        notifHelper.showNotification(PlaybackStateCompat.ACTION_PLAY)
        ContextCompat.startForegroundService(this, Intent(this, RecitationPlayerService::class.java))

        Log.d("CURRENT: ${recParams.currentVerse} RANGE: ${recParams.firstVerse} to ${recParams.lastVerse}")

        recParams.previouslyPlaying = true
    }

    fun pauseMedia() {
        if (isPlaying) {
            player?.pause()
            progressHandler?.removeCallbacksAndMessages(null)
        }

        setSessionState(PlaybackStateCompat.STATE_PAUSED)

        notifHelper.showNotification(PlaybackStateCompat.ACTION_PAUSE)
        recParams.previouslyPlaying = false

        recPlayer?.onPauseMedia(recParams)
    }

    private fun stopMedia() {
        destroy()
        recPlayer?.onStopMedia(recParams)
    }

    fun playControl() {
        if (recParams.currentRangeCompleted) {
            restartRange()
        } else if (recParams.currentVerseCompleted) {
            restartVerse()
        } else if (player != null) {
            if (isPlaying) {
                pauseMedia()
            } else {
                playMedia()
            }
        }
    }


    fun seek(amountOrDirection: Int) {
        if (player == null || isLoadingInProgress) {
            return
        }

        val p = player!!

        recParams.currentVerseCompleted = false

        val seekAmount = when (amountOrDirection) {
            ACTION_SEEK_RIGHT -> 5000
            ACTION_SEEK_LEFT -> -5000
            else -> amountOrDirection
        }

        val fromBtnClick = amountOrDirection == ACTION_SEEK_LEFT || amountOrDirection == ACTION_SEEK_RIGHT
        var seekFinal = seekAmount

        if (fromBtnClick) {
            seekFinal += p.currentPosition
            seekFinal = seekFinal.coerceAtLeast(0)
            seekFinal = seekFinal.coerceAtMost(p.duration)
        }

        p.seekTo(seekFinal)
        setSessionState(
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING
            else PlaybackStateCompat.STATE_PAUSED,
        )

        recPlayer?.let {
            it.updateTimelineText()

            if (fromBtnClick) {
                it.updateProgressBar()
            }
        }
    }

    private fun runAudioProgress() {
        progressHandler?.removeCallbacksAndMessages(null)

        Log.d("TRYING AUDIO PROGRESS", player, recPlayer, isPlaying)

        if (player == null || recPlayer == null || !isPlaying) return

        Log.d("RUNNING AUDIO PROGRESS")

        progressHandler = runOnInterval({
            if (!isPlaying) {
                progressHandler?.removeCallbacksAndMessages(null)
                return@runOnInterval
            }

            recPlayer?.let {
                it.updateProgressBar()
                it.updateTimelineText()
            }
        }, 100L, true)
    }

    val isPlaying get() = player?.isPlaying ?: false

    val currentPosition get() = player?.currentPosition ?: 0

    val duration get() = player?.duration ?: 0

    fun isReciting(chapterNo: Int, verseNo: Int) = recParams.isCurrentVerse(chapterNo, verseNo) && isPlaying


    fun recitePreviousVerse() {
        if (isLoadingInProgress || quranMeta == null) return

        val chapterNo: Int
        val verseNo: Int

        val previousVerse = recParams.getPreviousVerse(quranMeta!!)
        chapterNo = previousVerse.first
        verseNo = previousVerse.second

        Log.d("PREV VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    fun reciteNextVerse() {
        if (isLoadingInProgress || quranMeta == null) return

        val chapterNo: Int
        val verseNo: Int

        val nextVerse = recParams.getNextVerse(quranMeta!!)
        chapterNo = nextVerse.first
        verseNo = nextVerse.second

        Log.d("NEXT VERSE - $chapterNo:$verseNo")

        reciteVerse(chapterNo, verseNo)
    }

    private fun restartRange() {
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
        if (audioFile.length() > 0) {
            if (verseLoader.isPending(model.slug, chapterNo, verseNo)) {
                pauseMedia()
                verseLoadCallback.preLoad()
                verseLoader.addCallback(model.slug, chapterNo, verseNo, verseLoadCallback)
            } else {
                val audioURI: Uri = fileUtils.getFileURI(audioFile)
                prepareMediaPlayer(audioURI, model.slug, chapterNo, verseNo)
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

    fun setupOnLoadingInProgress(inProgress: Boolean) {
        isLoadingInProgress = inProgress

        recPlayer?.setupOnLoadingInProgress(inProgress)
    }

    fun setRepeat(repeat: Boolean) {
        updateRepeatMode(repeat)
    }

    fun setContinueChapter(continueChapter: Boolean) {
        updateContinuePlaying(continueChapter)
    }

    fun cancelLoading() {
        verseLoader.cancelAll()
    }

    private fun setSessionState(state: Int) {
        var progress = player?.currentPosition?.toLong() ?: 0

        if (state == PlaybackStateCompat.STATE_STOPPED) {
            progress = 0
        }

        session!!.setPlaybackState(
            playbackStateBuilder.setState(
                state,
                progress,
                1.0f
            ).build()
        )
    }

    fun popMiniMsg(msg: String, duration: Int) {
        MessageUtils.showRemovableToast(this, msg, duration)
    }*/
}