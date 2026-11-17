package com.quranapp.android.utils.app

enum class ResourceDownloadProxy(val value: String) {
    ALFAAZ_PLUS("alfaazplus"),
    GITHUB("github"),
    JSDELIVR("jsdelivr"),
    ;

    companion object {
        val DEFAULT = ALFAAZ_PLUS

        fun fromValue(value: String): ResourceDownloadProxy {
            return entries.firstOrNull { it.value == value } ?: ALFAAZ_PLUS
        }
    }
}
