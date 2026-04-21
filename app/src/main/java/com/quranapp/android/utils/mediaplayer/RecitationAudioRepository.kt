package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.extensions.isGzip
import com.quranapp.android.utils.reader.recitation.RecitationUtils.URL_CHAPTER_PATTERN
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.workers.RecitationAudioDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

        fun prepareAudioUrl(urlTemplate: String, chapterNo: Int): String? {
            var url = urlTemplate
            return try {
                var matcher = URL_CHAPTER_PATTERN.matcher(url)
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
     * Resolves chapter audio URIs and timing for playback.
     *
     * Audio URIs
     * - If the chapter file under the reciter directory exists and is non-empty, `RecitationAudioTrack.audioUri`
     *   is a `file://` URI (explicit / bulk download). This always wins.
     * - Otherwise, when the device is online, `audioUri` is the prepared HTTPS URL for that chapter.
     *   Playback buffers via ExoPlayer; bytes are not written to the reciter folder by this call.
     *   Repeat plays use ExoPlayer `SimpleCache` (see `RecitationService`), not `RecitationAudioDownloadWorker`.
     * - Offline with an empty placeholder file fails with `NoInternetException`.
     *
     * Timing: cache if valid; otherwise fetch when online (ChapterTimingMetadata may be null).
     * Missing audio for a required track fails; missing timing does not fail the flow.
     */
    fun resolveAudioUris(
        chapterNo: Int,
        settings: PlayerSettings
    ): Flow<ResolvedAudioResult> = flow {
        val (quranModel, translationModel) = modelManager.resolveModels(settings)

        val audioOption = settings.audioOption

        val shouldPlayArabic = audioOption != AudioOption.ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != AudioOption.ONLY_QURAN

        val failed = when {
            shouldPlayArabic && quranModel == null -> true
            shouldPlayTranslation && translationModel == null -> true
            else -> false
        }

        if (failed) {
            emit(ResolvedAudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        val quranFile = quranModel?.let {
            modelManager.getRecitationAudioFile(it.id, chapterNo)
        }
        val translationFile = translationModel?.let {
            modelManager.getRecitationAudioFile(it.id, chapterNo)
        }

        val quranNeedsStream =
            shouldPlayArabic && quranFile != null && quranFile.length() == 0L
        val translationNeedsStream =
            shouldPlayTranslation && translationFile != null && translationFile.length() == 0L

        try {
            coroutineScope {
                val quranTimingDeferred = async {
                    quranModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                val translationTimingDeferred = async {
                    translationModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                if (quranNeedsStream || translationNeedsStream) {
                    if (!NetworkStateReceiver.isNetworkConnected(context)) {
                        throw NoInternetException()
                    }
                }

                val quranTiming = quranTimingDeferred.await()
                val translationTiming = translationTimingDeferred.await()

                Log.d(
                    "ChapterTiming",
                    "Resolved timings for chapter $chapterNo - quran: $quranTiming, translation: $translationTiming"
                )

                fun resolveTrackUri(
                    needsStream: Boolean,
                    file: File?,
                    urlTemplate: String,
                ): Uri {
                    if (file == null) {
                        throw IllegalStateException("Missing audio file path")
                    }

                    if (!needsStream) {
                        return file.toUri()
                    }

                    val url = prepareAudioUrl(urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")

                    return url.toUri()
                }

                emit(
                    ResolvedAudioResult.Resoved(
                        chapter = chapterNo,
                        quran = if (shouldPlayArabic && quranModel != null && quranFile != null) {
                            RecitationAudioTrack(
                                kind = RecitationAudioKind.QURAN,
                                chapterNo = chapterNo,
                                reciterId = quranModel.id,
                                audioUri = resolveTrackUri(
                                    needsStream = quranNeedsStream,
                                    file = quranFile,
                                    urlTemplate = quranModel.urlTemplate,
                                ),
                                timingMetadata = quranTiming,
                            )
                        } else {
                            null
                        },
                        translation = if (shouldPlayTranslation && translationModel != null && translationFile != null) {
                            RecitationAudioTrack(
                                kind = RecitationAudioKind.TRANSLATION,
                                chapterNo = chapterNo,
                                reciterId = translationModel.id,
                                audioUri = resolveTrackUri(
                                    needsStream = translationNeedsStream,
                                    file = translationFile,
                                    urlTemplate = translationModel.urlTemplate,
                                ),
                                timingMetadata = translationTiming,
                            )
                        } else {
                            null
                        },
                    ),
                )
            }
        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.resolveAudioUris")
            emit(ResolvedAudioResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        var timingUrl = model.timingUrl?.trim().orEmpty()

        if (timingUrl.isEmpty()) return@withContext null

        val cacheFile = modelManager.getRecitationTimingFile(model.id)

        if (cacheFile.length() > 0L) {
            try {
                when (val parsed = parseTimingFile(
                    cacheFile.inputStream().buffered(),
                    chapterNo,
                    model.timingVersion ?: 0
                )) {
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
                    parseTimingFile(
                        contentFile.inputStream().buffered(),
                        chapterNo,
                        model.timingVersion ?: 0
                    )) {
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

    /**
     * Parses only the requested chapter from the timing JSON, avoiding full
     * deserialization of all 114 chapters and their verse/segment data.
     */
    private fun parseTimingFile(
        contentStream: InputStream,
        chapterNo: Int,
        upstreamVersion: Int
    ): TimingParseResult {
        try {
            val root = contentStream.use { stream ->
                JsonHelper.json.parseToJsonElement(
                    stream.bufferedReader().readText()
                ).jsonObject
            }

            val version = root["version"]?.jsonPrimitive?.intOrNull ?: 0

            if (version < upstreamVersion) {
                return TimingParseResult.ParseFailed
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
            RetrofitInstance.githubLike.getRawContent(
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
