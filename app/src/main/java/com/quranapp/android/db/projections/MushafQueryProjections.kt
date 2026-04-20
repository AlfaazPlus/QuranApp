package com.quranapp.android.db.projections

import androidx.room.ColumnInfo

data class AyahPageProjection(
    @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
)

data class PageJuzProjection(
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "juz_no") val juzNo: Int,
)

data class PageHizbProjection(
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "hizb_no") val hizbNo: Int,
)
