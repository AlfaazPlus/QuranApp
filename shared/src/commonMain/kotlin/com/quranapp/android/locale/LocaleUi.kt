package com.quranapp.android.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided; wrap content with QuranAppTheme")
}

@Composable
fun formattedStringResource(resource: StringResource, vararg formatArgs: Any): String {
    return stringResource(resource, *formatArgs)
}
