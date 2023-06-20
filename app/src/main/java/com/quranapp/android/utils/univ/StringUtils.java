package com.quranapp.android.utils.univ;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
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
}