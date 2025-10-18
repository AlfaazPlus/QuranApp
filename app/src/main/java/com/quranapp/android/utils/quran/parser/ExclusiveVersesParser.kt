package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.QuranMeta
import org.json.JSONObject
import java.util.Locale

open class ExclusiveVersesParser {
    protected fun parseFromAssets(
        context: Context,
        quranMeta: QuranMeta,
        filename: String
    ): List<ExclusiveVerse> {
        val map = context.assets.open("verses/$filename/map.json").bufferedReader().use {
            it.readText()
        }

        val fallbackLocale = "en"
        val currentLocale = with(Locale.getDefault().language) {
            if (this == "in") "id" else this // Hosted weblate uses "id" for Indonesian but Android uses "in"
        }
        val pathFormat = "verses/$filename/%s"
        val fileName = "$filename.json"

        val fallbackTexts = context.assets.open("${pathFormat.format(fallbackLocale)}/$fileName")
            .bufferedReader().use {
                it.readText()
            }

        val localeTexts = context.assets.takeIf {
            currentLocale != fallbackLocale && it.list(pathFormat.format(currentLocale))
                ?.contains(fileName) == true
        }?.open("${pathFormat.format(currentLocale)}/$fileName")
            ?.bufferedReader()?.use {
                it.readText()
            }

        return parseVersesInternal(
            context,
            map,
            localeTexts ?: fallbackTexts,
            fallbackTexts,
            quranMeta
        )
    }

    private fun parseVersesInternal(
        context: Context,
        mapStr: String,
        localeTexts: String,
        fallbackTexts: String,
        quranMeta: QuranMeta
    ): List<ExclusiveVerse> {
        val map = JSONObject(mapStr)
        val localeValues = JSONObject(localeTexts)
        val fallbackValues = JSONObject(fallbackTexts)
        val duas = ArrayList<ExclusiveVerse>()

        map.keys().forEachRemaining { key ->
            val versesStr = map.getString(key)

            val (title, description) = resolveValues(
                key,
                localeValues,
                fallbackValues
            )

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
                    title = title,
                    description = description,
                    versesRaw = ParserUtils.prepareVersesList(versesStr, true),
                    verses = verses,
                    chapters = chapters,
                    inChapters = inChapters
                )
            )
        }

        return duas
    }

    private fun resolveValues(
        key: String,
        localeValues: JSONObject,
        fallbackValues: JSONObject
    ): Pair<String, String?> {
        /**
         * Contents example:
         * {
         *   "id": {
         *     "title": "Exclusive Verse Title",
         *     "description": "Optional description about the exclusive verse."
         *   }
         * }
         *
         * Or,
         *
         * {
         *   "id": "Exclusive Verse Title"
         * }
         *
         */

        val localeObj = localeValues.opt(key)
        val fallbackObj = fallbackValues.opt(key)

        var title: String
        var description: String? = null

        if (localeObj is JSONObject) {
            title = localeObj.optString("title", "")
            description = localeObj.optString("description")
        } else if (localeObj is String) {
            title = localeObj
        } else if (fallbackObj is JSONObject) {
            title = fallbackObj.optString("title", "")
            description = fallbackObj.optString("description")
        } else if (fallbackObj is String) {
            title = fallbackObj
        } else {
            title = ""
        }

        return Pair(title, description)
    }
}
