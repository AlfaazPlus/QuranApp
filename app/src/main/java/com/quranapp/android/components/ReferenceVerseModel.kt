package com.quranapp.android.components

import android.os.Bundle

data class ReferenceVerseModel(
    val title: String,
    val desc: String?,
    val translSlugs: Array<String>,
    val chapters: List<Int>,
    val verses: List<String>,
) {

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("title", title)
            putString("desc", desc)
            putStringArray("translSlugs", translSlugs)
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
                translSlugs = bundle.getStringArray("translSlugs") ?: emptyArray(),
                chapters = bundle.getIntegerArrayList("chapters") ?: emptyList(),
                verses = bundle.getStringArrayList("verses") ?: emptyList(),
            )
        }
    }
}
