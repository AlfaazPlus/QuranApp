package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quranapp.android.R
import com.quranapp.android.compose.utils.AppLocale
import com.quranapp.android.compose.utils.appLocaleFlow
import com.quranapp.android.compose.utils.formatString
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FeaturedQuranModel(
    val chapterNo: Int,
    val verseRange: Pair<Int, Int>,
    val title: String,
    val subtext: String,
)

class HomepageViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = DatabaseProvider.getQuranRepository(application)

    private val _featured = MutableStateFlow<List<FeaturedQuranModel>?>(null)
    val featured: StateFlow<List<FeaturedQuranModel>?> = _featured.asStateFlow()

    init {
        viewModelScope.launch {
            appLocaleFlow.collectLatest { locale ->
                val items = withContext(Dispatchers.IO) {
                    loadFeatured(locale)
                }
                _featured.value = items
            }
        }
    }

    private suspend fun loadFeatured(
        locale: AppLocale,
    ): List<FeaturedQuranModel> {
        val context = getApplication<Application>()
        val resources = context.resources
        val itemsArray = resources.obtainTypedArray(R.array.arrFeaturedQuranItems)

        val chapNameFormat = resources.getString(R.string.strLabelSurah)
        val verseNoFormat = resources.getString(R.string.strLabelVerseNo)
        val versesFormat = resources.getString(R.string.strLabelVerses)
        val miniInfoFormat = resources.getString(R.string.strLabelVerseWithChapNameWithBar)
        val miniInfoChapFormat = resources.getString(R.string.strLabelFeatureQuranMiniInfo)

        try {
            return List(itemsArray.length()) { i ->
                val raw = itemsArray.getString(i)!!
                val (chapterNo, start, end) = parseItem(raw, repository)
                val chapterName = repository.getChapterName(chapterNo)

                val title: String
                val subtext: String
                if (start == 1 && end == repository.getChapterVerseCount(chapterNo) && !raw.contains(":")) {
                    title = formatString(context, locale, chapNameFormat, chapterName)
                    subtext = formatString(context, locale, miniInfoChapFormat, chapterNo, 1, end)
                } else if (start == end) {
                    if (chapterNo == 2 && start == 255) {
                        title = resources.getString(R.string.strAyatulKursi)
                        subtext = formatString(context, locale, miniInfoFormat, chapterName, 255)
                    } else {
                        title = formatString(context, locale, chapNameFormat, chapterName)
                        subtext = formatString(context, locale, verseNoFormat, start)
                    }
                } else {
                    title = formatString(context, locale, chapNameFormat, chapterName)
                    subtext = formatString(context, locale, versesFormat, start, end)
                }

                FeaturedQuranModel(
                    chapterNo = chapterNo,
                    verseRange = start to end,
                    title = title,
                    subtext = subtext,
                )
            }
        } finally {
            itemsArray.recycle()
        }
    }
}

private suspend fun parseItem(
    raw: String,
    repo: QuranRepository
): Triple<Int, Int, Int> {
    val colonIndex = raw.indexOf(':')

    if (colonIndex == -1) {
        val chapter = raw.toInt()
        return Triple(
            chapter,
            1,
            repo.getChapterVerseCount(chapter)
        )
    }

    val chapter = raw.substring(0, colonIndex).toInt()
    val versePart = raw.substring(colonIndex + 1)

    val dashIndex = versePart.indexOfFirst { it == '-' || it == '–' }

    return if (dashIndex == -1) {
        val verse = versePart.toInt()
        Triple(chapter, verse, verse)
    } else {
        val start = versePart.substring(0, dashIndex).toInt()
        val end = versePart.substring(dashIndex + 1).toInt()
        Triple(chapter, start, end)
    }
}
