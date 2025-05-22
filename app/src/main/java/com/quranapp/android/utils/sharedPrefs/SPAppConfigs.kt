package com.quranapp.android.utils.sharedPrefs

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import com.quranapp.android.utils.app.DownloadSourceUtils.DOWNLOAD_SRC_DEFAULT

object SPAppConfigs {
    const val SP_APP_CONFIGS = "sp_app_configs"
    private const val KEY_APP_THEME = "key.app.theme"
    private const val KEY_APP_LANGUAGE = "key.app.language"
    private const val KEY_URLS_VERSION = "key.versions.urls"
    private const val KEY_TRANSLATIONS_VERSION = "key.versions.translations"
    private const val KEY_RECITATIONS_VERSION = "key.versions.recitations"
    private const val KEY_RECITATION_TRANSLATIONS_VERSION = "key.versions.recitation_translations"
    private const val KEY_TAFSIRS_VERSION = "key.versions.tafsirs"
    private const val KEY_RESOURCE_DOWNLOAD_SRC = "key.resource.download_src"

    const val LOCALE_DEFAULT = "default"
    const val THEME_MODE_DEFAULT = "app.theme.default"
    const val THEME_MODE_LIGHT = "app.theme.light"
    const val THEME_MODE_DARK = "app.theme.dark"


    private fun sp(context: Context) = context.getSharedPreferences(
        SP_APP_CONFIGS,
        Context.MODE_PRIVATE
    )

    @JvmStatic
    fun setThemeMode(ctx: Context, themeMode: String?) {
        sp(ctx).edit() {
            putString(KEY_APP_THEME, themeMode)
        }
    }

    fun getThemeMode(ctx: Context): String =
        sp(ctx).getString(KEY_APP_THEME, THEME_MODE_DEFAULT) ?: THEME_MODE_DEFAULT

    @JvmStatic
    fun setLocale(ctx: Context, locale: String?) {
        sp(ctx).edit(commit = true) {
            putString(KEY_APP_LANGUAGE, locale)
        }
    }

    @JvmStatic
    fun getLocale(ctx: Context): String =
        sp(ctx).getString(KEY_APP_LANGUAGE, LOCALE_DEFAULT) ?: LOCALE_DEFAULT

    fun getUrlsVersion(ctx: Context): Long = sp(ctx).getLong(KEY_URLS_VERSION, 0)

    fun setUrlsVersion(ctx: Context, version: Long) {
        sp(ctx).edit() {
            putLong(KEY_URLS_VERSION, version)
        }
    }

    fun getTranslationsVersion(ctx: Context): Long = sp(ctx).getLong(KEY_TRANSLATIONS_VERSION, 0)

    fun setTranslationsVersion(ctx: Context, version: Long) {
        sp(ctx).edit() {
            putLong(KEY_TRANSLATIONS_VERSION, version)
        }
    }

    fun getRecitationsVersion(ctx: Context): Long = sp(ctx).getLong(KEY_RECITATIONS_VERSION, 0)

    fun setRecitationsVersion(ctx: Context, version: Long) {
        sp(ctx).edit() {
            putLong(KEY_RECITATIONS_VERSION, version)
        }
    }

    fun getRecitationTranslationsVersion(ctx: Context): Long =
        sp(ctx).getLong(KEY_RECITATION_TRANSLATIONS_VERSION, 0)

    fun setRecitationTranslationsVersion(ctx: Context, version: Long) {
        sp(ctx).edit() {
            putLong(KEY_RECITATIONS_VERSION, version)
        }
    }

    fun getTafsirsVersion(ctx: Context): Long = sp(ctx).getLong(KEY_TAFSIRS_VERSION, 0)

    fun setTafsirsVersion(ctx: Context, version: Long) {
        sp(ctx).edit() {
            putLong(KEY_TAFSIRS_VERSION, version)
        }
    }

    @JvmStatic
    fun getResourceDownloadSrc(ctx: Context): String {
        return sp(ctx).getString(
            KEY_RESOURCE_DOWNLOAD_SRC,
            DOWNLOAD_SRC_DEFAULT
        ) ?: DOWNLOAD_SRC_DEFAULT
    }

    @JvmStatic
    fun setResourceDownloadSrc(ctx: Context, src: String?) {
        sp(ctx).edit(commit = true) {
            putString(KEY_RESOURCE_DOWNLOAD_SRC, src)
        }
    }
}
