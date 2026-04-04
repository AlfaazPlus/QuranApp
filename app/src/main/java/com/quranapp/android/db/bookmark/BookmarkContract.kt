package com.quranapp.android.db.bookmark

import android.provider.BaseColumns

object BookmarkContract {

    object BookmarkEntry : BaseColumns {
        const val TABLE_NAME = "QuranBookmark"

        const val _ID = "_id"
        const val COL_CHAPTER_NO = "ChapterNumber"
        const val COL_FROM_VERSE_NO = "FromVerseNumber"
        const val COL_TO_VERSE_NO = "ToVerseNumber"
        const val COL_DATETIME = "Date"
        const val COL_NOTE = "Note"
    }
}