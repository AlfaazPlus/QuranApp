package com.quranapp.android.utils.mediaplayer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.peacedesign.android.utils.ColorUtils
import com.quranapp.android.R
import com.quranapp.android.utils.quran.QuranGlyphs
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object RecitationChapterArtwork {
    const val ARTWORK_VERSION = 2

    fun getChapterArtworkUri(context: Context, chapterNo: Int): Uri {
        val appContext = context.applicationContext
        return try {
            val file =
                File(appContext.cacheDir, "artwork_surah_v${ARTWORK_VERSION}_$chapterNo.png")

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.provider",
                file,
            )

            grantGearheadAutoRead(appContext, uri)

            if (file.exists()) {
                return uri
            }

            val size = 600
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)

            ContextCompat.getDrawable(appContext, R.drawable.quran_wallpaper)?.let {
                it.setBounds(0, 0, size, size)
                it.draw(canvas)
            }

            if (chapterNo > 0) {
                val typeface = ResourcesCompat.getFont(appContext, R.font.suracon)

                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.color = ColorUtils.createAlphaColor(Color.WHITE, 0.75f)
                    textAlign = Paint.Align.CENTER
                }

                val chapterText = QuranGlyphs.Chapter.get(chapterNo)

                val padding = size * 0.15f
                val maxTextWidth = size - padding * 2

                var textSize = size * 0.5f
                paint.textSize = textSize
                val textWidth = paint.measureText(chapterText)

                if (textWidth > maxTextWidth) {
                    val scale = maxTextWidth / textWidth
                    textSize *= scale
                    paint.textSize = textSize
                }

                val textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText(chapterText, size / 2f, textY, paint)
            }

            ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                try {
                    file.writeBytes(outputStream.toByteArray())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bitmap.recycle()

            uri
        } catch (_: Exception) {
            androidFallbackWallpaperUri(appContext)
        }
    }

    private fun grantGearheadAutoRead(context: Context, uri: Uri) {
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

    fun loadChapterArtworkThumbnailBitmap(
        context: Context,
        chapterNo: Int,
        maxSidePx: Int,
    ): Bitmap {
        val app = context.applicationContext
        getChapterArtworkUri(app, chapterNo)

        val file = File(app.cacheDir, "artwork_surah_v${ARTWORK_VERSION}_$chapterNo.png")

        val raw = try {
            if (file.exists() && file.length() > 0L) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } ?: try {
            BitmapFactory.decodeResource(app.resources, R.drawable.quran_wallpaper)
        } catch (_: Exception) {
            null
        } ?: createBitmap(1, 1)

        val w = raw.width
        val h = raw.height
        if (w <= 0 || h <= 0) return raw

        val cap = max(1, maxSidePx)
        if (w <= cap && h <= cap) return raw

        val scale = minOf(cap.toFloat() / w, cap.toFloat() / h)
        val nw = max(1, (w * scale).roundToInt())
        val nh = max(1, (h * scale).roundToInt())
        val scaled = raw.scale(nw, nh)
        if (scaled != raw) raw.recycle()
        return scaled
    }

    fun androidFallbackWallpaperUri(context: Context): Uri {
        val resId = R.drawable.quran_wallpaper
        return (
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                        context.resources.getResourcePackageName(resId) +
                        '/' +
                        context.resources.getResourceTypeName(resId) +
                        '/' +
                        context.resources.getResourceEntryName(resId)
                ).toUri()
    }
}
