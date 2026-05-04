package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwAudioTimingEntity
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.extensions.isGzip
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository.AUDIO_ID
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

object WbwAudioRepository {
    private const val DIR_NAME = "wbw_audio"

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME
    )

    private const val AUDIO_ID: String = "wbw_a1"

    private const val TIMING_URL =
        "ghraw://AlfaazPlus/QuranAppInventory/master/wbw_timings/wbw_a1.json.gz"

    private const val AUDIO_URL_TEMPLATE =
        "https://github.com/dabatase/wbw_a1/releases/download/v1/{chapNo:%03d}.webm"


    private val timingLoadMutex = Mutex()

    fun migrateLegacyData(appContext: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val dir = File(appContext.cacheDir, "wbw_audio")

            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    private fun getRootDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, ROOT_DIR_PATH)

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun chapterAudioFile(context: Context, chapterNo: Int): File {
        return File(getRootDir(context), StringUtils.formatInvariant("%03d.webm", chapterNo))
    }

    fun prepareChapterAudioUrl(chapterNo: Int): String? {
        var url = AUDIO_URL_TEMPLATE

        return try {
            var matcher = RecitationUtils.URL_CHAPTER_PATTERN.matcher(url)

            while (matcher.find()) {
                val group = matcher.group(1)

                if (group != null) {
                    url = matcher.replaceFirst(StringUtils.formatInvariant(group, chapterNo))
                    matcher.reset(url)
                }
            }

            url
        } catch (e: Exception) {
            Log.saveError(e, "WbwAudioRepository.prepareChapterAudioUrl")
            null
        }
    }

    suspend fun getTimingCount(context: Context): Int =
        withContext(Dispatchers.IO) {
            DatabaseProvider.getExternalQuranDatabase(context.applicationContext)
                .wbwDao()
                .getTimingCount(AUDIO_ID)
        }

    suspend fun resolveChapterAudioUri(context: Context, chapterNo: Int): Uri? =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext

            val local = chapterAudioFile(app, chapterNo)

            if (local.exists() && local.length() > 0L) {
                return@withContext local.toUri()
            }

            if (!NetworkStateReceiver.isNetworkConnected(app)) {
                return@withContext null
            }

            val url = prepareChapterAudioUrl(chapterNo) ?: return@withContext null

            url.toUri()
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

        return DatabaseProvider.getExternalQuranDatabase(context.applicationContext)
            .wbwDao()
            .getWordTiming(AUDIO_ID, ayahId, wordIndex)
    }

    suspend fun ensureTimingsAvailable(context: Context) {
        ensureTimingsInDb(context.applicationContext)
    }

    suspend fun clearImportedTimings(context: Context) {
        withContext(Dispatchers.IO) {
            DatabaseProvider.getExternalQuranDatabase(context.applicationContext)
                .wbwDao()
                .deleteTimingByAudioId(AUDIO_ID)
        }
    }

    /**
     * @return true if timing rows exist for [AUDIO_ID] after any required download/import attempt.
     */
    private suspend fun ensureTimingsInDb(appContext: Context): Boolean {
        val db = DatabaseProvider.getExternalQuranDatabase(appContext)
        val dao = db.wbwDao()

        if (dao.getTimingCount(AUDIO_ID) > 0) return true

        return timingLoadMutex.withLock {
            if (db.wbwDao().getTimingCount(AUDIO_ID) > 0) return@withLock true
            if (!NetworkStateReceiver.isNetworkConnected(appContext)) return@withLock false

            downloadAndImportTimings(appContext)

            db.wbwDao().getTimingCount(AUDIO_ID) > 0
        }
    }

    private suspend fun downloadAndImportTimings(appContext: Context) {
        val tmp = runCatching { downloadTimingToTemp(appContext) }
            .onFailure { Log.saveError(it, "WbwAudioRepository.downloadTimingToTemp") }
            .getOrNull() ?: return

        try {
            importTimingFromFile(appContext, tmp)
        } catch (e: Exception) {
            Log.saveError(e, "WbwAudioRepository.importTimingFromFile")
        } finally {
            tmp.delete()
        }
    }

    private suspend fun downloadTimingToTemp(appContext: Context): File =
        withContext(Dispatchers.IO) {
            val dest = File(appContext.cacheDir, "wbw_timing_${System.currentTimeMillis()}.tmp")

            val response = fetchInventoryStreamingResponse(TIMING_URL)

            if (!response.isSuccessful) {
                throw IOException("WBW timing download failed: HTTP ${response.code()}")
            }

            val body = response.body() ?: throw IOException("WBW timing body is null")

            body.byteStream().use { input ->
                dest.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            dest
        }

    private suspend fun importTimingFromFile(appContext: Context, file: File) =
        withContext(Dispatchers.IO) {
            val INSERT_CHUNK = 750

            val db = DatabaseProvider.getExternalQuranDatabase(appContext)
            val dao = db.wbwDao()

            val rawStream = if (file.isGzip()) {
                GZIPInputStream(file.inputStream().buffered())
            } else {
                file.inputStream().buffered()
            }

            rawStream.use { raw ->
                JsonReader(InputStreamReader(raw, StandardCharsets.UTF_8)).use { reader ->
                    db.withTransaction {
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
