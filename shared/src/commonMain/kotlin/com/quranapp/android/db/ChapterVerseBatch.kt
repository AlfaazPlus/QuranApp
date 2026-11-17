package com.quranapp.android.db

import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.SurahWithLocalizations

data class ChapterVerseBatch(
    val surah: SurahWithLocalizations,
    val ayahByVerseNo: Map<Int, AyahEntity>,
    val wordsByVerseNo: Map<Int, List<AyahWordEntity>>,
    val pageByVerseNo: Map<Int, Int>,
)
