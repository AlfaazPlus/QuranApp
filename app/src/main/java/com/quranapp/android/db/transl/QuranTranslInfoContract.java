package com.quranapp.android.db.transl;

public final class QuranTranslInfoContract {
    private QuranTranslInfoContract() {}

    public static class QuranTranslInfoEntry {
        public static final String TABLE_NAME = "QuranTranslationBookInfo";
        public static final String COL_LANG_CODE = "langCode";
        public static final String COL_SLUG = "slug";
        public static final String COL_LANG_NAME = "langName";
        public static final String COL_BOOK_NAME = "bookName";
        public static final String COL_AUTHOR_NAME = "authorName";
        public static final String COL_DISPLAY_NAME = "displayName";
        public static final String COL_IS_PREMIUM = "isPremium";
        public static final String COL_LAST_UPDATED = "lastUpdated";
        public static final String COL_DOWNLOAD_PATH = "downloadPath";
    }
}