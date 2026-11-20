package com.quranapp.android.utils.miscellenous

const val DASH: String = "–"
const val VERTICAL_BAR: String = "│"
const val HYPHEN: String = "—"

fun Char.isRTL(): Boolean {
    val d = Character.getDirectionality(this)
    return d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
}


fun String.isRtlLanguageCode(): Boolean {
    if (isEmpty()) {
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

    val parts = split("[_-]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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

fun String.stripHtmlTags(preserveContent: Boolean): String {
    return replace("<.*?>(.*?)<.*?>".toRegex(), if (preserveContent) "$1" else "")
}
