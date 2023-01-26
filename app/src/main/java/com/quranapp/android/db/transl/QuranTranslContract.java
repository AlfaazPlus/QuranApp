/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 6/6/2022.
 * All rights reserved.
 */

package com.quranapp.android.db.transl;

import android.provider.BaseColumns;

public final class QuranTranslContract {
    private QuranTranslContract() {}

    public static class QuranTranslEntry implements BaseColumns {
        public static final String TABLE_NAME = "Translation";
        public static final String COL_CHAPTER_NO = "chapterNo";
        public static final String COL_VERSE_NO = "verseNo";
        public static final String COL_TEXT = "text";
        public static final String COL_FOOTNOTES = "footnotes";
    }
}