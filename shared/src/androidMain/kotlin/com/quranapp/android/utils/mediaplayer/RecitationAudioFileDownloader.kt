package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.downloadTo
import com.quranapp.android.utils.app.AppFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

internal object RecitationAudioFileDownloader {
    suspend fun downloadToFile(
        urlStr: String,
        finalFile: Path,
        onProgress: (suspend (Long, Long) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        val parent = finalFile.parent
            ?: throw IllegalStateException("Output path has no parent directory")

        FileSystem.SYSTEM.createDirectories(parent)

        val tmpFile = parent / "${finalFile.name}.part"

        if (AppFiles.exists(tmpFile)) {
            AppFiles.delete(tmpFile)
        }

        try {
            val response = ApiClient.downloadStreaming(urlStr)

            response.downloadTo(
                tmpFile,
                onFailed = {
                    if (it.code == 404) {
                        throw IllegalStateException("Audio file not found")
                    }

                    throw IllegalStateException("Download failed: HTTP ${it.code}")
                },
                onProgress = onProgress?.let { progress ->
                    { downloaded, total ->
                        progress(downloaded, total ?: -1L)
                    }
                },
            )

            if (AppFiles.size(tmpFile) <= 0L) {
                AppFiles.delete(tmpFile)
                throw IllegalStateException("Download produced empty file")
            }

            if (AppFiles.exists(finalFile)) {
                AppFiles.delete(finalFile)
            }

            AppFiles.move(tmpFile, finalFile)
        } catch (e: CancellationException) {
            AppFiles.delete(tmpFile)
            throw e
        } catch (e: Exception) {
            AppFiles.delete(tmpFile)
            throw e
        }
    }
}
