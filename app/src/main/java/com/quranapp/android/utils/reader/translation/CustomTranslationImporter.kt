package com.quranapp.android.utils.reader.translation

import android.content.Context
import android.net.Uri
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.quran.QuranConstants
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import com.quranapp.android.utils.reader.importing.ImportManifest
import com.quranapp.android.utils.reader.importing.readImportText
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

object CustomTranslationImporter {
    private const val MAX_VERSE_NUMBER = 286

    fun import(context: Context, manifest: ImportManifest, uri: Uri) {
        try {
            if (TranslUtils.isPrebuilt(manifest.slug)) {
                throw ImportException(ImportFailure.RESERVED)
            }

            val text = readImportText(context, uri)
            val root = try {
                JSONObject(text)
            } catch (error: JSONException) {
                invalid(error.message ?: "The file is not valid JSON.")
            }

            val chapters = root.optJSONArray(QuranConstants.KEY_CHAPTER_LIST)
                ?: invalid("Missing \"suras\" array.")

            validateVerses(chapters)

            QuranTranslationFactory(context).use { factory ->
                factory.dbHelper.storeTranslation(manifest.toBookInfo(), text)
            }
            SearchIndexScheduler.enqueueSlug(context.applicationContext, manifest.slug)
        } catch (error: ImportException) {
            throw error
        } catch (error: Exception) {
            Log.saveError(error, "CustomTranslationImporter")
            throw ImportException(ImportFailure.UNREADABLE)
        }
    }

    private fun ImportManifest.toBookInfo(): TranslationBookInfoModel {
        return TranslationBookInfoModel(slug).apply {
            bookName = book
            authorName = author
            displayName = this@toBookInfo.displayName
            langCode = this@toBookInfo.langCode
            langName = this@toBookInfo.langName
            version = this@toBookInfo.version
            lastUpdated = this@toBookInfo.version
            downloadPath = ""
        }
    }

    private fun validateVerses(chapters: JSONArray) {
        if (chapters.length() == 0) invalid("\"suras\" must contain at least one chapter.")

        val parser = newTranslationMarkupParser()
        val seenChapters = BooleanArray(QuranMeta.chapterRange.last + 1)
        val seenVerses = BooleanArray(MAX_VERSE_NUMBER + 1)

        for (chapterIndex in 0 until chapters.length()) {
            val chapter = chapters.optJSONObject(chapterIndex)
                ?: invalid("Chapter at position ${chapterIndex + 1} must be an object.")

            val chapterNo = positiveInt(chapter, QuranConstants.KEY_NUMBER)
            if (chapterNo == null) {
                invalid("Chapter at position ${chapterIndex + 1} is missing a positive \"index\".")
            }

            if (!QuranMeta.isChapterValid(chapterNo)) {
                invalid("Chapter $chapterNo is outside 1 to 114.")
            }
            if (seenChapters[chapterNo]) invalid("Chapter $chapterNo is repeated.")

            seenChapters[chapterNo] = true

            val verses = chapter.optJSONArray(QuranConstants.KEY_VERSE_LIST)
                ?: invalid("Chapter $chapterNo is missing an \"ayas\" array.")

            seenVerses.fill(false)

            for (verseIndex in 0 until verses.length()) {
                val verse = verses.optJSONObject(verseIndex)
                    ?: invalid("Chapter $chapterNo has a verse at position ${verseIndex + 1} that is not an object.")

                val verseNo = positiveInt(verse, QuranConstants.KEY_NUMBER)
                if (verseNo == null) {
                    invalid("Chapter $chapterNo, verse at position ${verseIndex + 1}, is missing a positive \"index\".")
                }
                if (verseNo > MAX_VERSE_NUMBER) {
                    invalid("Chapter $chapterNo verse $verseNo is outside 1 to $MAX_VERSE_NUMBER.")
                }

                if (seenVerses[verseNo]) invalid("Chapter $chapterNo verse $verseNo is repeated.")
                seenVerses[verseNo] = true

                val translation = verse.opt(QuranConstants.KEY_TRANSLATION_TEXT)
                if (translation !is String) {
                    invalid("Chapter $chapterNo verse $verseNo is missing \"translation\" text.")
                }

                requireValidTranslationMarkup(
                    parser,
                    translation,
                    chapterNo,
                    verseNo,
                    "translation"
                )

                if (verse.has(QuranConstants.KEY_FOOTNOTE_LIST) &&
                    !verse.isNull(QuranConstants.KEY_FOOTNOTE_LIST)
                ) {
                    validateFootnotes(
                        verse.optJSONArray(QuranConstants.KEY_FOOTNOTE_LIST)
                            ?: invalid("Chapter $chapterNo verse $verseNo \"footnotes\" must be an array."),
                        parser,
                        chapterNo,
                        verseNo,
                    )
                }
            }
        }
    }

    private fun validateFootnotes(
        footnotes: JSONArray,
        parser: XmlPullParser,
        chapterNo: Int,
        verseNo: Int,
    ) {
        val seen = HashSet<Int>(footnotes.length())
        for (index in 0 until footnotes.length()) {
            val footnote = footnotes.optJSONObject(index)
                ?: invalid("Chapter $chapterNo verse $verseNo has a footnote at position ${index + 1} that is not an object.")
            val number = positiveInt(footnote, QuranConstants.KEY_NUMBER)
            val text = footnote.opt(QuranConstants.KEY_FOOTNOTE_SINGLE_TEXT)
            if (number == null) {
                invalid("Chapter $chapterNo verse $verseNo has a footnote without a positive \"index\".")
            }
            if (!seen.add(number)) invalid("Chapter $chapterNo verse $verseNo repeats footnote $number.")
            if (text !is String) {
                invalid("Chapter $chapterNo verse $verseNo footnote $number is missing \"text\".")
            }
            requireValidTranslationMarkup(parser, text, chapterNo, verseNo, "footnote#$number")
        }
    }

    private fun positiveInt(obj: JSONObject, key: String): Int? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = when (val raw = obj.get(key)) {
            is Int -> raw
            is Long -> if (raw in 1..Int.MAX_VALUE) raw.toInt() else return null
            is String -> raw.trim().toIntOrNull() ?: return null
            else -> return null
        }
        return value.takeIf { it >= 1 }
    }

    private fun invalid(message: String): Nothing {
        throw ImportException(ImportFailure.INVALID, message)
    }
}
