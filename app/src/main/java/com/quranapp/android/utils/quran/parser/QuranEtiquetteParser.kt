package com.quranapp.android.utils.quran.parser

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.utils.Log
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object QuranEtiquetteParser : ExclusiveVersesParser() {
    fun parseVerses(
        context: Context,
        quranMeta: QuranMeta,
        situationVersesRef: AtomicReference<List<ExclusiveVerse>>,
        postRunnable: Runnable
    ) {
        Thread {
            try {
                situationVersesRef.set(
                    parseFromAssets(
                        context,
                        quranMeta,
                        "type2"
                    )
                )
            } catch (e: Exception) {
                Log.saveError(e, "QuranEtiquetteParser.parseVerses")
            }

            Handler(Looper.getMainLooper()).post(postRunnable)
        }.start()
    }
}
