package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.mediaplayer.RecitationController
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
open class ReaderProviderViewModel(private val application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)
    val userRepository get() = DatabaseProvider.getUserRepository(application)
    val repository get() = DatabaseProvider.getQuranRepository(application)
    val fontResolver = FontResolver.getInstance(application)
    val externalQuranDb get() = DatabaseProvider.getExternalQuranDatabase(application)

    val atlasBundle: StateFlow<QuranAtlasBundle?> = ReaderPreferences.quranScriptFlow()
        .mapLatest { script ->
            if (!script.isQuranAtlasScript()) {
                null
            } else {
                QuranAtlasLoader.getBundle(application, externalQuranDb, script)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
