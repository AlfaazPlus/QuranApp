package com.quranapp.android.compose.screens.onboarding

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.components.dialogs.BottomSheetBare
import com.quranapp.android.compose.components.settings.TranslationDownloadList
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.TranslationDownloadEvent
import com.quranapp.android.viewModels.TranslationDownloadUiEvent
import com.quranapp.android.viewModels.TranslationDownloadViewModel
import com.quranapp.android.viewModels.TranslationEvent
import com.quranapp.android.viewModels.TranslationViewModel
import verticalFadingEdge

@Composable
fun OnboardingTranslationsPage(modifier: Modifier = Modifier) {
    val viewModel = viewModel<TranslationViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(TranslationEvent.Refresh)
    }

    when {
        uiState.isLoading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        }

        uiState.error != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.strMsgSomethingWrong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.onEvent(TranslationEvent.Refresh) }) {
                    Text(stringResource(R.string.strLabelRetry))
                }
            }
        }

        else -> {
            var showDownloadSheet by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()

            Box(modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = { showDownloadSheet = true },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_download),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.primary,
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(stringResource(R.string.strTitleDownloadTranslations))
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .verticalFadingEdge(listState)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 4.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (group in uiState.translationGroups) {
                                item(key = "h_${group.langCode}") {
                                    Text(
                                        text = group.langName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 8.dp
                                        ),
                                    )
                                }
                                items(
                                    items = group.translations,
                                    key = { it.bookInfo.slug },
                                ) { transl ->
                                    OnboardingTranslationRow(
                                        translation = transl,
                                        isSelected = uiState.selectedSlugs.contains(transl.bookInfo.slug),
                                        onToggle = { checked ->
                                            viewModel.onEvent(
                                                TranslationEvent.SelectionChanged(transl, checked),
                                            )
                                        },
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                    )
                                }
                            }
                        }
                    }
                }

                OnboardingTranslationDownloadSheet(
                    isOpen = showDownloadSheet,
                    onDismiss = {
                        showDownloadSheet = false
                        viewModel.onEvent(TranslationEvent.RefreshQuiet)
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingTranslationDownloadSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    BottomSheetBare(
        isOpen = isOpen,
        onDismiss = onDismiss,
        header = null,
    ) {
        val downloadVm = viewModel<TranslationDownloadViewModel>()
        val downloadUi by downloadVm.uiState.collectAsState()
        val context = LocalContext.current
        val resources = LocalResources.current

        LaunchedEffect(Unit) {
            downloadVm.events.collect { event ->
                when (event) {
                    is TranslationDownloadUiEvent.ShowMessage -> {
                        MessageUtils.popMessage(
                            context,
                            title = event.title,
                            msg = event.message,
                            resources.getString(R.string.strLabelClose),
                            null,
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.strTitleDownloadTranslations),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    painterResource(R.drawable.dr_icon_refresh),
                ) {
                    downloadVm.onEvent(TranslationDownloadEvent.Refresh)
                }
            }

            SearchTextField(
                value = downloadUi.searchQuery,
                onValueChange = { downloadVm.onEvent(TranslationDownloadEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(R.string.strHintSearch),
                singleLine = true,
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                when {
                    downloadUi.isLoading -> Loader(true)
                    downloadUi.error != null -> ErrorMessageCard(
                        error = downloadUi.error!!,
                        onRetry = { downloadVm.onEvent(TranslationDownloadEvent.Refresh) },
                    )

                    else -> TranslationDownloadList(
                        groups = downloadUi.groups,
                        searchQuery = downloadUi.searchQuery,
                        downloadStates = downloadUi.downloadStates,
                        onEvent = downloadVm::onEvent,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 32.dp
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTranslationRow(
    translation: TranslModel,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val bookInfo = translation.bookInfo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colorScheme.primary),
                onClick = { onToggle(!isSelected) },
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant,
            ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                text = bookInfo.bookName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bookInfo.authorName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Light,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
