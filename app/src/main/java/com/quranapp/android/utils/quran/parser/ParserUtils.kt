/*
 * Created by Faisal Khan on (c) 14/8/2021.
 */
package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.R
import com.quranapp.android.repository.QuranRepository

object ParserUtils {
    @JvmStatic
    fun prepareVersesList(text: String, sort: Boolean = true): Set<String> {
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

        return verses.toSet()
    }

    fun prepareChaptersList(verses: Set<String>): Set<Int> {
        val chapters = mutableSetOf<Int>()

        for (verseStr in verses) {
            val chapterNo = verseStr.split(":")[0].trim().toInt()
            if (!chapters.contains(chapterNo)) {
                chapters.add(chapterNo)
            }
        }

        return chapters
    }

    /**
     * Compresses verse references by chapter, collapsing contiguous verses into ranges.
     * Example: 1:1, 1:2, 1:3, 2:5 -> [1:1-3, 2:5]
     */
    @JvmStatic
    fun compressVerseRefsByChapter(verseRefs: Collection<String>): List<String> {
        if (verseRefs.isEmpty()) return emptyList()

        val grouped = linkedMapOf<Int, MutableSet<Int>>()

        verseRefs.forEach { ref ->
            val chapter = ref.substringBefore(':').trim().toIntOrNull() ?: return@forEach
            val versePart = ref.substringAfter(':', "").trim()

            if (versePart.isBlank()) return@forEach

            val verses = grouped.getOrPut(chapter) { linkedSetOf() }

            versePart.split(',').forEach { token ->
                val piece = token.trim()
                if (piece.isBlank()) return@forEach

                val rangeParts = piece.split('-')

                when (rangeParts.size) {
                    1 -> {
                        rangeParts[0].toIntOrNull()?.let(verses::add)
                    }

                    2 -> {
                        val from = rangeParts[0].toIntOrNull()
                        val to = rangeParts[1].toIntOrNull()
                        if (from != null && to != null) {
                            val start = minOf(from, to)
                            val end = maxOf(from, to)
                            for (verseNo in start..end) {
                                verses.add(verseNo)
                            }
                        }
                    }
                }
            }
        }

        return grouped.entries.flatMap { (chapter, versesSet) ->
            val sorted = versesSet.toList().sorted()
            if (sorted.isEmpty()) return@flatMap emptyList<String>()

            val collapsed = mutableListOf<String>()
            var start = sorted.first()
            var prev = start

            for (i in 1 until sorted.size) {
                val current = sorted[i]

                if (current == prev + 1) {
                    prev = current
                } else {
                    collapsed += if (start == prev) {
                        "$chapter:$start"
                    } else {
                        "$chapter:$start-$prev"
                    }

                    start = current
                    prev = current
                }
            }

            collapsed += if (start == prev) {
                "$chapter:$start"
            } else {
                "$chapter:$start-$prev"
            }

            collapsed
        }
    }

    suspend fun prepareChapterText(
        ctx: Context,
        repository: QuranRepository,
        chapters: Set<Int>,
        limit: Int
    ): String {
        val count = chapters.size
        if (count == 0) return ""

        val firstNChapters = chapters.toList().subList(0, minOf(count, limit))
        val inChapters = firstNChapters
            .map { repository.getChapterName(it) }
            .joinToString(", ")

        return if (count > 2) {
            ctx.getString(R.string.inPlacesMore, inChapters, count - limit)
        } else {
            ctx.getString(R.string.inPlaces, inChapters)
        }
    }
}
