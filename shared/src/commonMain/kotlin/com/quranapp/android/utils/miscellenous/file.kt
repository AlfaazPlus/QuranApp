package com.quranapp.android.utils.miscellenous

import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer

fun Path.isGzip(): Boolean {
    return FileSystem.SYSTEM.source(this).isGzip()
}

fun Source.isGzip(): Boolean {
    return buffer().isGzip()
}

fun BufferedSource.isGzip(): Boolean =
    peek().use {
        it.readByte() == 0x1f.toByte() &&
                it.readByte() == 0x8b.toByte()
    }

fun ByteArray.isGzip(): Boolean =
    size >= 2 && this[0] == 0x1f.toByte() && this[1] == 0x8b.toByte()
