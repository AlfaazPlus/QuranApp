package com.quranapp.android.utils.miscellenous

import java.util.Locale

object StringUtils {
    const val DASH: String = "–"
    const val VERTICAL_BAR: String = "│"
    const val HYPHEN: String = "—"
    const val RTL_MARK: String = "\u200F"

    /**
     * Remove a substring from a string.
     * 
     * @param string The string.
     * @return Returns a new string with the all HTML tags removed.
     */
    fun removeHTML(string: String, preserveContent: Boolean): String {
        return string.replace("<.*?>(.*?)<.*?>".toRegex(), if (preserveContent) "$1" else "")
    }

    fun formatInvariant(format: String, vararg args: Any?): String {
        return format.format(Locale.ENGLISH, *args)
    }

    fun isRtlLanguage(langCode: String?): Boolean {
        if (langCode == null || langCode.isEmpty()) {
            return false
        }

        val rtlLangs = arrayOf<String?>(
            "ar",
            "fa",
            "ur",
            "ps",
            "sd",
            "ug",
            "dv",
            "he",
            "yi",
            "ku",
            "syr",
            "az-Arab",
            "ckb"
        )

        val parts: Array<String?> =
            langCode.split("[_-]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (parts.size == 0) {
            return false
        }

        val code = parts[0]

        for (rtlLang in rtlLangs) {
            if (rtlLang.equals(code, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
}
