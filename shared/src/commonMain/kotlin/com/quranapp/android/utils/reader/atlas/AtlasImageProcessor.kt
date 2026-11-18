package com.quranapp.android.utils.reader.atlas

import okio.Path

/**
 * One decoded atlas page.
 *
 * Android keeps a single ALPHA_8 bitmap (about 1 byte per pixel). Glyphs are
 * rectangles inside that image, so they do not allocate their own bitmaps.
 */
expect class AtlasPixels {
    val width: Int
    val height: Int
}

internal expect fun decodeAtlasPage(
    file: Path,
    width: Int,
    height: Int,
    colorType: Int,
): AtlasPixels?

/** Drops a page that was never published. Published pages are left for the GC. */
internal expect fun AtlasPixels.release()
