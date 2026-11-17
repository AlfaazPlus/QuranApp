package com.quranapp.android.search

data class TranslationOption(
    val slug: String,
    val displayName: String,
)

enum class SearchMatchingStrategy(val value: String) {
    ANY_WORD("any"),
    ALL_WORDS("all"),
    EXACT_PHRASE("exact");

    companion object {
        fun fromValue(value: String?): SearchMatchingStrategy {
            return entries.find { it.value == value } ?: ANY_WORD
        }
    }
}

data class SearchFilters(
    val selectedSlugs: Set<String>? = null,
    val matchingStrategy: SearchMatchingStrategy = SearchMatchingStrategy.ANY_WORD,
) {
    val isEmpty: Boolean
        get() = selectedSlugs.isNullOrEmpty() && matchingStrategy == SearchMatchingStrategy.ANY_WORD

    val isValid: Boolean
        get() = selectedSlugs == null || selectedSlugs.isNotEmpty()
}
