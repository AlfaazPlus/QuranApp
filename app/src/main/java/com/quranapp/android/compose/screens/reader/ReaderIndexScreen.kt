package com.quranapp.android.compose.screens.reader

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peacedesign.android.utils.ColorUtils
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySearch
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.compose.components.ChapterCard
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.FavChaptersViewModel
import kotlinx.coroutines.launch

@Composable
fun ReaderIndexScreen(
) {
    val context = LocalContext.current

    var quranMeta by remember { mutableStateOf<QuranMeta?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                quranMeta = r
                isLoading = false
            }
        })
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var chaptersReversed by remember { mutableStateOf(false) }
    var juzReversed by remember { mutableStateOf(false) }

    val chaptersListState = rememberLazyListState()
    val juzListState = rememberLazyListState()
    val favListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val selectedTab = pagerState.currentPage

    LaunchedEffect(chaptersReversed) {
        chaptersListState.scrollToItem(0)
    }

    LaunchedEffect(juzReversed) {
        juzListState.scrollToItem(0)
    }

    if (isLoading || quranMeta == null) {
        ReaderIndexLoading()
        return
    }

    val meta = quranMeta ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainer)
                    .statusBarsPadding(),
            ) {
                ReaderIndexHeader()
                ReaderIndexTopBar(
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ReaderIndexChaptersList(
                            quranMeta = meta,
                            reversed = chaptersReversed,
                            listState = chaptersListState
                        )

                        1 -> ReaderIndexJuzList(
                            quranMeta = meta,
                            reversed = juzReversed,
                            listState = juzListState
                        )

                        2 -> ReaderIndexFavChaptersList(
                            quranMeta = meta,
                            listState = favListState
                        )
                    }
                }

                ReaderIndexTabs(
                    modifier = Modifier.align(Alignment.TopCenter),
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        if (index == selectedTab) {
                            scope.launch {
                                when (index) {
                                    0 -> chaptersListState.animateScrollToItem(0)
                                    1 -> juzListState.animateScrollToItem(0)
                                    2 -> favListState.animateScrollToItem(0)
                                }
                            }
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                )
            }
        }

        ReaderIndexSortFab(
            visible = !isLoading && quranMeta != null && selectedTab != 2,
            onClick = {
                when (selectedTab) {
                    0 -> chaptersReversed = !chaptersReversed
                    1 -> juzReversed = !juzReversed
                }
                scope.launch {
                    when (selectedTab) {
                        0 -> chaptersListState.animateScrollToItem(0)
                        1 -> juzListState.animateScrollToItem(0)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun ReaderIndexTopBar(
    modifier: Modifier
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = { backDispatcher?.onBackPressed() },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_chevron_left),
                contentDescription = stringResource(R.string.strLabelBack),
                tint = colorScheme.onSurface
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        ActivitySearch::class.java
                    )
                )

            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_search),
                contentDescription = stringResource(R.string.strHintSearch),
                tint = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReaderIndexHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.quran_kareem),
            contentDescription = stringResource(R.string.strTitleHolyQuran),
            modifier = Modifier
                .height(100.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.strTitleHolyQuran),
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurface
        )
    }
}

private val ReaderIndexTabHeight = 48.dp

@Composable
private fun ReaderIndexTabs(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        Pair(R.string.strTitleReaderChapters, 0),
        Pair(R.string.strTitleReaderJuz, 0),
        Pair(R.string.favourites, 0),
    )
    val borderColor = colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()

                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            },
        shadowElevation = 2.dp,
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colorScheme.primary,
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, (titleRes, iconRes) ->
                val isSelected = selectedTab == index

                Tab(
                    selected = isSelected,
                    selectedContentColor = colorScheme.primary,
                    unselectedContentColor = colorScheme.onSurfaceVariant,
                    onClick = { onTabSelected(index) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (iconRes != 0) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                )
                            }

                            if (titleRes != 0) {
                                Text(
                                    text = stringResource(titleRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun ReaderIndexChaptersList(
    quranMeta: QuranMeta,
    reversed: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favChaptersViewModel = viewModel<FavChaptersViewModel>()
    val favChapters = favChaptersViewModel.getFavouriteChapters()
    val chapterNumbers = remember(quranMeta, reversed) {
        if (!reversed) {
            (1..QuranMeta.totalChapters()).toList()
        } else {
            (QuranMeta.totalChapters() downTo 1).toList()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = ReaderIndexTabHeight + 16.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(chapterNumbers, key = { it }) { chapterNo ->
            val isFav = favChapters.contains(chapterNo)

            ChapterCard(
                chapterNo = chapterNo,
                chapterName = quranMeta.getChapterName(context, chapterNo),
                chapterTrans = quranMeta.getChapterNameTranslation(chapterNo),
                isFavourite = isFav,
                onClick = {
                    ReaderFactory.startChapter(context, chapterNo)
                },
                onToggleFavourite = {
                    scope.launch {
                        if (isFav) {
                            favChaptersViewModel.removeFromFavourites(
                                context,
                                chapterNo,
                                favChapters
                            )
                        } else {
                            favChaptersViewModel.addToFavourites(
                                context,
                                chapterNo,
                                favChapters
                            )
                        }
                    }
                }
            )
        }
    }
}

private data class JuzItem(
    val juzNo: Int,
    val chapterMetas: List<QuranMeta.ChapterMeta>
)

@Composable
private fun ReaderIndexJuzList(
    quranMeta: QuranMeta,
    reversed: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val juzItems = remember(quranMeta, reversed) {
        val items = mutableListOf<JuzItem>()
        val from = if (reversed) QuranMeta.totalJuzs() else 1
        val to = if (reversed) 1 else QuranMeta.totalJuzs()
        var juzNo = from

        while (true) {
            val chaptersRange = quranMeta.getChaptersInJuz(juzNo)
            val chapterMetas = (chaptersRange.first..chaptersRange.second)
                .mapNotNull { quranMeta.getChapterMeta(it) }

            items += JuzItem(juzNo, chapterMetas)

            if (reversed) {
                juzNo--
                if (juzNo < to) break
            } else {
                juzNo++
                if (juzNo > to) break
            }
        }
        items
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = ReaderIndexTabHeight + 16.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(juzItems, key = { it.juzNo }) { item ->
            JuzCard(
                item = item,
                quranMeta = quranMeta,
                onOpenJuz = { ReaderFactory.startJuz(context, item.juzNo) },
                onOpenVerseRange = { chapterNo, range ->
                    ReaderFactory.startVerseRange(context, chapterNo, range)
                }
            )
        }
    }
}

@Composable
private fun JuzCard(
    item: JuzItem,
    quranMeta: QuranMeta,
    onOpenJuz: () -> Unit,
    onOpenVerseRange: (Int, Pair<Int, Int>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenJuz)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.strLabelJuzNo, item.juzNo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                item.chapterMetas.forEach { chapterMeta ->
                    val versesInJuz =
                        quranMeta.getVerseRangeOfChapterInJuz(item.juzNo, chapterMeta.chapterNo)

                    if (versesInJuz != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenVerseRange(chapterMeta.chapterNo, versesInJuz)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chapterMeta.chapterNo.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = colorScheme.onSurface
                                )
                            }


                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = chapterMeta.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val translation = chapterMeta.nameTranslation
                                if (!translation.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = translation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = stringResource(R.string.strLabelVersesText) + ":",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Normal,
                                )
                                Text(
                                    text = "${versesInJuz.first}-${versesInJuz.second}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderIndexFavChaptersList(
    quranMeta: QuranMeta,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favChaptersViewModel = viewModel<FavChaptersViewModel>()
    val favChapters = favChaptersViewModel.getFavouriteChapters()

    when {
        favChapters.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        16.dp,
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_star_outlined),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = stringResource(R.string.msgNoFavouriteChapters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = ReaderIndexTabHeight + 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favChapters, key = { it }) { chapterNo ->
                    ChapterCard(
                        chapterNo = chapterNo,
                        chapterName = quranMeta.getChapterName(context, chapterNo),
                        chapterTrans = quranMeta.getChapterNameTranslation(chapterNo),
                        isFavourite = true,
                        onClick = {
                            ReaderFactory.startChapter(context, chapterNo)
                        },
                        onToggleFavourite = {
                            MessageUtils.showConfirmationDialog(
                                context,
                                title = context.getString(R.string.titleRemoveFromFavourites),
                                msg = quranMeta.getChapterName(context, chapterNo),
                                btn = context.getString(R.string.strLabelRemove),
                                btnColor = ColorUtils.DANGER,
                                action = Runnable {
                                    scope.launch {
                                        favChaptersViewModel.removeFromFavourites(
                                            context,
                                            chapterNo,
                                            favChapters
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderIndexSortFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            modifier = Modifier.size(48.dp),
            onClick = onClick,
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_sort),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ReaderIndexLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}