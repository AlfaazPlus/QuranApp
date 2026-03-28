package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.mediaplayer.AudioTimingMetadata
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.maangers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class RecitationAudioRepository(private val context: Context) {
    private val fileUtils = FileUtils.newInstance(context)
    private val modelManager = RecitationModelManager.getInstance(context)

    /** Reserved for cancelable downloads; no-op until downloads are job-backed. */
    fun cancelAll() {}

    private val _downloadState =
        MutableStateFlow(ResourceDownloadStatus.Idle)
    val downloadState: StateFlow<ResourceDownloadStatus> = _downloadState.asStateFlow()

    /**
     * Result class for verse-by-verse audio resolution
     */
    sealed class AudioResult {
        data class Success(
            val chapter: Int,
            val quran: RecitationAudioResult?,
            val translation: RecitationAudioResult?,
        ) : AudioResult()

        data class Error(val error: Throwable) : AudioResult()
        data class Downloading(val progress: Int) : AudioResult()
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
    ): Flow<AudioResult> = flow {
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
            emit(AudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
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
                emit(AudioResult.Error(NoInternetException()))
                return@flow
            }

            val quranFileCreated = quranFile == null || fileUtils.createFile(quranFile)
            val translationFileCreated =
                translationFile == null || fileUtils.createFile(translationFile)

            if (!quranFileCreated || !translationFileCreated) {
                emit(AudioResult.Error(IllegalStateException("Failed to create audio files")))
                return@flow
            }

            emit(AudioResult.Downloading(0))

            var deleteQuran = false
            var deleteTranslation = false
            try {
                if (quranNeedsDownload) {
                    val url = prepareAudioUrl(quranModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")
                    try {
                        downloadFile(quranFile, url) { }
                    } catch (e: Exception) {
                        deleteQuran = true
                        throw e
                    }
                }

                if (translationNeedsDownload) {
                    val url = prepareAudioUrl(translationModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare translation audio URL")
                    try {
                        downloadFile(translationFile, url) { }
                    } catch (e: Exception) {
                        deleteTranslation = true
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.saveError(e, "RecitationAudioRepository.resolveAudioUris - audio download")

                if (deleteQuran) quranFile?.delete()
                if (deleteTranslation) translationFile?.delete()

                emit(AudioResult.Error(e))

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
            AudioResult.Success(
                chapter = chapterNo,
                quran = quranFile?.let { file ->
                    RecitationAudioResult(
                        chapterNo = chapterNo,
                        reciterId = quranModel.id,
                        audioUri = file.toUri(),
                        timingMetadata = quranTiming,
                    )
                },
                translation = translationFile?.let { file ->
                    RecitationAudioResult(
                        chapterNo = chapterNo,
                        reciterId = translationModel.id,
                        audioUri = file.toUri(),
                        timingMetadata = translationTiming,
                    )
                },
            ),
        )
    }.flowOn(Dispatchers.IO)

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase?,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        if (model == null) return@withContext null

        val timingUrl = model.timingUrl?.trim().orEmpty()

        if (timingUrl.isEmpty()) return@withContext null

        val cacheFile = modelManager.getRecitationTimingFile(model.id)

        if (cacheFile.length() > 0L) {
            try {
                when (val parsed = parseTimingFile(cacheFile.readText(), chapterNo)) {
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
            val tempFile = File(context.cacheDir, "timing_temp_${System.currentTimeMillis()}.json")
            downloadFile(tempFile, timingUrl)

            val content = tempFile.readText()
            tempFile.delete()

            when (val parsed = parseTimingFile(content, chapterNo)) {
                is TimingParseResult.Found -> {
                    if (fileUtils.createFile(cacheFile)) {
                        cacheFile.writeText(content)
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

    private fun parseTimingFile(content: String, chapterNo: Int): TimingParseResult {
        try {
            val bundle = JsonHelper.json.decodeFromString<AudioTimingMetadata>(content)
            val chapter = bundle.chapters.find { it.chapterNo == chapterNo }

            return if (chapter != null) {
                TimingParseResult.Found(chapter)
            } else {
                TimingParseResult.ChapterMissing
            }
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

        conn.inputStream.buffered().use { input ->
            file.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
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
