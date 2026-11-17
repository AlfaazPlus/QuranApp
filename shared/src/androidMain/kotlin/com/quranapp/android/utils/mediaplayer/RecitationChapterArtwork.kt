package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.quran_wallpaper
import com.quranapp.android.generated.resources.suracon
import com.quranapp.android.resources.ComposeResourceFiles
import com.quranapp.android.utils.quran.QuranGlyphs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

object RecitationChapterArtwork {

    private const val ARTWORK_VERSION = 2
    private const val SIZE = 600

    private val artworkMutex = Mutex()

    private fun artworkFile(context: Context, chapterNo: Int): File {
        return File(
            context.applicationContext.cacheDir,
            "artwork_surah_v${ARTWORK_VERSION}_$chapterNo.png"
        )
    }

    suspend fun getChapterArtworkUri(
        context: Context,
        chapterNo: Int,
    ): Uri {
        val appContext = context.applicationContext
        val file = artworkFile(appContext, chapterNo)

        return artworkMutex.withLock {
            try {
                if (!file.exists() || file.length() == 0L) {
                    createArtwork(appContext, chapterNo, file)
                }

                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.provider",
                    file,
                )

                grantGearheadAutoRead(appContext, uri)

                uri
            } catch (e: Exception) {
                e.printStackTrace()
                androidFallbackWallpaperUri(appContext)
            }
        }
    }

    private fun createArtwork(
        context: Context,
        chapterNo: Int,
        file: File,
    ) {
        val bitmap = createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)

        try {
            val canvas = Canvas(bitmap)

            ComposeResourceFiles.drawableBitmap(Res.drawable.quran_wallpaper)?.let { wallpaper ->
                canvas.drawBitmap(wallpaper, null, android.graphics.Rect(0, 0, SIZE, SIZE), null)
                wallpaper.recycle()
            }

            if (chapterNo > 0) {
                drawChapterNumber(
                    context = context,
                    canvas = canvas,
                    chapterNo = chapterNo,
                )
            }

            // Write atomically so another coroutine never reads
            // a partially-written PNG.
            val tempFile = File(file.parentFile, "${file.name}.tmp")

            FileOutputStream(tempFile).use { output ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        output,
                    )
                ) {
                    "Failed to compress artwork"
                }

                output.flush()
                output.fd.sync()
            }

            if (!tempFile.renameTo(file)) {
                tempFile.delete()
                throw IOException("Could not rename artwork file")
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawChapterNumber(
        context: Context,
        canvas: Canvas,
        chapterNo: Int,
    ) {
        val typeface = ComposeResourceFiles.typeface(Res.font.suracon, "suracon.ttf")

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.color = android.graphics.Color.argb(191, 255, 255, 255)
            textAlign = Paint.Align.CENTER
        }

        val chapterText = QuranGlyphs.Chapter.get(chapterNo)

        val padding = SIZE * 0.15f
        val maxTextWidth = SIZE - padding * 2

        var textSize = SIZE * 0.5f

        paint.textSize = textSize

        val textWidth = paint.measureText(chapterText)

        if (textWidth > maxTextWidth) {
            textSize *= maxTextWidth / textWidth
            paint.textSize = textSize
        }

        val textY =
            SIZE / 2f -
                    (paint.descent() + paint.ascent()) / 2f

        canvas.drawText(
            chapterText,
            SIZE / 2f,
            textY,
            paint,
        )
    }

    suspend fun getChapterArtworkBitmap(
        context: Context,
        chapterNo: Int,
        maxSidePx: Int,
    ): Bitmap {
        val app = context.applicationContext

        getChapterArtworkUri(app, chapterNo)

        return withContext(Dispatchers.IO) {
            val file = artworkFile(app, chapterNo)

            decodeSampledBitmap(
                file = file,
                maxSidePx = maxSidePx,
            ) ?: ComposeResourceFiles.drawableBitmap(Res.drawable.quran_wallpaper)
            ?: createBitmap(1, 1)
        }
    }

    private fun decodeSampledBitmap(
        file: File,
        maxSidePx: Int,
    ): Bitmap? {
        if (!file.exists() || file.length() <= 0L) {
            return null
        }

        val cap = max(1, maxSidePx)

        // First determine dimensions without allocating pixels.
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            file.absolutePath,
            bounds,
        )

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sampleSize = 1

        while (
            bounds.outWidth / (sampleSize * 2) >= cap &&
            bounds.outHeight / (sampleSize * 2) >= cap
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeFile(
            file.absolutePath,
            options,
        )
    }

    private fun grantGearheadAutoRead(
        context: Context,
        uri: Uri,
    ) {
        try {
            context.grantUriPermission(
                "com.google.android.projection.gearhead",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )

            context.grantUriPermission(
                "com.google.android.autosimulator",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
    }

    fun androidFallbackWallpaperUri(
        context: Context,
    ): Uri {
        val file = File(context.cacheDir, "quran_wallpaper.webp")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(ComposeResourceFiles.drawableBytes(Res.drawable.quran_wallpaper))
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )
    }
}
