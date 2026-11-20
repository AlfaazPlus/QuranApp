package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.RecitationAudioTrack
import com.quranapp.android.api.models.mediaplayer.ResolvedAudioResult
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.platform.Platform
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

class RecitationAudioRepository() {
    private val modelManager = RecitationModelManager

    fun cancelAll() {
        Platform.system.taskManager.cancelByTag(RecitationAudioDownloadWorker.TAG)
        Platform.system.taskManager.cancelByTag(RecitationBulkDownloadWorker.TAG)
    }

    private sealed class TimingParseResult {
        data class Found(val metadata: ChapterTimingMetadata) : TimingParseResult()
        data object ChapterMissing : TimingParseResult()
        data object ParseFailed : TimingParseResult()
    }

    /**
     * Resolves chapter audio URIs and timing for playback.
     */
    fun resolveAudioUris(
        chapterNo: Int,
        settings: PlayerSettings,
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
                    if (!Platform.networkMonitor.isConnected) {
                        throw NoInternetException()
                    }
                }

                val quranTiming = quranTimingDeferred.await()
                val translationTiming = translationTimingDeferred.await()

                Logger.d(
                    "ChapterTiming",
                    "Resolved timings for chapter $chapterNo - quran: $quranTiming, translation: $translationTiming",
                )

                fun resolveTrackUri(
                    needsStream: Boolean,
                    file: Path?,
                    urlTemplate: String,
                ): String {
                    if (file == null) {
                        throw IllegalStateException("Missing audio file path")
                    }

                    if (!needsStream) {
                        return file.toString()
                    }

                    val url = RecitationUtils.prepareAudioUrl(urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")

                    return url
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
                val jsonText = if (cacheFile.isGzip()) {
                    AppFiles.readTextGzipped(cacheFile)
                } else {
                    AppFiles.readText(cacheFile)
                }

                when (val parsed = parseTimingText(
                    jsonText,
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
                    "RecitationAudioRepository.resolveChapterTimingMetadata - read cache",
                )
                AppFiles.delete(cacheFile)
            }
        }

        if (!Platform.networkMonitor.isConnected) {
            return@withContext null
        }

        val tempFile =
            Platform.files.cacheDirectory / "timing_temp_${System.currentTimeMillis()}"

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
                "RecitationAudioRepository.resolveTimingFromCacheOrNetwork - download",
            )
            return@withContext null
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
                        chapterObj,
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
