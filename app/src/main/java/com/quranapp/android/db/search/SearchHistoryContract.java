package com.quranapp.android.db.search;

import android.provider.BaseColumns;

public final class SearchHistoryContract {
    private SearchHistoryContract() {}

    public static class SearchEntry implements BaseColumns {
        public static final String TABLE_NAME = "SearchHistory";
        public static final String COL_TEXT = "Text";
        public static final String COL_DATE = "Date";
    }
}