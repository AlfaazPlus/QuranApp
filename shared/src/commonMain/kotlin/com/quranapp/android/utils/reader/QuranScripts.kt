package com.quranapp.android.utils.reader

object QuranScripts {
    const val UTHMANI = "uthmani"
    const val DK_INDOPAK = "dk_indopak"
    const val NASTALEEQ_INDOPAK = "nastaleeq_indopak"
    const val KFQPC_V1 = "kfqpc_v1"
    const val KFQPC_V2 = "kfqpc_v2"
    const val KFQPC_V4 = "kfqpc_v4_tajweed"
    const val DEFAULT = UTHMANI

    val supported: Set<String> = setOf(
        UTHMANI,
        DK_INDOPAK,
        KFQPC_V1,
        KFQPC_V2,
        KFQPC_V4,
    )

    fun coerce(scriptCode: String): String =
        if (scriptCode in supported) scriptCode else DEFAULT
}

enum class QuranScriptVariant(val value: String) {
    INDOPAK_16("indopak_16"),
    INDOPAK_15("indopak_15"),
    ;

    companion object {
        fun fromValue(value: String?): QuranScriptVariant? {
            return entries.find { it.value == value }
        }
    }
}
