package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.utils.reader.rememberQuranMushafId
import com.quranapp.android.viewModels.ReaderIntentData
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class NavTab { Chapter, Juz, Hizb, Page }

@Composable
fun ReaderNavigator(
    readerVm: ReaderViewModel,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val readerMode by readerVm.readerMode.collectAsState()
    val isReading = readerMode == ReaderMode.Reading


    val tabs = remember(isReading) {
        if (isReading) NavTab.entries
        else listOf(NavTab.Chapter, NavTab.Juz, NavTab.Hizb)
    }

    var selectedTabIndex by readerVm.selectedNavigationTabIndex

    if (selectedTabIndex >= tabs.size) {
        selectedTabIndex = 0
    }

    val selectedTab = tabs[selectedTabIndex]

    fun navigateChapter(chapterNo: Int) {
        scope.launch {
            if (isReading) {
                val page = withContext(Dispatchers.IO) {
                    readerVm.repository.getFirstPageOfChapter(chapterNo)
                }
                if (page != null) readerVm.requestPageNavigation(page)
            } else {
                readerVm.initReader(ReaderIntentData.FullChapter(chapterNo))
            }
            onClose()
        }
    }

    fun navigateVerse(chapterNo: Int, verseNo: Int) {
        scope.launch {
            if (isReading) {
                val page = withContext(Dispatchers.IO) {
                    readerVm.repository.getPageForVerse(chapterNo, verseNo)
                }
                if (page != null) readerVm.requestPageNavigation(page)
            } else {
                val isInCurrentView = readerVm.verseByVerseItems.value.any { item ->
                    item is ReaderLayoutItem.VerseUI &&
                            item.verse.chapterNo == chapterNo &&
                            item.verse.verseNo == verseNo
                }

                if (isInCurrentView) {
                    readerVm.requestVerseNavigation(chapterNo, verseNo)
                } else {
                    readerVm.initReader(ReaderIntentData.FullChapter(chapterNo))
                    readerVm.requestVerseNavigation(chapterNo, verseNo)
                }
            }
            onClose()
        }
    }

    fun navigateJuz(juzNo: Int) {
        scope.launch {
            if (isReading) {
                val page = withContext(Dispatchers.IO) {
                    readerVm.repository.getFirstPageOfJuz(juzNo)
                }
                if (page != null) readerVm.requestPageNavigation(page)
            } else {
                readerVm.initReader(ReaderIntentData.FullJuz(juzNo))
            }
            onClose()
        }
    }

    fun navigateHizb(hizbNo: Int) {
        scope.launch {
            if (isReading) {
                val page = withContext(Dispatchers.IO) {
                    readerVm.repository.getFirstPageOfHizb(hizbNo)
                }
                if (page != null) readerVm.requestPageNavigation(page)
            } else {
                readerVm.initReader(ReaderIntentData.FullHizb(hizbNo))
            }
            onClose()
        }
    }

    fun navigatePage(pageNo: Int) {
        scope.launch {
            readerVm.requestPageNavigation(pageNo)
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.primary,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            when (tab) {
                                NavTab.Chapter -> stringResource(R.string.strTitleReaderChapters)
                                NavTab.Juz -> stringResource(R.string.strTitleReaderJuz)
                                NavTab.Hizb -> stringResource(R.string.strTitleReaderHizb)
                                NavTab.Page -> stringResource(R.string.strTitleChapInfoPages)
                            },
                            style = typography.labelLarge
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            NavTab.Chapter -> ChapterNavigatorList(
                readerVm = readerVm,
                onChapterSelected = ::navigateChapter,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Juz -> JuzNavigationList(
                readerVm,
                onJuzSelected = ::navigateJuz,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Hizb -> HizbNavigationList(
                readerVm,
                onHizbSelected = ::navigateHizb,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Page -> PageNavigationList(
                readerVm,
                onPageSelected = ::navigatePage,
            )
        }
    }
}

