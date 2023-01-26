package com.quranapp.android.db.bookmark;

import android.provider.BaseColumns;

public final class BookmarkContract {
    private BookmarkContract() {}

    public static class BookmarkEntry implements BaseColumns {
        public static final String TABLE_NAME = "QuranBookmark";
        public static final String COL_CHAPTER_NO = "ChapterNumber";
        public static final String COL_FROM_VERSE_NO = "FromVerseNumber";
        public static final String COL_TO_VERSE_NO = "ToVerseNumber";
        public static final String COL_DATETIME = "Date";
        public static final String COL_NOTE = "Note";
    }
}