package com.quranapp.android.search

data class TranslationOption(
    val slug: String,
    val displayName: String,
)

data class SearchFilters(
    val selectedSlugs: Set<String>? = null,
) {
    val isEmpty: Boolean
        get() = selectedSlugs.isNullOrEmpty()
}
