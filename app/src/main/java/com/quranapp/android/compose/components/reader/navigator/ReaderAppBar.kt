package com.quranapp.android.compose.components.reader.navigator

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.viewModels.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppBar(
    readerVm: ReaderViewModel,
    isWideScreen: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onReaderTitleClick: () -> Unit,
) {
    val context = LocalContext.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()

    val currentJuzNo = uiState.currentJuzNo
    val currentChapterNo = uiState.currentChapterNo

    Surface(
        shadowElevation = 4.dp,
    ) {
        CenterAlignedTopAppBar(
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
                            onClick = onReaderTitleClick,
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
                            chapterNo = currentChapterNo,
                            fontSize = 36.sp,
                            color = colorScheme.primary
                        )
                    }

                    if (!isWideScreen) {
                        Row {
                            Text(
                                modifier = Modifier.widthIn(max = 150.dp),
                                text = "navigatorSubtitle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 0.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_arrow_drop_down),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
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
                        context.startActivity(Intent(context, ActivitySettings::class.java))
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_settings),
                            contentDescription = stringResource(R.string.strTitleSettings),
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}
