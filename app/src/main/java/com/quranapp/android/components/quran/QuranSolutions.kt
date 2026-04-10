package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.ExclusiveVersesParser
import java.util.concurrent.atomic.AtomicReference

object QuranSolutions {
    private val sSolutionsRef = AtomicReference<List<ExclusiveVerse>>()

    suspend fun get(
        context: Context,
    ): List<ExclusiveVerse> {
        val cached = sSolutionsRef.get()

        if (cached != null) {
            return cached
        }

        val verses = ExclusiveVersesParser.parseFromAssets(
            context,
            "type0"
        )

        sSolutionsRef.set(verses)

        return verses
    }
}
