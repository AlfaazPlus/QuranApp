package com.quranapp.android.compose.components.reader.navigator

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.components.JuzIcon
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.dialogs.AutoScrollSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.getQuranMushafId
import com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import kotlinx.coroutines.launch

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

    Surface(
        shadowElevation = 4.dp,
    ) {
        Column() {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
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
                            openReaderSetting(context, -1)
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
                color = colorScheme.outlineVariant.alpha(0.5f)
            )

            when (readerMode) {
                ReaderMode.Reading -> {
                    StickyHeaderModeMushaf(readerVm, uiState) {
                        showNavigatorSheet = true
                    }
                }

                ReaderMode.Translation -> {}

                else -> {
                    StickyHeaderModeVbV(readerVm, uiState) {
                        showNavigatorSheet = true
                    }
                }
            }
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
                        ActivitySettings.SETTINGS_TRANSLATION
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
    uiState: ReaderUiState,
    onNavigatorRequest: () -> Unit
) {
    val currentPageNo = uiState.currentPageNo
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
        val mushafId = scriptCode.getQuranMushafId(scriptVariant)
        if (mushafId <= 0) {
            value = ""
            return@produceState
        }
        value = readerVm.repository.getChapterNamesOnMushafPage(
            mushafId,
            pageNo,
        )
    }

    Row(
        modifier = Modifier
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            chaptersOfPage,
            style = typography.labelMedium,
            modifier = Modifier.widthIn(max = 120.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )


        Spacer(Modifier.weight(1f))

        TextButton(
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface
            ),
            onClick = onNavigatorRequest,
        ) {
            if (currentPageNo != null) {
                Text(
                    stringResource(R.string.strLabelPageNo, currentPageNo),
                    style = typography.titleMedium,
                    color = colorScheme.primary,
                )
            }
            Icon(
                painterResource(R.drawable.dr_icon_chevron_down),
                contentDescription = null
            )
        }
    }
}

fun openReaderSetting(context: Context, destination: Int) {
    context.startActivity(
        Intent(context, ActivitySettings::class.java).apply {
            putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, destination)
            putExtra(READER_KEY_SETTING_IS_FROM_READER, true)
        },
        null
    )
}