package com.quranapp.android.utils.reader.importing

data class ImportManifest(
    val slug: String,
    val book: String,
    val author: String,
    val displayName: String,
    val langCode: String,
    val langName: String,
    val version: Long,
)

enum class ImportFieldId {
    BOOK,
    AUTHOR,
    DISPLAY_NAME,
    LANG_CODE,
}

enum class ImportFailure {
    DETAILS,
    INVALID,
    UNREADABLE,
    TOO_LARGE,
    RESERVED,
}

class ImportException(
    val failure: ImportFailure,
    message: String? = null,
) : Exception(message)
