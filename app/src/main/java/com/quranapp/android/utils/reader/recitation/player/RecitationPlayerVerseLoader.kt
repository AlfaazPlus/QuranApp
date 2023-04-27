package com.quranapp.android.utils.reader.recitation.player

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class RecitationPlayerVerseLoader {
    private val callbacks = HashMap<String, RecitationPlayerVerseLoadCallback?>()
    private val jobs = HashMap<String, Job>()
    private val handler = Handler(Looper.getMainLooper())

    fun addTask(
        verseFile: File?,
        verseTranslFile: File?,
        model: RecitationInfoModel?,
        translModel: RecitationTranslationInfoModel?,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        load(verseFile, verseTranslFile, model, translModel, chapterNo, verseNo, callback)
    }

    private fun load(
        verseFile: File?,
        verseTranslFile: File?,
        model: RecitationInfoModel?,
        translModel: RecitationTranslationInfoModel?,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        addCallback(model?.slug, translModel?.slug, chapterNo, verseNo, callback)

        val job = Job()
        jobs[makeKey(makeRecitersKey(model?.slug, translModel?.slug), chapterNo, verseNo)] = job

        var deleteVerseFile = false
        var deleteTranslFile = false

        CoroutineScope(job).launch {
            flow {
                if (verseFile != null) {
                    try {
                        downloadAudio(
                            this,
                            verseFile,
                            RecitationUtils.prepareRecitationAudioUrl(model, chapterNo, verseNo)!!
                        )
                    } catch (e: Exception) {
                        deleteVerseFile = true
                        throw e
                    }
                }

                if (verseTranslFile != null) {
                    try {
                        downloadAudio(
                            this,
                            verseTranslFile,
                            RecitationUtils.prepareRecitationAudioUrl(translModel, chapterNo, verseNo)!!
                        )
                    } catch (e: Exception) {
                        deleteTranslFile = true
                        throw e
                    }
                }

                emit(VerseLoadFlow.Loaded(verseFile, verseTranslFile))
            }.flowOn(Dispatchers.IO).catch {
                Log.saveError(it, "RecitationPlayerVerseLoader")
                it.printStackTrace()
                emit(
                    VerseLoadFlow.Failed(
                        it,
                        verseFile,
                        verseTranslFile,
                        deleteVerseFile = deleteVerseFile,
                        deleteTranslFile = deleteTranslFile
                    )
                )
            }.collect {
                handler.post {
                    when (it) {
                        is VerseLoadFlow.Progress -> {
                            callback?.onProgress(String.format(Locale.getDefault(), "%d%%", it.progress))
                        }
                        is VerseLoadFlow.Failed -> {
                            it.e.printStackTrace()
                            publishVerseLoadStatus(
                                verseFile,
                                verseTranslFile,
                                model?.slug,
                                translModel?.slug,
                                chapterNo,
                                verseNo,
                                false,
                                it.e,
                                it.deleteVerseFile,
                                it.deleteTranslFile
                            )
                        }
                        is VerseLoadFlow.Loaded -> {
                            publishVerseLoadStatus(
                                verseFile,
                                verseTranslFile,
                                model?.slug,
                                translModel?.slug,
                                chapterNo,
                                verseNo,
                                true,
                                null,
                                deleteVerseFile = false,
                                deleteTranslFile = false
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun downloadAudio(flow: FlowCollector<VerseLoadFlow>, verseFile: File, urlStr: String) {
        val conn = withContext(Dispatchers.IO) {
            URL(urlStr).openConnection()
        } as HttpURLConnection

        conn.setRequestProperty("Content-Length", "0")
        conn.setRequestProperty("Connection", "close")
        conn.connectTimeout = 180000
        conn.readTimeout = 180000
        conn.allowUserInteraction = false

        withContext(Dispatchers.IO) {
            conn.connect()
        }

        if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw HttpNotFoundException()
        }

        flow.emit(VerseLoadFlow.Progress(0))

        val totalLength: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) conn.contentLengthLong
        else conn.contentLength.toLong()

        conn.inputStream.buffered().use { input ->
            verseFile.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalConsumed = 0L

                while (true) {
                    val bytes = input.read(buffer)

                    if (bytes <= 0) break

                    output.write(buffer, 0, bytes)

                    totalConsumed += bytes
                    flow.emit(VerseLoadFlow.Progress((totalConsumed / totalLength * 100).toInt()))
                }

                output.flush()
            }
        }
    }

    private fun publishVerseLoadStatus(
        verseFile: File?,
        verseTranslFile: File?,
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int,
        loaded: Boolean,
        e: Throwable?,
        deleteVerseFile: Boolean,
        deleteTranslFile: Boolean
    ) {
        val key = makeKey(makeRecitersKey(reciter, translReciter), chapterNo, verseNo)
        publishVerseLoadStatus(verseFile, verseTranslFile, callbacks[key], loaded, e, deleteVerseFile, deleteTranslFile)
        removeCallback(key)
        jobs.remove(key)
    }


    fun publishVerseLoadStatus(
        verseFile: File?,
        verseTranslFile: File?,
        callback: RecitationPlayerVerseLoadCallback?,
        loaded: Boolean,
        e: Throwable?,
        deleteVerseFile: Boolean = false,
        deleteTranslFile: Boolean = false
    ) {
        callback?.let {
            if (loaded) it.onLoaded(verseFile, verseTranslFile)
            else it.onFailed(e, verseFile, verseTranslFile, deleteVerseFile, deleteTranslFile)

            it.postLoad()
        }
    }

    private fun makeRecitersKey(reciterSlug: String?, translReciterSlug: String?): String {
        return "${reciterSlug}-${translReciterSlug}"
    }

    private fun makeKey(reciter: String, chapterNo: Int, verseNo: Int): String {
        return "$reciter:$chapterNo:$verseNo"
    }

    fun isPending(reciterSlug: String?, translReciterSlug: String?, chapterNo: Int, verseNo: Int): Boolean {
        return jobs[makeKey(makeRecitersKey(reciterSlug, translReciterSlug), chapterNo, verseNo)]?.isActive == true
    }

    fun addCallback(
        reciter: String?,
        translReciter: String?,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        val key = makeKey(makeRecitersKey(reciter, translReciter), chapterNo, verseNo)

        if (callback == null) {
            callbacks.remove(key)
        } else {
            callback.setCurrentVerse(reciter, translReciter, chapterNo, verseNo)
            callbacks[key] = callback
        }
    }

    private fun removeCallback(key: String) {
        callbacks.remove(key)
    }

    fun cancelAll() {
        jobs.forEach { job -> job.value.cancel() }
    }

    sealed class VerseLoadFlow {
        data class Progress(val progress: Int) : VerseLoadFlow()
        data class Loaded(val verseFile: File?, val verseTranslFile: File?) : VerseLoadFlow()
        data class Failed(
            val e: Throwable,
            val verseFile: File?,
            val verseTranslFile: File?,
            val deleteVerseFile: Boolean,
            val deleteTranslFile: Boolean
        ) : VerseLoadFlow()
    }
}