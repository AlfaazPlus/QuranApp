/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/7/2022.
 * All rights reserved.
 */
package com.quranapp.android.components.quran.subcomponents

import java.io.Serializable

class Translation() : Serializable {
    var footnotes = HashMap<Int, Footnote>()
    var chapterNo = 0
    var verseNo = 0
    var text = ""
    var isUrdu = false
    var bookSlug = ""

    private constructor(translation: Translation) : this() {
        chapterNo = translation.chapterNo
        verseNo = translation.verseNo
        text = translation.text
        isUrdu = translation.isUrdu
        bookSlug = translation.bookSlug
        translation.footnotes.forEach { (_, footnote) -> addFootnote(footnote.copy()) }
    }

    private fun addFootnote(footnote: Footnote) {
        footnote.bookSlug = bookSlug
        footnotes[footnote.number] = footnote
    }

    fun getFootnote(footnoteNo: Int): Footnote? {
        return footnotes[footnoteNo]
    }

    fun getFootnotesCount(): Int {
        return footnotes.size
    }

    fun copy(): Translation {
        return Translation(this)
    }

    override fun toString(): String {
        return "Translation{verse=$chapterNo:$verseNo, text='$text'}"
    }
}
