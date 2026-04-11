package com.quranapp.android.compose.components.player.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheetBare
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.ReciterSelectorViewModel
import kotlinx.coroutines.launch

@Composable
fun ReciterSelectorSheet(
    controller: RecitationController,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = viewModel<ReciterSelectorViewModel>()

    val tabs = listOf(
        R.string.strTitleQuran,
        R.string.labelTranslation,
    )
    val pagerContentHeight = 580.dp
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val reciterListState = rememberLazyListState()
    val translationListState = rememberLazyListState()

    BottomSheetBare(
        isOpen = isOpen,
        onDismiss = onClose,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.strTitleSelectReciter),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.strTitleSelectReciter),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))

                IconButton(
                    painter = painterResource(R.drawable.dr_icon_download)
                ) {
                    context.startActivity(
                        Intent(context, ActivitySettings::class.java).apply {
                            putExtra(Keys.NAV_DESTINATION, SettingRoutes.RECITATION_DOWNLOAD)
                        },
                    )
                }

                IconButton(
                    painter = painterResource(R.drawable.dr_icon_refresh)
                ) {
                    viewModel.invalidateReciters()
                }
            }
        },
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(stringResource(titleRes)) },
                        unselectedContentColor = colorScheme.onSurface.alpha(0.8f)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerContentHeight),
            ) { page ->
                if (page == 0) {
                    QuranReciters(
                        viewModel = viewModel,
                        controller = controller,
                        listState = reciterListState,
                    )
                } else {
                    TranslationReciters(
                        viewModel = viewModel,
                        controller = controller,
                        listState = translationListState,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranReciters(
    viewModel: ReciterSelectorViewModel,
    controller: RecitationController,
    listState: LazyListState,
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedQuranReciter = RecitationPreferences.observeReciterId()
    val quranReciters by viewModel.quranReciters.collectAsState()

    // on initial load, scroll to the selected reciter
    LaunchedEffect(quranReciters) {
        val index = quranReciters?.indexOfFirst { it.id == selectedQuranReciter } ?: 0

        if (index != -1) {
            listState.scrollToItem(index)
        }
    }

    if (quranReciters == null) {
        return Loader(fill = true)
    }

    val reciters = quranReciters!!

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
    ) {
        items(
            reciters.size,
            key = {
                reciters[it].id
            },
        ) { index ->
            val reciter = reciters[index]

            RadioItem(
                titleStr = reciter.getReciterName(),
                subtitleStr = reciter.getStyleName(),
                selected = reciter.id == selectedQuranReciter,
                onClick = {
                    if (reciter.id != selectedQuranReciter) {
                        coroutineScope.launch {
                            RecitationPreferences.setReciterId(reciter.id)
                            controller.setReciter(reciter.id, RecitationAudioKind.QURAN)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TranslationReciters(
    viewModel: ReciterSelectorViewModel,
    controller: RecitationController,
    listState: LazyListState,
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedTranslationReciter = RecitationPreferences.observeTranslationReciterId()
    val translationReciters by viewModel.translationReciters.collectAsState()

    // on initial load, scroll to the selected reciter
    LaunchedEffect(translationReciters) {
        val index = translationReciters?.indexOfFirst { it.id == selectedTranslationReciter } ?: 0

        if (index != -1) {
            listState.scrollToItem(index)
        }
    }

    if (translationReciters == null) {
        return Loader(fill = true)
    }

    val reciters = translationReciters!!

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
    ) {
        items(
            reciters.size,
            key = {
                reciters[it].id
            },
        ) { index ->
            val reciter = reciters[index]

            RadioItem(
                titleStr = reciter.getReciterName(),
                subtitleStr = reciter.langName,
                selected = reciter.id == selectedTranslationReciter,
                onClick = {
                    if (reciter.id != selectedTranslationReciter) {
                        coroutineScope.launch {
                            RecitationPreferences.setTranslationReciterId(reciter.id)
                            controller.setReciter(reciter.id, RecitationAudioKind.TRANSLATION)
                        }
                    }
                },
            )
        }
    }
}