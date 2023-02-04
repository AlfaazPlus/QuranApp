/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */

package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class SPAppActions {
    public static final String SP_APP_ACTION = "sp_app_action";

    // Fetch translations forced in ActivityTranslationDownload.
    public static final String KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE = "app.action.translations.fetch_force";
    public static final String KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE = "app.action.recitations.fetch_force";
    public static final String KEY_APP_ACTION_SP_URLS_FETCH_FORCE = "app.action.urls.fetch_force";
    public static final String KEY_APP_ACTION_SP_PENDING = "app.action.pending";
    public static final String KEY_APP_ACTION_ONBOARDING_REQUIRED = "app.action.onboarding_required";
    public static final String KEY_APP_ACTION_ACC_REMINDER_REQUIRED = "app.action.acc_reminder_required";
    public static final String KEY_APP_LATEST_APP_VERSION = "app.action.app_latest_version";

    public static void setFetchTranslsForce(Context ctx, boolean fetchForce) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE, fetchForce);
        editor.apply();
    }

    public static boolean getFetchTranslsForce(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_APP_ACTION_SP_TRANSLS_FETCH_FORCE, false);
    }

    public static void setFetchRecitationsForce(Context ctx, boolean fetchForce) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE, fetchForce);
        editor.apply();
    }

    public static boolean getFetchRecitationsForce(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_APP_ACTION_SP_RECITATIONS_FETCH_FORCE, false);
    }

    public static void setFetchUrlsForce(Context ctx, boolean fetchForce) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_APP_ACTION_SP_URLS_FETCH_FORCE, fetchForce);
        editor.apply();
    }

    public static boolean getFetchUrlsForce(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_APP_ACTION_SP_URLS_FETCH_FORCE, false);
    }

    public static void addToPendingAction(Context ctx, String action, String victim) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);

        Set<String> pendingActionsSP = sp.getStringSet(KEY_APP_ACTION_SP_PENDING, null);

        final Set<String> pendingActions;
        if (pendingActionsSP != null && !pendingActionsSP.isEmpty()) {
            pendingActions = new HashSet<>(pendingActionsSP);
        } else {
            pendingActions = new HashSet<>();
        }

        pendingActions.add(makePendingActionKey(action, victim));

        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions);
        editor.apply();
    }

    public static void removeFromPendingAction(Context ctx, String action, String victim) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);

        Set<String> pendingActionsSP = sp.getStringSet(KEY_APP_ACTION_SP_PENDING, null);

        if (pendingActionsSP != null && !pendingActionsSP.isEmpty()) {
            final Set<String> pendingActions = new HashSet<>(pendingActionsSP);
            pendingActions.remove(makePendingActionKey(action, victim));

            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet(KEY_APP_ACTION_SP_PENDING, pendingActions);
            editor.apply();
        }
    }

    private static String makePendingActionKey(String action, String victim) {
        if (TextUtils.isEmpty(victim)) {
            return action;
        }
        return action + ":" + victim;
    }

    public static Set<String> getPendingActions(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        return sp.getStringSet(KEY_APP_ACTION_SP_PENDING, new HashSet<>());
    }

    public static boolean getRequireOnboarding(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        if (!sp.contains(KEY_APP_ACTION_ONBOARDING_REQUIRED)) {
            setRequireOnboarding(ctx, true);
            return true;
        }
        return sp.getBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, false);
    }

    public static void setRequireOnboarding(Context ctx, boolean require) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_APP_ACTION_ONBOARDING_REQUIRED, require);
        editor.apply();
    }

    public static boolean getRequireAccReminder(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        if (!sp.contains(KEY_APP_ACTION_ACC_REMINDER_REQUIRED)) {
            setRequireAccReminder(ctx, true);
            return true;
        }
        return sp.getBoolean(KEY_APP_ACTION_ACC_REMINDER_REQUIRED, false);
    }

    public static void setRequireAccReminder(Context ctx, boolean require) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_APP_ACTION_ACC_REMINDER_REQUIRED, require);
        editor.apply();
    }

    public static long getLatestAppVersion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE);
        return sp.getLong(KEY_APP_LATEST_APP_VERSION, -1);
    }

    public static void setLatestAppVersion(Context ctx, long version) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(SP_APP_ACTION, Context.MODE_PRIVATE).edit();
        editor.putLong(KEY_APP_LATEST_APP_VERSION, version);
        editor.apply();
    }
}
