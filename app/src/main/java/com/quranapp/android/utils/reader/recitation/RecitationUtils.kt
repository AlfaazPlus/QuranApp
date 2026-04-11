package com.quranapp.android.utils.reader.recitation

import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import java.util.Locale
import java.util.regex.Pattern

@Deprecated("")
object RecitationUtils {
    @JvmField
    val DIR_NAME: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "recitations"
    )
    val URL_CHAPTER_PATTERN: Pattern =
        Pattern.compile("\\{chapNo:(.*?)\\}", Pattern.CASE_INSENSITIVE)
    val URL_VERSE_PATTERN: Pattern =
        Pattern.compile("\\{verseNo:(.*?)\\}", Pattern.CASE_INSENSITIVE)

    const val KEY_RECITATION_RECITER: String = "key.recitation.reciter"
    const val KEY_RECITATION_TRANSLATION_RECITER: String = "key.recitation_translation.reciter"
    const val KEY_RECITATION_SPEED: String = "key.recitation.speed"
    const val KEY_RECITATION_REPEAT: String = "key.recitation.repeat"
    const val KEY_RECITATION_CONTINUE_CHAPTER: String = "key.recitation.continue_chapter"
    const val KEY_RECITATION_SCROLL_SYNC: String = "key.recitation.verse_sync"
    const val KEY_RECITATION_AUDIO_OPTION: String = "key.recitation.option_audio"
    const val RECITATION_DEFAULT_SPEED: Float = 1.0f
    const val RECITATION_DEFAULT_REPEAT: Boolean = false
    const val RECITATION_DEFAULT_CONTINUE_CHAPTER: Boolean = true
    const val RECITATION_DEFAULT_VERSE_SYNC: Boolean = true

    const val AVAILABLE_RECITATIONS_FILENAME: String = "available_recitations.json"
    const val AVAILABLE_RECITATION_TRANSLATIONS_FILENAME: String =
        "available_recitation_translations.json"
    const val RECITATION_AUDIO_NAME_FORMAT_LOCAL: String = "%03d-%03d.mp3"

    const val AUDIO_OPTION_ONLY_QURAN: Int = 0
    const val AUDIO_OPTION_ONLY_TRANSLATION: Int = 1
    const val AUDIO_OPTION_BOTH: Int = 2
    val AUDIO_OPTION_DEFAULT: Int = AUDIO_OPTION_ONLY_QURAN

}
