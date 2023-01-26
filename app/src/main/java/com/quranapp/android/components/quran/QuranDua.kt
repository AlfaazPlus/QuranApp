package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.quran.parser.QuranDuaParser
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class QuranDua(
    /**
     * To display in recycler view
     */
    var inChapters: String,
    var chapters: List<Int>,
    /**
     * Item format -> chapNo:VERSE or chapNo:fromVERSE-toVERSE
     */
    var verses: List<String>
) {

    companion object {
        private val sQuranProphetRef = AtomicReference<QuranDua>()
        fun prepareInstance(
            context: Context,
            quranMeta: QuranMeta,
            resultReadyCallback: OnResultReadyCallback<QuranDua>
        ) {
            if (sQuranProphetRef.get() == null) {
                prepare(context, quranMeta, resultReadyCallback)
            } else {
                resultReadyCallback.onReady(sQuranProphetRef.get())
            }
        }

        private fun prepare(
            context: Context,
            quranMeta: QuranMeta,
            resultReadyCallback: OnResultReadyCallback<QuranDua>
        ) {
            QuranDuaParser.parseDua(
                context, quranMeta, sQuranProphetRef
            ) { resultReadyCallback.onReady(sQuranProphetRef.get()) }
        }
    }
}