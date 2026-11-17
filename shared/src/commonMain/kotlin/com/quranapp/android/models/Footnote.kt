/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 21/7/2022.
 * All rights reserved.
 */
package com.quranapp.android.models

import kotlinx.serialization.Serializable

@Serializable
class Footnote() {
    var chapterNo = 0
    var verseNo = 0
    var number = 0
    var text = ""
    var bookSlug = ""

    private constructor(footnote: Footnote) : this() {
        chapterNo = footnote.chapterNo
        verseNo = footnote.verseNo
        number = footnote.number
        text = footnote.text
        bookSlug = footnote.bookSlug
    }

    fun copy(): Footnote {
        return Footnote(this)
    }

    override fun toString(): String {
        return "Footnote{number=$chapterNo:$verseNo:$number, text='$text'}"
    }
}
