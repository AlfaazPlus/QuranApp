package com.quranapp.android.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "chapter_no")
    val chapterNo: Int,

    @ColumnInfo(name = "from_verse_no")
    val fromVerseNo: Int,

    @ColumnInfo(name = "to_verse_no")
    val toVerseNo: Int,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "date")
    val dateTime: Date = Date(),
)


@Immutable
data class BookmarkKey(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int
)