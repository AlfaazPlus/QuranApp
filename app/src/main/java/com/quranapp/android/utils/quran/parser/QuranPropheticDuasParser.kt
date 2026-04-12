package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranPropheticDua
import com.quranapp.android.components.quran.QuranPropheticDua.Prophet
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChapterText
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChaptersList
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareVersesList
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object QuranPropheticDuasParser {
    private data class CachedPropheticDuas(
        val localeTag: String,
        val quranPropheticDua: QuranPropheticDua,
    )

    private val cacheLock = Any()
    private var cached: CachedPropheticDuas? = null

    private const val PROPHETS_TAG_PROPHET = "prophet"
    private const val PROPHETS_ATTR_ORDER = "order"
    private const val PROPHETS_ATTR_NAME = "name"
    private const val PROPHETS_ATTR_ICON_RES = "drawable"

    /**
     * Parsed strings and chapter labels depend on [appLocale]. Cached per locale tag.
     */
    suspend fun parsePropheticDuas(context: Context): QuranPropheticDua {
        val localeTag = appLocale().toLanguageTag()

        synchronized(cacheLock) {
            cached?.takeIf { it.localeTag == localeTag }?.let {
                return it.quranPropheticDua
            }
        }

        val parsed = parseDuasInternal(context)

        synchronized(cacheLock) {
            cached = CachedPropheticDuas(localeTag, parsed)
        }

        return parsed
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun parseDuasInternal(
        context: Context,
    ): QuranPropheticDua {
        val repository = DatabaseProvider.getQuranRepository(context)
        val parser = context.resources.getXml(R.xml.quran_prophetic_duas)
        val prophetList: MutableList<Prophet> = ArrayList()

        val honorificMuhammad = context.getString(R.string.honorificMuhammad)
        val honorificProphet = context.getString(R.string.honorificProphet)

        var lastReference: Prophet? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (!PROPHETS_TAG_PROPHET.equals(parser.name, ignoreCase = true)) {
                        continue
                    }

                    val order = parser.getAttributeIntValue(null, PROPHETS_ATTR_ORDER, -1)
                    val lastProphet = Prophet(
                        order = order,
                        name = context.getString(
                            parser.getAttributeResourceValue(
                                "http://schemas.android.com/apk/res/android",
                                PROPHETS_ATTR_NAME,
                                -1
                            )
                        ),
                        honorific = if (order == 25) honorificMuhammad else honorificProphet,
                        iconRes = parser.getAttributeResourceValue(
                            "http://schemas.android.com/apk/res/android",
                            PROPHETS_ATTR_ICON_RES,
                            -1
                        ),
                    )

                    lastReference = lastProphet
                    prophetList.add(lastProphet)
                }

                XmlPullParser.TEXT -> {
                    lastReference?.let { prophet ->
                        prophet.references = parser.text
                        prophet.references?.let {
                            prophet.verses = prepareVersesList(it, true)
                            prophet.chapters = prepareChaptersList(prophet.verses)
                            prophet.inChapters =
                                prepareChapterText(context, repository, prophet.chapters, 2)
                        }
                    }
                }
            }
        }

        prophetList.sortBy { it.order }

        return QuranPropheticDua(prophetList)
    }
}
