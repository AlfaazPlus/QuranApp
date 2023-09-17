package com.quranapp.android.utils.sharedPrefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.quranapp.android.utils.univ.Keys;

/**
 * SharedPreferences utility class for verse related utils.
 */
public abstract class SPVerses {
    public static final String SP_VOTD = "sp_votd";

    public static void saveVOTD(Context context, long datetime, int chapterNo, int verseNo) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.putLong(Keys.KEY_VOTD_DATE, datetime);
        editor.putInt(Keys.KEY_VOTD_CHAPTER_NO, chapterNo);
        editor.putInt(Keys.KEY_VOTD_VERSE_NO, verseNo);
        editor.apply();
    }

    public static int[] getVOTD(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE);
        return new int[]{sp.getInt(Keys.KEY_VOTD_CHAPTER_NO, -2), sp.getInt(Keys.KEY_VOTD_VERSE_NO, -2)};
    }

    public static void removeVOTD(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static void setVOTDReminderEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE).edit();
        editor.putBoolean(Keys.KEY_VOTD_REMINDER_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getVOTDReminderEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_VOTD, Context.MODE_PRIVATE);
        return sp.getBoolean(Keys.KEY_VOTD_REMINDER_ENABLED, true);
    }
}
