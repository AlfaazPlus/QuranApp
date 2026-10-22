package com.quranapp.android.db.entities.quran

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "mushafs",
    indices = [
        Index(value = ["mushaf_code"], unique = true)
    ]
)
data class MushafEntity(
    @PrimaryKey
    @ColumnInfo(name = "mushaf_id")
    val mushafId: Int,

    @ColumnInfo(name = "mushaf_code")
    val mushafCode: String,

    @ColumnInfo(name = "no_of_pages")
    val noOfPages: Int,

    @ColumnInfo(name = "lines_per_page")
    val linesPerPage: Int
)