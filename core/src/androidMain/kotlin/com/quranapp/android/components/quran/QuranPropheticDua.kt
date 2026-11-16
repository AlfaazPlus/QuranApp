package com.quranapp.android.components.quran

import android.content.Context
import androidx.annotation.DrawableRes
import com.quranapp.android.utils.quran.parser.QuranPropheticDuasParser
import java.io.Serializable
import java.text.MessageFormat

class QuranPropheticDua(val prophets: List<Prophet>) {
    companion object {
        suspend fun load(context: Context): QuranPropheticDua =
            QuranPropheticDuasParser.parsePropheticDuas(context)

    }

    data class Prophet(
        val order: Int = 0,
        val name: String,
        val honorific: String,
        @param:DrawableRes val iconRes: Int = 0,
        val thumbnail: String?
    ) : Serializable {
        var references: String? = null

        /**
         * To display in recycler view
         */
        var inChapters: String? = null
        var chapters: Set<Int> = setOf()

        /**
         * Item format -> chapNo:VERSE or chapNo:fromVERSE-toVERSE
         */
        var verses: Set<String> = setOf()
        override fun toString(): String {
            return MessageFormat.format(
                "Prophet: {0} ({1}) {2} : [order={3}, iconRes={4}]", name, honorific, order, iconRes
            )
        }
    }
}
