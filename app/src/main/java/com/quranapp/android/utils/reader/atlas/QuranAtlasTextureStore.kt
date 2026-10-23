package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val ATLAS_TEXTURE_CACHE_FRACTION = 16
private const val ATLAS_TEXTURE_CACHE_MIN_BYTES = 4 * 1024 * 1024
private const val ATLAS_TEXTURE_CACHE_MAX_BYTES = 16 * 1024 * 1024
private const val MAX_TEXTURE_BYTES = 16 * 1024 * 1024

data class QuranAtlasTexture(
    val bitmap: Bitmap,
    val imageBitmap: ImageBitmap,
)

class QuranAtlasTextureStore(
    private val filesByIndex: Map<Int, File>,
) {
    private val cache = object : LruCache<Int, QuranAtlasTexture>(cacheSizeBytes()) {
        override fun sizeOf(key: Int, value: QuranAtlasTexture): Int =
            value.bitmap.allocationByteCount
    }

    val size: Int
        get() = filesByIndex.size

    val onlyIndex: Int?
        get() = filesByIndex.keys.singleOrNull()

    @Synchronized
    fun get(index: Int): QuranAtlasTexture? {
        cache.get(index)?.let { return it }

        val file = filesByIndex[index]?.takeIf { it.isFile && it.length() > 0L } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        BitmapFactory.decodeFile(file.path, bounds)

        val estimatedBytes = bounds.outWidth.toLong() * bounds.outHeight.toLong()

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || estimatedBytes > MAX_TEXTURE_BYTES) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ALPHA_8
        }

        val bitmap = try {
            BitmapFactory.decodeFile(file.path, decodeOptions)
        } catch (_: OutOfMemoryError) {
            null
        } ?: return null

        val texture = try {
            QuranAtlasTexture(bitmap, bitmap.asImageBitmap())
        } catch (_: OutOfMemoryError) {
            bitmap.recycle()
            return null
        }

        cache.put(index, texture)

        return texture
    }

    suspend fun prefetch(indices: Collection<Int>) {
        val missing = indices
            .asSequence()
            .distinct()
            .filter { cache.get(it) == null }
            .toList()

        if (missing.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (index in missing) {
                get(index)
            }
        }
    }

    fun clear() {
        cache.evictAll()
    }

    private fun cacheSizeBytes(): Int {
        val runtime = Runtime.getRuntime()
        val available = (runtime.maxMemory() / ATLAS_TEXTURE_CACHE_FRACTION)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        return available.coerceIn(
            ATLAS_TEXTURE_CACHE_MIN_BYTES,
            ATLAS_TEXTURE_CACHE_MAX_BYTES,
        )
    }
}
