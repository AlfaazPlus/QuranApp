package com.quranapp.android.components.quran.subcomponents

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.quranapp.android.utils.verse.VerseUtils
import java.io.Serializable
import java.util.Locale


class Verse : Serializable {
    val id: Int

    @JvmField
    val pageNo: Int

    @JvmField
    val chapterNo: Int

    @JvmField
    val verseNo: Int

    @JvmField
    val arabicText: String = ""

    var segments: List<String> = emptyList()

    @JvmField
    val endText: String
    var translations: List<Translation> = ArrayList()
    var includeChapterNameInSerial = false

    @Deprecated("")
    @Transient
    @JvmField
    var arabicTextSpannable: CharSequence? = null

    @Deprecated("")
    @Transient
    @JvmField
    var translTextSpannable: CharSequence? = null

    constructor(
        id: Int,
        chapterNo: Int,
        verseNo: Int,
        pageNo: Int,
        segments: List<String>,
        endText: String,
    ) {
        this.id = id
        this.chapterNo = chapterNo
        this.verseNo = verseNo
        this.pageNo = pageNo
        this.segments = segments
        this.endText = endText
    }

    constructor(verse: Verse) {
        id = verse.id
        chapterNo = verse.chapterNo
        verseNo = verse.verseNo
        pageNo = verse.pageNo
        segments = verse.segments
        endText = verse.endText

        // not copying
        translations = verse.translations
        includeChapterNameInSerial = verse.includeChapterNameInSerial
    }

    fun getTranslationCount() = translations.size

    fun isVOTD(ctx: Context) = VerseUtils.isVOTD(chapterNo, verseNo)

    fun isIdealForVOTD() = arabicText.length in 6..300

    fun copy() = Verse(this)

    override fun toString(): String {
        return String.format(
            Locale.getDefault(),
            "VERSE: ChapterNo - %d, VerseNo - %d\n",
            chapterNo,
            verseNo
        )
    }
}