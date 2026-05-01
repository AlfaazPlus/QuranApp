package com.quranapp.android.compose.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

@Composable
fun formattedStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val appLocale = LocalAppLocale.current
    val resources = LocalResources.current

    val pattern = resources.getString(id)

    return String.format(appLocale.platformLocale, pattern, *formatArgs)
}

fun formatString(
    context: Context,
    appLocale: AppLocale,
    pattern: String,
    vararg formatArgs: Any,
): String {
    return String.format(appLocale.platformLocale, pattern, *formatArgs)
}

fun formatString(
    context: Context,
    pattern: String,
    vararg formatArgs: Any,
): String {
    return formatString(context, readAppLocale(context), pattern, *formatArgs)
}

fun formatStringResource(
    context: Context,
    appLocale: AppLocale,
    @StringRes id: Int,
    vararg formatArgs: Any,
): String {
    val pattern = context.resources.getString(id)
    return formatString(context, appLocale, pattern, *formatArgs)
}

fun formatStringResource(
    context: Context,
    @StringRes id: Int,
    vararg formatArgs: Any,
): String = formatStringResource(context, readAppLocale(context), id, *formatArgs)
