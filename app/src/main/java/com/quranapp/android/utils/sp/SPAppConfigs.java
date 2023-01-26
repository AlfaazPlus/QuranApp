/*
 * (c) Faisal Khan. Created on 17/10/2021.
 */

/*
 * (c) Faisal Khan. Created on 12/10/2021.
 */

package com.quranapp.android.utils.sp;

import android.content.Context;
import android.content.SharedPreferences;

public class SPAppConfigs {
    private static final String SP_APP_CONFIGS = "sp_app_configs";
    private static final String KEY_APP_THEME = "key.app.theme";
    private static final String KEY_APP_LANGUAGE = "key.app.language";
    private static final String KEY_URLS_VERSION = "key.versions.urls";
    private static final String KEY_TRANSLATIONS_VERSION = "key.versions.translations";
    private static final String KEY_RECITATIONS_VERSION = "key.versions.recitations";

    public static final String LOCALE_DEFAULT = "default";

    public static final String THEME_MODE_DEFAULT = "app.theme.default";
    public static final String THEME_MODE_LIGHT = "app.theme.light";
    public static final String THEME_MODE_DARK = "app.theme.dark";

    public static void setThemeMode(Context ctx, String themeMode) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_APP_THEME, themeMode);
        editor.apply();
    }

    public static String getThemeMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        return sp.getString(KEY_APP_THEME, THEME_MODE_DEFAULT);
    }

    public static void setLocale(Context ctx, String locale) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_APP_LANGUAGE, locale);
        editor.apply();
    }

    public static String getLocale(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        return sp.getString(KEY_APP_LANGUAGE, LOCALE_DEFAULT);
    }

    public static long getUrlsVersion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        return sp.getLong(KEY_URLS_VERSION, 0);
    }

    public static void setUrlsVersion(Context ctx, long version) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(KEY_URLS_VERSION, version);
        editor.apply();
    }

    public static long getTranslationsVersion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        return sp.getLong(KEY_TRANSLATIONS_VERSION, 0);
    }

    public static void setTranslationsVersion(Context ctx, long version) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(KEY_TRANSLATIONS_VERSION, version);
        editor.apply();
    }

    public static long getRecitationsVersion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        return sp.getLong(KEY_RECITATIONS_VERSION, 0);
    }

    public static void setRecitationsVersion(Context ctx, long version) {
        SharedPreferences sp = ctx.getSharedPreferences(SP_APP_CONFIGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(KEY_RECITATIONS_VERSION, version);
        editor.apply();
    }
}
