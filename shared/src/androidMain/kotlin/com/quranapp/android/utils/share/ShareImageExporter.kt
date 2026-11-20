package com.quranapp.android.utils.share

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.quranapp.android.platform.Platform
import com.quranapp.android.platform.files.PublicDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareImageExporter {
    private const val EXPORT_DIR = "share_images"
    private const val MIME_PNG = "image/png"

    suspend fun share(
        context: Context,
        bitmap: Bitmap,
        chooserTitle: CharSequence,
    ): Boolean = withContext(Dispatchers.IO) {
        val uri = writeCachePng(context, bitmap) ?: return@withContext false

        withContext(Dispatchers.Main) {
            try {
                Platform.links.shareImage(
                    uri.toString(),
                    chooserTitle.toString(),
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val displayName = "quranapp_share_${System.currentTimeMillis()}.png"
                val bytes = ByteArrayOutputStream().use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        return@withContext false
                    }
                    out.toByteArray()
                }

                Platform.externalFiles
                    .insert(PublicDirectory.Pictures, "QuranApp", displayName, MIME_PNG)
                    .buffer()
                    .use { it.write(bytes) }

                true
            } catch (_: Exception) {
                false
            } finally {
                recycleQuietly(bitmap)
            }
        }

    private fun writeCachePng(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { old ->
                if (old.isFile) old.delete()
            }
            val file = File(dir, "share_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return null
                }
            }

            recycleQuietly(bitmap)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file,
            )
        } catch (_: IOException) {
            recycleQuietly(bitmap)
            null
        }
    }

    private fun recycleQuietly(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
