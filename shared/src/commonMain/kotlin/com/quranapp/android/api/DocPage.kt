package com.quranapp.android.api

enum class DocPage(private val file: String) {
    CUSTOM_TRANSLATION("custom-translation/README.md"),
    CUSTOM_TAFSIR("custom-tafsir/README.md"),
    ;

    fun url(): String = "${ApiConfig.GITHUB_REPOSITORY_URL}/blob/master/docs/$file"
}
