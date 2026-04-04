package com.quranapp.android.db.bookmark

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_CHAPTER_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_DATETIME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_NOTE
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.TABLE_NAME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry._ID

@Entity(tableName = TABLE_NAME)
data class BookmarkEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = _ID)
    val id: Long? = null,

    @ColumnInfo(name = COL_CHAPTER_NO)
    val chapterNo: Int?,

    @ColumnInfo(name = COL_FROM_VERSE_NO)
    val fromVerseNo: Int?,

    @ColumnInfo(name = COL_TO_VERSE_NO)
    val toVerseNo: Int?,

    @ColumnInfo(name = COL_DATETIME)
    val dateTime: String?,

    @ColumnInfo(name = COL_NOTE)
    val note: String?
)


@Immutable
data class BookmarkKey(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int
)