package com.quranapp.android.utils.quran.parser

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.SparseArray
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.components.quran.QuranProphet.Prophet
import com.quranapp.android.components.quran.QuranProphet.ProphetReference
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChapterText
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareChaptersList
import com.quranapp.android.utils.quran.parser.ParserUtils.prepareVersesList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

object QuranProphetParser {
    private const val PROPHETS_TAG_PROPHET = "prophet"
    private const val PROPHETS_ATTR_ORDER = "order"
    private const val PROPHETS_ATTR_NAME_AR = "name-ar"
    private const val PROPHETS_ATTR_NAME_EN = "name-en"
    private const val PROPHETS_ATTR_NAME = "name"
    private const val PROPHETS_ATTR_HONORIFIC = "honorific"
    private const val PROPHETS_ATTR_ICON_RES = "drawable"

    fun parseProphet(context: Context, quranMeta: QuranMeta, instanceRef: AtomicReference<QuranProphet>, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parsedQuranProphets = parseProphetInternal(
                    context,
                    quranMeta,
                    parseProphetReferencesInternal(context.resources.getXml(R.xml.quran_prophets_reference))
                )

                instanceRef.set(parsedQuranProphets)
            } catch (e: Exception) {
                Log.saveError(e, "QuranProphetParser.parseProphet")
            }

            withContext(Dispatchers.Main) { callback() }
        }
    }

    private fun parseProphetReferencesInternal(parser: XmlResourceParser): SparseArray<ProphetReference> {
        val referenceArray = SparseArray<ProphetReference>()
        var lastReference: ProphetReference? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (PROPHETS_TAG_PROPHET.equals(parser.name, ignoreCase = true)) {
                        lastReference = ProphetReference(
                            order = parser.getAttributeIntValue(null, PROPHETS_ATTR_ORDER, -1),
                            nameAr = parser.getAttributeValue(null, PROPHETS_ATTR_NAME_AR),
                            nameEn = parser.getAttributeValue(null, PROPHETS_ATTR_NAME_EN),
                            iconRes = parser.getAttributeResourceValue(
                                "http://schemas.android.com/apk/res/android",
                                PROPHETS_ATTR_ICON_RES,
                                -1
                            )
                        )

                        referenceArray.put(lastReference.order, lastReference)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (lastReference != null) {
                        lastReference.references = parser.text
                    }
                }
            }
        }

        return referenceArray
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseProphetInternal(
        context: Context,
        quranMeta: QuranMeta,
        prophetReferences: SparseArray<ProphetReference>
    ): QuranProphet {
        val parser = context.resources.getXml(R.xml.quran_prophets)
        val prophetList: MutableList<Prophet> = ArrayList()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || !PROPHETS_TAG_PROPHET.equals(parser.name, ignoreCase = true)) {
                continue
            }

            val order = parser.getAttributeIntValue(null, PROPHETS_ATTR_ORDER, -1)
            val reference = prophetReferences[order]

            val lastProphet = Prophet(
                order = order,
                nameAr = reference.nameAr,
                nameEn = reference.nameEn,
                name = parser.getAttributeValue(null, PROPHETS_ATTR_NAME),
                honorific = parser.getAttributeValue(null, PROPHETS_ATTR_HONORIFIC),
                iconRes = reference.iconRes,
            )

            reference.references?.let {
                lastProphet.verses = prepareVersesList(it, true)
                lastProphet.chapters = prepareChaptersList(lastProphet.verses)
                lastProphet.inChapters = prepareChapterText(context, quranMeta, lastProphet.chapters, 2)
            }

            prophetList.add(lastProphet)

        }

        prophetList.sortBy { it.order }

        return QuranProphet(prophetList)
    }

}