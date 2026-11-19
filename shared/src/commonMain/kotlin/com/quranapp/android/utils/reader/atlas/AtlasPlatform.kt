package com.quranapp.android.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform bitmap for one atlas page or a rasterized ayah.
 * On Android this is `android.graphics.Bitmap`.
 */
expect class AtlasBitmap {
    fun getWidth(): Int

    fun getHeight(): Int

    fun isRecycled(): Boolean
}

internal expect fun AtlasPixels.platformBitmap(): AtlasBitmap

internal expect fun AtlasBitmap.toComposeImage(): ImageBitmap

internal expect fun createAtlasBitmap(width: Int, height: Int): AtlasBitmap

internal expect fun AtlasBitmap.atlasSurface(argbColor: Int): AtlasSurface

internal expect fun AtlasBitmap.encodePng(): ByteArray?

internal expect fun AtlasBitmap.recycleAtlas()

internal fun interface AtlasGlyphDrawer {
    fun draw(
        source: AtlasBitmap,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
    )
}

internal class AtlasSurface(
    private val drawer: AtlasGlyphDrawer,
) {
    fun drawGlyph(
        source: AtlasBitmap,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
    ) {
        drawer.draw(
            source,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
        )
    }
}

internal expect class PlacementCache() {
    fun get(key: String): List<AtlasGlyphPlacement>?

    fun contains(key: String): Boolean

    fun getOrInsert(key: String, value: List<AtlasGlyphPlacement>): List<AtlasGlyphPlacement>
}

internal expect class AtlasSignalMap() {
    fun getOrPut(bundle: QuranAtlasBundle, create: () -> AtlasDrawSignal): AtlasDrawSignal
}

internal expect class MainPoster() {
    fun post(block: () -> Unit)
}
