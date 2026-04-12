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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.transls.TranslModel
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
            val listState = rememberLazyListState()

            Box(
                Modifier.verticalFadingEdge(listState)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
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
