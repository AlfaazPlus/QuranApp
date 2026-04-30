package com.quranapp.android.compose.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.search.SearchFilters
import com.quranapp.android.search.TranslationOption

@Composable
fun SearchFiltersSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    filters: SearchFilters,
    availableTranslations: List<TranslationOption>,
    quranTextEnabled: Boolean,
    onApplyFilters: (SearchFilters) -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.dr_icon_filter,
        title = stringResource(R.string.strTitleFilters),
        headerArrangement = Arrangement.Start,
    ) {
        var draft by remember(filters, isOpen) { mutableStateOf(filters) }

        Column {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TranslationsSection(
                    available = availableTranslations,
                    draft = draft,
                    quranTextEnabled = quranTextEnabled,
                    onChange = { draft = it },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { draft = SearchFilters() }) {
                    Text(stringResource(R.string.reset))
                }

                Button(
                    enabled = draft.selectedSlugs == null || draft.selectedSlugs!!.isNotEmpty(),
                    onClick = { onApplyFilters(draft) }
                ) {
                    Text(stringResource(R.string.strLabelApply))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun TranslationsSection(
    available: List<TranslationOption>,
    draft: SearchFilters,
    quranTextEnabled: Boolean,
    onChange: (SearchFilters) -> Unit,
) {
    val effectiveSelected = remember(draft.selectedSlugs, available) {
        draft.selectedSlugs ?: available.map { it.slug }.toSet()
    }
    val allSelected = effectiveSelected.size == available.size
    val sectionAlpha = if (quranTextEnabled) 0.5f else 1f

    Column(
        modifier = Modifier.alpha(if (quranTextEnabled) 0.5f else 1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                SectionTitle(stringResource(R.string.strLabelSelectTranslations))
            }

            if (available.isNotEmpty() && !quranTextEnabled) {
                TextButton(
                    onClick = {
                        val newSlugs = if (allSelected) emptySet()
                        else available.map { it.slug }.toSet()

                        onChange(draft.copy(selectedSlugs = newSlugs))
                    }
                ) {
                    Text(
                        text = stringResource(
                            if (allSelected) R.string.clear
                            else R.string.selectAll
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        when {
            quranTextEnabled -> {
                Text(
                    text = stringResource(R.string.filterTranslationsDisabledNote),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.alpha(sectionAlpha),
                    modifier = Modifier.padding(16.dp)
                )
            }

            available.isEmpty() -> {
                Text(
                    text = stringResource(R.string.filterTranslationsEmpty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    for (option in available) {
                        val checked = effectiveSelected.contains(option.slug)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = effectiveSelected.toMutableSet().apply {
                                        if (checked) remove(option.slug) else add(option.slug)
                                    }
                                    val finalSelection: Set<String>? = when {
                                        next.isEmpty() -> emptySet()
                                        next.size == available.size -> null
                                        else -> next.toSet()
                                    }
                                    onChange(draft.copy(selectedSlugs = finalSelection))
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colorScheme.primary,
                                    uncheckedColor = colorScheme.onSurfaceVariant,
                                ),
                            )

                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
