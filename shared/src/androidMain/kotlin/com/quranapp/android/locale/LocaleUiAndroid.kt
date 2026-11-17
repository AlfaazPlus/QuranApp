package com.quranapp.android.locale

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
