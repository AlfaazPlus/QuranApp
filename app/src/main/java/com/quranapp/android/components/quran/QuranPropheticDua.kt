package com.quranapp.android.components.quran

import android.content.Context
import androidx.annotation.DrawableRes
import com.quranapp.android.utils.quran.parser.QuranPropheticDuasParser
import java.io.Serializable
import java.text.MessageFormat
import java.util.concurrent.atomic.AtomicReference

class QuranPropheticDua(val prophets: List<Prophet>) {
    companion object {
        private val INSTANCE_REF = AtomicReference<QuranPropheticDua>()


        fun prepareInstance(context: Context, quranMeta: QuranMeta, readyCallback: (QuranPropheticDua) -> Unit) {
            if (INSTANCE_REF.get() == null) {
                synchronized(QuranPropheticDua::class.java) { prepare(context, quranMeta, readyCallback) }
            } else {
                readyCallback(INSTANCE_REF.get())
            }
        }

        private fun prepare(context: Context, quranMeta: QuranMeta, readyCallback: (QuranPropheticDua) -> Unit) {
            QuranPropheticDuasParser.parseDua(context, quranMeta, INSTANCE_REF) { readyCallback(INSTANCE_REF.get()) }
        }
    }

    data class Prophet(
        val order: Int = 0,
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
                "Prophet: {0} ({1}) {2} : [order={3}, iconRes={4}]", name, honorific, order, iconRes
            )
        }
    }
}