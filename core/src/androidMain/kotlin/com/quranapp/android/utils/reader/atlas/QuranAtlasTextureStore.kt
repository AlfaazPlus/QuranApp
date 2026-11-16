package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * One decoded atlas page, shared by every glyph.
 *
 * A page is a grayscale coverage mask. Stored as ALPHA_8 that is 1 byte per pixel
 * (about 14MB for the bundled 4093×3409 page). Glyphs are rectangles inside that
 * image, so they cost no extra bitmap.
 *
 */
private const val MAX_PAGE_PIXELS = 20 * 1024 * 1024

data class QuranAtlasTexture(
    val bitmap: Bitmap,
)

interface AtlasPageListener {
    fun onDecoded(index: Int, bitmap: Bitmap)
    fun onCleared() {}
}

class QuranAtlasTextureStore(
    private val filesByIndex: Map<Int, File>,
) {
    private val pages = HashMap<Int, QuranAtlasTexture>()
    private val listeners = ArrayList<AtlasPageListener>(2)

    val size: Int
        get() = filesByIndex.size

    val onlyIndex: Int?
        get() = filesByIndex.keys.singleOrNull()

    @Synchronized
    fun cached(index: Int): QuranAtlasTexture? = pages[index]

    /** Decode this page if it is not already in memory. Safe to call from a background thread. */
    fun load(index: Int): QuranAtlasTexture? {
        cached(index)?.let { return it }

        val bitmap = decode(index) ?: return null
        return put(index, bitmap)
    }

    suspend fun prefetch(indices: Collection<Int>) {
        val missing = indices.distinct().filter { cached(it) == null }
        if (missing.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (index in missing) {
                load(index)
            }
        }
    }

    fun addListener(listener: AtlasPageListener, replay: Boolean = true) {
        val existing = synchronized(this) {
            if (listeners.any { it === listener }) return
            listeners.add(listener)
            if (!replay || pages.isEmpty()) emptyList() else pages.map { it.key to it.value.bitmap }
        }

        for ((index, bitmap) in existing) {
            listener.onDecoded(index, bitmap)
        }
    }

    fun removeListener(listener: AtlasPageListener) {
        synchronized(this) {
            listeners.remove(listener)
        }
    }

    fun clear() {
        val notify = synchronized(this) {
            pages.clear()
            listeners.toList()
        }

        for (listener in notify) {
            listener.onCleared()
        }
    }

    private fun put(index: Int, bitmap: Bitmap): QuranAtlasTexture {
        val notify = ArrayList<AtlasPageListener>(2)

        val texture = synchronized(this) {
            pages[index]?.let { existing ->
                if (existing.bitmap !== bitmap) bitmap.recycle()
                return existing
            }

            QuranAtlasTexture(bitmap).also { created ->
                pages[index] = created
                notify.addAll(listeners)
            }
        }

        for (listener in notify) {
            listener.onDecoded(index, bitmap)
        }

        return texture
    }

    private fun decode(index: Int): Bitmap? {
        val file = filesByIndex[index]?.takeIf { it.isFile && it.length() > 0L } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)

        val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || pixels > MAX_PAGE_PIXELS) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            // Screen density must not scale a 4096px atlas up again.
            inScaled = false
            inPreferredConfig = Bitmap.Config.ALPHA_8
            inPremultiplied = false
        }

        val decoded = try {
            BitmapFactory.decodeFile(file.path, options)
        } catch (_: OutOfMemoryError) {
            null
        } ?: return null

        return try {
            toAlphaMask(decoded)
        } catch (_: OutOfMemoryError) {
            if (decoded.isRecycled) null else decoded
        }
    }

    /**
     * Some devices ignore ALPHA_8 and return an opaque RGB bitmap (4 bytes per pixel).
     * The glyph shape is the gray value, so copy that into alpha and free the RGB bitmap.
     */
    private fun toAlphaMask(source: Bitmap): Bitmap {
        if (source.config == Bitmap.Config.ALPHA_8) return source

        val mask = createBitmap(source.width, source.height, Bitmap.Config.ALPHA_8)
        val width = source.width
        val height = source.height
        val stride = mask.rowBytes
        val gray = IntArray(width)
        val packed = ByteArray(stride * height)

        for (y in 0 until height) {
            source.getPixels(gray, 0, width, 0, y, width, 1)
            val row = y * stride

            for (x in 0 until width) {
                packed[row + x] = (gray[x] ushr 16 and 0xFF).toByte()
            }
        }

        try {
            mask.copyPixelsFromBuffer(ByteBuffer.wrap(packed))
        } catch (_: RuntimeException) {
            mask.recycle()
            return source
        }

        source.recycle()

        return mask
    }
}
