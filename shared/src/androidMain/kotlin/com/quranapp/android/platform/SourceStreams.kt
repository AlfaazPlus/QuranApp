package com.quranapp.android.platform

import okio.Source
import okio.buffer
import java.io.InputStream

internal fun Source.toInputStream(): InputStream {
    val buffered = buffer()

    return object : InputStream() {
        override fun read(): Int {
            if (buffered.exhausted()) return -1
            return buffered.readByte().toInt() and 0xff
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (buffered.exhausted()) return -1
            return buffered.read(b, off, len).toInt()
        }

        override fun close() {
            buffered.close()
        }
    }
}
