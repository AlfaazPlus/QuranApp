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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.dialogs.AutoScrollSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.utils.univ.Keys.READER_KEY_READ_TYPE
import com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES
import com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER
import com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    readerVm: ReaderViewModel,
    isWideScreen: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigatorRequest: () -> Unit,
) {
    val context = LocalContext.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    var autoScrollSheetOpen by rememberSaveable { mutableStateOf(false) }

    val currentJuzNo = uiState.currentJuzNo
    val currentChapterNo = uiState.currentChapterNo

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(
                                enabled = !isWideScreen,
                                onClick = onNavigatorRequest,
                            )
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    ) {
                        if (currentJuzNo != null) {
                            Text(
                                text = stringResource(
                                    R.string.strLabelJuzNo,
                                    currentJuzNo,
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        } else if (currentChapterNo != null) {
                            ChapterIcon(
                                modifier = Modifier.padding(top = 8.dp),
                                chapterNo = currentChapterNo,
                                fontSize = 42.sp,
                                color = colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        SimpleTooltip(text = stringResource(R.string.autoScroll)) {
                            IconButton(
                                onClick = {
                                    autoScrollSheetOpen = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_scroll_down),
                                    contentDescription = stringResource(R.string.autoScroll),
                                )
                            }
                        }
                    }
                },
                actions = {
                    SimpleTooltip(text = stringResource(R.string.strLabelSelectTranslations)) {
                        IconButton(
                            onClick = {
                                openReaderSetting(
                                    context,
                                    uiState,
                                    ActivitySettings.SETTINGS_TRANSLATION
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_translations),
                                contentDescription = stringResource(R.string.strLabelSelectTranslations),
                            )
                        }
                    }
                    SimpleTooltip(text = stringResource(R.string.strTitleSettings)) {
                        IconButton(onClick = {
                            openReaderSetting(context, uiState, -1)
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

            StickyHeader(readerVm, uiState, onNavigatorRequest)
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
private fun StickyHeader(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    onNavigatorRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quranMeta = QuranMeta2.remember()
    val chapterName = remember(context, uiState.currentChapterNo) {
        uiState.currentChapterNo?.let { quranMeta?.getChapterName(context, it) } ?: ""
    }

    val readerMode by readerVm.readerMode.collectAsState()

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface
            ),
            onClick = onNavigatorRequest,
        ) {
            Text(chapterName)
            Icon(
                painterResource(R.drawable.dr_icon_chevron_down),
                contentDescription = null
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(colorScheme.background)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ReaderMode.entries.forEach { mode ->
                val isSelected = mode == readerMode

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
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
                        contentDescription = stringResource(
                            when (mode) {
                                ReaderMode.VerseByVerse -> R.string.modeVerseByVerse
                                ReaderMode.Reading -> R.string.modeMushaf
                                ReaderMode.Translation -> R.string.labelTranslation
                            },
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

fun openReaderSetting(context: Context, state: ReaderUiState, destination: Int) {
    context.startActivity(
        Intent(context, ActivitySettings::class.java).apply {
            putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, destination)
            putExtra(READER_KEY_SETTING_IS_FROM_READER, true)
            putExtra(READER_KEY_SAVE_TRANSL_CHANGES, state.transientTranslationSlugs == null)

            if (state.transientTranslationSlugs != null) {
                putExtra(
                    READER_KEY_TRANSL_SLUGS,
                    state.transientTranslationSlugs.toTypedArray()
                )
            }

            if (state.transientReaderMode != null) {
                putExtra(READER_KEY_READ_TYPE, state.transientReaderMode)
            }
        },
        null
    )
}