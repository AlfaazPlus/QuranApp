package com.quranapp.android.utils.reader.atlas

import okio.FileSystem
import okio.Path
import okio.buffer

data class PngDimensions(
    val width: Int,
    val height: Int,
    val colorType: Int,
) {
    val pixels: Long
        get() = width.toLong() * height.toLong()
}

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(),
    0x50.toByte(),
    0x4E.toByte(),
    0x47.toByte(),
    0x0D.toByte(),
    0x0A.toByte(),
    0x1A.toByte(),
    0x0A.toByte(),
)

private val PNG_IHDR = byteArrayOf(
    0x49.toByte(),
    0x48.toByte(),
    0x44.toByte(),
    0x52.toByte(),
)

/**
 * Reads PNG dimensions from the IHDR chunk.
 *
 * Does not decode the image or allocate its pixel buffer.
 */
internal fun readPngDimensions(
    file: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
): PngDimensions? {
    return try {
        fileSystem.source(file).buffer().use { source ->

            // PNG signature (8 bytes).
            val signature = source.readByteArray(8)

            if (!signature.contentEquals(PNG_SIGNATURE)) {
                return@use null
            }

            // First chunk length (4 bytes, unsigned big-endian).
            val chunkLength = source.readInt().toLong() and 0xFFFF_FFFFL

            if (chunkLength != 13L) {
                return@use null
            }

            // First chunk must be IHDR.
            val chunkType = source.readByteArray(4)

            if (!chunkType.contentEquals(PNG_IHDR)) {
                return@use null
            }

            // Width and height are unsigned 32-bit integers.
            val width = source.readInt().toLong() and 0xFFFF_FFFFL
            val height = source.readInt().toLong() and 0xFFFF_FFFFL
            source.readByte()
            val colorType = source.readByte().toInt() and 0xFF

            if (
                width <= 0L ||
                height <= 0L ||
                width > Int.MAX_VALUE ||
                height > Int.MAX_VALUE
            ) {
                return@use null
            }

            PngDimensions(
                width = width.toInt(),
                height = height.toInt(),
                colorType = colorType,
            )
        }
    } catch (_: Exception) {
        null
    }
}
