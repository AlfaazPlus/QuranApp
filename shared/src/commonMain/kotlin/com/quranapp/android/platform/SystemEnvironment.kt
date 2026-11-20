package com.quranapp.android.platform

enum class AppThemeMode(val storageKey: String) {
    System("app.theme.default"),
    Light("app.theme.light"),
    Dark("app.theme.dark");

    companion object {
        fun fromStorage(key: String?): AppThemeMode {
            if (key.isNullOrEmpty()) return System
            return entries.firstOrNull { it.storageKey == key } ?: System
        }
    }
}

interface SystemEnvironment {
    val density: Float
    val screenWidthPx: Int
    val screenHeightPx: Int
    val systemLanguageTag: String
    val isDarkTheme: Boolean
    val isDynamicColorSupported: Boolean

    fun applyThemeMode(mode: AppThemeMode)

    /** Null follows the system locale. */
    fun applyLanguage(languageTag: String?)
}

val SystemEnvironment.screenWidthDp: Float
    get() = screenWidthPx / density

val SystemEnvironment.screenHeightDp: Float
    get() = screenHeightPx / density

fun Float.dpToPx(): Float = this * Platform.environment.density

fun Float.pxToDp(): Float = this / Platform.environment.density
