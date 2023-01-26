package com.quranapp.android.db.recitation;

import android.provider.BaseColumns;

public final class RecitationContract {
    private RecitationContract() {}

    public static class RecitationEntry implements BaseColumns {
        public static final String TABLE_NAME = "Recitation";
        public static final String COL_SLUG = "slug";
        public static final String COL_RECITER = "reciter";
        public static final String COL_STYLE = "style";
        public static final String COL_URL_HOST = "url_host";
        public static final String COL_URL_PATH = "url_path";
        public static final String COL_IS_PREMIUM = "is_premium";
    }
}