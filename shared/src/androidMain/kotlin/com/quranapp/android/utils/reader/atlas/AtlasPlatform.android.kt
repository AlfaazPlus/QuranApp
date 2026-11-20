package com.quranapp.android.utils.reader.atlas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import android.graphics.Bitmap as AndroidBitmap

actual typealias AtlasBitmap = AndroidBitmap

internal actual fun AtlasPixels.platformBitmap(): AtlasBitmap = bitmap

internal actual fun AtlasBitmap.toComposeImage(): ImageBitmap = asImageBitmap()

internal actual fun createAtlasBitmap(width: Int, height: Int): AtlasBitmap =
    createBitmap(width, height)

internal actual fun AtlasBitmap.atlasSurface(argbColor: Int): AtlasSurface {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(argbColor, PorterDuff.Mode.SRC_IN)
    }
    val canvas = Canvas(this)

    return AtlasSurface { source, srcLeft, srcTop, srcRight, srcBottom, dstLeft, dstTop, dstRight, dstBottom ->
        canvas.drawBitmap(
            source,
            Rect(srcLeft, srcTop, srcRight, srcBottom),
            Rect(dstLeft, dstTop, dstRight, dstBottom),
            paint,
        )
    }
}

internal actual fun AtlasBitmap.encodePng(): ByteArray? {
    val out = ByteArrayOutputStream(getWidth() * getHeight() / 4)

    if (!compress(AndroidBitmap.CompressFormat.PNG, 100, out)) {
        return null
    }

    return out.toByteArray()
}

internal actual fun AtlasBitmap.recycleAtlas() {
    if (!isRecycled) {
        recycle()
    }
}

internal actual class PlacementCache actual constructor() {
    private val map = ConcurrentHashMap<String, List<AtlasGlyphPlacement>>()

    actual fun get(key: String): List<AtlasGlyphPlacement>? = map[key]

    actual fun contains(key: String): Boolean = map.containsKey(key)

    actual fun getOrInsert(
        key: String,
        value: List<AtlasGlyphPlacement>,
    ): List<AtlasGlyphPlacement> {
        return map.putIfAbsent(key, value) ?: value
    }
}

internal actual class AtlasSignalMap actual constructor() {
    private val map = WeakHashMap<QuranAtlasBundle, AtlasDrawSignal>()

    actual fun getOrPut(
        bundle: QuranAtlasBundle,
        create: () -> AtlasDrawSignal,
    ): AtlasDrawSignal {
        synchronized(map) {
            map[bundle]?.let { return it }

            val created = create()
            map[bundle] = created
            return created
        }
    }
}

internal actual class MainPoster actual constructor() {
    private val handler = Handler(Looper.getMainLooper())

    actual fun post(block: () -> Unit) {
        handler.post(block)
    }
}
