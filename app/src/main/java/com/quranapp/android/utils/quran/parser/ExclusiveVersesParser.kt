package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.utils.Log
import org.json.JSONObject
import java.util.Locale

open class ExclusiveVersesParser {
    protected fun parseFromAssets(context: Context, quranMeta: QuranMeta, filename: String): List<ExclusiveVerse> {
        val map = context.assets.open("verses/$filename/map.json").bufferedReader().use {
            it.readText()
        }

        val fallbackLocale = "en"
        val currentLocale = with(Locale.getDefault().language) {
            if (this == "in") "id" else this // Hosted weblate uses "id" for Indonesian but Android uses "in"
        }
        val pathFormat = "verses/$filename/%s"
        val fileName = "$filename.json"

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

        return parseVersesInternal(
            context,
            map,
            localeNames ?: fallbackNames,
            fallbackNames,
            quranMeta
        )
    }

    private fun parseVersesInternal(
        context: Context,
        mapStr: String,
        localeNamesStr: String,
        fallbackNamesStr: String,
        quranMeta: QuranMeta
    ): List<ExclusiveVerse> {
        val map = JSONObject(mapStr)
        val localeNames = JSONObject(localeNamesStr)
        val fallbackNames = JSONObject(fallbackNamesStr)
        val duas = ArrayList<ExclusiveVerse>()

        map.keys().forEachRemaining { key ->
            val versesStr = map.getString(key)
            val name = localeNames.optString(key).ifEmpty { fallbackNames.getString(key) }
            val verses = versesStr.split(",").map { verse ->
                val split = verse.split(":")

                return@map try {
                    val chapterNo = split[0].toInt()
                    val verseRange = split[1].split("-")
                    val fromVerse = verseRange[0].toInt()
                    val toVerse = if (verseRange.size == 2) verseRange[1].toInt()
                    else fromVerse

                    Triple(chapterNo, fromVerse, toVerse)
                } catch (e: Exception) {
                    Triple(-1, -1, -1)
                }
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
                ExclusiveVerse(
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
