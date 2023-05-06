package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.QuranEtiquetteParser
import com.quranapp.android.utils.quran.parser.SituationVersesParser
import java.util.concurrent.atomic.AtomicReference

object QuranEtiquette {
    private val sQuranEtiquetteRef = AtomicReference<List<ExclusiveVerse>>()
    fun prepareInstance(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<ExclusiveVerse>) -> Unit
    ) {
        if (sQuranEtiquetteRef.get() == null) {
            prepare(context, quranMeta, callback)
        } else {
            callback(sQuranEtiquetteRef.get())
        }
    }

    private fun prepare(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<ExclusiveVerse>) -> Unit
    ) {
        QuranEtiquetteParser.parseVerses(
            context,
            quranMeta,
            sQuranEtiquetteRef
        ) { callback(sQuranEtiquetteRef.get()) }
    }
}
