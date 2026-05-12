package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val ATLAS_TEXTURE_CACHE_FRACTION = 8
private const val ATLAS_TEXTURE_CACHE_MIN_BYTES = 8 * 1024 * 1024
private const val ATLAS_TEXTURE_CACHE_MAX_BYTES = 32 * 1024 * 1024

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

        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: QuranAtlasTexture,
            newValue: QuranAtlasTexture?,
        ) {
            if (oldValue.bitmap != newValue?.bitmap) {
                oldValue.bitmap.recycle()
            }
        }
    }

    val size: Int
        get() = filesByIndex.size

    val onlyIndex: Int?
        get() = filesByIndex.keys.singleOrNull()

    @Synchronized
    fun get(index: Int): QuranAtlasTexture? {
        cache.get(index)?.let { return it }

        val file = filesByIndex[index]?.takeIf { it.isFile && it.length() > 0L } ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ALPHA_8
        }
        val bitmap = BitmapFactory.decodeFile(file.path, decodeOptions) ?: return null
        val texture = QuranAtlasTexture(bitmap, bitmap.asImageBitmap())

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
