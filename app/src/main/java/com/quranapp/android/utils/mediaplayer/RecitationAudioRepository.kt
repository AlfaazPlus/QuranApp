package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.core.util.Pair
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.exceptions.NoInternetException
import com.quranapp.android.api.models.player.ChapterAudioResult
import com.quranapp.android.api.models.player.ChapterTimingMetadata
import com.quranapp.android.api.models.player.ReciterAudioType
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for managing recitation audio files.
 * Handles audio file resolution, downloading, and caching.
 * Completely decoupled from UI and playback concerns.
 */
class RecitationAudioRepository(private val context: Context) {

    private val fileUtils = FileUtils.newInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = mutableMapOf<String, Job>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Cache for timing metadata to avoid repeated file reads
    private val timingCache = mutableMapOf<String, ChapterTimingMetadata?>()

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
    suspend fun resolveAudioUris(
        chapter: Int,
        verse: Int,
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

        val isBoth = audioOption == RecitationUtils.AUDIO_OPTION_BOTH
        val isOnlyTranslation = audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

        // Validate models
        val failed = when {
            isBoth -> recModel == null || translModel == null
            isOnlyTranslation -> translModel == null
            else -> recModel == null
        }

        if (failed) {
            emit(AudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        // Get file references
        val verseFile = recModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapter, verse) }
        val verseTranslFile =
            translModel?.let { fileUtils.getRecitationAudioFile(it.slug, chapter, verse) }

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
                    chapter = chapter,
                    verse = verse
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
                recModel = recModel,
                translModel = translModel,
                chapter = chapter,
                verse = verse
            ).collect { result ->
                emit(result)
            }
        }
    }.flowOn(Dispatchers.IO)

    // ======================= Chapter Audio Methods =======================

    /**
     * Resolves full chapter audio for a reciter.
     * Downloads if not cached, fetches timing metadata if available.
     *
     * @param reciterModel The reciter model (must have audioType = FULL_CHAPTER)
     * @param chapter The chapter number
     * @return Flow of download results culminating in Success with audio URI and timing
     */
    suspend fun resolveChapterAudio(
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
            val timing = fetchTimingMetadata(reciterModel, chapter)
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
            val timing = fetchTimingMetadata(reciterModel, chapter)

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
    suspend fun fetchTimingMetadata(
        reciterModel: RecitationInfoModel,
        chapter: Int
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        val cacheKey = "${reciterModel.slug}:$chapter"

        // Check memory cache
        timingCache[cacheKey]?.let { return@withContext it }

        // Check if reciter has timing URL
        val timingUrlPath = reciterModel.timingUrlPath
        if (timingUrlPath.isNullOrEmpty()) {
            timingCache[cacheKey] = null
            return@withContext null
        }

        // Check file cache
        val timingFile = getTimingMetadataFile(reciterModel.slug, chapter)
        if (timingFile.length() > 0) {
            try {
                val content = timingFile.readText()
                val metadata = json.decodeFromString<ChapterTimingMetadata>(content)
                timingCache[cacheKey] = metadata
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
            val timingUrl = prepareTimingMetadataUrl(reciterModel, chapter)
                ?: return@withContext null

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

            timingCache[cacheKey] = metadata
            return@withContext metadata

        } catch (e: HttpNotFoundException) {
            // Timing not available for this chapter - not an error
            timingCache[cacheKey] = null
            return@withContext null
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
    private fun getTimingMetadataFile(reciterSlug: String, chapter: Int): File {
        val dir = File(fileUtils.recitationDir, reciterSlug)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ChapterTimingMetadata.getCacheFileName(reciterSlug, chapter))
    }

    /**
     * Checks if chapter audio is cached locally
     */
    fun isChapterAudioCached(reciterSlug: String, chapter: Int): Boolean {
        return getChapterAudioFile(reciterSlug, chapter).length() > 0
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

    /**
     * Prepares the URL for downloading timing metadata.
     * URL pattern uses {chapNo} placeholder.
     */
    private fun prepareTimingMetadataUrl(reciterModel: RecitationInfoModel, chapter: Int): String? {
        val host = reciterModel.urlHost ?: return null
        val path = reciterModel.timingUrlPath
            ?.replace("{chapNo}", chapter.toString().padStart(3, '0'))
            ?.replace("{chapter}", chapter.toString().padStart(3, '0'))
            ?: return null
        return "$host$path"
    }

    /**
     * Clears timing cache (useful after reciter change)
     */
    fun clearTimingCache() {
        timingCache.clear()
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
            if (verseFile != null && verseFile.length() == 0L) {
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
            if (verseTranslFile != null && verseTranslFile.length() == 0L) {
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
        chapter: Int, fromVerse: Int, toVerse: Int, count: Int = 10
    ) {
        if (!NetworkStateReceiver.isNetworkConnected(context)) return

        scope.launch {
            val audioOption = SPReader.getRecitationAudioOption(context)
            val isBoth = audioOption == RecitationUtils.AUDIO_OPTION_BOTH
            val isOnlyTranslation = audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

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
                                if (!isOnlyTranslation && verseFile != null && verseFile.length() == 0L) {
                                    if (fileUtils.createFile(verseFile)) {
                                        val url = RecitationUtils.prepareRecitationAudioUrl(
                                            recModel, chapter, verseToPreload
                                        )
                                        if (url != null) {
                                            downloadFile(verseFile, url)
                                        }
                                    }
                                }

                                if ((isBoth || isOnlyTranslation) && verseTranslFile != null && verseTranslFile.length() == 0L) {
                                    if (fileUtils.createFile(verseTranslFile)) {
                                        val url = RecitationUtils.prepareRecitationAudioUrl(
                                            translModel, chapter, verseToPreload
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
     * Checks if verse audio is ready (cached or no download needed based on audio option)
     */
    fun isVerseReady(chapter: Int, verse: Int): Boolean {
        val audioOption = SPReader.getRecitationAudioOption(context)
        val reciterSlug = SPReader.getSavedRecitationSlug(context)
        val translReciterSlug = SPReader.getSavedRecitationTranslationSlug(context)

        val isBoth = audioOption == RecitationUtils.AUDIO_OPTION_BOTH
        val isOnlyTranslation = audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION

        val arabicReady = isOnlyTranslation || isAudioCached(reciterSlug, chapter, verse)
        val translReady = !isBoth && !isOnlyTranslation || isAudioCached(translReciterSlug, chapter, verse)

        return arabicReady && translReady
    }

    /**
     * Waits for a specific verse to be preloaded (with timeout).
     * Returns true if verse is ready, false if timeout or error.
     */
    suspend fun awaitVerseReady(chapter: Int, verse: Int, timeoutMs: Long = 30000): Boolean {
        // Check if already ready
        if (isVerseReady(chapter, verse)) return true

        // Wait for preload completion
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                var resumed = false
                val job = scope.launch {
                    preloadCompleted.collect { preloaded ->
                        if (preloaded.chapter == chapter && preloaded.verse == verse) {
                            if (!resumed) {
                                resumed = true
                                cont.resume(true) {}
                            }
                        }
                    }
                }

                cont.invokeOnCancellation {
                    job.cancel()
                }
            }
        } ?: false
    }

    /**
     * Checks if a download is pending for a specific verse
     */
    fun isDownloadPending(
        reciter: String?, translReciter: String?, chapter: Int, verse: Int
    ): Boolean {
        val key = makeKey(reciter, translReciter, chapter, verse)
        return downloadJobs[key]?.isActive == true
    }

    /**
     * Gets the audio file for a specific reciter and verse (for checking cache)
     */
    fun getAudioFile(reciterSlug: String?, chapter: Int, verse: Int): File? {
        return reciterSlug?.let { fileUtils.getRecitationAudioFile(it, chapter, verse) }
    }

    /**
     * Checks if audio is cached locally
     */
    fun isAudioCached(reciterSlug: String?, chapter: Int, verse: Int): Boolean {
        val file = getAudioFile(reciterSlug, chapter, verse)
        return file != null && file.length() > 0
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

