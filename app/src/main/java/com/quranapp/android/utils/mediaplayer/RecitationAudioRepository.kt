package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.core.util.Pair
import com.quranapp.android.api.models.mediaplayer.ChapterAudioResult
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.ReciterAudioType
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RecitationAudioRepository(private val context: Context) {

    private val fileUtils = FileUtils.newInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = mutableMapOf<String, Job>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Emits when a verse preload completes (chapter, verse)
    private val _preloadCompleted = MutableSharedFlow<PreloadedVerse>(extraBufferCapacity = 10)
    val preloadCompleted: SharedFlow<PreloadedVerse> = _preloadCompleted.asSharedFlow()

    // Track which verses are currently being preloaded
    private val preloadingVerses = mutableSetOf<String>()

    /**
     * Represents a preloaded verse with its audio URIs
     */
    data class PreloadedVerse(
        val chapter: Int,
        val verse: Int,
        val reciter: String?,
        val translationReciter: String?,
        val audioUri: Uri?,
        val translationUri: Uri?
    )

    /**
     * Sealed class representing download states
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val reciter: String?, val chapter: Int, val verse: Int, val progress: Int
        ) : DownloadState()

        data class Ready(
            val reciter: String?,
            val chapter: Int,
            val verse: Int,
            val audioUri: Uri,
            val translationUri: Uri?
        ) : DownloadState()

        data class Error(
            val reciter: String?, val chapter: Int, val verse: Int, val error: Throwable
        ) : DownloadState()
    }

    /**
     * Result class for verse-by-verse audio resolution
     */
    sealed class AudioResult {
        data class Success(
            val audioUri: Uri?,
            val translationUri: Uri?,
            val reciter: String?,
            val translationReciter: String?,
            val chapter: Int,
            val verse: Int
        ) : AudioResult()

        data class Error(val error: Throwable) : AudioResult()
        data class Downloading(val progress: Int) : AudioResult()
    }

    /**
     * Result class for chapter audio resolution
     */
    sealed class ChapterAudioDownloadResult {
        data class Success(
            val result: ChapterAudioResult
        ) : ChapterAudioDownloadResult()

        data class Error(val error: Throwable) : ChapterAudioDownloadResult()
        data class Downloading(val progress: Int) : ChapterAudioDownloadResult()
    }

    /**
     * Resolves audio URIs for a verse. Returns cached URIs if available,
     * otherwise triggers download.
     */
    private suspend fun resolveAudioUris(
        chapterNo: Int,
        verseNo: Int,
        forceManifestFetch: Boolean = false,
        forceTranslationManifestFetch: Boolean = false
    ): Flow<AudioResult> = flow {
        // Check network first if files don't exist
        val audioOption = SPReader.getRecitationAudioOption(context)

        // Get recitation models
        val models = suspendCoroutine { cont ->
            RecitationUtils.obtainRecitationModels(
                context,
                forceManifestFetch,
                forceTranslationManifestFetch,
                object :
                    OnResultReadyCallback<Pair<RecitationInfoModel, RecitationTranslationInfoModel>> {
                    override fun onReady(r: Pair<RecitationInfoModel, RecitationTranslationInfoModel>) {
                        cont.resume(r)
                    }
                })
        }

        val recModel = models.first
        val translModel = models.second

        val shouldPlayArabic = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_ARABIC

        // Validate models
        val failed = when {
            shouldPlayArabic && recModel == null -> true
            shouldPlayTranslation && translModel == null -> true
            else -> false
        }

        if (failed) {
            emit(AudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        // Get file references
        val verseFile =
            recModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }
        val verseTranslFile =
            translModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapterNo, verseNo) }

        // Check if files exist
        val verseFileExists = verseFile == null || verseFile.length() > 0
        val verseTranslFileExists = verseTranslFile == null || verseTranslFile.length() > 0

        if (verseFileExists && verseTranslFileExists) {
            // Files are cached, return URIs immediately
            emit(
                AudioResult.Success(
                    audioUri = verseFile?.toUri(),
                    translationUri = verseTranslFile?.toUri(),
                    reciter = recModel?.slug,
                    translationReciter = translModel?.slug,
                    chapter = chapterNo,
                    verse = verseNo
                )
            )
        } else {
            // Need to download
            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                emit(AudioResult.Error(NoInternetException()))
                return@flow
            }

            // Create files if needed
            val verseFileCreated = verseFile == null || fileUtils.createFile(verseFile)
            val verseTranslFileCreated =
                verseTranslFile == null || fileUtils.createFile(verseTranslFile)

            if (!verseFileCreated || !verseTranslFileCreated) {
                emit(AudioResult.Error(IllegalStateException("Failed to create audio files")))
                return@flow
            }

            // Download and emit progress
            downloadAudioFiles(
                verseFile = verseFile,
                verseTranslFile = verseTranslFile,
                recModel = if (shouldPlayArabic) recModel else null,
                translModel = if (shouldPlayTranslation) translModel else null,
                chapter = chapterNo,
                verse = verseNo
            ).collect { result ->
                emit(result)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Resolves full chapter audio for a reciter.
     * Downloads if not cached, fetches timing metadata if available.
     *
     * @param reciterModel The reciter model (must have audioType = FULL_CHAPTER)
     * @param chapter The chapter number
     * @return Flow of download results culminating in Success with audio URI and timing
     */
    private suspend fun resolveChapterAudio(
        reciterModel: RecitationInfoModel,
        chapter: Int
    ): Flow<ChapterAudioDownloadResult> = flow {
        // Verify this is a chapter-based reciter
        if (reciterModel.audioType != ReciterAudioType.FULL_CHAPTER) {
            emit(
                ChapterAudioDownloadResult.Error(
                    IllegalArgumentException("Reciter ${reciterModel.slug} is not a chapter-based reciter")
                )
            )
            return@flow
        }

        // Get file reference for chapter audio
        val chapterFile = getChapterAudioFile(reciterModel.slug, chapter)

        // Check if already cached
        if (chapterFile.length() > 0) {
            // Audio is cached, fetch timing if available
            val timing = resolveTimingMetadata(reciterModel, chapter)
            emit(
                ChapterAudioDownloadResult.Success(
                    ChapterAudioResult(
                        audioUri = chapterFile.toUri(),
                        chapterNo = chapter,
                        reciterSlug = reciterModel.slug,
                        timingMetadata = timing
                    )
                )
            )
            return@flow
        }

        // Need to download
        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            emit(ChapterAudioDownloadResult.Error(NoInternetException()))
            return@flow
        }

        // Create file
        if (!fileUtils.createFile(chapterFile)) {
            emit(
                ChapterAudioDownloadResult.Error(
                    IllegalStateException("Failed to create chapter audio file")
                )
            )
            return@flow
        }

        emit(ChapterAudioDownloadResult.Downloading(0))

        try {
            // Prepare URL for chapter audio
            val audioUrl = prepareChapterAudioUrl(reciterModel, chapter)
                ?: throw IllegalStateException("Failed to prepare chapter audio URL")

            // Download chapter audio
            downloadFile(chapterFile, audioUrl) { progress ->
                // Progress updates could be emitted here if needed
            }

            // Fetch timing metadata (may be null if not available)
            val timing = resolveTimingMetadata(reciterModel, chapter)

            emit(
                ChapterAudioDownloadResult.Success(
                    ChapterAudioResult(
                        audioUri = chapterFile.toUri(),
                        chapterNo = chapter,
                        reciterSlug = reciterModel.slug,
                        timingMetadata = timing
                    )
                )
            )

        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.resolveChapterAudio")
            chapterFile.delete()
            emit(ChapterAudioDownloadResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches timing metadata for a chapter. Checks cache first, then downloads if available.
     * Returns null if timing data is not available for this reciter.
     */
    suspend fun resolveTimingMetadata(
        reciterModel: RecitationInfoModel,
        chapter: Int
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        val cacheKey = "${reciterModel.slug}:$chapter"

        val timingUrlPath = reciterModel.timingUrlPath
        if (timingUrlPath.isNullOrEmpty()) {
            return@withContext null
        }

        // Check file cache
        val timingFile = getTimingMetadataFile(reciterModel.slug)
        if (timingFile.length() > 0) {
            try {
                val content = timingFile.readText()
                val metadata = json.decodeFromString<ChapterTimingMetadata>(content)
                return@withContext metadata
            } catch (e: Exception) {
                Log.saveError(e, "RecitationAudioRepository.fetchTimingMetadata - parse cached")
                timingFile.delete()
            }
        }

        // Download timing metadata
        if (!NetworkStateReceiver.isNetworkConnected(context)) {
            return@withContext null
        }

        try {
            val timingUrl = reciterModel.timingUrlPath ?: return@withContext null

            // Download to temp and parse
            val tempFile = File(context.cacheDir, "timing_temp_${System.currentTimeMillis()}.json")
            downloadFile(tempFile, timingUrl)

            val content = tempFile.readText()
            val metadata = json.decodeFromString<ChapterTimingMetadata>(content)

            // Save to cache file
            if (fileUtils.createFile(timingFile)) {
                timingFile.writeText(content)
            }

            tempFile.delete()
            return@withContext metadata
        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.fetchTimingMetadata - download")
            return@withContext null
        }
    }

    /**
     * Gets the cached chapter audio file reference
     */
    fun getChapterAudioFile(reciterSlug: String, chapter: Int): File {
        val dir = File(fileUtils.recitationDir, reciterSlug)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "chapter_${chapter.toString().padStart(3, '0')}.mp3")
    }

    /**
     * Gets the cached timing metadata file reference
     */
    private fun getTimingMetadataFile(reciterSlug: String): File {
        val dir = File(fileUtils.recitationDir, reciterSlug)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ChapterTimingMetadata.getCacheFileName(reciterSlug))
    }

    /**
     * Prepares the URL for downloading chapter audio.
     * URL pattern uses {chapNo} placeholder.
     */
    private fun prepareChapterAudioUrl(reciterModel: RecitationInfoModel, chapter: Int): String? {
        val host = reciterModel.urlHost ?: return null
        val path = reciterModel.urlPath
            .replace("{chapNo}", chapter.toString().padStart(3, '0'))
            .replace("{chapter}", chapter.toString().padStart(3, '0'))
        return "$host$path"
    }

    // ======================= Verse Audio Methods =======================

    /**
     * Downloads audio files for a verse
     */
    private fun downloadAudioFiles(
        verseFile: File?,
        verseTranslFile: File?,
        recModel: RecitationInfoModel?,
        translModel: RecitationTranslationInfoModel?,
        chapter: Int,
        verse: Int
    ): Flow<AudioResult> = flow {
        var deleteVerseFile = false
        var deleteTranslFile = false

        try {
            emit(AudioResult.Downloading(0))

            // Download Arabic audio if needed
            if (verseFile != null && verseFile.length() == 0L && recModel != null) {
                val url = RecitationUtils.prepareRecitationAudioUrl(recModel, chapter, verse)
                    ?: throw IllegalStateException("Failed to prepare audio URL")

                try {
                    downloadFile(verseFile, url) { progress ->
                        // We could emit progress here if needed
                    }
                } catch (e: Exception) {
                    deleteVerseFile = true
                    throw e
                }
            }

            // Download translation audio if needed
            if (verseTranslFile != null && verseTranslFile.length() == 0L && translModel != null) {
                val url = RecitationUtils.prepareRecitationAudioUrl(translModel, chapter, verse)
                    ?: throw IllegalStateException("Failed to prepare translation audio URL")

                try {
                    downloadFile(verseTranslFile, url) { progress ->
                        // We could emit progress here if needed
                    }
                } catch (e: Exception) {
                    deleteTranslFile = true
                    throw e
                }
            }

            emit(
                AudioResult.Success(
                    audioUri = verseFile?.toUri(),
                    translationUri = verseTranslFile?.toUri(),
                    reciter = recModel?.slug,
                    translationReciter = translModel?.slug,
                    chapter = chapter,
                    verse = verse
                )
            )

        } catch (e: Exception) {
            Log.saveError(e, "RecitationAudioRepository.downloadAudioFiles")
            if (deleteVerseFile) verseFile?.delete()
            if (deleteTranslFile) verseTranslFile?.delete()
            emit(AudioResult.Error(e))
        }
    }

    /**
     * Downloads a file from URL
     */
    private suspend fun downloadFile(
        file: File, urlStr: String, onProgress: (Int) -> Unit = {}
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

    /**
     * Preloads audio files for upcoming verses with completion notifications.
     * Emits to [preloadCompleted] when each verse finishes downloading.
     */
    fun preloadVerses(
        chapter: Int,
        fromVerse: Int,
        toVerse: Int,
        count: Int = 10,
    ) {
        if (!NetworkStateReceiver.isNetworkConnected(context)) return

        scope.launch {
            val audioOption = SPReader.getRecitationAudioOption(context)

            val shouldPlayArabic = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
            val shouldPlayTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_ARABIC

            val models =
                suspendCoroutine { cont ->
                    RecitationUtils.obtainRecitationModels(
                        context, false, false,
                        object :
                            OnResultReadyCallback<Pair<RecitationInfoModel, RecitationTranslationInfoModel>> {
                            override fun onReady(r: Pair<RecitationInfoModel, RecitationTranslationInfoModel>) {
                                cont.resume(r)
                            }
                        }
                    )
                }

            val recModel = models.first
            val translModel = models.second

            var versesToLoad = count
            var currentVerse = fromVerse + 1

            while (versesToLoad > 0 && currentVerse <= toVerse) {
                val verseToPreload = currentVerse
                val verseFile = recModel?.let {
                    fileUtils.getRecitationAudioFile(it.slug, chapter, verseToPreload)
                }
                val verseTranslFile = translModel?.let {
                    fileUtils.getRecitationAudioFile(it.slug, chapter, verseToPreload)
                }

                val verseFileExists = verseFile == null || verseFile.length() > 0
                val verseTranslFileExists = verseTranslFile == null || verseTranslFile.length() > 0

                if (!verseFileExists || !verseTranslFileExists) {
                    // Need to download
                    val key = makeKey(recModel?.slug, translModel?.slug, chapter, verseToPreload)

                    if (downloadJobs[key]?.isActive != true) {
                        synchronized(preloadingVerses) {
                            preloadingVerses.add(key)
                        }

                        downloadJobs[key] = scope.launch {
                            try {
                                if (!shouldPlayArabic && verseFile != null && verseFile.length() == 0L) {
                                    if (fileUtils.createFile(verseFile)) {
                                        val url = RecitationUtils.prepareRecitationAudioUrl(
                                            recModel,
                                            chapter,
                                            verseToPreload,
                                        )

                                        if (url != null) {
                                            downloadFile(verseFile, url)
                                        }
                                    }
                                }

                                if (shouldPlayTranslation && verseTranslFile != null && verseTranslFile.length() == 0L) {
                                    if (fileUtils.createFile(verseTranslFile)) {
                                        val url = RecitationUtils.prepareRecitationAudioUrl(
                                            translModel,
                                            chapter,
                                            verseToPreload,
                                        )

                                        if (url != null) {
                                            downloadFile(verseTranslFile, url)
                                        }
                                    }
                                }

                                // Emit preload completion
                                _preloadCompleted.emit(
                                    PreloadedVerse(
                                        chapter = chapter,
                                        verse = verseToPreload,
                                        reciter = recModel?.slug,
                                        translationReciter = translModel?.slug,
                                        audioUri = verseFile?.toUri(),
                                        translationUri = verseTranslFile?.toUri()
                                    )
                                )
                            } catch (e: Exception) {
                                Log.saveError(e, "RecitationAudioRepository.preloadVerses")
                            } finally {
                                downloadJobs.remove(key)
                                synchronized(preloadingVerses) {
                                    preloadingVerses.remove(key)
                                }
                            }
                        }
                    }
                }

                currentVerse++
                versesToLoad--
            }
        }
    }

    /**
     * Checks if a verse is currently being preloaded
     */
    fun isPreloading(reciter: String?, translReciter: String?, chapter: Int, verse: Int): Boolean {
        val key = makeKey(reciter, translReciter, chapter, verse)
        synchronized(preloadingVerses) {
            return preloadingVerses.contains(key)
        }
    }

    /**
     * Waits for a specific verse to be preloaded (with timeout).
     * Returns true if verse is ready, false if timeout or error.
     */
    suspend fun getAudioUri(chapter: Int, verse: Int): Boolean {
        // Wait for preload completion
        return suspendCancellableCoroutine { cont ->
            var resumed = false
            val job = scope.launch {
                preloadCompleted.collect { preloaded ->
                    if (preloaded.chapter == chapter && preloaded.verse == verse) {
                        if (!resumed) {
                            resumed = true
                            cont.resume(true)
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                job.cancel()
            }
        }
    }


    /**
     * Gets the audio file for a specific reciter and verse (for checking cache)
     */
    fun getAudioFile(reciterSlug: String?, chapter: Int, verse: Int): File? {
        return reciterSlug?.let { fileUtils.getRecitationAudioFile(it, chapter, verse) }
    }


    /**
     * Cancels all pending downloads
     */
    fun cancelAll() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }

    /**
     * Cleanup resources
     */
    fun destroy() {
        cancelAll()
        scope.cancel()
    }

    private fun makeKey(
        reciter: String?, translReciter: String?, chapter: Int, verse: Int
    ): String {
        return "$reciter-$translReciter:$chapter:$verse"
    }

    companion object {
        @Volatile
        private var instance: RecitationAudioRepository? = null

        fun getInstance(context: Context): RecitationAudioRepository {
            return instance ?: synchronized(this) {
                instance ?: RecitationAudioRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

