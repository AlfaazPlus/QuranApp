package com.quranapp.android.utils.quran.parser

import com.quranapp.android.api.safeString
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.locale.appFallbackLanguageCodes
import com.quranapp.android.locale.appLocale
import com.quranapp.android.models.ExclusiveVerse
import com.quranapp.android.models.ExclusiveVersesDataset
import com.quranapp.android.platform.PlatformServices
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

object QuranExclusiveVerses {

    private data class CachedVerses(
        val localeTag: String,
        val verses: List<ExclusiveVerse>,
    )

    private val cache = ConcurrentHashMap<String, CachedVerses>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(filename: String): Mutex = locks.getOrPut(filename) { Mutex() }

    suspend fun get(
        dataset: ExclusiveVersesDataset,
    ): List<ExclusiveVerse> {
        val filename = dataset.assetKey
        val localeTag = appLocale().languageTag
        val mutex = lockFor(filename)

        return mutex.withLock {
            val existing = cache[filename]
            if (existing != null && existing.localeTag == localeTag) {
                return@withLock existing.verses
            }

            val verses = parseFromAssetsUncached(filename)

            cache[filename] = CachedVerses(localeTag, verses)

            verses
        }
    }

    private suspend fun parseFromAssetsUncached(
        filename: String,
    ): List<ExclusiveVerse> {
        val map = PlatformServices.assets.readAssetText("verses/$filename/map.json")

        val filenameWithExt = "$filename.json"
        val fallbackPath = "verses/$filename/en/$filenameWithExt"
        val fallbackTexts = PlatformServices.assets.readAssetText(fallbackPath)

        val localeTexts = appFallbackLanguageCodes().firstNotNullOfOrNull { code ->
            PlatformServices.assets.readAssetTextOrNull("verses/$filename/$code/$filenameWithExt")
        } ?: fallbackTexts

        return parseVersesInternal(
            map,
            localeTexts,
            fallbackTexts,
        )
    }

    private suspend fun parseVersesInternal(
        mapStr: String,
        localeTexts: String,
        fallbackTexts: String,
    ): List<ExclusiveVerse> {
        val repository = DatabaseProvider.getQuranRepository()

        val map = Json.parseToJsonElement(mapStr).jsonObject
        val localeValues = Json.parseToJsonElement(localeTexts).jsonObject
        val fallbackValues = Json.parseToJsonElement(fallbackTexts).jsonObject

        val exclusiveVerses = ArrayList<ExclusiveVerse>()

        for ((key, value) in map) {
            val versesStr = value.jsonPrimitive.content

            val (title, description) = resolveValues(
                key,
                localeValues,
                fallbackValues,
            )

            val verses = versesStr.split(",")
                .map { verse ->
                    try {
                        val split = verse.split(":")
                        val chapterNo = split[0].toInt()

                        val verseRange = split[1].split("-")
                        val fromVerse = verseRange[0].toInt()
                        val toVerse = if (verseRange.size == 2) {
                            verseRange[1].toInt()
                        } else {
                            fromVerse
                        }

                        Triple(chapterNo, fromVerse, toVerse)
                    } catch (_: Exception) {
                        Triple(-1, -1, -1)
                    }
                }
                .sortedWith { o1, o2 ->
                    var comp = o1.first - o2.first

                    if (comp == 0) {
                        var comp2 = o1.second - o2.second

                        if (
                            comp2 == 0 &&
                            o1.second != o1.third &&
                            o2.second != o2.third
                        ) {
                            comp2 = o1.third - o2.third
                        }

                        comp = comp2
                    }

                    comp
                }
                .toSet()

            val chapters = verses.map { it.first }.toSet()

            val inChapters = ParserUtils.prepareChapterText(
                repository,
                chapters,
                1,
            )

            exclusiveVerses.add(
                ExclusiveVerse(
                    id = key.toInt(),
                    title = title,
                    description = description,
                    versesRaw = ParserUtils.prepareVersesList(versesStr, true),
                    verses = verses,
                    chapters = chapters,
                    inChapters = inChapters,
                )
            )
        }

        return exclusiveVerses
    }

    private fun resolveValues(
        key: String,
        localeValues: JsonObject,
        fallbackValues: JsonObject,
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

        fun extract(value: JsonElement?): Pair<String, String?> {
            return when {
                value is JsonObject -> {
                    value.safeString("title").orEmpty() to
                            value.safeString("description").orEmpty()
                }

                value is JsonPrimitive && value.isString -> {
                    value.content to null
                }

                else -> "" to null
            }
        }

        val locale = localeValues[key]

        // Prefer locale only when it contains a valid title.
        val localeResult = extract(locale)

        if (localeResult.first.isNotEmpty()) {
            return localeResult
        }

        return extract(fallbackValues[key])
    }
}
