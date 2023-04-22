package com.quranapp.android.components.quran.subcomponents

import android.content.Context
import com.quranapp.android.utils.verse.VerseUtils
import java.io.Serializable
import java.util.*

class Verse : Serializable {
    val id: Int

    @JvmField
    val pageNo: Int

    @JvmField
    val chapterNo: Int

    @JvmField
    val verseNo: Int

    @JvmField
    val arabicText: String

    @JvmField
    val endText: String
    var translations: List<Translation> = ArrayList()
    var includeChapterNameInSerial = false

    @Transient
    @JvmField
    var arabicTextSpannable: CharSequence? = null

    @Transient
    @JvmField
    var translTextSpannable: CharSequence? = null

    constructor(id: Int, chapterNo: Int, verseNo: Int, pageNo: Int, arabicText: String, endText: String) {
        this.id = id
        this.chapterNo = chapterNo
        this.verseNo = verseNo
        this.pageNo = pageNo
        this.arabicText = arabicText
        this.endText = endText
    }

    constructor(verse: Verse) {
        id = verse.id
        chapterNo = verse.chapterNo
        verseNo = verse.verseNo
        pageNo = verse.pageNo
        arabicText = verse.arabicText
        endText = verse.endText
        // not copying
        translations = verse.translations
        includeChapterNameInSerial = verse.includeChapterNameInSerial
    }

    fun getTranslationCount() = translations.size

    fun isVOTD(ctx: Context) = VerseUtils.isVOTD(ctx, chapterNo, verseNo)

    fun isIdealForVOTD() = arabicText.length in 6..300

    fun copy() = Verse(this)

    override fun toString(): String {
        return String.format(Locale.getDefault(), "VERSE: ChapterNo - %d, VerseNo - %d\n", chapterNo, verseNo)
    }
}