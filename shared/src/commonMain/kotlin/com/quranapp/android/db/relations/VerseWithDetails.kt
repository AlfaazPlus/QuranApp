package com.quranapp.android.db.relations

import com.quranapp.android.models.Translation
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.interfaces.SurahMethods

data class VerseWithDetails(
    val words: List<AyahWordEntity>,
    val pageNo: Int,
    val verse: AyahEntity,
    val chapter: SurahWithLocalizations
) : SurahMethods by chapter {
    val id get() = verse.ayahId
    val chapterNo get() = verse.surahNo
    val verseNo get() = verse.ayahNo

    var translations: List<Translation> = ArrayList()
    var includeChapterNameInSerial = false

    fun getTranslationCount() = translations.size

    fun isIdealForVOTD(): Boolean {
        val arabicText = words.joinToString(" ") { it.text }
        return arabicText.length in 6..300
    }

    override fun toString(): String {
        return "VERSE ($id) -  $chapterNo:$verseNo"
    }
}

