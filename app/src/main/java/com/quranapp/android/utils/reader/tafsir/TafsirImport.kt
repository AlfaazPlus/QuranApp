package com.quranapp.android.utils.reader.tafsir

import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

suspend fun parseTafsirImport(
    text: String,
    key: String,
    isVerseValid: suspend (chapterNo: Int, verseNo: Int) -> Boolean,
): List<TafsirModel> {
    val root = try {
        Json.parseToJsonElement(text)
    } catch (error: Exception) {
        invalid(error.message ?: "The file is not valid JSON.")
    }

    if (root !is JsonObject) invalid("The file must be a JSON object.")

    val blocks = root["tafsirs"] as? JsonArray ?: invalid("Missing \"tafsirs\" array.")
    if (blocks.isEmpty()) invalid("\"tafsirs\" must contain at least one block.")

    val occupied = mutableMapOf<Int, MutableList<IntRange>>()
    val models = ArrayList<TafsirModel>(blocks.size)

    for (index in blocks.indices) {
        val blockNo = index + 1
        val block = blocks[index] as? JsonObject
            ?: invalid("Tafsir block $blockNo must be an object.")
        val verses = block["verses"] as? JsonArray
            ?: invalid("Tafsir block $blockNo is missing a \"verses\" array.")

        if (verses.isEmpty()) invalid("Tafsir block $blockNo has an empty \"verses\" array.")

        val parsed = verses.mapIndexed { verseIndex, element ->
            val raw = element.stringOrNull()
                ?: invalid("Verse ${verseIndex + 1} in block $blockNo must be a verse key such as \"2:1\".")
            parseVerseKey(raw) ?: invalid("\"$raw\" is not a verse key. Use the form \"2:1\".")
        }

        val (firstChapter, firstVerse) = parsed.first()
        val verseKey = "$firstChapter:$firstVerse"

        val chapterNo = firstChapter
        if (!QuranMeta.isChapterValid(chapterNo)) {
            invalid("Chapter $chapterNo is outside 1 to 114.")
        }

        if (parsed.any { it.first != chapterNo }) {
            invalid("Tafsir block $blockNo mixes verses from more than one chapter.")
        }

        val numbers = parsed.map { it.second }

        val outside = parsed.firstOrNull { !isVerseValid(chapterNo, it.second) }
        if (outside != null) {
            invalid("Verse ${outside.first}:${outside.second} is outside that chapter.")
        }

        if (numbers.distinct().size != numbers.size) {
            invalid("Chapter $chapterNo repeats a verse in one block.")
        }

        val range = numbers.min()..numbers.max()
        if (range.last - range.first + 1 != numbers.size) {
            invalid("Chapter $chapterNo verses ${range.first}-${range.last} must be a continuous range.")
        }

        val chapterRanges = occupied.getOrPut(chapterNo) { mutableListOf() }
        if (chapterRanges.any { it.first <= range.last && range.first <= it.last }) {
            invalid("Chapter $chapterNo verses ${range.first}-${range.last} overlap another block.")
        }

        chapterRanges += range

        models += TafsirModel(
            key = key,
            verseKey = verseKey,
            verses = parsed.map { "${it.first}:${it.second}" },
            text = block["text"]?.stringOrNull().orEmpty(),
        )
    }

    return models
}

private fun parseVerseKey(raw: String): Pair<Int, Int>? {
    val parts = raw.trim().split(":")

    if (parts.size != 2) return null

    val chapterNo = parts[0].toIntOrNull() ?: return null
    val verseNo = parts[1].toIntOrNull() ?: return null

    if (verseNo < 1) return null

    return chapterNo to verseNo
}

private fun JsonElement.stringOrNull(): String? {
    return (this as? JsonPrimitive)?.contentOrNull
}

private fun invalid(message: String): Nothing {
    throw ImportException(ImportFailure.INVALID, message)
}
