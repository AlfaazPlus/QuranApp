package com.quranapp.android.views.recitation

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.quranapp.android.utils.exceptions.HttpNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class RecitationPlayerVerseLoader {
    private val callbacks = HashMap<String, RecitationPlayerVerseLoadCallback?>()
    private val jobs = HashMap<String, Job>()
    private val handler = Handler(Looper.getMainLooper())

    fun addTask(
        verseFile: File,
        url: String,
        reciter: String,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        load(verseFile, url, reciter, chapterNo, verseNo, callback)
    }

    private fun load(
        verseFile: File,
        urlStr: String,
        slug: String,
        chapterNo: Int,
        verseNo: Int,
        callback: RecitationPlayerVerseLoadCallback?
    ) {
        addCallback(slug, chapterNo, verseNo, callback)

        val job = Job()
        jobs[makeKey(slug, chapterNo, verseNo)] = job

        CoroutineScope(job).launch {
            flow {
                val url = URL(urlStr)

                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Content-Length", "0")
                conn.setRequestProperty("Connection", "close")
                conn.connectTimeout = 180000
                conn.readTimeout = 180000
                conn.allowUserInteraction = false
                conn.connect()

                if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw HttpNotFoundException()
                }

                emit(VerseLoadFlow.Progress(0))

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
                            emit(VerseLoadFlow.Progress((totalConsumed / totalLength * 100).toInt()))
                        }

                        output.flush()

                        emit(VerseLoadFlow.Loaded(verseFile))
                    }
                }
            }.flowOn(Dispatchers.IO).catch {
                it.printStackTrace()
                emit(VerseLoadFlow.Failed(it, verseFile))
            }.collect {
                handler.post {
                    when (it) {
                        is VerseLoadFlow.Progress -> {
                            callback?.onProgress(String.format(Locale.getDefault(), "%d%%", it.progress))
                        }
                        is VerseLoadFlow.Failed -> {
                            it.e.printStackTrace()
                            publishVerseLoadStatus(verseFile, slug, chapterNo, verseNo, false, it.e)
                        }
                        is VerseLoadFlow.Loaded -> {
                            publishVerseLoadStatus(verseFile, slug, chapterNo, verseNo, true, null)
                        }
                    }
                }
            }
        }
    }

    private fun publishVerseLoadStatus(
        verseFile: File,
        reciter: String,
        chapterNo: Int,
        verseNo: Int,
        loaded: Boolean,
        e: Throwable?
    ) {
        val key = makeKey(reciter, chapterNo, verseNo)
        publishVerseLoadStatus(verseFile, callbacks[key], loaded, e)
        removeCallback(key)
        jobs.remove(key)
    }


    fun publishVerseLoadStatus(
        verseFile: File,
        callback: RecitationPlayerVerseLoadCallback?,
        loaded: Boolean,
        e: Throwable?
    ) {
        callback?.let {
            if (loaded) it.onLoaded(verseFile)
            else it.onFailed(e, verseFile)

            it.postLoad()
        }
    }

    private fun makeKey(reciter: String, chapterNo: Int, verseNo: Int): String {
        return "$reciter:$chapterNo:$verseNo"
    }

    fun isPending(reciter: String, chapterNo: Int, verseNo: Int): Boolean {
        return jobs[makeKey(reciter, chapterNo, verseNo)]?.isActive == true
    }

    fun addCallback(reciter: String, chapterNo: Int, verseNo: Int, callback: RecitationPlayerVerseLoadCallback?) {
        val key = makeKey(reciter, chapterNo, verseNo)

        if (callback == null) {
            callbacks.remove(key)
        } else {
            callback.setCurrentVerse(reciter, chapterNo, verseNo)
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
        data class Loaded(val file: File) : VerseLoadFlow()
        data class Failed(val e: Throwable, val file: File) : VerseLoadFlow()
    }
}