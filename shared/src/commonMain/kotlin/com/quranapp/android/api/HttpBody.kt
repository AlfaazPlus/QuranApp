package com.quranapp.android.api


import com.quranapp.android.utils.miscellenous.DateUtils
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer

class HttpBody(
    val code: Int,
    val contentLength: Long?,
    private val channel: ByteReadChannel,
    private val response: HttpResponse
) {
    val isSuccessful: Boolean
        get() = code in 200..299

    suspend fun readAvailable(buffer: ByteArray): Int =
        channel.readAvailable(buffer)

    suspend fun readRemaining(): ByteArray =
        channel.readBuffer().readByteArray()

    fun cancel() {
        channel.cancel(CancellationException("Response closed"))
        response.call.cancel()
    }
}

private const val DOWNLOAD_PROGRESS_THROTTLE_MS = 200L

suspend fun HttpBody.downloadTo(
    path: Path,
    onFailed: ((HttpBody) -> Unit) = {
        throw Exception("Download failed: HTTP $code")
    },
    onProgress: (suspend (downloaded: Long, total: Long?) -> Unit)? = null,
) {
    if (!isSuccessful) {
        cancel()
        onFailed(this)
        return
    }

    val buffer = ByteArray(64 * 1024)
    val total = contentLength
    var downloaded = 0L
    var lastEmitTime = 0L
    val progress = onProgress

    try {
        FileSystem.SYSTEM.createDirectories(path.parent!!)

        FileSystem.SYSTEM.sink(path).buffer().use { sink ->
            while (true) {
                val count = readAvailable(buffer)

                if (count == -1) break
                if (count == 0) continue

                sink.write(buffer, 0, count)
                downloaded += count

                if (progress == null) continue

                val now = DateUtils.now().toEpochMilliseconds()
                val isFinished = total != null && total > 0L && downloaded == total

                if (now - lastEmitTime >= DOWNLOAD_PROGRESS_THROTTLE_MS || isFinished) {
                    lastEmitTime = now
                    progress(downloaded, total)
                }
            }
        }

        if (progress != null && downloaded > 0L) {
            progress(downloaded, total)
        }
    } finally {
        cancel()
    }
}
