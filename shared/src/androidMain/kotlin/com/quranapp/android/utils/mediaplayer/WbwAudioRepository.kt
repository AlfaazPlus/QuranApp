package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import androidx.core.net.toUri
import androidx.room3.withWriteTransaction
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwAudioTimingEntity
import com.quranapp.android.locale.toFormatted
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.platform.toInputStream
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.isGzip
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

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
            AppFiles.deleteRecursively(PlatformServices.files.cacheDirectory / "wbw_audio")
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
        var url = AUDIO_URL_TEMPLATE

        return try {
            var matcher = RecitationUtils.URL_CHAPTER_PATTERN.matcher(url)

            while (matcher.find()) {
                val group = matcher.group(1)

                if (group != null) {
                    url = matcher.replaceFirst(group.toFormatted(chapterNo))
                    matcher.reset(url)
                }
            }

            url
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioRepository.prepareChapterAudioUrl")
            null
        }
    }

    suspend fun getTimingCount(context: Context): Int =
        withContext(Dispatchers.IO) {
            DatabaseProvider.getExternalQuranDatabase()
                .wbwDao()
                .getTimingCount(AUDIO_ID)
        }

    suspend fun resolveChapterAudioUri(chapterNo: Int): Uri? =
        withContext(Dispatchers.IO) {
            val local = getChapterAudioFile(chapterNo)

            if (hasValidChapterAudioFile(local)) {
                return@withContext local.toFile().toUri()
            }

            if (!PlatformServices.networkMonitor.isConnected) {
                return@withContext null
            }

            val url = prepareChapterAudioUrl(chapterNo) ?: return@withContext null

            url.toUri()
        }

    suspend fun resolveWordPlaybackSource(
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ): WbwWordPlaybackSource? = withContext(Dispatchers.IO) {
        val local = getChapterAudioFile(chapterNo)

        if (hasValidChapterAudioFile(local)) {
            return@withContext WbwWordPlaybackSource.Chapter(local.toFile().toUri())
        }

        if (!PlatformServices.networkMonitor.isConnected) {
            return@withContext null
        }

        val oneOffUrl = prepareOneOffWordAudioUrl(chapterNo, verseNo, appWordIndex)
            ?: return@withContext null

        WbwWordPlaybackSource.OneOff(oneOffUrl.toUri())
    }

    suspend fun getWordTiming(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        wordIndex: Int,
    ): WbwAudioTimingEntity? {
        if (!ensureTimingsInDb(context.applicationContext)) {
            return null
        }

        val ayahId = chapterNo * 1000 + verseNo

        return DatabaseProvider.getExternalQuranDatabase()
            .wbwDao()
            .getWordTiming(AUDIO_ID, ayahId, wordIndex)
    }

    suspend fun ensureTimingsAvailable(context: Context) {
        ensureTimingsInDb(context.applicationContext)
    }

    /**
     * Intended when the published WBW audio timings version increases.
     */
    suspend fun refreshTimingsFromRemote(context: Context) {
        val app = context.applicationContext
        timingLoadMutex.withLock {
            if (!PlatformServices.networkMonitor.isConnected) return@withLock
            downloadAndImportTimings(app)
        }
    }

    /**
     * @return true if timing rows exist for [AUDIO_ID] after any required download/import attempt.
     */
    private suspend fun ensureTimingsInDb(appContext: Context): Boolean {
        val db = DatabaseProvider.getExternalQuranDatabase()
        val dao = db.wbwDao()

        if (dao.getTimingCount(AUDIO_ID) > 0) return true

        return timingLoadMutex.withLock {
            if (db.wbwDao().getTimingCount(AUDIO_ID) > 0) return@withLock true
            if (!PlatformServices.networkMonitor.isConnected) return@withLock false

            downloadAndImportTimings(appContext)

            db.wbwDao().getTimingCount(AUDIO_ID) > 0
        }
    }

    private suspend fun downloadAndImportTimings(appContext: Context) {
        val tmp = try {
            downloadTimingToTemp()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.saveError(e, "WbwAudioRepository.downloadTimingToTemp")
            return
        }

        try {
            importTimingFromFile(appContext, tmp)
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
                PlatformServices.files.cacheDirectory / "wbw_timing_${System.currentTimeMillis()}.tmp"

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

    private suspend fun importTimingFromFile(appContext: Context, file: Path) =
        withContext(Dispatchers.IO) {
            val INSERT_CHUNK = 750

            val db = DatabaseProvider.getExternalQuranDatabase()
            val dao = db.wbwDao()

            val source = FileSystem.SYSTEM.source(file)
            val rawStream = if (file.isGzip()) {
                GZIPInputStream(source.toInputStream())
            } else {
                source.toInputStream()
            }

            rawStream.use { raw ->
                JsonReader(InputStreamReader(raw, StandardCharsets.UTF_8)).use { reader ->
                    db.withWriteTransaction {
                        dao.deleteTimingByAudioId(AUDIO_ID)

                        val buffer = ArrayList<WbwAudioTimingEntity>(INSERT_CHUNK)

                        reader.beginObject()

                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            reader.beginArray()

                            val startMs = reader.nextLong()
                            val endMs = reader.nextLong()

                            reader.endArray()

                            val triple = parseTimingKey(key) ?: continue

                            val (chapterNo, verseNo, wordIdx) = triple

                            if (endMs <= startMs || startMs < 0L) continue

                            val ayahId = chapterNo * 1000 + verseNo

                            buffer.add(
                                WbwAudioTimingEntity(
                                    audioId = AUDIO_ID,
                                    ayahId = ayahId,
                                    wordIndex = wordIdx,
                                    startMillis = startMs,
                                    endMillis = endMs,
                                ),
                            )

                            if (buffer.size >= INSERT_CHUNK) {
                                dao.upsertTimings(ArrayList(buffer))

                                buffer.clear()
                            }
                        }

                        reader.endObject()

                        if (buffer.isNotEmpty()) {
                            dao.upsertTimings(buffer)
                        }
                    }
                }
            }
        }

    /**
     * Parses timing map keys shaped like "1_1_0" -> (chapter, verse, wordIndex).
     */
    private fun parseTimingKey(key: String): Triple<Int, Int, Int>? {
        val parts = key.split('_')
        if (parts.size < 3) return null

        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        val wordIndex = parts[2].toIntOrNull() ?: return null

        if (chapter <= 0 || verse <= 0) return null

        return Triple(chapter, verse, wordIndex)
    }
}

sealed interface WbwWordPlaybackSource {
    data class Chapter(val uri: Uri) : WbwWordPlaybackSource
    data class OneOff(val uri: Uri) : WbwWordPlaybackSource
}
