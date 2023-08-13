package com.quranapp.android.components.quran

import android.content.Context
import androidx.annotation.DrawableRes
import com.quranapp.android.utils.quran.parser.QuranProphetParser
import java.io.Serializable
import java.text.MessageFormat
import java.util.concurrent.atomic.AtomicReference

class QuranProphet(val prophets: List<Prophet>) {
    companion object {
        private val INSTANCE_REF = AtomicReference<QuranProphet>()


        fun prepareInstance(context: Context, quranMeta: QuranMeta, readyCallback: (QuranProphet) -> Unit) {
            if (INSTANCE_REF.get() == null) {
                synchronized(QuranProphet::class.java) { prepare(context, quranMeta, readyCallback) }
            } else {
                readyCallback(INSTANCE_REF.get())
            }
        }

        private fun prepare(context: Context, quranMeta: QuranMeta, readyCallback: (QuranProphet) -> Unit) {
            QuranProphetParser.parseProphet(context, quranMeta, INSTANCE_REF) { readyCallback(INSTANCE_REF.get()) }
        }
    }

    data class Prophet(
        val order: Int = 0,
        val nameAr: String,
        val nameEn: String,
        val name: String,
        val honorific: String,
        @DrawableRes val iconRes: Int = 0
    ) : Serializable {
        var references: String? = null

        /**
         * To display in recycler view
         */
        var inChapters: String? = null
        var chapters: List<Int> = ArrayList()

        /**
         * Item format -> chapNo:VERSE or chapNo:fromVERSE-toVERSE
         */
        var verses: List<String> = ArrayList()
        override fun toString(): String {
            return MessageFormat.format(
                "Prophet: {0} ({1}) {2} : [order={3}, iconRes={4}]", name, nameEn,
                honorific, order,
                iconRes
            )
        }
    }
}