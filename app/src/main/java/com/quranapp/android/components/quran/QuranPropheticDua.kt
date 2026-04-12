package com.quranapp.android.components.quran

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.utils.quran.parser.QuranPropheticDuasParser
import java.io.Serializable
import java.text.MessageFormat

class QuranPropheticDua(val prophets: List<Prophet>) {
    companion object {
        suspend fun load(context: Context): QuranPropheticDua =
            QuranPropheticDuasParser.parsePropheticDuas(context)

        @Composable
        fun observe(range: IntRange? = null): List<Prophet>? {
            val context = LocalContext.current
            val configuration = LocalConfiguration.current

            val prophets by produceState<List<Prophet>?>(
                null,
                context,
                configuration,
                appLocale(),
            ) {
                val q = load(context)

                if (range == null) {
                    value = q.prophets
                } else {
                    value = q.prophets.subList(range.first, range.last)
                }
            }

            return prophets
        }
    }

    data class Prophet(
        val order: Int = 0,
        val name: String,
        val honorific: String,
        @param:DrawableRes val iconRes: Int = 0
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
