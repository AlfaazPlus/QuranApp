/*
 * Created by Faisal Khan on (c) 14/8/2021.
 */
package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta

object ParserUtils {
    @JvmStatic
    fun prepareVersesList(text: String, sort: Boolean = true): List<String> {
        var verses = text.split(",")
        if (sort) {
            verses = verses.sortedWith { o1, o2 ->
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
        }
        return verses
    }

    @JvmStatic
    fun prepareChaptersList(verses: List<String>): List<Int> {
        val chapters = ArrayList<Int>()
        for (verseStr in verses) {
            val chapterNo = verseStr.split(":")[0].trim().toInt()
            if (!chapters.contains(chapterNo)) {
                chapters.add(chapterNo)
            }
        }
        return chapters
    }

    @JvmStatic
    fun prepareChapterText(ctx: Context, quranMeta: QuranMeta, chapters: List<Int>, limit: Int): String {
        val count = chapters.size
        if (count == 0) return ""

        val firstNChapters = chapters.subList(0, minOf(count, limit))
        val inChapters = firstNChapters.joinToString(", ") {
            quranMeta.getChapterName(ctx, it) ?: ""
        }

        return if (count > 2) {
            ctx.getString(R.string.inPlacesMore, inChapters, count - limit)
        } else {
            ctx.getString(R.string.inPlaces, inChapters)
        }
    }
}
