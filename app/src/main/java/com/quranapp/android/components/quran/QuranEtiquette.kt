package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.ExclusiveVersesParser
import java.util.concurrent.atomic.AtomicReference

object QuranEtiquette {
    private val sQuranEtiquetteRef = AtomicReference<List<ExclusiveVerse>>()

    suspend fun get(
        context: Context,
    ): List<ExclusiveVerse> {
        val cached = sQuranEtiquetteRef.get()

        if (cached != null) {
            return cached
        }

        val verses = ExclusiveVersesParser.parseFromAssets(
            context,
            "type2"
        )

        sQuranEtiquetteRef.set(verses)

        return verses
    }
}
