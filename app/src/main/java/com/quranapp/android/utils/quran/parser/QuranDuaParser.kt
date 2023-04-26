package com.quranapp.android.utils.quran.parser

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.dua.Dua
import com.quranapp.android.utils.Log
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object QuranDuaParser {
    fun parseDua(
        context: Context,
        quranMeta: QuranMeta,
        quranDuaRef: AtomicReference<List<Dua>>,
        postRunnable: Runnable
    ) {
        Thread {
            try {
                val map = context.assets.open("verses/type1/map.json").bufferedReader().use {
                    it.readText()
                }
                val names = try {
                    context.assets.open("verses/type1/${Locale.getDefault().language}/type1.json").bufferedReader()
                        .use {
                            it.readText()
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.assets.open("verses/type1/en/type1.json").bufferedReader().use {
                        it.readText()
                    }
                }
                quranDuaRef.set(parseDuaInternal(context, map, names, quranMeta))
            } catch (e: Exception) {
                Log.saveError(e, "QuranDuaParser.parseDua")
            }

            Handler(Looper.getMainLooper()).post(postRunnable)
        }.start()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDuaInternal(context: Context, mapStr: String, namesStr: String, quranMeta: QuranMeta): List<Dua> {
        val map = JSONObject(mapStr)
        val names = JSONObject(namesStr)
        val duas = ArrayList<Dua>()

        map.keys().forEachRemaining { key ->
            val versesStr = map.getString(key)
            val name = names.getString(key)
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
                Dua(
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
