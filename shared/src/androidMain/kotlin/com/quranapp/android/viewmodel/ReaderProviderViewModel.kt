package com.quranapp.android.viewmodel

import androidx.lifecycle.viewModelScope
import com.quranapp.android.AndroidAppContext
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.QuranAtlasLoader
import com.quranapp.android.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
open class ReaderProviderViewModel() : AppViewModel() {
    private val context get() = AndroidAppContext.get()

    val controller = Platform.audio.controller
    val userRepository get() = DatabaseProvider.getUserRepository()
    val repository get() = DatabaseProvider.getQuranRepository()
    val fontResolver = FontResolver.getInstance(context)
    val externalQuranDb get() = DatabaseProvider.getExternalQuranDatabase()

    val atlasBundle: StateFlow<QuranAtlasBundle?> = ReaderPreferences.quranScriptFlow()
        .mapLatest { script ->
            if (!script.isQuranAtlasScript()) {
                null
            } else {
                QuranAtlasLoader.getBundle(externalQuranDb, script)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
