package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.extensions.isGzip
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.workers.RecitationAudioDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPInputStream

class RecitationAudioRepository(private val context: Context) {
    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 4096
    }

    private val fileUtils = FileUtils.newInstance(context)
    private val modelManager = RecitationModelManager.getInstance(context)
    private val workManager = WorkManager.getInstance(context)

    fun cancelAll() {
        workManager.cancelAllWorkByTag(RecitationAudioDownloadWorker.TAG)
    }


    private sealed class TimingParseResult {
        data class Found(val metadata: ChapterTimingMetadata) : TimingParseResult()
        data object ChapterMissing : TimingParseResult()
        data object ParseFailed : TimingParseResult()
    }

    /**
     * Resolves chapter audio and timing. Each resource is handled on its own:
     * - Audio: uses local file if present; otherwise downloads (requires network).
     * - Timing: uses cache if valid; otherwise downloads when online (timingMetadata may be null).
     *
     * Missing audio still fails the flow when that track is required; missing timing never fails.
     */
    fun resolveAudioUris(
        chapterNo: Int,
    ): Flow<ResolvedAudioResult> = flow {
        val audioOption = SPReader.getRecitationAudioOption(context)

        val (quranModel, translationModel) = modelManager.resolveModels()

        val shouldPlayArabic = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_ARABIC

        val failed = when {
            shouldPlayArabic && quranModel == null -> true
            shouldPlayTranslation && translationModel == null -> true
            else -> false
        }

        if (failed) {
            emit(ResolvedAudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        val quranFile =
            quranModel?.let { modelManager.getRecitationAudioFile(it.id, chapterNo) }
        val translationFile =
            translationModel?.let { modelManager.getRecitationAudioFile(it.id, chapterNo) }

        val quranNeedsDownload =
            shouldPlayArabic && quranFile != null && quranFile.length() == 0L
        val translationNeedsDownload =
            shouldPlayTranslation && translationFile != null && translationFile.length() == 0L

        if (quranNeedsDownload || translationNeedsDownload) {
            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                emit(ResolvedAudioResult.Error(NoInternetException()))
                return@flow
            }

            val quranFileCreated = quranFile == null || fileUtils.createFile(quranFile)
            val translationFileCreated =
                translationFile == null || fileUtils.createFile(translationFile)

            if (!quranFileCreated || !translationFileCreated) {
                emit(ResolvedAudioResult.Error(IllegalStateException("Failed to create audio files")))
                return@flow
            }

            emit(ResolvedAudioResult.Downloading(0))

            var deleteQuran = false
            var deleteTranslation = false

            try {
                if (quranNeedsDownload) {
                    val url = prepareAudioUrl(quranModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")
                    try {
                        downloadAudioWithWorker(
                            reciterId = quranModel.id,
                            chapterNo = chapterNo,
                            url = url,
                            destinationFile = quranFile,
                            title = "Downloading recitation",
                            subtitle = quranModel.reciter,
                        )
                    } catch (e: Exception) {
                        deleteQuran = true
                        throw e
                    }
                }

                if (translationNeedsDownload) {
                    val url = prepareAudioUrl(translationModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare translation audio URL")
                    try {
                        downloadAudioWithWorker(
                            reciterId = translationModel.id,
                            chapterNo = chapterNo,
                            url = url,
                            destinationFile = translationFile,
                            title = "Downloading recitation translation",
                            subtitle = translationModel.reciter,
                        )
                    } catch (e: Exception) {
                        deleteTranslation = true
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.saveError(e, "RecitationAudioRepository.resolveAudioUris - audio download")

                if (deleteQuran) quranFile?.delete()
                if (deleteTranslation) translationFile?.delete()

                emit(ResolvedAudioResult.Error(e))

                return@flow
            }
        }

        val (quranTiming, translationTiming) = coroutineScope {
            val q = async {
                resolveChapterTimingMetadata(
                    if (shouldPlayArabic) quranModel else null,
                    chapterNo,
                )
            }
            val t = async {
                resolveChapterTimingMetadata(
                    if (shouldPlayTranslation) translationModel else null,
                    chapterNo,
                )
            }
            q.await() to t.await()
        }

        emit(
            ResolvedAudioResult.Resoved(
                chapter = chapterNo,
                quran = quranFile?.let { file ->
                    RecitationAudioTrack(
                        kind = RecitationAudioKind.QURAN,
                        chapterNo = chapterNo,
                        reciterId = quranModel.id,
                        audioUri = file.toUri(),
                        timingMetadata = quranTiming,
                    )
                },
                translation = translationFile?.let { file ->
                    RecitationAudioTrack(
                        kind = RecitationAudioKind.TRANSLATION,
                        chapterNo = chapterNo,
                        reciterId = translationModel.id,
                        audioUri = file.toUri(),
                        timingMetadata = translationTiming,
                    )
                },
            ),
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadAudioWithWorker(
        reciterId: String,
        chapterNo: Int,
        url: String,
        destinationFile: File,
        title: String,
        subtitle: String?,
    ) {
        val workName = RecitationAudioDownloadWorker.uniqueWorkName(reciterId, chapterNo)
        val request = OneTimeWorkRequestBuilder<RecitationAudioDownloadWorker>()
            .setInputData(
                RecitationAudioDownloadWorker.inputData(
                    url = url,
                    outputPath = destinationFile.absolutePath,
                    title = title,
                    subtitle = subtitle,
                )
            )
            .addTag(RecitationAudioDownloadWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
        awaitWorkCompletion(workName)
    }

    private suspend fun awaitWorkCompletion(workName: String) {
        workManager.getWorkInfosForUniqueWorkLiveData(workName).asFlow()
            .mapNotNull { infos -> infos.firstOrNull() }
            .first { info ->
                when (info.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.BLOCKED -> false

                    WorkInfo.State.SUCCEEDED -> true
                    WorkInfo.State.FAILED -> {
                        val message =
                            info.outputData.getString(RecitationAudioDownloadWorker.KEY_ERROR)
                                ?: "Download failed"
                        throw IllegalStateException(message)
                    }

                    WorkInfo.State.CANCELLED -> {
                        throw IllegalStateException("Download cancelled")
                    }
                }
            }
    }

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase?,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        if (model == null) return@withContext null

        var timingUrl = model.timingUrl?.trim().orEmpty()

        if (timingUrl.isEmpty()) return@withContext null

        val cacheFile = modelManager.getRecitationTimingFile(model.id)

        if (cacheFile.length() > 0L) {
            try {
                when (val parsed = parseTimingFile(cacheFile.inputStream().buffered(), chapterNo)) {
                    is TimingParseResult.Found -> return@withContext parsed.metadata
                    TimingParseResult.ChapterMissing -> return@withContext null
                    TimingParseResult.ParseFailed -> cacheFile.delete()
                }
            } catch (e: Exception) {
                Log.saveError(
                    e,
                    "RecitationAudioRepository.resolveChapterTimingMetadata - read cache"
                )
                cacheFile.delete()
            }
        }

        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            return@withContext null
        }

        try {
            val tempFile =
                File(context.cacheDir, "timing_temp_${System.currentTimeMillis()}")

            // If timing url start will ghraw://, the use user preferred ghraw mirror url,
            // otherwise use the url as is (which may also be a full ghraw url or a direct link to timing file)
            if (timingUrl.startsWith("ghraw://")) {
                val root = DownloadSourceUtils.getDownloadSourceRoot(context)
                val relativePath = timingUrl.removePrefix("ghraw://")
                timingUrl = root + relativePath
            }

            downloadFile(tempFile, timingUrl)

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext null
            }

            // It maybe .json.gz or .json; handle gzipped case when needed
            val contentStream: InputStream = try {
                if (tempFile.isGzip()) {
                    GZIPInputStream(tempFile.inputStream().buffered())
                } else {
                    tempFile.inputStream().buffered()
                }
            } catch (e: Exception) {
                tempFile.inputStream().buffered()
            }

            tempFile.delete()

            when (val parsed = parseTimingFile(contentStream, chapterNo)) {
                is TimingParseResult.Found -> {
                    if (fileUtils.createFile(cacheFile)) {
                        contentStream.use { input ->
                            cacheFile.outputStream().buffered().use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                    }

                    return@withContext parsed.metadata
                }

                TimingParseResult.ChapterMissing -> {
                    Log.saveError(
                        Exception("Timing JSON had no entry for chapter $chapterNo"),
                        "RecitationAudioRepository.resolveTimingFromCacheOrNetwork",
                    )
                    return@withContext null
                }

                TimingParseResult.ParseFailed -> return@withContext null
            }
        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.resolveTimingFromCacheOrNetwork - download")
            return@withContext null
        }
    }

    private fun prepareAudioUrl(urlTemplate: String, chapterNo: Int): String? {
        var url = urlTemplate
        return try {
            var matcher = RecitationUtils.URL_CHAPTER_PATTERN.matcher(url)
            while (matcher.find()) {
                val group = matcher.group(1)
                if (group != null) {
                    url = matcher.replaceFirst(String.format(Locale.ENGLISH, group, chapterNo))
                    matcher.reset(url)
                }
            }

            url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses only the requested chapter from the timing JSON, avoiding full
     * deserialization of all 114 chapters and their verse/segment data.
     */
    private fun parseTimingFile(contentStream: InputStream, chapterNo: Int): TimingParseResult {
        try {
            val root = contentStream.use { stream ->
                JsonHelper.json.parseToJsonElement(
                    stream.bufferedReader().readText()
                ).jsonObject
            }

            val versesArray = root["verses"]?.jsonArray ?: return TimingParseResult.ParseFailed

            for (element in versesArray) {
                val obj = element.jsonObject
                val chapter = obj["chapter"]?.jsonPrimitive?.intOrNull ?: continue
                if (chapter == chapterNo) {
                    val metadata = JsonHelper.json.decodeFromJsonElement<ChapterTimingMetadata>(obj)
                    return TimingParseResult.Found(metadata)
                }
            }

            return TimingParseResult.ChapterMissing
        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.parseTimingFile")
            return TimingParseResult.ParseFailed
        }
    }

    private suspend fun downloadFile(
        file: File,
        urlStr: String,
        onProgress: (Int) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection

        conn.setRequestProperty("Content-Length", "0")
        conn.setRequestProperty("Connection", "close")
        conn.connectTimeout = 180000
        conn.readTimeout = 180000
        conn.allowUserInteraction = false

        conn.connect()

        if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw HttpNotFoundException()
        }

        val totalLength: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conn.contentLengthLong
        } else {
            conn.contentLength.toLong()
        }

        conn.inputStream.buffered(DOWNLOAD_BUFFER_SIZE).use { input ->
            file.outputStream().buffered(DOWNLOAD_BUFFER_SIZE).use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var totalConsumed = 0L

                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes <= 0) break

                    output.write(buffer, 0, bytes)
                    totalConsumed += bytes

                    if (totalLength > 0) {
                        onProgress((totalConsumed * 100 / totalLength).toInt())
                    }
                }

                output.flush()
            }
        }
    }
}
