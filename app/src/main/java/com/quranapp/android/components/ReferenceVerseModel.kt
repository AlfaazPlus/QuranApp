/*
 * Created by Faisal Khan on (c) 13/8/2021.
 */
package com.quranapp.android.components

import java.io.Serializable

class ReferenceVerseModel(
    private val showChaptersSugg: Boolean = false,
    val title: String,
    val desc: String,
    val translSlugs: Array<String>,
    val chapters: List<Int>,
    val verses: List<String>
) : Serializable {
    fun showChaptersSugg(): Boolean {
        return showChaptersSugg
    }

    override fun toString(): String {
        return "showChaptersSugg:$showChaptersSugg, title: $title, desc: $desc, desc: $translSlugs, chapters: $chapters, verses: $verses"
    }
}
