package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import androidx.core.graphics.createBitmap
import okio.Path
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min

private const val BAND_ROWS = 64

actual class AtlasPixels(
    val bitmap: Bitmap,
) {
    actual val width: Int = bitmap.width
    actual val height: Int = bitmap.height
}

internal actual fun AtlasPixels.release() {
    if (!bitmap.isRecycled) {
        bitmap.recycle()
    }
}

/**
 * The bundled page is 8-bit grayscale, about 14 million pixels.
 * A full ARGB decode is roughly 56MB. Decode it in short strips and keep one ALPHA_8 mask.
 */
internal actual fun decodeAtlasPage(
    file: Path,
    width: Int,
    height: Int,
    colorType: Int,
): AtlasPixels? {
    if (width <= 0 || height <= 0) return null

    val path = file.toFile().path
    val shift = if (colorType == 4 || colorType == 6) 24 else 16

    val banded = try {
        decodeInBands(path, width, height, shift)
    } catch (_: OutOfMemoryError) {
        return null
    }

    if (banded != null) return banded

    return try {
        decodeWholeFile(path)
    } catch (_: OutOfMemoryError) {
        null
    }
}

private fun decodeInBands(
    path: String,
    width: Int,
    height: Int,
    shift: Int,
): AtlasPixels? {
    val decoder = openRegionDecoder(path) ?: return null

    var mask: Bitmap? = null
    var band: Bitmap? = null

    try {
        if (decoder.width != width || decoder.height != height) return null

        val sampleCount = width.toLong() * BAND_ROWS.toLong()

        if (sampleCount <= 0L || sampleCount > Int.MAX_VALUE) return null

        val created = createBitmap(width, height, Bitmap.Config.ALPHA_8)
        mask = created

        val stride = created.rowBytes
        val byteCount = stride.toLong() * height.toLong()

        if (byteCount <= 0L || byteCount > Int.MAX_VALUE) return null

        val packed = ByteArray(byteCount.toInt())
        val pixels = IntArray(sampleCount.toInt())
        var top = 0

        while (top < height) {
            val rows = min(BAND_ROWS, height - top)
            val decoded = decodeBand(decoder, band, width, top, rows) ?: return null

            if (decoded !== band) {
                band?.recycle()
                band = decoded
            }

            decoded.getPixels(pixels, 0, width, 0, 0, width, rows)

            for (row in 0 until rows) {
                val src = row * width
                val dest = (top + row) * stride

                for (x in 0 until width) {
                    packed[dest + x] = ((pixels[src + x] ushr shift) and 0xFF).toByte()
                }
            }

            top += rows
        }

        band?.recycle()
        band = null

        created.copyPixelsFromBuffer(ByteBuffer.wrap(packed))
        mask = null

        return AtlasPixels(created)
    } catch (_: OutOfMemoryError) {
        return null
    } catch (_: RuntimeException) {
        return null
    } finally {
        band?.recycle()

        if (mask != null && !mask.isRecycled) {
            mask.recycle()
        }

        decoder.recycle()
    }
}

private fun decodeBand(
    decoder: BitmapRegionDecoder,
    reusable: Bitmap?,
    width: Int,
    top: Int,
    rows: Int,
): Bitmap? {
    val region = Rect(0, top, width, top + rows)
    val canReuse = reusable != null &&
            !reusable.isRecycled &&
            reusable.width == width &&
            reusable.height == rows

    if (canReuse) {
        val reused = decodeRegion(decoder, region, reusable)

        if (reused != null) return reused
    }

    return decodeRegion(decoder, region, null)
}

private fun decodeRegion(
    decoder: BitmapRegionDecoder,
    region: Rect,
    reusable: Bitmap?,
): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inPremultiplied = false
        inMutable = true
        inBitmap = reusable
    }

    return try {
        decoder.decodeRegion(region, options)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun decodeWholeFile(path: String): AtlasPixels? {
    val options = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = Bitmap.Config.ALPHA_8
        inPremultiplied = false
    }

    val decoded = BitmapFactory.decodeFile(path, options) ?: return null

    if (decoded.config == Bitmap.Config.ALPHA_8) {
        return AtlasPixels(decoded)
    }

    val mask = toAlphaMask(decoded)

    if (mask !== decoded && !decoded.isRecycled) {
        decoded.recycle()
    }

    return AtlasPixels(mask)
}

private fun toAlphaMask(source: Bitmap): Bitmap {
    val mask = try {
        createBitmap(source.width, source.height, Bitmap.Config.ALPHA_8)
    } catch (_: OutOfMemoryError) {
        return source
    }

    val width = source.width
    val height = source.height
    val stride = mask.rowBytes
    val byteCount = stride.toLong() * height.toLong()

    if (byteCount <= 0L || byteCount > Int.MAX_VALUE) {
        mask.recycle()
        return source
    }

    val gray = IntArray(width)
    val packed = try {
        ByteArray(byteCount.toInt())
    } catch (_: OutOfMemoryError) {
        mask.recycle()
        return source
    }

    for (y in 0 until height) {
        source.getPixels(gray, 0, width, 0, y, width, 1)
        val row = y * stride

        for (x in 0 until width) {
            packed[row + x] = ((gray[x] ushr 16) and 0xFF).toByte()
        }
    }

    try {
        mask.copyPixelsFromBuffer(ByteBuffer.wrap(packed))
    } catch (_: RuntimeException) {
        mask.recycle()
        return source
    }

    return mask
}

private fun openRegionDecoder(path: String): BitmapRegionDecoder? {
    return try {
        if (Build.VERSION.SDK_INT >= 31) {
            BitmapRegionDecoder.newInstance(path)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(path, false)
        }
    } catch (_: IOException) {
        null
    }
}
