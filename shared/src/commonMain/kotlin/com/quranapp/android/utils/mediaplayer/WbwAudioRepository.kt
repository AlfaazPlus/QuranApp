package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwAudioTimingEntity
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.toFormatted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path

object WbwAudioRepository {
    private const val DIR_NAME = "wbw_audio"

    const val AUDIO_ID = "wbw_a1"

    private const val TIMING_URL =
        "ghraw://AlfaazPlus/QuranAppInventory/master/wbw_timings/wbw_a1.json.gz"

    private const val AUDIO_URL_TEMPLATE =
        "https://github.com/dabatase/wbw_a1/releases/download/v1/{chapNo:%03d}.webm"

    const val ONE_OFF_AUDIO_URL_BASE =
        "https://audio.qurancdn.com/wbw/"

    private val timingLoadMutex = Mutex()

    fun migrateLegacyData() {
        CoroutineScope(Dispatchers.IO).launch {
            AppFiles.deleteRecursively(Platform.files.cacheDirectory / "wbw_audio")
        }
    }

    private fun getRootDir(): Path = AppFiles.savedDir(DIR_NAME)

    fun getChapterAudioFile(chapterNo: Int): Path {
        return getRootDir() / "%03d.webm".toFormatted(chapterNo)
    }

    private fun hasValidChapterAudioFile(file: Path): Boolean {
        return AppFiles.size(file) > 0L
    }

    fun isChapterAudioDownloaded(chapterNo: Int): Boolean {
        return hasValidChapterAudioFile(getChapterAudioFile(chapterNo))
    }

    fun prepareOneOffWordAudioUrl(
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): String? {
        if (chapterNo <= 0 || verseNo <= 0 || appWordIndex < 0) {
            return null
        }

        val sourceWordIndex = appWordIndex + 1
        val fileName = "%03d_%03d_%03d.mp3".toFormatted(
            chapterNo,
            verseNo,
            sourceWordIndex,
        )
        return ONE_OFF_AUDIO_URL_BASE + fileName
    }

    suspend fun deleteChapterAudio(chapterNo: Int) =
        withContext(Dispatchers.IO) {
            AppFiles.delete(getChapterAudioFile(chapterNo))
        }

    fun prepareChapterAudioUrl(chapterNo: Int): String? {
        return RecitationUtils.prepareAudioUrl(AUDIO_URL_TEMPLATE, chapterNo)
    }

    suspend fun getTimingCount(): Int =
        withContext(Dispatchers.IO) {
            DatabaseProvider.getExternalQuranDatabase()
                .wbwDao()
                .getTimingCount(AUDIO_ID)
        }

    suspend fun resolveWordPlaybackSource(
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): WbwWordPlaybackSource? = withContext(Dispatchers.IO) {
        val local = getChapterAudioFile(chapterNo)

        if (hasValidChapterAudioFile(local)) {
            return@withContext WbwWordPlaybackSource.Chapter(local.toString())
        }

        if (!Platform.networkMonitor.isConnected) {
            return@withContext null
        }

        val oneOffUrl = prepareOneOffWordAudioUrl(chapterNo, verseNo, appWordIndex)
            ?: return@withContext null

        WbwWordPlaybackSource.OneOff(oneOffUrl)
    }

    suspend fun getWordTiming(
        chapterNo: Int,
        verseNo: Int,
        wordIndex: Int,
    ): WbwAudioTimingEntity? {
        if (!ensureTimingsInDb()) {
            return null
        }

        val ayahId = chapterNo * 1000 + verseNo

        return DatabaseProvider.getExternalQuranDatabase()
            .wbwDao()
            .getWordTiming(AUDIO_ID, ayahId, wordIndex)
    }

    suspend fun ensureTimingsAvailable() {
        ensureTimingsInDb()
    }

    suspend fun refreshTimingsFromRemote() {
        timingLoadMutex.withLock {
            if (!Platform.networkMonitor.isConnected) return@withLock
            downloadAndImportTimings()
        }
    }

    private suspend fun ensureTimingsInDb(): Boolean {
        val db = DatabaseProvider.getExternalQuranDatabase()
        val dao = db.wbwDao()

        if (dao.getTimingCount(AUDIO_ID) > 0) return true

        return timingLoadMutex.withLock {
            if (db.wbwDao().getTimingCount(AUDIO_ID) > 0) return@withLock true
            if (!Platform.networkMonitor.isConnected) return@withLock false

            downloadAndImportTimings()

            db.wbwDao().getTimingCount(AUDIO_ID) > 0
        }
    }

    private suspend fun downloadAndImportTimings() {
        val tmp = try {
            downloadTimingToTemp()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioRepository.downloadTimingToTemp")
            return
        }

        try {
            WbwTimingImporter.importTimingFromFile(tmp)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioRepository.importTimingFromFile")
        } finally {
            AppFiles.delete(tmp)
        }
    }

    private suspend fun downloadTimingToTemp(): Path =
        withContext(Dispatchers.IO) {
            val dest =
                Platform.files.cacheDirectory / "wbw_timing_${System.currentTimeMillis()}.tmp"

            try {
                val response = fetchInventoryStreamingResponse(TIMING_URL)

                response.downloadTo(
                    dest,
                    onFailed = {
                        throw IllegalStateException("WBW timing download failed: HTTP ${it.code}")
                    },
                )

                dest
            } catch (e: Exception) {
                AppFiles.delete(dest)
                throw e
            }
        }
}

sealed interface WbwWordPlaybackSource {
    val uriString: String

    data class Chapter(override val uriString: String) : WbwWordPlaybackSource
    data class OneOff(override val uriString: String) : WbwWordPlaybackSource
}
