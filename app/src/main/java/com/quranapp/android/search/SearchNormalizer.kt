package com.quranapp.android.search

import java.text.Normalizer
import java.util.Locale

enum class ScriptType {
    LATIN,
    ARABIC,
    OTHER,
}


object SearchNormalizer {
    fun scriptForLang(langCode: String): ScriptType {
        val code = langCode.lowercase(Locale.ROOT)

        return when (code) {
            "en", "fr", "es", "id", "tr", "de", "nl", "sv", "no", "da", "it", "pt", "ms" -> ScriptType.LATIN
            "ar", "ur", "fa" -> ScriptType.ARABIC
            else -> ScriptType.OTHER
        }
    }

    fun normalize(input: String, script: ScriptType): String {
        var text = Normalizer.normalize(input, Normalizer.Form.NFKC)

        text = text.replace("\\s+".toRegex(), " ").trim()

        text = when (script) {
            ScriptType.LATIN -> latinNormalize(text)
            ScriptType.ARABIC -> arabicNormalize(text)
            ScriptType.OTHER -> text
        }

        return text.replace("\\s+".toRegex(), " ").trim()
    }

    private fun latinNormalize(s: String): String {
        return s
            .lowercase(Locale.ROOT)
            .replace("[\\p{Punct}]".toRegex(), " ")
    }

    private fun arabicNormalize(s: String): String {
        return s
            .replace("[\\u064B-\\u065F\\u0670]".toRegex(), "")
            .replace("ـ", "")
            .replace("[أإآٱ]".toRegex(), "ا")
            .replace("ى", "ي")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
