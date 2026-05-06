package com.quranapp.android.components

import android.os.Bundle

data class ReferenceVerseModel(
    val title: String,
    val desc: String?,
    val translSlugs: Set<String>,
    val chapters: Set<Int>,
    val verses: Set<String>,
) {

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("title", title)
            putString("desc", desc)
            putStringArrayList("translSlugs", ArrayList(translSlugs))
            putIntegerArrayList("chapters", ArrayList(chapters))
            putStringArrayList("verses", ArrayList(verses))
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle?): ReferenceVerseModel? {
            if (bundle == null) {
                return null
            }

            return ReferenceVerseModel(
                title = bundle.getString("title") ?: "",
                desc = bundle.getString("desc"),
                translSlugs = bundle.getStringArray("translSlugs")?.let { it.toSet() }
                    ?: emptySet(),
                chapters = bundle.getIntegerArrayList("chapters")?.let { it.toSet() } ?: emptySet(),
                verses = bundle.getStringArrayList("verses")?.let { it.toSet() } ?: emptySet(),
            )
        }
    }
}
