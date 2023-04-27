package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.components.quran.dua.Dua
import com.quranapp.android.utils.quran.parser.QuranDuaParser
import java.util.concurrent.atomic.AtomicReference

object QuranDua {
    private val sQuranDuaRef = AtomicReference<List<Dua>>()
    fun prepareInstance(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<Dua>) -> Unit
    ) {
        if (sQuranDuaRef.get() == null) {
            prepare(context, quranMeta, callback)
        } else {
            callback(sQuranDuaRef.get())
        }
    }

    private fun prepare(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<Dua>) -> Unit
    ) {
        QuranDuaParser.parseDua(
            context,
            quranMeta,
            sQuranDuaRef
        ) { callback(sQuranDuaRef.get()) }
    }
}
