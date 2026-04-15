package com.quranapp.android.db.relations

import androidx.room.Embedded
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity

data class SurahNoSearchResult(
    val surahNo: Int
)