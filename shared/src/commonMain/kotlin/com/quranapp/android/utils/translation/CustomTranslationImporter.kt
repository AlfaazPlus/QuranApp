package com.quranapp.android.utils.translation

import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonArray
import com.quranapp.android.api.safeString
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.importing.ImportException
import com.quranapp.android.utils.importing.ImportFailure
import com.quranapp.android.utils.importing.ImportManifest
import com.quranapp.android.utils.quran.QuranConstants
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.TranslUtils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object CustomTranslationImporter {
    private const val MAX_VERSE_NUMBER = 286

    suspend fun import(manifest: ImportManifest, file: PlatformFile) {
        try {
            if (TranslUtils.isPrebuilt(manifest.slug)) {
                throw ImportException(ImportFailure.RESERVED)
            }

            val text = file.readString()
            val root = try {
                Json.parseToJsonElement(text).jsonObject
            } catch (error: Exception) {
                invalid(error.message ?: "The file is not valid JSON.")
            }

            val chapters = root.safeJsonArray(QuranConstants.KEY_CHAPTER_LIST)
                ?: invalid("Missing \"suras\" array.")

            validateVerses(chapters)

            DatabaseProvider.getTranslationRepository()
                .store(manifest.toBookInfo(), text)
            SearchIndexScheduler.enqueueSlug(applicationContext, manifest.slug)
        } catch (error: ImportException) {
            throw error
        } catch (error: Exception) {
            Logger.saveError(error, "CustomTranslationImporter")
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

    private fun validateVerses(chapters: JsonArray) {
        if (chapters.size == 0) invalid("\"suras\" must contain at least one chapter.")

        val seenChapters = BooleanArray(QuranMeta.chapterRange.last + 1)
        val seenVerses = BooleanArray(MAX_VERSE_NUMBER + 1)

        for (chapterIndex in 0 until chapters.size) {
            val chapter = chapters.getOrNull(chapterIndex)?.jsonObject
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

            val verses = chapter.safeJsonArray(QuranConstants.KEY_VERSE_LIST)
                ?: invalid("Chapter $chapterNo is missing an \"ayas\" array.")

            seenVerses.fill(false)

            for (verseIndex in 0 until verses.size) {
                val verse = verses.getOrNull(verseIndex)?.jsonObject
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

                val translation = verse.safeString(QuranConstants.KEY_TRANSLATION_TEXT)
                if (translation == null) {
                    invalid("Chapter $chapterNo verse $verseNo is missing \"translation\" text.")
                }

                requireValidTranslationMarkup(
                    translation,
                    chapterNo,
                    verseNo,
                    "translation"
                )

                if (verse.contains(QuranConstants.KEY_FOOTNOTE_LIST)) {
                    validateFootnotes(
                        verse.safeJsonArray(QuranConstants.KEY_FOOTNOTE_LIST)
                            ?: invalid("Chapter $chapterNo verse $verseNo \"footnotes\" must be an array."),
                        chapterNo,
                        verseNo,
                    )
                }
            }
        }
    }

    private fun validateFootnotes(
        footnotes: JsonArray,
        chapterNo: Int,
        verseNo: Int,
    ) {
        val seen = HashSet<Int>(footnotes.size)
        for (index in 0 until footnotes.size) {
            val footnote = footnotes.getOrNull(index)?.jsonObject
                ?: invalid("Chapter $chapterNo verse $verseNo has a footnote at position ${index + 1} that is not an object.")
            val number = positiveInt(footnote, QuranConstants.KEY_NUMBER)
            val text = footnote.safeString(QuranConstants.KEY_FOOTNOTE_SINGLE_TEXT)

            if (number == null) {
                invalid("Chapter $chapterNo verse $verseNo has a footnote without a positive \"index\".")
            }

            if (!seen.add(number)) invalid("Chapter $chapterNo verse $verseNo repeats footnote $number.")
            if (text !is String) {
                invalid("Chapter $chapterNo verse $verseNo footnote $number is missing \"text\".")
            }

            requireValidTranslationMarkup(text, chapterNo, verseNo, "footnote#$number")
        }
    }

    private fun positiveInt(obj: JsonObject, key: String): Int? {
        if (!obj.contains(key)) return null

        val value = obj.safeInt(key) ?: return null

        return value.takeIf { it >= 1 }
    }

    private fun invalid(message: String): Nothing {
        throw ImportException(ImportFailure.INVALID, message)
    }
}
