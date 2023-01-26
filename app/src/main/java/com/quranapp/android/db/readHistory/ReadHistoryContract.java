package com.quranapp.android.db.readHistory;

import android.provider.BaseColumns;

public final class ReadHistoryContract {
    private ReadHistoryContract() {}

    public static class ReadHistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "ReadHistory";

        public static final String COL_READ_TYPE = "ReadType";
        public static final String COL_READER_STYLE = "ReaderStyle";
        public static final String COL_JUZ_NO = "JuzNumber";
        public static final String COL_CHAPTER_NO = "ChapterNumber";
        public static final String COL_FROM_VERSE_NO = "FromVerseNumber";
        public static final String COL_TO_VERSE_NO = "ToVerseNumber";
        public static final String COL_DATETIME = "Date";
    }
}