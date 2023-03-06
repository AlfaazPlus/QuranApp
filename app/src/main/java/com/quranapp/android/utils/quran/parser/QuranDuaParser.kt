package com.quranapp.android.utils.quran.parser

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.content.res.XmlResourceParser
import android.os.Handler
import android.os.Looper
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranDua
import com.quranapp.android.components.quran.QuranMeta
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

object QuranDuaParser {
    private const val DUAS_TAG_ROOT = "prophets"
    private const val DUAS_TAG_RABBANA = "rabbana"
    private const val DUAS_TAG_OTHER = "other"
    fun parseDua(
        context: Context,
        quranMeta: QuranMeta,
        quranDuaRef: AtomicReference<QuranDua>,
        postRunnable: Runnable
    ) {
        Thread {
            try {
                val parser = context.resources.getXml(R.xml.quran_duas)
                val parsedQuranTopics = parseDuaInternal(context, parser, quranMeta)
                quranDuaRef.set(parsedQuranTopics)
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).post(postRunnable)
        }.start()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDuaInternal(context: Context, parser: XmlResourceParser, quranMeta: QuranMeta): QuranDua {
        val verses = ArrayList<String>()

        var isDuaTag = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            if (eventType == XmlPullParser.START_TAG) {
                isDuaTag = tagName == DUAS_TAG_RABBANA || tagName == DUAS_TAG_OTHER
            } else if (eventType == XmlPullParser.TEXT) {
                if (isDuaTag) {
                    verses.addAll(ParserUtils.prepareVersesList(parser.text, false))
                }
            }
            eventType = parser.next()
        }

        val sortedVerses = verses.sortedWith { o1, o2 ->
            val split1 = o1.split(":")
            val split2 = o2.split(":")

            val chapterNoStr1 = split1[0]
            val chapterNoStr2 = split2[0]

            var comp = chapterNoStr1.trim().toInt() - chapterNoStr2.trim().toInt()

            if (comp == 0) {
                val verses1 = split1[1].split("-")
                val verses2 = split2[1].split("-")

                var comp2 = verses1[0].trim().toInt() - verses2[0].trim().toInt()
                if (comp2 == 0 && verses1.size > 1 && verses2.size > 1) {
                    comp2 = verses1[1].trim().toInt() - verses2[1].trim().toInt()
                }
                comp = comp2
            }

            comp
        }

        val chapters = ParserUtils.prepareChaptersList(sortedVerses)
        val inChapters = ParserUtils.prepareChapterText(context, quranMeta, chapters, 2)
        return QuranDua(inChapters, chapters, sortedVerses)
    }
}
