package com.quranapp.android.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.msgChapterAddedToFavourites
import com.quranapp.android.generated.resources.msgChapterRemovedFromFavourites
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.ReaderChapterIndexFilters
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

class ReaderIndexViewModel() : AppViewModel() {
    val repository get() = DatabaseProvider.getQuranRepository()

    private val filtersJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val chapterFiltersKey = stringPreferencesKey("reader_index_chapter_filters")
    private val chapterFiltersDefaultJson =
        filtersJson.encodeToString(ReaderChapterIndexFilters.Default)

    val chapterIndexFilters: StateFlow<ReaderChapterIndexFilters> = DataStoreManager
        .flow(chapterFiltersKey, chapterFiltersDefaultJson)
        .map { raw ->
            try {
                filtersJson.decodeFromString<ReaderChapterIndexFilters>(raw)
            } catch (e: Exception) {
                Logger.saveError(e, "chapterIndexFilters")
                ReaderChapterIndexFilters.Default
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderChapterIndexFilters.Default
        )

    private val _surahNosWithSajdah = MutableStateFlow<Set<Int>>(emptySet())
    val surahNosWithSajdah: StateFlow<Set<Int>> = _surahNosWithSajdah.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _surahNosWithSajdah.value = repository.getSurahNosWithSajdah()
            } catch (e: Exception) {
                Logger.saveError(e, "surahNosWithSajdah")
            }
        }
    }

    fun setChapterIndexFilters(filters: ReaderChapterIndexFilters) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreManager.write(
                chapterFiltersKey,
                filtersJson.encodeToString(filters)
            )
        }
    }

    val surahs = repository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val juzs = repository.getJuzs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hizbs = repository.getHizbs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favouriteChapters: StateFlow<List<Int>> = DataStoreManager
        .flow(KEY, Json.encodeToString(emptyList<Int>()))
        .map { raw ->
            try {
                Json.decodeFromString<List<Int>>(raw)
            } catch (e: Exception) {
                Logger.saveError(e, "getFavouriteChapters")
                emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    suspend fun addToFavourites(chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr.toMutableList().apply { add(0, chapterNo) })
        )

        PlatformServices.messages.show(getString(Res.string.msgChapterAddedToFavourites))
    }

    suspend fun removeFromFavourites(chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr - chapterNo)
        )

        PlatformServices.messages.show(getString(Res.string.msgChapterRemovedFromFavourites))
    }

    companion object {
        private val KEY = stringPreferencesKey(Keys.FAVOURITE_CHAPTERS)

        fun migrateFavourites(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val old = SPFavouriteChapters.getFavouriteChapters(context)

                if (old.isEmpty()) return@launch

                try {
                    DataStoreManager.write(
                        KEY,
                        Json.encodeToString(old)
                    )
                } catch (e: Exception) {
                    Logger.saveError(
                        e,
                        "migrateFavouriteChapters",
                    )
                }
            }
        }

    }
}
