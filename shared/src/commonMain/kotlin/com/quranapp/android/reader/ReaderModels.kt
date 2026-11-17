package com.quranapp.android.reader


enum class ReaderMode(val value: String) {
    VerseByVerse("mode_vbv"),
    Reading("mode_reading"),
    Translation("mode_translation");

    companion object {
        fun fromValue(value: String): ReaderMode {
            return entries.find { it.value == value } ?: VerseByVerse
        }

        fun fromLegacyStyleInt(style: Int): ReaderMode = when (style) {
            0x2 -> Reading
            else -> VerseByVerse
        }
    }
}

/* Layout used inside [ReaderMode.Reading]. */
enum class ReadingLayout(val value: String) {
    Page("page"),
    Flow("flow");

    companion object {
        fun fromValue(value: String): ReadingLayout {
            return entries.find { it.value == value } ?: Page
        }
    }
}
