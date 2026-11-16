package com.quranapp.android.search

import java.text.Normalizer
import java.util.Locale

object SearchNormalizer {
    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .trim()
    }

    /** Keeps original case. The FTS tokenizer casefolds, and the stored text is shown in results. */
    fun normalizeForTranslationIndex(input: String): String {
        return foldArabicLetters(
            Normalizer.normalize(input, Normalizer.Form.NFKC)
        ).trim()
    }

    fun foldArabicLetters(input: String): String {
        if (input.isEmpty()) return input
        val folded = StringBuilder(input.length)
        for (char in input) {
            folded.append(
                when (char) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ى' -> 'ي'
                    else -> char
                }
            )
        }
        return folded.toString()
    }
}
