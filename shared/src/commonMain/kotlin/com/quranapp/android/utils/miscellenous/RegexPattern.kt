package com.quranapp.android.utils.miscellenous

object RegexPattern {

    val DIACRITICS_PATTERN = Regex("[^\\p{ASCII}]")

    val DIACRITICS_PATTERN_UNICODE = Regex("\\p{M}")

    val VERSE_RANGE_PATTERN = Regex("(\\d+)-(\\d+)")

    val CHAPTER_OR_JUZ_PATTERN = Regex("(\\d+)")

    val VERSE_JUMP_PATTERN = Regex("(\\d+)\\s*:\\s*(\\d+)")

    val VERSE_RANGE_JUMP_PATTERN = Regex(
        "(\\d+)\\s*:\\s*(\\d+)[-](\\d+)"
    )
}
