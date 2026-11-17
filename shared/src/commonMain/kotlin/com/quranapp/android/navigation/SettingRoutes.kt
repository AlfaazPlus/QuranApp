package com.quranapp.android.navigation

import com.quranapp.android.utils.miscellenous.Keys

object SettingRoutes {
    val MAIN = SingleArgRoute("settings.main", Keys.SHOW_READER_SETTINGS_ONLY)
    const val LANGUAGE = "settings.language"
    const val THEME = "settings.theme"
    const val TRANSLATIONS = "settings.translations"
    const val TRANSLATIONS_DOWNLOAD = "settings.translations.download"
    const val TAFSIR = "settings.tafsir"
    const val SCRIPT = "settings.script"
    const val WWB = "settings.wbw"
    const val RECITATION_DOWNLOAD = "settings.recitation_download"
    const val ADVANCED_SETTINGS = "settings.advanced"
    const val APP_LOGS = "settings.app_logs"
}
