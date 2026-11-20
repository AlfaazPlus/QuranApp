@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.quranapp.android.utils.reader.atlas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import kotlin.concurrent.atomics.AtomicReference

/** One bundled page is about 4096×4096 alpha. Reject anything larger before decoding. */
const val MAX_ATLAS_PAGE_PIXELS = 20L * 1024L * 1024L

interface AtlasPageListener {
    fun onDecoded(index: Int)
    fun onCleared() {}
}

private class TextureStoreState(
    val generation: Int,
    val pages: Map<Int, AtlasPixels>,
)

/**
 * Decoded atlas pages for one bundle.
 *
 * Pages stay as one alpha mask. Android fills that mask from short strips, so a
 * grayscale page is not decoded as a full-color bitmap. Glyph rectangles stay in
 * the PNG's pixel space: a failed decode is not retried at a smaller size.
 */
class QuranAtlasTextureStore(
    private val filesByIndex: Map<Int, Path>,
    private val maxPagePixels: Long = MAX_ATLAS_PAGE_PIXELS,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    val size: Int = filesByIndex.size

    val onlyIndex: Int? = filesByIndex.keys.singleOrNull()

    private val mutex = Mutex()
    private val state = AtomicReference(TextureStoreState(0, emptyMap()))
    private val listeners = AtomicReference<List<AtlasPageListener>>(emptyList())

    fun cached(index: Int): AtlasPixels? = state.load().pages[index]

    /**
     * Decode on the caller thread. Export and the widget rasterizer call this
     * off the main thread. The reader only draws [cached] pages.
     */
    fun load(index: Int): AtlasPixels? {
        cached(index)?.let { return it }

        return runBlocking(Dispatchers.IO) {
            decodeOnIo(index)
        }
    }

    suspend fun decode(index: Int): AtlasPixels? {
        cached(index)?.let { return it }

        return withContext(Dispatchers.IO) {
            decodeOnIo(index)
        }
    }

    private suspend fun decodeOnIo(index: Int): AtlasPixels? {
        cached(index)?.let { return it }

        val generation = state.load().generation
        var created = false

        val pixels = mutex.withLock {
            cached(index)?.let { return@withLock it }

            val decoded = read(index) ?: return@withLock null

            if (!publish(generation, index, decoded)) {
                decoded.release()
                null
            } else {
                created = true
                decoded
            }
        } ?: return null

        if (created && cached(index) === pixels) {
            notifyDecoded(index)
        }

        return pixels
    }

    suspend fun prefetch(indices: Collection<Int>) {
        if (indices.isEmpty()) return

        val pending = LinkedHashSet<Int>(indices.size)

        for (index in indices) {
            if (cached(index) == null) {
                pending.add(index)
            }
        }

        for (index in pending) {
            decode(index)
        }
    }

    fun addListener(listener: AtlasPageListener, replay: Boolean = true) {
        while (true) {
            val current = listeners.load()

            if (current.any { it === listener }) return

            if (listeners.compareAndSet(current, current + listener)) break
        }

        if (!replay) return

        for (index in state.load().pages.keys) {
            listener.onDecoded(index)
        }
    }

    fun removeListener(listener: AtlasPageListener) {
        while (true) {
            val current = listeners.load()
            val next = current.filterNot { it === listener }

            if (next.size == current.size) return

            if (listeners.compareAndSet(current, next)) return
        }
    }

    fun remove(index: Int) {
        while (true) {
            val current = state.load()

            if (!current.pages.containsKey(index)) return

            val next = TextureStoreState(
                generation = current.generation,
                pages = current.pages - index,
            )

            if (state.compareAndSet(current, next)) return
        }
    }

    fun clear() {
        while (true) {
            val current = state.load()
            val next = TextureStoreState(
                generation = current.generation + 1,
                pages = emptyMap(),
            )

            if (!state.compareAndSet(current, next)) continue

            notifyCleared()
            return
        }
    }

    private fun read(index: Int): AtlasPixels? {
        val file = filesByIndex[index] ?: return null

        val fileSize = try {
            fileSystem.metadataOrNull(file)?.size
        } catch (_: Exception) {
            null
        } ?: return null

        if (fileSize <= 0L) return null

        val dimensions = readPngDimensions(file, fileSystem) ?: return null

        if (dimensions.pixels <= 0L || dimensions.pixels > maxPagePixels) {
            return null
        }

        return decodeAtlasPage(
            file = file,
            width = dimensions.width,
            height = dimensions.height,
            colorType = dimensions.colorType,
        )
    }

    private fun publish(generation: Int, index: Int, pixels: AtlasPixels): Boolean {
        while (true) {
            val current = state.load()

            if (current.generation != generation) return false

            val next = TextureStoreState(
                generation = current.generation,
                pages = current.pages + (index to pixels),
            )

            if (state.compareAndSet(current, next)) return true
        }
    }

    private fun notifyDecoded(index: Int) {
        val snapshot = listeners.load()

        for (listener in snapshot) {
            listener.onDecoded(index)
        }
    }

    private fun notifyCleared() {
        val snapshot = listeners.load()

        for (listener in snapshot) {
            listener.onCleared()
        }
    }
}
