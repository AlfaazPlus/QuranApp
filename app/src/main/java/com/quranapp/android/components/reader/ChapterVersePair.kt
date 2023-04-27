package com.quranapp.android.components.reader

import com.quranapp.android.components.quran.subcomponents.Verse
import java.io.Serializable

data class ChapterVersePair(val chapterNo: Int, val verseNo: Int) : Serializable {
    constructor(verse: Verse) : this(verse.chapterNo, verse.verseNo)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChapterVersePair) return false

        return this.chapterNo == other.chapterNo && this.verseNo == other.verseNo
    }

    override fun toString(): String {
        return "($chapterNo:$verseNo)"
    }

    override fun hashCode(): Int {
        var result = chapterNo
        result = 31 * result + verseNo
        return result
    }
}
