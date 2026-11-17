package com.quranapp.android.db.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.entities.quran.AyahWordEntity

data class AyahWithWords(
    @Embedded
    val ayah: AyahEntity,

    @Relation(
        parentColumns = ["ayah_id"],
        entityColumns = ["ayah_id"],
    )
    val words: List<AyahWordEntity>,
)
