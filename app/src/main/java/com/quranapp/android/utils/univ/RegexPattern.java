package com.quranapp.android.utils.univ;

import java.util.regex.Pattern;

public class RegexPattern {
    public static final Pattern DIACRITICS_PATTERN = Pattern.compile("[^\\p{ASCII}]");
    public static final Pattern DIACRITICS_PATTERN_UNICODE = Pattern.compile("\\p{M}");
    public static final Pattern VERSE_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    public static final Pattern CHAPTER_OR_JUZ_PATTERN = Pattern.compile("(\\d+)");
    public static final Pattern VERSE_JUMP_PATTERN = Pattern.compile("(\\d+)[\\s+]?:[\\s+]?(\\d+)");
    public static final Pattern VERSE_RANGE_JUMP_PATTERN = Pattern.compile("(\\d+)[\\s+]?:[\\s+]?(\\d+)[\\-](\\d+)");
}
