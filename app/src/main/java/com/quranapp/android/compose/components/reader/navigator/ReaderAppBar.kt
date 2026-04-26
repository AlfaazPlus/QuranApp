package com.quranapp.android.compose.components.reader.navigator

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.components.JuzIcon
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.dialogs.AutoScrollSheet
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.toQuranMushafId
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import kotlinx.coroutines.launch

private val ReaderAppBarHeight = 86.dp
private val ReaderHeaderHeight = 52.dp
private val ReaderDividerHeight = 1.dp
internal val ReaderAppBarExpandedHeight =
    ReaderAppBarHeight + ReaderHeaderHeight + (ReaderDividerHeight * 2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    readerVm: ReaderViewModel,
    isWideScreen: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()
    var showNavigatorSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(true)

    val density = LocalDensity.current
    val heightOffset = scrollBehavior.state.heightOffset
    val visibleHeight = with(density) {
        (ReaderAppBarExpandedHeight.toPx() + heightOffset).coerceAtLeast(0f).toDp()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(visibleHeight),
        shadowElevation = 4.dp,
        color = colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .requiredHeight(ReaderAppBarExpandedHeight)
        ) {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(ReaderAppBarHeight),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surfaceContainer,
                    scrolledContainerColor = colorScheme.surfaceContainer,
                ),
                title = {
                    ModeTabs(readerVm, readerMode)
                },
                navigationIcon = {
                    SimpleTooltip(text = stringResource(R.string.strDescGoBack)) {
                        IconButton(
                            onClick = {
                                backPressedDispatcher?.onBackPressed()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_chevron_left),
                                contentDescription = stringResource(R.string.strDescGoBack),
                            )
                        }
                    }
                },
                actions = {
                    SimpleTooltip(text = stringResource(R.string.strTitleSettings)) {
                        IconButton(onClick = {
                            openReaderSetting(context, null)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_settings),
                                contentDescription = stringResource(R.string.strTitleSettings),
                            )
                        }
                    }
                },
            )

            HorizontalDivider(
                thickness = ReaderDividerHeight,
                color = colorScheme.outlineVariant.alpha(0.5f)
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ReaderHeaderHeight),
                contentAlignment = Alignment.Center
            ) {
                when (readerMode) {
                    ReaderMode.Reading -> {
                        StickyHeaderModeMushaf(readerVm) {
                            showNavigatorSheet = true
                        }
                    }

                    ReaderMode.Translation -> {
                        StickyHeaderModeTranslation(readerVm) {
                            showNavigatorSheet = true
                        }
                    }

                    else -> {
                        StickyHeaderModeVbV(readerVm, uiState) {
                            showNavigatorSheet = true
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = ReaderDividerHeight,
                color = colorScheme.outlineVariant.alpha(0.5f)
            )
        }
    }

    if (showNavigatorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNavigatorSheet = false },
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.background,
            contentColor = colorScheme.onSurface,
            dragHandle = null,
            sheetGesturesEnabled = false
        ) {
            ReaderNavigator(
                readerVm = readerVm,
            ) { showNavigatorSheet = false }
        }
    }
}

@Composable
private fun ModeTabs(
    readerVm: ReaderViewModel,
    readerMode: ReaderMode?
) {
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colorScheme.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            ReaderMode.VerseByVerse,
            ReaderMode.Reading,
            ReaderMode.Translation,
        ).forEach { mode ->
            val isSelected = mode == readerMode
            val label = stringResource(
                when (mode) {
                    ReaderMode.VerseByVerse -> R.string.modeVerseByVerse
                    ReaderMode.Reading -> R.string.modeMushaf
                    ReaderMode.Translation -> R.string.labelTranslation
                },
            )

            SimpleTooltip(label) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) colorScheme.surface else Color.Transparent,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                scope.launch {
                                    ReaderPreferences.setReaderMode(mode)
                                }
                            },
                        )
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(
                            when (mode) {
                                ReaderMode.VerseByVerse -> R.drawable.ic_mode_verse
                                ReaderMode.Reading -> R.drawable.ic_mode_mushaf
                                ReaderMode.Translation -> R.drawable.ic_mode_translation
                            },
                        ),
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) colorScheme.onSurface else colorScheme.onSurface.alpha(
                            0.6f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyHeaderModeVbV(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    onNavigatorRequest: () -> Unit
) {
    val context = LocalContext.current

    var autoScrollSheetOpen by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        SimpleTooltip(text = stringResource(R.string.autoScroll)) {
            IconButton(
                onClick = {
                    autoScrollSheetOpen = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_scroll_down),
                    contentDescription = stringResource(R.string.autoScroll),
                    tint = colorScheme.onSurface.alpha(0.75f)
                )
            }
        }

        SimpleTooltip(text = stringResource(R.string.strLabelSelectTranslations)) {
            IconButton(
                onClick = {
                    openReaderSetting(
                        context,
                        SettingRoutes.TRANSLATIONS
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_translations),
                    contentDescription = stringResource(R.string.strLabelSelectTranslations),
                    tint = colorScheme.onSurface.alpha(0.75f)
                )
            }
        }

        Spacer(
            Modifier.weight(1f)
        )

        TextButton(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.primary
            ),
            onClick = onNavigatorRequest,
        ) {
            Icon(
                painterResource(R.drawable.dr_icon_chevron_down),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
            )

            when (val vt = uiState.viewType) {
                is ReaderViewType.Juz -> JuzIcon(
                    juzNo = vt.juzNo,
                    fontSize = 22.sp,
                    color = colorScheme.primary
                )

                is ReaderViewType.Hizb -> Text(
                    text = stringResource(R.string.labelHizbNo, vt.hizbNo),
                    style = typography.titleMedium,
                    color = colorScheme.primary,
                )

                is ReaderViewType.Chapter -> ChapterIcon(
                    modifier = Modifier.padding(top = 8.dp),
                    chapterNo = vt.chapterNo,
                    fontSize = 32.sp,
                    color = colorScheme.primary
                )

                null -> {}
            }
        }
    }

    AutoScrollSheet(
        readerVm,
        autoScrollSheetOpen,
    ) {
        autoScrollSheetOpen = false
    }
}

@Composable
private fun StickyHeaderModeMushaf(
    readerVm: ReaderViewModel,
    onNavigatorRequest: () -> Unit
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val resources = LocalResources.current
    val currentPageNo = mushafSession.currentPageNo
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()

    val chaptersOfPage by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }
        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }
        value = readerVm.repository.getChapterNamesOnMushafPage(
            mushafId,
            pageNo,
        )
    }

    val pageDivisionLabel by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo

        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)

        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        val juzNo = readerVm.repository.getJuzForMushafPages(mushafId, listOf(pageNo))[pageNo]
            ?: 0
        val hizbNos = readerVm.repository.getHizbForMushafPages(mushafId, listOf(pageNo))[pageNo]
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

        value = buildString {
            if (juzNo > 0) {
                append(resources.getString(R.string.strLabelJuzNo, juzNo))
            }

            if (hizbNos.isNotEmpty()) {
                if (isNotEmpty()) append(" \u2022 ")
                append(resources.getString(R.string.strTitleReaderHizb))
                append(" ")
                append(hizbNos.joinToString(" / "))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            chaptersOfPage,
            style = typography.labelMedium.copy(
                lineHeightStyle = LineHeightStyle.Default.copy(
                    mode = LineHeightStyle.Mode.Tight,
                    alignment = LineHeightStyle.Alignment.Center,
                )
            ),
            modifier = Modifier.weight(0.4f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface.alpha(0.85f)
        )


        Box(
            Modifier.weight(0.6f),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = colorScheme.onSurface
                ),
                onClick = onNavigatorRequest,
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    if (currentPageNo != null) {
                        Text(
                            stringResource(R.string.strLabelPageNo, currentPageNo),
                            style = typography.titleSmall.copy(
                                lineHeightStyle = LineHeightStyle.Default.copy(
                                    mode = LineHeightStyle.Mode.Tight,
                                    alignment = LineHeightStyle.Alignment.Center,
                                )
                            ),
                            color = colorScheme.primary,
                        )
                    }

                    if (pageDivisionLabel.isNotBlank()) {
                        Text(
                            text = pageDivisionLabel,
                            style = typography.labelSmall.copy(
                                lineHeightStyle = LineHeightStyle.Default.copy(
                                    mode = LineHeightStyle.Mode.Tight,
                                    alignment = LineHeightStyle.Alignment.Center,
                                )
                            ),
                            color = colorScheme.onSurface.alpha(0.85f),
                        )
                    }
                }

                Icon(
                    painterResource(R.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StickyHeaderModeTranslation(
    readerVm: ReaderViewModel,
    onNavigatorRequest: () -> Unit
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val context = LocalContext.current
    val currentPageNo = mushafSession.currentPageNo
    val translationSlug = ReaderPreferences.observePrimaryTranslationSlug()
    val bookName by produceState("", translationSlug) {
        value = QuranTranslationFactory(context).use {
            it.getTranslationBookInfo(translationSlug).displayName
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .background(colorScheme.background, shapes.extraLarge)
                .clip(shapes.extraLarge)
                .clickable(
                    onClick = {
                        openReaderSetting(
                            context,
                            SettingRoutes.TRANSLATIONS
                        )
                    }
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_translations),
                contentDescription = stringResource(R.string.strLabelSelectTranslations),
                tint = colorScheme.onSurface.alpha(0.75f),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp)
            )

            Text(
                bookName,
                style = typography.labelMedium,
                color = colorScheme.onSurface.alpha(0.75f),
                maxLines = 1,
                modifier = Modifier
                    .basicMarquee(
                        initialDelayMillis = 900,
                        repeatDelayMillis = 1_200,
                    )
            )

            Icon(
                painterResource(R.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onSurface.alpha(0.75f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(
            Modifier.weight(1f)
        )

        TextButton(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface
            ),
            onClick = onNavigatorRequest,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                if (currentPageNo != null) {
                    Text(
                        stringResource(R.string.strLabelPageNo, currentPageNo),
                        style = typography.titleSmall,
                        color = colorScheme.primary,
                    )
                }
            }

            Icon(
                painterResource(R.drawable.dr_icon_chevron_down),
                contentDescription = null,
                tint = colorScheme.primary,
            )
        }
    }
}

@Composable
fun FullscreenMushafHeader(
    modifier: Modifier = Modifier,
    readerVm: ReaderViewModel,
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val resources = LocalResources.current
    val currentPageNo = mushafSession.currentPageNo
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()

    val chapterName by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        value = readerVm.repository.getChapterNamesOnMushafPage(mushafId, pageNo)
    }

    val juzHizb by produceState(
        initialValue = "",
        currentPageNo,
        scriptCode,
        scriptVariant,
    ) {
        val pageNo = currentPageNo
        if (pageNo == null || pageNo <= 0) {
            value = ""
            return@produceState
        }

        val mushafId = scriptCode.toQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }

        val juzNo = readerVm.repository.getJuzForMushafPages(mushafId, listOf(pageNo))[pageNo] ?: 0
        val hizbNos = readerVm.repository.getHizbForMushafPages(mushafId, listOf(pageNo))[pageNo]
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

        value = buildString {
            if (juzNo > 0) {
                append(resources.getString(R.string.strLabelJuzNo, juzNo))
            }

            if (hizbNos.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(resources.getString(R.string.strTitleReaderHizb))
                append(" ")
                append(hizbNos.joinToString(" / "))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chapterName,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    initialDelayMillis = 900,
                    repeatDelayMillis = 1_200,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
        )

        Text(
            text = currentPageNo?.let { stringResource(R.string.strLabelPageNo, it) }.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = juzHizb,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    initialDelayMillis = 900,
                    repeatDelayMillis = 1_200,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

fun openReaderSetting(context: Context, destination: String?) {
    context.startActivity(
        Intent(context, ActivitySettings::class.java).apply {
            if (destination != null) {
                putExtra(Keys.NAV_DESTINATION, destination)
            }
            putExtra(Keys.SHOW_READER_SETTINGS_ONLY, true)
        },
        null
    )
}