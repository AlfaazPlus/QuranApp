package com.quranapp.android.utils.mediaplayer

import android.content.Context
import androidx.core.net.toUri
import com.quranapp.android.api.models.mediaplayer.RecitationAudioResult
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.maangers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale

class RecitationAudioRepository(private val context: Context) {
    private val fileUtils = FileUtils.newInstance(context)
    private val modelManager = RecitationModelManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = mutableMapOf<String, Job>()

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

    /**
     * Resolves audio URIs for a chapter. Returns cached URIs if available,
     * otherwise triggers download.
     */
    suspend fun resolveAudioUris(
        chapterNo: Int,
    ): Flow<AudioResult> = flow {
        val audioOption = SPReader.getRecitationAudioOption(context)

        val (quranModel, translationModel) = modelManager.resolveModels()

        val shouldPlayArabic = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_ARABIC

        // Validate models
        val failed = when {
            shouldPlayArabic && quranModel == null -> true
            shouldPlayTranslation && translationModel == null -> true
            else -> false
        }

        if (failed) {
            emit(AudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        // Get file references
        val quranFile =
            quranModel?.let { modelManager.getRecitationAudioFile(it.id, chapterNo) }
        val translationFile =
            translationModel?.let { modelManager.getRecitationAudioFile(it.id, chapterNo) }

        // Check if files exist
        val quranFileExists = quranFile == null || quranFile.length() > 0
        val translationFileExists = translationFile == null || translationFile.length() > 0

        if (quranFileExists && translationFileExists) {
            // Files are cached, return URIs immediately
            emit(
                AudioResult.Success(
                    chapter = chapterNo,
                    quran = quranFile?.let {
                        RecitationAudioResult(
                            chapterNo = chapterNo,
                            reciterId = quranModel.id,
                            audioUri = it.toUri(),
                            timingMetadata = null
                        )
                    },
                    translation = translationFile?.let {
                        RecitationAudioResult(
                            chapterNo = chapterNo,
                            reciterId = translationModel.id,
                            audioUri = it.toUri(),
                            timingMetadata = null
                        )
                    },
                )
            )
        } else {
            // Need to download
            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                emit(AudioResult.Error(NoInternetException()))
                return@flow
            }

            // Create files if needed
            val quranFileCreated = quranFile == null || fileUtils.createFile(quranFile)
            val translationFileCreated =
                translationFile == null || fileUtils.createFile(translationFile)

            if (!quranFileCreated || !translationFileCreated) {
                emit(AudioResult.Error(IllegalStateException("Failed to create audio files")))
                return@flow
            }

            // Download and emit progress
            downloadAudioFiles(
                chapterNo = chapterNo,
                quranFile = quranFile,
                translationFile = translationFile,
                quranModel = if (shouldPlayArabic) quranModel else null,
                translationModel = if (shouldPlayTranslation) translationModel else null,
            ).collect { result ->
                emit(result)
            }
        }
    }.flowOn(Dispatchers.IO)


    private fun downloadAudioFiles(
        chapterNo: Int,
        quranFile: File?,
        translationFile: File?,
        quranModel: RecitationQuranModel?,
        translationModel: RecitationTranslationModel?,
    ): Flow<AudioResult> = flow {
        var deleteVerseFile = false
        var deleteTranslFile = false

        try {
            emit(AudioResult.Downloading(0))

            // Download Arabic audio if needed
            if (quranFile != null && quranFile.length() == 0L && quranModel != null) {
                val url = prepareAudioUrl(quranModel.urlTemplate, chapterNo)
                    ?: throw IllegalStateException("Failed to prepare audio URL")

                try {
                    // TODO: ensure timing metadata downloaded
                    downloadFile(quranFile, url) { progress ->
                        // We could emit progress here if needed
                    }
                } catch (e: Exception) {
                    deleteVerseFile = true
                    throw e
                }
            }

            // Download translation audio if needed
            if (translationFile != null && translationFile.length() == 0L && translationModel != null) {
                val url =
                    prepareAudioUrl(translationModel.urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare translation audio URL")

                try {
                    // TODO: ensure timing metadata downloaded
                    downloadFile(translationFile, url) { progress ->
                        // We could emit progress here if needed
                    }
                } catch (e: Exception) {
                    deleteTranslFile = true
                    throw e
                }
            }

            emit(
                AudioResult.Success(
                    chapter = chapterNo,
                    quran = quranFile?.let {
                        RecitationAudioResult(
                            chapterNo = chapterNo,
                            reciterId = quranModel!!.id,
                            audioUri = it.toUri(),
                            timingMetadata = null
                        )
                    },
                    translation = translationFile?.let {
                        RecitationAudioResult(
                            chapterNo = chapterNo,
                            reciterId = translationModel!!.id,
                            audioUri = it.toUri(),
                            timingMetadata = null
                        )
                    },
                )
            )

        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.downloadAudioFiles")

            if (deleteVerseFile) quranFile?.delete()
            if (deleteTranslFile) translationFile?.delete()

            emit(AudioResult.Error(e))
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
}



