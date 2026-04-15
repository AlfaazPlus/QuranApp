package com.quranapp.android.utils.extensions

import java.io.File

fun File.isGzip(): Boolean {
    inputStream().use { input ->
        val buffered = input.buffered()
        buffered.mark(2)
        val byte1 = buffered.read()
        val byte2 = buffered.read()
        buffered.reset()
        return byte1 == 0x1f && byte2 == 0x8b
    }
}