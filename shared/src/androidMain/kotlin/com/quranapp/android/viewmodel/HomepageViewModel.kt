package com.quranapp.android.viewmodel

import androidx.lifecycle.viewModelScope
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strAyatulKursi
import com.quranapp.android.generated.resources.strLabelFeatureQuranMiniInfo
import com.quranapp.android.generated.resources.strLabelSurah
import com.quranapp.android.generated.resources.strLabelVerseNo
import com.quranapp.android.generated.resources.strLabelVerseWithChapNameWithBar
import com.quranapp.android.generated.resources.strLabelVerses
import com.quranapp.android.locale.AppLocale
import com.quranapp.android.locale.appLocaleFlow
import com.quranapp.android.reader.featuredQuranItems
import com.quranapp.android.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

data class FeaturedQuranModel(
    val chapterNo: Int,
    val verseRange: Pair<Int, Int>,
    val title: String,
    val subtext: String,
)

class HomepageViewModel() : AppViewModel() {

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
        val context = application

        val chapNameFormat = getString(Res.string.strLabelSurah)
        val verseNoFormat = getString(Res.string.strLabelVerseNo)
        val versesFormat = getString(Res.string.strLabelVerses)
        val miniInfoFormat = getString(Res.string.strLabelVerseWithChapNameWithBar)
        val miniInfoChapFormat = getString(Res.string.strLabelFeatureQuranMiniInfo)

        return featuredQuranItems.map { raw ->
            val (chapterNo, start, end) = parseItem(raw, repository)
            val chapterName = repository.getChapterName(chapterNo)

            val title: String
            val subtext: String
            if (start == 1 && end == repository.getChapterVerseCount(chapterNo) && !raw.contains(
                    ":"
                )
            ) {
                title = locale.format(chapNameFormat, chapterName)
                subtext = locale.format(miniInfoChapFormat, chapterNo, 1, end)
            } else if (start == end) {
                if (chapterNo == 2 && start == 255) {
                    title = getString(Res.string.strAyatulKursi)
                    subtext = locale.format(miniInfoFormat, chapterName, 255)
                } else {
                    title = locale.format(chapNameFormat, chapterName)
                    subtext = locale.format(verseNoFormat, start)
                }
            } else {
                title = locale.format(chapNameFormat, chapterName)
                subtext = locale.format(versesFormat, start, end)
            }

            FeaturedQuranModel(
                chapterNo = chapterNo,
                verseRange = start to end,
                title = title,
                subtext = subtext,
            )
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
