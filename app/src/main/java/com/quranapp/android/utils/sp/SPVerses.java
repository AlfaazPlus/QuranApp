package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences utility class for verse related utils.
 */
public abstract class SPVerses {
    public static final String SP_VOTD = "sp_votd";
    public static final String KEY_VOTD_DATE = "votd_date";
    public static final String KEY_VOTD_CHAPTER_NO = "votd_chapter_no";
    public static final String KEY_VOTD_VERSE_NO = "votd_verse_no";
    public static final String KEY_VOTD_REMINDER_ENABLED = "votd_reminder_enabled";

    public static void saveVOTD(Context context, long datetime, int chapterNo, int verseNo) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.putLong(KEY_VOTD_DATE, datetime);
        editor.putInt(KEY_VOTD_CHAPTER_NO, chapterNo);
        editor.putInt(KEY_VOTD_VERSE_NO, verseNo);
        editor.apply();
    }

    public static int[] getVOTD(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE);
        return new int[]{sp.getInt(KEY_VOTD_CHAPTER_NO, -2), sp.getInt(KEY_VOTD_VERSE_NO, -2)};
    }

    public static void removeVOTD(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static void setVOTDReminderEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_VOTD_REMINDER_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getVOTDReminderEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_VOTD_REMINDER_ENABLED, true);
    }
}
