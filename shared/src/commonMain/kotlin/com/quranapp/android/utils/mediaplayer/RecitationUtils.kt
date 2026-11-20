package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.utils.miscellenous.toFormatted

object RecitationUtils {
    const val DIR_NAME = "recitations"
    val URL_CHAPTER_PATTERN = Regex("""\{chapNo:(.*?)\}""", RegexOption.IGNORE_CASE)
    val URL_VERSE_PATTERN = Regex("""\{verseNo:(.*?)\}""", RegexOption.IGNORE_CASE)

    const val AVAILABLE_RECITATIONS_FILENAME = "available_recitations.json"
    const val AVAILABLE_RECITATION_TRANSLATIONS_FILENAME = "available_recitation_translations.json"

    const val RECITATION_AUDIO_NAME_FORMAT_LOCAL = "%03d-%03d.mp3"

    fun prepareAudioUrl(
        urlTemplate: String,
        chapterNo: Int
    ): String? {
        return try {
            URL_CHAPTER_PATTERN.replace(urlTemplate) { matchResult ->
                matchResult.groupValues[1].toFormatted(null, chapterNo)
            }
        } catch (_: Exception) {
            null
        }
    }
}
