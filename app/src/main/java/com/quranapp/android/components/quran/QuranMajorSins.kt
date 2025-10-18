package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.MajorSinVersesParser
import com.quranapp.android.utils.quran.parser.QuranEtiquetteParser
import java.util.concurrent.atomic.AtomicReference

object QuranMajorSins {
    private val sQuranMajorSinsRef = AtomicReference<List<ExclusiveVerse>>()
    fun prepareInstance(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<ExclusiveVerse>) -> Unit
    ) {
        if (sQuranMajorSinsRef.get() == null) {
            prepare(context, quranMeta, callback)
        } else {
            callback(sQuranMajorSinsRef.get())
        }
    }

    private fun prepare(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<ExclusiveVerse>) -> Unit
    ) {
        MajorSinVersesParser.parseVerses(
            context,
            quranMeta,
            sQuranMajorSinsRef
        ) { callback(sQuranMajorSinsRef.get()) }
    }
}
