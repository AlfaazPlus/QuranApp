package com.quranapp.android.db.entities.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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