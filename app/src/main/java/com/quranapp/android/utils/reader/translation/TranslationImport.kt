package com.quranapp.android.utils.reader.translation

import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import com.quranapp.android.utils.reader.importing.ImportFieldId
import com.quranapp.android.utils.reader.importing.ImportManifest
import java.text.Normalizer
import java.util.Locale

data class ImportLanguage(
    val code: String,
    val name: String,
)

private const val MAX_SLUG_LENGTH = 128
private const val SLUG_MARKER = "_custom-"

private val combiningMarks = Regex("\\p{Mn}+")
private val nonSlugChars = Regex("[^a-z0-9]+")

private class ImportLanguageIndex(
    val languages: List<ImportLanguage>,
    val byCode: Map<String, ImportLanguage>,
)

private val importLanguageIndex: ImportLanguageIndex by lazy {
    val languages = Locale.getISOLanguages()
        .map { code ->
            val name = Locale.forLanguageTag(code)
                .getDisplayLanguage(Locale.ENGLISH)
                .trim()
                .replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
                }

            ImportLanguage(code, name)
        }
        .filter { language ->
            language.name.isNotEmpty() && !language.name.equals(language.code, ignoreCase = true)
        }
        .distinctBy { it.code }
        .sortedBy { it.name }

    ImportLanguageIndex(languages, languages.associateBy { it.code })
}

fun importLanguages(): List<ImportLanguage> = importLanguageIndex.languages

fun translationSlug(langCode: String, book: String): String? {
    val code = langCode.trim().lowercase()
    if (code !in importLanguageIndex.byCode) return null

    val bookPart = book.toBookSlug()
    if (bookPart.isEmpty()) return null

    val prefix = "$code$SLUG_MARKER"
    val maxBookLength = MAX_SLUG_LENGTH - prefix.length
    if (maxBookLength < 1) return null

    val trimmedBook = bookPart.take(maxBookLength).trim('-')
    if (trimmedBook.isEmpty()) return null
    return prefix + trimmedBook
}

fun Map<ImportFieldId, String>.toTranslationManifest(): ImportManifest {
    fun value(id: ImportFieldId) = this[id].orEmpty().trim()

    val book = value(ImportFieldId.BOOK)
    val author = value(ImportFieldId.AUTHOR)
    val language = importLanguageIndex.byCode[value(ImportFieldId.LANG_CODE).lowercase()]
    val slug = language?.let { translationSlug(it.code, book) }

    if (book.isEmpty() || author.isEmpty() || language == null || slug == null) {
        throw ImportException(ImportFailure.DETAILS)
    }

    return ImportManifest(
        slug = slug,
        book = book,
        author = author,
        displayName = value(ImportFieldId.DISPLAY_NAME).ifEmpty { book },
        langCode = language.code,
        langName = language.name,
        version = 1,
    )
}

private fun String.toBookSlug(): String {
    val ascii = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(combiningMarks, "")

    return ascii
        .lowercase()
        .replace(nonSlugChars, "-")
        .trim('-')
}
