package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.ReaderChapterIndexFilters
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReaderIndexViewModel(application: Application) : AndroidViewModel(application) {
    val repository = DatabaseProvider.getQuranRepository(application)

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
                Log.saveError(e, "chapterIndexFilters")
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
                Log.saveError(e, "surahNosWithSajdah")
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


    @Composable
    fun getFavouriteChapters(): List<Int> {
        val raw = DataStoreManager.observe(
            KEY,
            Json.encodeToString(emptyList<Int>())
        )

        return try {
            Json.decodeFromString<List<Int>>(raw)
        } catch (e: Exception) {
            Log.saveError(
                e,
                "getFavouriteChapters",
            )
            emptyList()
        }
    }

    suspend fun addToFavourites(ctx: Context, chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr.toMutableList().apply { add(0, chapterNo) })
        )

        MessageUtils.showRemovableToast(
            ctx,
            R.string.msgChapterAddedToFavourites,
            Toast.LENGTH_SHORT
        )
    }

    suspend fun removeFromFavourites(ctx: Context, chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr - chapterNo)
        )


        MessageUtils.showRemovableToast(
            ctx,
            R.string.msgChapterRemovedFromFavourites,
            Toast.LENGTH_SHORT
        )
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
                    Log.saveError(
                        e,
                        "migrateFavouriteChapters",
                    )
                }
            }
        }

    }
}