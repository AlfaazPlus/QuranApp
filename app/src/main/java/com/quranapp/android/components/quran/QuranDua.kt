package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.ExclusiveVersesParser
import java.util.concurrent.atomic.AtomicReference

object QuranDua {
    private val sQuranDuaRef = AtomicReference<List<ExclusiveVerse>>()

    suspend fun get(
        context: Context,
    ): List<ExclusiveVerse> {
        val cached = sQuranDuaRef.get()

        if (cached != null) {
            return cached
        }

        val verses = ExclusiveVersesParser.parseFromAssets(
            context,
            "type1"
        )

        sQuranDuaRef.set(verses)

        return verses
    }
}
