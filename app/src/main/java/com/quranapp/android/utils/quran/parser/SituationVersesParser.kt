package com.quranapp.android.utils.quran.parser

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.utils.Log
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object SituationVersesParser : ReferenceVersesParser() {
    fun parseVerses(
        context: Context,
        quranMeta: QuranMeta,
        situationVersesRef: AtomicReference<List<ExclusiveVerse>>,
        postRunnable: Runnable
    ) {
        Thread {
            try {
                val map = context.assets.open("verses/type0/map.json").bufferedReader().use {
                    it.readText()
                }

                val fallbackLocale = "en"
                val currentLocale = Locale.getDefault().language
                val pathFormat = "verses/type0/%s"
                val fileName = "type0.json"

                val fallbackNames = context.assets.open("${pathFormat.format(fallbackLocale)}/$fileName")
                    .bufferedReader().use {
                        it.readText()
                    }

                val localeNames = context.assets.takeIf {
                    currentLocale != fallbackLocale && it.list(pathFormat.format(currentLocale))
                        ?.contains(fileName) == true
                }?.open("${pathFormat.format(currentLocale)}/$fileName")
                    ?.bufferedReader()?.use {
                        it.readText()
                    }

                situationVersesRef.set(
                    parseVersesInternal(
                        context,
                        map,
                        localeNames ?: fallbackNames,
                        fallbackNames,
                        quranMeta
                    )
                )
            } catch (e: Exception) {
                Log.saveError(e, "SituationVersesParser.parseVerses")
            }

            Handler(Looper.getMainLooper()).post(postRunnable)
        }.start()
    }
}
