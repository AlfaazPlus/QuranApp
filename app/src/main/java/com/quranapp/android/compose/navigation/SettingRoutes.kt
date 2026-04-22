package com.quranapp.android.compose.navigation

import com.quranapp.android.utils.univ.Keys

data class SingleArgRoute(private val route: String, private val argName: String) {
    operator fun invoke() = "$route/{$argName}"

    fun arg(arg: Any) = "$route/$arg"
}

data class MultiArgRoute(private val route: String, private val argNames: List<String>) {
    operator fun invoke() = "$route?${argNames.joinToString("&") { "$it={$it}" }}"

    fun args(vararg args: Any) = "$route?${
        argNames
            .zip(args)
            .joinToString("&") { "${it.first}=${it.second}" }
    }"
}

object SettingRoutes {
    val MAIN = SingleArgRoute("settings.main", Keys.SHOW_READER_SETTINGS_ONLY)
    const val LANGUAGE = "settings.language"
    const val THEME = "settings.theme"
    const val TRANSLATIONS = "settings.translations"
    const val TRANSLATIONS_DOWNLOAD = "settings.translations_download"
    const val TAFSIR = "settings.tafsir"
    const val SCRIPT = "settings.script"
    const val WWB = "settings.wbw"
    const val RECITATION_DOWNLOAD = "settings.recitation_download"
    const val APP_LOGS = "settings.app_logs"
}