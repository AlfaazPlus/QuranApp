package com.quranapp.android.compose.screens.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.viewModels.TafsirEvent
import com.quranapp.android.viewModels.TafsirViewModel
import verticalFadingEdge

@Composable
fun OnboardingTafsirPage(modifier: Modifier = Modifier) {
    val viewModel = viewModel<TafsirViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            ErrorMessageCard(
                error = uiState.error!!,
                onRetry = { viewModel.onEvent(TafsirEvent.Refresh) },
                modifier = modifier.fillMaxSize(),
            )
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
                    items(uiState.tafsirGroups, key = { it.langCode }) {
                        Column() {
                            Text(
                                text = it.langName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(8.dp),
                            )

                            it.tafsirs.forEach { tafsir ->
                                OnboardingTafsirRow(
                                    tafsir = tafsir,
                                    isSelected = tafsir.key == uiState.selectedTafsirKey,
                                    onSelect = { viewModel.onEvent(TafsirEvent.SelectTafsir(tafsir.key)) },
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp,
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
private fun OnboardingTafsirRow(
    tafsir: TafsirInfoModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSelect,
            )
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            enabled = true,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorScheme.primary,
                unselectedColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = tafsir.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tafsir.author,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
