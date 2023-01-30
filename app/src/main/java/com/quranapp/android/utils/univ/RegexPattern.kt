package com.quranapp.android.utils.univ

import java.util.regex.Pattern

object RegexPattern {
    @JvmField
    val DIACRITICS_PATTERN = Pattern.compile("[^\\p{ASCII}]")
    val DIACRITICS_PATTERN_UNICODE = Pattern.compile("\\p{M}")
    @JvmField
    val VERSE_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)")
    @JvmField
    val CHAPTER_OR_JUZ_PATTERN = Pattern.compile("(\\d+)")
    @JvmField
    val VERSE_JUMP_PATTERN = Pattern.compile("(\\d+)[\\s+]?:[\\s+]?(\\d+)")
    @JvmField
    val VERSE_RANGE_JUMP_PATTERN = Pattern.compile("(\\d+)[\\s+]?:[\\s+]?(\\d+)[\\-](\\d+)")
}