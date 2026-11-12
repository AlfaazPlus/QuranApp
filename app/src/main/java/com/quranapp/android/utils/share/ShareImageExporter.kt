package com.quranapp.android.utils.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.peacedesign.android.utils.AppBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                AppBridge.newSharer(context)
                    .setData(uri)
                    .setChooserTitle(chooserTitle)
                    .setPlatform(AppBridge.Platform.SYSTEM_SHARE)
                    .share()
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
                val resolver = context.contentResolver

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/QuranApp",
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values,
                    ) ?: return@withContext false

                    resolver.openOutputStream(uri)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            return@withContext false
                        }
                    } ?: return@withContext false

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    val pictures = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES,
                    )
                    val dir = File(pictures, "QuranApp").apply { mkdirs() }
                    val file = File(dir, displayName)
                    FileOutputStream(file).use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            return@withContext false
                        }
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, file.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                    }
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    true
                }
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
