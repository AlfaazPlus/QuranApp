package com.quranapp.android.locale

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided; wrap content with QuranAppTheme")
}

@Composable
fun formattedStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val appLocale = LocalAppLocale.current
    val resources = LocalResources.current
    val pattern = resources.getString(id)
    return String.format(appLocale.platformLocale, pattern, *formatArgs)
}

@Composable
fun <T> rememberLocalized(vararg keys: Any?, load: suspend (Context) -> T): T? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = appPlatformLocale()
    val state by produceState<T?>(null, context, configuration, locale, *keys) {
        value = withContext(Dispatchers.IO) { load(context) }
    }
    return state
}
