package com.quranapp.android.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.SurahEntity

data class SurahWithAyahs(
    @Embedded
    val surah: SurahEntity,

    @Relation(
        parentColumn = "surah_no",
        entityColumn = "surah_no"
    )
    val ayahs: List<AyahEntity>
)