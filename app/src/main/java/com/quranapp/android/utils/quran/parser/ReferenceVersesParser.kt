package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.VerseReference
import org.json.JSONObject

open class ReferenceVersesParser {
    protected fun parseVersesInternal(
        context: Context,
        mapStr: String,
        localeNamesStr: String,
        fallbackNamesStr: String,
        quranMeta: QuranMeta
    ): List<VerseReference> {
        val map = JSONObject(mapStr)
        val localeNames = JSONObject(localeNamesStr)
        val fallbackNames = JSONObject(fallbackNamesStr)
        val duas = ArrayList<VerseReference>()

        map.keys().forEachRemaining { key ->
            val versesStr = map.getString(key)
            val name = localeNames.optString(key).ifEmpty { fallbackNames.getString(key) }
            val verses = versesStr.split(",").map { verse ->
                val split = verse.split(":")
                val chapterNo = split[0].toInt()
                val verseRange = split[1].split("-")
                val fromVerse = verseRange[0].toInt()
                val toVerse = if (verseRange.size == 2) verseRange[1].toInt()
                else fromVerse

                return@map Triple(chapterNo, fromVerse, toVerse)
            }.sortedWith { o1, o2 ->
                var comp = o1.first - o2.first
                if (comp == 0) {
                    var comp2 = o1.second - o2.second

                    if (comp2 == 0 && o1.second != o1.third && o2.second != o2.third) {
                        comp2 = o1.third - o2.third
                    }

                    comp = comp2
                }
                comp
            }
            val chapters = ArrayList<Int>()
            verses.forEach { if (!chapters.contains(it.first)) chapters.add(it.first) }

            val inChapters = ParserUtils.prepareChapterText(context, quranMeta, chapters, 2)

            duas.add(
                VerseReference(
                    id = key.toInt(),
                    name = name,
                    versesRaw = ParserUtils.prepareVersesList(versesStr, true),
                    verses = verses,
                    chapters = chapters,
                    inChapters = inChapters
                )
            )
        }

        return duas
    }
}
