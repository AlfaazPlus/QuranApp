package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.platform.toInputStream
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.miscellenous.isGzip
import com.quranapp.android.utils.workers.RecitationAudioDownloadWorker
import com.quranapp.android.utils.workers.RecitationBulkDownloadWorker
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
import okio.FileSystem
import okio.Path
import java.io.InputStream
import java.util.zip.GZIPInputStream

class RecitationAudioRepository(private val context: Context) {
    private val modelManager = RecitationModelManager

    fun cancelAll() {
        PlatformServices.system.taskManager.cancelByTag(RecitationAudioDownloadWorker.TAG)
        PlatformServices.system.taskManager.cancelByTag(RecitationBulkDownloadWorker.TAG)
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
            shouldPlayArabic && quranFile != null && AppFiles.size(quranFile) == 0L
        val translationNeedsStream =
            shouldPlayTranslation && translationFile != null && AppFiles.size(translationFile) == 0L

        try {
            coroutineScope {
                val quranTimingDeferred = async {
                    quranModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                val translationTimingDeferred = async {
                    translationModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                if (quranNeedsStream || translationNeedsStream) {
                    if (!PlatformServices.networkMonitor.isConnected) {
                        throw NoInternetException()
                    }
                }

                val quranTiming = quranTimingDeferred.await()
                val translationTiming = translationTimingDeferred.await()

                Logger.d(
                    "ChapterTiming",
                    "Resolved timings for chapter $chapterNo - quran: $quranTiming, translation: $translationTiming"
                )

                fun resolveTrackUri(
                    needsStream: Boolean,
                    file: Path?,
                    urlTemplate: String,
                ): Uri {
                    if (file == null) {
                        throw IllegalStateException("Missing audio file path")
                    }

                    if (!needsStream) {
                        return file.toFile().toUri()
                    }

                    val url = RecitationUtils.prepareAudioUrl(urlTemplate, chapterNo)
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
            Logger.saveError(e, "RecitationAudioRepository.resolveAudioUris")
            emit(ResolvedAudioResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        val timingUrl = model.timingUrl?.trim().orEmpty()

        if (timingUrl.isEmpty()) return@withContext null

        val cacheFile = modelManager.getRecitationTimingFile(model.id)

        if (AppFiles.size(cacheFile) > 0L) {
            try {
                when (val parsed = parseTimingFile(
                    openTimingInputStream(cacheFile),
                    chapterNo,
                    model.timingVersion ?: 0,
                )) {
                    is TimingParseResult.Found -> return@withContext parsed.metadata
                    TimingParseResult.ChapterMissing -> return@withContext null
                    TimingParseResult.ParseFailed -> AppFiles.delete(cacheFile)
                }
            } catch (e: Exception) {
                Logger.saveError(
                    e,
                    "RecitationAudioRepository.resolveChapterTimingMetadata - read cache"
                )
                AppFiles.delete(cacheFile)
            }
        }

        if (!PlatformServices.networkMonitor.isConnected) {
            return@withContext null
        }

        val tempFile =
            PlatformServices.files.cacheDirectory / "timing_temp_${System.currentTimeMillis()}"

        try {
            downloadTimingMetadata(tempFile, timingUrl)

            if (AppFiles.size(tempFile) <= 0L) {
                AppFiles.delete(tempFile)
                return@withContext null
            }

            val jsonText = try {
                if (tempFile.isGzip()) {
                    AppFiles.readTextGzipped(tempFile)
                } else {
                    AppFiles.readText(tempFile)
                }
            } finally {
                AppFiles.delete(tempFile)
            }

            when (
                val parsed = parseTimingText(
                    jsonText,
                    chapterNo,
                    model.timingVersion ?: 0,
                )
            ) {
                is TimingParseResult.Found -> {
                    cacheFile.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
                    AppFiles.writeText(cacheFile, jsonText)
                    return@withContext parsed.metadata
                }

                TimingParseResult.ChapterMissing -> {
                    Logger.saveError(
                        Exception("Timing JSON had no entry for chapter $chapterNo"),
                        "RecitationAudioRepository.resolveTimingFromCacheOrNetwork",
                    )
                    return@withContext null
                }

                TimingParseResult.ParseFailed -> return@withContext null
            }
        } catch (e: Exception) {
            AppFiles.delete(tempFile)
            Logger.saveError(
                e,
                "RecitationAudioRepository.resolveTimingFromCacheOrNetwork - download"
            )
            return@withContext null
        }
    }

    private fun openTimingInputStream(path: Path): InputStream {
        val source = FileSystem.SYSTEM.source(path)

        return try {
            if (path.isGzip()) {
                GZIPInputStream(source.toInputStream())
            } else {
                source.toInputStream()
            }
        } catch (_: Exception) {
            source.toInputStream()
        }
    }

    /**
     * Parses only the requested chapter from the timing JSON, avoiding full
     * deserialization of all 114 chapters and their verse/segment data.
     */
    private fun parseTimingFile(
        contentStream: InputStream,
        chapterNo: Int,
        upstreamVersion: Int,
    ): TimingParseResult {
        return try {
            val text = contentStream.use { it.bufferedReader().readText() }
            parseTimingText(text, chapterNo, upstreamVersion)
        } catch (e: Exception) {
            Logger.saveError(e, "RecitationAudioRepository.parseTimingFile")
            TimingParseResult.ParseFailed
        }
    }

    private fun parseTimingText(
        jsonText: String,
        chapterNo: Int,
        upstreamVersion: Int,
    ): TimingParseResult {
        try {
            val root = JsonHelper.json.parseToJsonElement(jsonText).jsonObject
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
            Logger.saveError(e, "RecitationAudioRepository.parseTimingText")
            return TimingParseResult.ParseFailed
        }
    }

    private suspend fun downloadTimingMetadata(
        file: Path,
        timingUrl: String,
    ) {
        val response = fetchInventoryStreamingResponse(timingUrl)
        response.downloadTo(
            file,
            onFailed = {
                if (it.code == 404) throw HttpNotFoundException()
                throw IllegalStateException("Timing metadata download failed: HTTP ${it.code}")
            },
        )
    }
}
