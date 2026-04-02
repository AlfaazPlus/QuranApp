package com.quranapp.android.utils.mediaplayer

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.Log
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
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

class RecitationAudioRepository(private val context: Context) {
    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 4096
    }

    private val fileUtils = FileUtils.newInstance(context)
    private val modelManager = RecitationModelManager.get(context)
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
        val audioOption = RecitationPreferences.getRecitationAudioOption()

        val (quranModel, translationModel) = modelManager.resolveModels()

        val shouldPlayArabic = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_QURAN

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

            emit(ResolvedAudioResult.Downloading(0))

            try {
                if (quranNeedsDownload) {
                    val url = prepareAudioUrl(quranModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")

                    downloadAudioWithWorker(
                        reciterId = quranModel.id,
                        chapterNo = chapterNo,
                        url = url,
                        destinationFile = quranFile,
                        title = "Downloading recitation",
                        subtitle = quranModel.reciter,
                    )
                }

                if (translationNeedsDownload) {
                    val url = prepareAudioUrl(translationModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare translation audio URL")

                    downloadAudioWithWorker(
                        reciterId = translationModel.id,
                        chapterNo = chapterNo,
                        url = url,
                        destinationFile = translationFile,
                        title = "Downloading recitation translation",
                        subtitle = translationModel.reciter,
                    )
                }
            } catch (e: Exception) {
                Log.saveError(e, "RecitationAudioRepository.resolveAudioUris - audio download")
                emit(ResolvedAudioResult.Error(e))
                return@flow
            }
        }

        val (quranTiming, translationTiming) = coroutineScope {
            val q = async {
                quranModel?.let {
                    resolveChapterTimingMetadata(
                        it,
                        chapterNo,
                    )
                }
            }
            val t = async {
                translationModel?.let {
                    resolveChapterTimingMetadata(
                        it,
                        chapterNo,
                    )
                }
            }

            q.await() to t.await()
        }

        Log.d(
            "ChapterTiming",
            "Resolved timings for chapter $chapterNo - quran: $quranTiming, translation: $translationTiming"
        )

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
        model: RecitationModelBase,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
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

            downloadTimingMetadata(tempFile, timingUrl)

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext null
            }

            // Decompress to a separate temp file so nothing is held in memory
            val contentFile =
                File(context.cacheDir, "timing_content_${System.currentTimeMillis()}")

            try {
                val input: InputStream = try {
                    if (tempFile.isGzip()) {
                        GZIPInputStream(tempFile.inputStream().buffered())
                    } else {
                        tempFile.inputStream().buffered()
                    }
                } catch (e: Exception) {
                    tempFile.inputStream().buffered()
                }

                input.use { src ->
                    contentFile.outputStream().buffered().use { dst -> src.copyTo(dst) }
                }
            } finally {
                tempFile.delete()
            }

            try {
                when (val parsed =
                    parseTimingFile(contentFile.inputStream().buffered(), chapterNo)) {
                    is TimingParseResult.Found -> {
                        if (fileUtils.createFile(cacheFile)) {
                            contentFile.inputStream().buffered().use { src ->
                                cacheFile.outputStream().buffered().use { dst ->
                                    src.copyTo(dst)
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
            } finally {
                contentFile.delete()
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

            val chaptersArray = root["chapters"]?.jsonArray ?: return TimingParseResult.ParseFailed

            for (element in chaptersArray) {
                val chapterObj = element.jsonObject
                val chapter = chapterObj["chapter"]?.jsonPrimitive?.intOrNull ?: continue

                if (chapter == chapterNo) {
                    val metadata = JsonHelper.json.decodeFromJsonElement<ChapterTimingMetadata>(
                        chapterObj
                    )
                    return TimingParseResult.Found(metadata)
                }
            }

            return TimingParseResult.ChapterMissing
        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.parseTimingFile")
            return TimingParseResult.ParseFailed
        }
    }

    /**
     * `ghraw://` is stripped to a relative path and fetched via GithubLikeApi (mirror root from user settings).
     * Any other string is treated as a full URL and fetched via AnyApi.
     */
    private suspend fun downloadTimingMetadata(
        file: File,
        timingUrl: String,
    ) = withContext(Dispatchers.IO) {
        val response = if (timingUrl.startsWith("ghraw://")) {
            RetrofitInstance.githubLike.getRecitationTimingMetadata(
                timingUrl.removePrefix("ghraw://").trimStart('/')
            )
        } else {
            RetrofitInstance.any.downloadStreaming(timingUrl)
        }

        if (!response.isSuccessful) {
            if (response.code() == 404) throw HttpNotFoundException()
            throw IOException("Timing metadata download failed: HTTP ${response.code()}")
        }

        val body = response.body()
            ?: throw IOException("Timing metadata response body is null")

        body.byteStream().use { input ->
            file.outputStream().buffered(DOWNLOAD_BUFFER_SIZE).use { output ->
                input.copyTo(output, DOWNLOAD_BUFFER_SIZE)
                output.flush()
            }
        }
    }
}
