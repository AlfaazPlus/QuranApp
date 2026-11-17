package com.quranapp.android.utils.reader.recitation

import java.util.regex.Pattern

object RecitationUtils {
    const val DIR_NAME = "recitations"
    val URL_CHAPTER_PATTERN: Pattern =
        Pattern.compile("\\{chapNo:(.*?)\\}", Pattern.CASE_INSENSITIVE)
    val URL_VERSE_PATTERN: Pattern =
        Pattern.compile("\\{verseNo:(.*?)\\}", Pattern.CASE_INSENSITIVE)

    const val AVAILABLE_RECITATIONS_FILENAME: String = "available_recitations.json"
    const val AVAILABLE_RECITATION_TRANSLATIONS_FILENAME: String =
        "available_recitation_translations.json"
    const val RECITATION_AUDIO_NAME_FORMAT_LOCAL: String = "%03d-%03d.mp3"
}
