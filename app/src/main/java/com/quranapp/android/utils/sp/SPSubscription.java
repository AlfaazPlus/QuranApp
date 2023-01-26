/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */

package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class SPSubscription {
    private static final String SP_SUBSCRIPTION = "sp_subscription";
    private static final String KEY_UNLOCKED_TRANSLS = "key.unlocked_transls";
    private static final String KEY_UNLOCKED_RECITATIONS = "key.unlocked_recitations";


    public static Set<String> getUnlockedTransls(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_SUBSCRIPTION, Context.MODE_PRIVATE);
        if (sp.contains(KEY_UNLOCKED_TRANSLS)) {
            return new HashSet<>(sp.getStringSet(KEY_UNLOCKED_TRANSLS, new HashSet<>()));
        }
        return new HashSet<>();
    }

    public static void setUnlockedTransls(Context ctx, Set<String> unlockedTransls) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_SUBSCRIPTION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_UNLOCKED_TRANSLS, unlockedTransls);
        editor.apply();
    }

    public static Set<String> getUnlockedRecitations(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_SUBSCRIPTION, Context.MODE_PRIVATE);
        if (sp.contains(KEY_UNLOCKED_RECITATIONS)) {
            return new HashSet<>(sp.getStringSet(KEY_UNLOCKED_RECITATIONS, new HashSet<>()));
        }
        return new HashSet<>();
    }

    public static void setUnlockedRecitations(Context ctx, Set<String> unlockedRecitations) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_SUBSCRIPTION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_UNLOCKED_RECITATIONS, unlockedRecitations);
        editor.apply();
    }
}
