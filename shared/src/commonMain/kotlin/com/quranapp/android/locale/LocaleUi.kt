package com.quranapp.android.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided; wrap content with QuranAppTheme")
}

@Composable
fun formattedStringResource(resource: StringResource, vararg formatArgs: Any): String {
    return stringResource(resource, *formatArgs)
}

@Composable
fun <T> rememberLocalized(vararg keys: Any?, load: suspend () -> T): T? {
    val locale = LocalAppLocale.current

    val state by produceState<T?>(null, locale, *keys) {
        value = withContext(Dispatchers.IO) { load() }
    }

    return state
}
