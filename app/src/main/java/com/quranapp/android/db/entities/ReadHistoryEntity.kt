package com.quranapp.android.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "read_history")
data class ReadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "read_type")
    val readType: String,

    @ColumnInfo(name = "reader_mode")
    val readerMode: String,

    @ColumnInfo(name = "division_no")
    val divisionNo: Int = 0,

    @ColumnInfo(name = "chapter_no")
    val chapterNo: Int = 0,

    @ColumnInfo(name = "from_verse_no")
    val fromVerseNo: Int = 0,

    @ColumnInfo(name = "to_verse_no")
    val toVerseNo: Int = 0,

    @ColumnInfo(name = "mushaf_code")
    val mushafCode: String? = null,

    @ColumnInfo(name = "mushaf_variant")
    val mushafVariant: String? = null,

    @ColumnInfo(name = "page_no")
    val pageNo: Int? = null,

    @ColumnInfo(name = "datetime")
    val datetime: Long = System.currentTimeMillis(),
)
