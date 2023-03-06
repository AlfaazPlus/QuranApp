package com.quranapp.android.utils.univ;

import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.ALPHABETIC;
import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.ALPHABETIC_LOWER;
import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.ALPHABETIC_UPPER;
import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.ALPHANUMERIC_LOWER;
import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.ALPHANUMERIC_UPPER;
import static com.quranapp.android.utils.univ.StringUtils.RandomPattern.NUMERIC;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public abstract class StringUtils {
    public static final String DASH = "–";
    public static final String VERTICAL_BAR = "│";
    public static final String HYPHEN = "—";
    public static final String RTL_MARK = "\u200F";

    public static String bundle2String(Bundle bundle) {
        StringBuilder builder = new StringBuilder();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            builder.append(key);
            builder.append("=");
            if (value instanceof Object[]) {
                builder.append(Arrays.toString((Object[]) value));
            } else {
                builder.append(value);
            }
            builder.append(", ");
        }
        return builder.toString();
    }

    /**
     * Remove a substring from a string.
     *
     * @param string    The string.
     * @param substring The substring which is to be removed.
     * @return Returns a new string with the substring removed.
     */
    @NonNull
    public static String remove(@NonNull String string, @NonNull String substring) {
        if (string.isEmpty() || substring.isEmpty() || !string.contains(substring)) return string;
        return string.replace(substring, "");
    }

    /**
     * Remove a substring from a string.
     *
     * @param string The string.
     * @return Returns a new string with the all HTML tags removed.
     */
    @NonNull
    public static String removeHTML(@NonNull String string, boolean preserveContent) {
        return string.replaceAll("<.*?>(.*?)<.*?>", preserveContent ? "$1" : "");
    }

    /**
     * Normalize a text.
     *
     * @param string The text.
     * @return Returns a new text with some special characters (_-:) removed. Returns "" if the
     * string is empty.
     */
    @NonNull
    public static String normalize(@NonNull String string) {
        if (string.isEmpty()) return "";
        String[] delimiters = {"_", "-", ":"};
        for (String del : delimiters) {
            if (string.contains(del)) string = string.replaceAll(del, " ");
        }
        return string;
    }

    /**
     * Capitalize a text. The text will have first character of each word in uppercase.
     *
     * @param text Text to be capitalized
     * @return Returns capitalized text
     */
    @NonNull
    public static String capitalize(@Nullable String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(text.length());
        boolean nextTitleCase = true;

        for (char c : text.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else c = Character.toLowerCase(c);
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Title case a text.
     *
     * @param text Text to be cased.
     * @return Returns title cased text.
     */
    @Nullable
    public static String toTitleCase(@Nullable String text) {
        if (text == null || text.isEmpty()) return null;
        return Character.toTitleCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    public static CharSequence replicate(CharSequence text, int times) {
        StringBuilder sb = new StringBuilder();
        replicate(sb, text, times);
        return sb;
    }

    public static void replicate(StringBuilder sb, CharSequence text, int times) {
        while (times-- > 0) {
            sb.append(text);
        }
    }

    public static String stripDiacritics(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = RegexPattern.DIACRITICS_PATTERN.matcher(str).replaceAll("");
        return str;
    }

    /**
     * <p> Delete one character from last from a text.</p>
     * Examples:
     * <blockquote><pre>
     * "hello".backspace() returns "hell"
     * "together".backspace() returns "togethe"
     * </pre></blockquote>
     *
     * @param text Text to be backspaced
     * @return Return backspaced text.
     */
    @NonNull
    public static String backspace(@NonNull String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.trim().substring(0, text.length() - 1);
    }

    /**
     * Find common substring in two strings.
     *
     * @param str1      First string
     * @param str2      Second string
     * @param delimiter The delimiter each string can be separated with, into substrings.
     * @return Returns the common string if found any, null otherwise.
     */
    @Nullable
    public static String commonString(@NonNull String str1, @NonNull String str2, @NonNull String delimiter) {
        if (str1.equals(str2)) return str1;
        String[] a1 = str1.split(delimiter);
        String[] b1 = str2.split(delimiter);

        String[] cRay = b1.length > a1.length ? a1 : b1;
        String[] dRay = b1.length > a1.length ? b1 : a1;
        List<String> cList = Arrays.asList(cRay);
        for (String val : dRay) if (cList.contains(val)) return val;
        return null;
    }

    /**
     * <p>Append first character of every word in a string.</p>
     * <blockquote>
     * Examples: If arguments passed are - "Hello Word", "[SPACE]", "_".
     * <pre>
     *     Then it returns "H_W"
     * </pre>
     * </blockquote>
     *
     * @param string       The string from which characters are to be appended
     * @param sepDelimiter What delimiter is separating words. eg - [UNDERSCORE] or [SPACE].
     * @param apdDelimiter The delimiter which will be used to append the characters.
     * @param count        How many characters should be taken from each word.
     * @return Returns first character of every word appended.
     */
    @NonNull
    public static String appendFirstChars(@NonNull String string,
                                          @NonNull String sepDelimiter,
                                          @NonNull String apdDelimiter, int count) {
        String[] substringArr = string.split(sepDelimiter);
        StringBuilder res = new StringBuilder();
        int len = substringArr.length;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < count; j++) res.append(substringArr[i].charAt(j));
            if (i < len - 1) res.append(apdDelimiter);
        }
        return res.toString();
    }

    /**
     * Generate a random string based of given parameters.
     *
     * @param length        The length of which the string should be.
     * @param randomPattern Pattern of the resulting string.
     *                      One of -
     *                      {@link RandomPattern#ALPHANUMERIC},
     *                      {@link RandomPattern#ALPHANUMERIC_LOWER},
     *                      {@link RandomPattern#ALPHANUMERIC_UPPER},
     *                      {@link RandomPattern#ALPHABETIC},
     *                      {@link RandomPattern#ALPHABETIC_LOWER},
     *                      {@link RandomPattern#ALPHABETIC_UPPER},
     *                      {@link RandomPattern#NUMERIC},
     * @return Returns a random string based of the given parameters.
     */
    @NonNull
    public static String random(int length, int randomPattern) {
        final String chars;
        switch (randomPattern) {
            case ALPHABETIC: chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"; break;
            case ALPHABETIC_LOWER: chars = "abcdefghijklmnopqrstuvwxyz"; break;
            case ALPHABETIC_UPPER: chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; break;
            case ALPHANUMERIC_LOWER: chars = "abcdefghijklmnopqrstuvwxyz1234567890"; break;
            case ALPHANUMERIC_UPPER: chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"; break;
            case NUMERIC: chars = "1234567890"; break;
            default: chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"; break;
        }

        StringBuilder string = new StringBuilder();
        Random rnd = new Random();
        int charLen = chars.length();
        while (string.length() < length) string.append(chars.charAt((int) (rnd.nextFloat() * charLen)));
        return string.toString();
    }

    public static String escapeRegex(String string) {
        return Pattern.quote(string);
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String str;
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }

        br.close();

        return sb.toString();
    }

    public static boolean isRTL(char c) {
        byte d = Character.getDirectionality(c);
        return d == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
                d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING ||
                d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
    }

    /**
     * Pattern for {@link #random(int, int)}
     */
    public static class RandomPattern {
        public static final int ALPHANUMERIC = 0x1;
        public static final int ALPHANUMERIC_LOWER = 0x2;
        public static final int ALPHANUMERIC_UPPER = 0x3;
        public static final int ALPHABETIC = 0x4;
        public static final int ALPHABETIC_LOWER = 0x5;
        public static final int ALPHABETIC_UPPER = 0x6;
        public static final int NUMERIC = 0x7;
    }
}