package com.quranapp.android.utils.quran.parser

import android.content.Context
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.appPlatformLocale
import com.quranapp.android.db.DatabaseProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object ExclusiveVersesParser {

    private data class CachedVerses(
        val localeTag: String,
        val verses: List<ExclusiveVerse>,
    )

    private val cache = ConcurrentHashMap<String, CachedVerses>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(filename: String): Mutex = locks.getOrPut(filename) { Mutex() }

    suspend fun parseFromAssets(
        context: Context,
        filename: String,
    ): List<ExclusiveVerse> {
        val localeTag = appPlatformLocale().toLanguageTag()
        val mutex = lockFor(filename)

        return mutex.withLock {
            val existing = cache[filename]
            if (existing != null && existing.localeTag == localeTag) {
                return@withLock existing.verses
            }

            val verses = parseFromAssetsUncached(context, filename)

            cache[filename] = CachedVerses(localeTag, verses)

            verses
        }
    }

    private suspend fun parseFromAssetsUncached(
        context: Context,
        filename: String,
    ): List<ExclusiveVerse> {
        val assets = context.assets

        val map = assets.open("verses/$filename/map.json").bufferedReader().use {
            it.readText()
        }

        val filenameWithExt = "$filename.json"
        val fallbackPath = "verses/$filename/en/$filenameWithExt"

        val fallbackTexts = assets.open(fallbackPath).bufferedReader().use { it.readText() }

        val localeTexts = appFallbackLanguageCodes().firstNotNullOfOrNull { code ->
            runCatching {
                assets.open("verses/$filename/$code/$filenameWithExt")
                    .bufferedReader()
                    .use {
                        it.readText()
                    }
            }.getOrNull()
        } ?: fallbackTexts

        return parseVersesInternal(
            context,
            map,
            localeTexts,
            fallbackTexts,
        )
    }

    private suspend fun parseVersesInternal(
        context: Context,
        mapStr: String,
        localeTexts: String,
        fallbackTexts: String,
    ): List<ExclusiveVerse> {
        val repository = DatabaseProvider.getQuranRepository(context)
        val map = JSONObject(mapStr)
        val localeValues = JSONObject(localeTexts)
        val fallbackValues = JSONObject(fallbackTexts)
        val exclusiveVerses = ArrayList<ExclusiveVerse>()

        val keys = map.keys()

        while (keys.hasNext()) {
            val key = keys.next()
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
            }.toSet()

            val chapters = verses.map { it.first }.toSet()

            val inChapters = ParserUtils.prepareChapterText(context, repository, chapters, 1)

            exclusiveVerses.add(
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

        return exclusiveVerses
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
