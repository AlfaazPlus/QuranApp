package com.quranapp.android.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.Chip
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.learnMore
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strTitleAppLanguage
import com.quranapp.android.generated.resources.translationHelp
import com.quranapp.android.locale.LOCALE_DEFAULT
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.NumeralSystem
import com.quranapp.android.locale.appLanguageOptions
import com.quranapp.android.locale.appLocaleForLanguageChange
import com.quranapp.android.locale.languageCodeOf
import com.quranapp.android.locale.languageDisplayName
import com.quranapp.android.locale.numeralSystemsForLanguage
import com.quranapp.android.locale.setAppLocale
import com.quranapp.android.platform.Platform
import com.quranapp.android.theme.alpha
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

data class LanguageModel(
    val rawLanguageTag: String,
    val language: String,
    val localizedName: String,
    val nativeName: String,
)

@Composable
fun LanguageSelectionScreen() {
    val scope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)

    val languages = remember(appLocale.languageTag, systemDefaultName) {
        appLanguageOptions.map { option ->
            val rawLanguageTag = option.tag
            val nativeName = if (option.systemDefault) systemDefaultName else option.nativeName
            val language = if (rawLanguageTag == LOCALE_DEFAULT) {
                ""
            } else {
                languageCodeOf(rawLanguageTag)
            }
            val localizedName = if (rawLanguageTag == LOCALE_DEFAULT) {
                nativeName
            } else {
                languageDisplayName(rawLanguageTag, appLocale.languageTag)
            }

            LanguageModel(
                rawLanguageTag,
                language,
                localizedName,
                nativeName
            )
        }
    }

    var committed by remember(appLocale.storedTag, appLocale.numeralSystem) {
        mutableStateOf(appLocale.storedTag to appLocale.numeralSystem)
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages by remember(searchQuery, languages) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                languages
            } else {
                val query = searchQuery.trim().lowercase()
                languages.filter {
                    it.localizedName.lowercase().contains(query) ||
                            it.nativeName.lowercase().contains(query) ||
                            it.rawLanguageTag.lowercase().contains(query)
                }
            }
        }
    }

    fun save(selectedTag: String, selectedNumeral: NumeralSystem?) {
        val applied = appLocaleForLanguageChange(selectedTag, selectedNumeral)
        committed = applied.storedTag to applied.numeralSystem
        scope.launch { setAppLocale(applied) }
    }

    val selectedTag = committed.first
    val selectedNumeral = committed.second

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        topBar = {
            AppBar(
                stringResource(Res.string.strTitleAppLanguage),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentPadding = PaddingValues(
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                AlertCard(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.translationHelp),
                            style = typography.bodyMedium
                        )

                        Text(
                            stringResource(Res.string.learnMore),
                            modifier = Modifier.clickable {
                                Platform.links.browse(ApiConfig.GITHUB_REPOSITORY_URL)
                            },
                            style = typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorScheme.primary
                        )
                    }
                }
            }

            if (filteredLanguages.isNotEmpty()) {
                items(filteredLanguages, key = { it.rawLanguageTag }) { model ->
                    val isSelected = model.rawLanguageTag == selectedTag
                    val numeralItems = numeralSystemsForLanguage(model.language)

                    Column(Modifier.fillMaxWidth()) {
                        LanguageItem(
                            language = model,
                            isSelected = isSelected,
                            onSelect = {
                                save(model.rawLanguageTag, numeralItems.firstOrNull()?.first)
                            },
                        )

                        if (isSelected && numeralItems.isNotEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 60.dp, end = 24.dp, bottom = 12.dp),
                            ) {
                                NumeralSystemChipRow(
                                    numeralItems = numeralItems,
                                    selected = selectedNumeral,
                                    onSelect = {
                                        save(selectedTag, it)
                                    },
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
private fun LanguageItem(
    language: LanguageModel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSelect
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorScheme.primary,
                unselectedColor = colorScheme.onSurfaceVariant.alpha(0.4f)
            ),
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = language.localizedName,
                style = typography.labelLarge,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface
            )

            if (language.nativeName != language.localizedName) {
                Text(
                    text = language.nativeName,
                    style = typography.bodyMedium,
                    color = (if (isSelected) colorScheme.primary else colorScheme.onSurface)
                        .alpha(0.7f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun NumeralSystemChipRow(
    numeralItems: List<Pair<NumeralSystem, String>>,
    selected: NumeralSystem?,
    onSelect: (NumeralSystem) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        numeralItems.forEach { (system, name) ->
            Chip(
                selected = selected == system,
                label = { Text(name) },
                onClick = { onSelect(system) },
            )
        }
    }
}
