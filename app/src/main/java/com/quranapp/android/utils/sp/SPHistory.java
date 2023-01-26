/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */

package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;

public class SPHistory {
    private static final String SP_HISTORY = "sp_history";
    public static final String KEY_HISTORY_SUBS_LAST_REMINDED = "key.history.subs_last_reminded";

    public static void addToHistory(Context ctx, String key, String value) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_HISTORY, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getHistory(Context ctx, String key) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_HISTORY, Context.MODE_PRIVATE);
        return sp.getString(key, null);
    }
}
