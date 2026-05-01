package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal object RecitationAudioFileDownloader {
    private const val DOWNLOAD_BUFFER_SIZE = 4096

    suspend fun downloadToFile(
        urlStr: String,
        finalFile: File,
        onProgress: suspend (Long, Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val parent = finalFile.parentFile
            ?: throw IllegalStateException("Output path has no parent directory")

        val tempFile = File(parent, "${finalFile.name}.part")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            val response = RetrofitInstance.any.downloadStreaming(urlStr)

            if (response.code() == 404) {
                throw IllegalStateException("Audio file not found")
            }

            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed: HTTP ${response.code()}")
            }

            val body = response.body()
                ?: throw IllegalStateException("Download response body is null")

            val totalLength = body.contentLength()

            body.use { responseBody ->
                responseBody.byteStream().buffered(DOWNLOAD_BUFFER_SIZE).use { input ->
                    FileOutputStream(tempFile).buffered(DOWNLOAD_BUFFER_SIZE).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        var totalConsumed = 0L
                        var lastEmitTime = 0L

                        while (true) {
                            ensureActive()

                            val bytes = input.read(buffer)
                            if (bytes <= 0) break

                            output.write(buffer, 0, bytes)
                            totalConsumed += bytes

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 200L) {
                                lastEmitTime = now
                                if (totalLength > 0L) {
                                    onProgress(totalConsumed, totalLength)
                                } else if (totalConsumed > 0L) {
                                    onProgress(totalConsumed, -1L)
                                }
                            }
                        }

                        // ensure final progress is emitted
                        if (totalLength > 0L) {
                            onProgress(totalConsumed, totalLength)
                        } else if (totalConsumed > 0L) {
                            onProgress(totalConsumed, -1L)
                        }

                        output.flush()
                    }
                }
            }

            commitTempFile(tempFile, finalFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun commitTempFile(tempFile: File, finalFile: File) {
        if (tempFile.renameTo(finalFile)) return

        try {
            tempFile.copyTo(finalFile, overwrite = true)
        } catch (e: Exception) {
            finalFile.delete()
            throw e
        }

        if (!tempFile.delete()) {
            finalFile.delete()
            throw IllegalStateException("Could not finalize download file")
        }
    }
}
