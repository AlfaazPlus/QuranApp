package com.quranapp.android.compose.screens.reference

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.compose.components.common.Chip
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT
import com.quranapp.android.compose.components.player.MiniPlayerVisibility
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.player.rememberMiniPlayerVisibilityState
import com.quranapp.android.compose.components.reader.LocalReaderViewModel
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.reader.TextStyleProvider
import com.quranapp.android.compose.components.reader.VerseView
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceVerses
import com.quranapp.android.compose.components.reader.dialogs.parseVerses
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.readAppLocale
import com.quranapp.android.db.entities.BookmarkKey
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.extensions.isSingleValue
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.Keys
import horizontalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private sealed class ReferenceRow {
    data class Description(val title: String, val desc: String?) : ReferenceRow()
    data class SectionTitle(
        val segmentKey: String,
        val ref: QuickReferenceVerses.Range,
        val titleText: String,
    ) : ReferenceRow()

    data class VerseRow(
        val verseUi: ReaderLayoutItem.VerseUI,
        val quranTextStyle: TextStyle? = null,
    ) : ReferenceRow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(refModel: ReferenceVerseModel) {
    var selectedChapterChip by rememberSaveable { mutableIntStateOf(0) }
    val translationSlugs = remember(refModel) { resolveTranslationSlugs(refModel) }

    ReaderProvider {
        ReferenceScreenContent(
            refModel = refModel,
            translationSlugs = translationSlugs,
            selectedChapterChip = selectedChapterChip,
            onChapterChipChange = { selectedChapterChip = it },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceScreenContent(
    refModel: ReferenceVerseModel,
    translationSlugs: Set<String>,
    selectedChapterChip: Int,
    onChapterChipChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val vm = LocalReaderViewModel.current

    val colors = colorScheme
    val type = typography

    var rows by remember { mutableStateOf<List<ReferenceRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val verseActions = LocalVerseActions.current

    val allBookmarks by vm.userRepository.getBookmarksFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val referenceBookmarkKeys = remember(rows) {
        rows.asSequence().mapNotNull { row ->
            when (row) {
                is ReferenceRow.SectionTitle -> BookmarkKey(
                    row.ref.chapterNo,
                    row.ref.range.first,
                    row.ref.range.last,
                )

                is ReferenceRow.VerseRow -> BookmarkKey(
                    row.verseUi.verse.chapterNo,
                    row.verseUi.verse.verseNo,
                    row.verseUi.verse.verseNo,
                )

                else -> null
            }
        }.toHashSet()
    }

    val chapterNames by produceState<Map<Int, String>?>(null, refModel.chapters) {
        value = vm.repository.getChapterNames(refModel.chapters)
    }

    val bookmarkedKeys = remember(allBookmarks, referenceBookmarkKeys) {
        allBookmarks.asSequence()
            .map { BookmarkKey(it.chapterNo, it.fromVerseNo, it.toVerseNo) }
            .filter { it in referenceBookmarkKeys }
            .toHashSet()
    }

    LaunchedEffect(refModel, selectedChapterChip, translationSlugs) {
        loading = true
        rows = withContext(Dispatchers.IO) {
            val params = TextBuilderParams(
                context = context,
                fontResolver = vm.fontResolver,
                verseActions = verseActions,
                colors = colors,
                type = type,
                arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                script = ReaderPreferences.getQuranScript(),
                arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                slugs = translationSlugs,
            )

            buildReferenceRows(
                context = context,
                refModel = refModel,
                selectedChapterFilter = selectedChapterChip,
                params = params,
                repository = vm.repository,
            )
        }
        loading = false
    }

    val listState = rememberLazyListState()
    val chaptersGroupState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val showCollapsedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
        }
    }

    val showTwoPane = currentWindowAdaptiveInfo().windowSizeClass.isAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )

    val playerVisibilityState = rememberMiniPlayerVisibilityState(
        MiniPlayerVisibility.HIDDEN_BY_DEFAULT
    )
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chromeCollapsedFraction = scrollBehavior.state.collapsedFraction

    val dynamicBottomPadding =
        navBarBottomInset + (if (playerVisibilityState.isVisible) MINI_PLAYER_HEIGHT else 0.dp) * (1f - chromeCollapsedFraction)

    Box {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    modifier = Modifier.shadow(if (showTwoPane) 2.dp else 0.dp),
                    title = {
                        if (showCollapsedTitle) {
                            Text(
                                text = refModel.title,
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    navigationIcon = {
                        val back =
                            LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

                        SimpleTooltip(stringResource(R.string.strLabelBack)) {
                            IconButton(
                                onClick = { back?.onBackPressed() },
                                painter = painterResource(R.drawable.dr_icon_arrow_left),
                                contentDescription = stringResource(R.string.strLabelBack),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surfaceContainer
                    ),
                )
            },
            containerColor = colorScheme.surfaceContainer
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val contentPane: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier = modifier) {
                        if (translationSlugs.isEmpty()) {
                            Text(
                                text = stringResource(R.string.strMsgTranslNoneSelected),
                                color = colorScheme.error,
                                style = typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        val referencePageTextStyles = remember(rows) {
                            buildMap {
                                for (row in rows) {
                                    if (row is ReferenceRow.VerseRow) {
                                        row.quranTextStyle?.let {
                                            put(
                                                row.verseUi.verse.pageNo,
                                                it
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        when {
                            loading -> Loader(fill = true)
                            else -> {
                                TextStyleProvider(referencePageTextStyles) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = dynamicBottomPadding + 64.dp)
                                    ) {
                                        items(
                                            items = rows,
                                            key = { row ->
                                                when (row) {
                                                    is ReferenceRow.Description -> "desc"
                                                    is ReferenceRow.SectionTitle -> row.segmentKey
                                                    is ReferenceRow.VerseRow -> row.verseUi.key
                                                }
                                            },
                                        ) { row ->
                                            when (row) {
                                                is ReferenceRow.Description -> ReferenceDescription(
                                                    row.title,
                                                    row.desc
                                                )

                                                is ReferenceRow.SectionTitle -> ReferenceSectionTitle(
                                                    row = row,
                                                    isBookmarked = bookmarkedKeys.contains(
                                                        BookmarkKey(
                                                            row.ref.chapterNo,
                                                            row.ref.range.first,
                                                            row.ref.range.last,
                                                        ),
                                                    ),
                                                    onOpenInReader = { chapterNo, range ->
                                                        val i =
                                                            ReaderFactory.prepareVerseRangeIntent(
                                                                chapterNo,
                                                                range.first,
                                                                range.last
                                                            )
                                                                .setClass(
                                                                    context,
                                                                    ActivityReader::class.java
                                                                )
                                                                .putExtra(
                                                                    Keys.READER_KEY_TRANSL_SLUGS,
                                                                    translationSlugs.toTypedArray()
                                                                )
                                                                .putExtra(
                                                                    Keys.READER_KEY_SAVE_TRANSL_CHANGES,
                                                                    false
                                                                )

                                                        context.startActivity(i)
                                                    },
                                                )

                                                is ReferenceRow.VerseRow -> ReferenceVerseViewWrapped(
                                                    verseUi = row.verseUi,
                                                    isBookmarked = bookmarkedKeys.contains(
                                                        BookmarkKey(
                                                            row.verseUi.verse.chapterNo,
                                                            row.verseUi.verse.verseNo,
                                                            row.verseUi.verse.verseNo,
                                                        ),
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showTwoPane) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        chapterNames?.let {
                            ReferenceChapterChipsSidebar(
                                selectedChapterChip = selectedChapterChip,
                                chapterNames = it,
                                chapters = refModel.chapters,
                                onChapterChipChange = onChapterChipChange,
                                listState = chaptersGroupState,
                            )
                        }

                        VerticalDivider(color = colorScheme.outlineVariant.alpha(0.6f))

                        contentPane(Modifier.weight(1f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        chapterNames?.let {
                            ReferenceChapterChipsTopBar(
                                selectedChapterChip = selectedChapterChip,
                                chapterNames = it,
                                chapters = refModel.chapters,
                                onChapterChipChange = onChapterChipChange,
                                listState = chaptersGroupState,
                            )
                        }
                        contentPane(Modifier.weight(1f))
                    }
                }
            }
        }

        RecitationPlayerSheet(
            collapsedBottomInset = navBarBottomInset,
            barsCollapsedFraction = chromeCollapsedFraction,
            playerVisibilityState = playerVisibilityState,
        )
    }
}

@Composable
private fun ReferenceChapterChipsTopBar(
    selectedChapterChip: Int,
    chapterNames: Map<Int, String>,
    chapters: List<Int>,
    onChapterChipChange: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .horizontalFadingEdge(listState, color = colorScheme.surface)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainer),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Chip(
                    selected = selectedChapterChip == 0,
                    onClick = { onChapterChipChange(0) },
                    label = { Text(stringResource(R.string.strLabelAllChapters)) },
                )
            }

            items(chapters) {
                Chip(
                    selected = selectedChapterChip == it,
                    onClick = { onChapterChipChange(it) },
                    label = {
                        Text(
                            chapterNames.getOrDefault(it, it.toString()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ReferenceChapterChipsSidebar(
    selectedChapterChip: Int,
    chapterNames: Map<Int, String>,
    chapters: List<Int>,
    onChapterChipChange: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val sidebarWidth = 220.dp

    LazyColumn(
        state = listState,
        modifier = Modifier
            .width(sidebarWidth)
            .background(colorScheme.surfaceContainer),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            ReferenceSidebarItem(
                selected = selectedChapterChip == 0,
                onClick = { onChapterChipChange(0) },
                text = stringResource(R.string.strLabelAllChapters),
            )
        }

        items(chapters) {
            ReferenceSidebarItem(
                selected = selectedChapterChip == it,
                onClick = { onChapterChipChange(it) },
                text = chapterNames.getOrDefault(it, it.toString()),
            )
        }
    }
}

@Composable
private fun ReferenceSidebarItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
) {
    val containerColor = if (selected) colorScheme.primary else Color.Transparent
    val contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.bodyMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun ReferenceDescription(title: String, desc: String?) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.surfaceContainer,
            Color.Transparent,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = typography.titleMedium,
            color = colorScheme.primary,
        )

        if (!desc.isNullOrBlank()) {
            Text(
                text = desc,
                color = colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ReferenceSectionTitle(
    row: ReferenceRow.SectionTitle,
    isBookmarked: Boolean,
    onOpenInReader: (Int, IntRange) -> Unit,
) {
    val verseActions = LocalVerseActions.current

    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(colorScheme.surface, shape)
                .border(1.dp, colorScheme.outlineVariant, shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.titleText,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = {
                    verseActions.onBookmarkRequest?.invoke(
                        row.ref.chapterNo, row.ref.range
                    )
                },
                painter = painterResource(
                    if (isBookmarked) R.drawable.ic_bookmark_added
                    else R.drawable.ic_bookmark,
                ),
                contentDescription = stringResource(R.string.strLabelBookmark),
                tint = if (isBookmarked) colorScheme.primary
                else colorScheme.onSurface.alpha(0.7f),
            )

            TextButton(
                onClick = { onOpenInReader(row.ref.chapterNo, row.ref.range) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = shapes.small
            ) {
                Text(
                    text = stringResource(R.string.strLabelOpenInReader),
                    style = typography.labelMedium
                )
            }
        }

        HorizontalDivider(
            color = colorScheme.outlineVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReferenceVerseViewWrapped(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
) {
    Surface {
        VerseView(
            verseUi = verseUi,
            isBookmarked = isBookmarked,
            showDivider = verseUi.showDivider,
        )
    }
}

private fun resolveTranslationSlugs(
    refModel: ReferenceVerseModel
): Set<String> {
    val fromModel = refModel.translSlugs.filter { it.isNotBlank() }.toSet()
    if (fromModel.isNotEmpty()) return fromModel

    val first = ReaderPreferences.getTranslations().firstOrNull().orEmpty()

    if (first.isNotBlank()) return setOf(first)

    return TranslUtils.defaultTranslationSlugs()
}

private suspend fun buildReferenceRows(
    context: android.content.Context,
    refModel: ReferenceVerseModel,
    selectedChapterFilter: Int,
    params: TextBuilderParams,
    repository: QuranRepository,
): List<ReferenceRow> = coroutineScope {
    val out = ArrayList<ReferenceRow>()

    out.add(ReferenceRow.Description(refModel.title, refModel.desc))

    data class Segment(
        val segmentIndex: Int,
        val chapterNo: Int,
        val versesRangeStr: String,
        val ref: QuickReferenceVerses.Range,
        val chapterName: String,
    )

    val segments = ArrayList<Segment>()
    var segmentIndex = 0
    val chapterNoCache = ConcurrentHashMap<Int, String>()

    for (refStr in refModel.verses) {
        val parts = refStr.split(":")
        if (parts.size < 2) continue

        val chapterNo = parts[0].trim().toIntOrNull() ?: continue
        if (selectedChapterFilter != 0 && chapterNo != selectedChapterFilter) continue

        val versesRangeStr = parts[1]
        val ref = parseVerses(chapterNo, versesRangeStr)

        if (ref !is QuickReferenceVerses.Range) continue

        val chapterName = chapterNoCache.getOrPut(
            chapterNo,
            { repository.getChapterName(chapterNo) },
        )

        segments.add(
            Segment(
                segmentIndex = segmentIndex,
                chapterNo = chapterNo,
                versesRangeStr = versesRangeStr,
                ref = ref,
                chapterName = chapterName,
            ),
        )

        segmentIndex++
    }

    val built = segments.map { seg ->
        async {
            val verseNos = seg.ref.range.toList()
            val prepared = ReaderItemsBuilder.buildQuickReferenceItems(
                context,
                params,
                repository,
                seg.chapterNo,
                verseNos,
            ) ?: return@async Triple(seg, emptyList<ReaderLayoutItem.VerseUI>(), emptyMap())
            val verseUis = prepared.items.filterIsInstance<ReaderLayoutItem.VerseUI>()
            Triple(seg, verseUis, prepared.textStyles)
        }
    }.awaitAll()

    val locale = readAppLocale(context).platformLocale

    for ((seg, verseUis, textStyles) in built) {
        val titleText = if (seg.ref.range.isSingleValue) {
            String.format(
                locale,
                $$"%1$s %2$d:%3$d",
                seg.chapterName,
                seg.chapterNo,
                seg.ref.range.first
            )
        } else {
            String.format(
                locale,
                $$"%1$s %2$d:%3$d-%4$d",
                seg.chapterName,
                seg.chapterNo,
                seg.ref.range.first,
                seg.ref.range.last
            )
        }

        out.add(
            ReferenceRow.SectionTitle(
                segmentKey = "st-${seg.segmentIndex}-${seg.chapterNo}-${seg.versesRangeStr}",
                ref = seg.ref,
                titleText = titleText,
            ),
        )

        for ((i, v) in verseUis.withIndex()) {
            out.add(
                ReferenceRow.VerseRow(
                    verseUi = v.copy(
                        key = "ref-${seg.segmentIndex}-${v.key}",
                        showDivider = i != verseUis.lastIndex,
                    ),
                    quranTextStyle = textStyles[v.verse.pageNo],
                ),
            )
        }
    }

    out
}
