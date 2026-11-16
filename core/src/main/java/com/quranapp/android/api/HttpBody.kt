package com.quranapp.android.api

import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.cancel
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

class HttpBody(
    val code: Int,
    val contentLength: Long,
    private val stream: InputStream,
    private val response: HttpResponse,
) : AutoCloseable {
    private val released = AtomicBoolean(false)

    val isSuccessful: Boolean get() = code in 200..299

    fun byteStream(): InputStream = object : FilterInputStream(stream) {
        override fun close() {
            try {
                super.close()
            } finally {
                release()
            }
        }
    }

    fun string(): String = stream.bufferedReader().use { it.readText() }

    override fun close() {
        try {
            stream.close()
        } finally {
            release()
        }
    }

    private fun release() {
        if (!released.compareAndSet(false, true)) return

        response.cancel()
    }
}
