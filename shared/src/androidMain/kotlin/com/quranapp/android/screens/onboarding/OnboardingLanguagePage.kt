package com.quranapp.android.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.extensions.verticalFadingEdge
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.locale.LOCALE_DEFAULT
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.NumeralSystem
import com.quranapp.android.locale.appLanguageOptions
import com.quranapp.android.locale.appLocaleForLanguageChange
import com.quranapp.android.locale.languageCodeOf
import com.quranapp.android.locale.numeralSystemsForLanguage
import com.quranapp.android.locale.setAppLocale
import com.quranapp.android.screens.settings.NumeralSystemChipRow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingLanguagePage() {
    val scope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current
    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)
    var committed by remember(appLocale.storedTag, appLocale.numeralSystem) {
        mutableStateOf(appLocale.storedTag to appLocale.numeralSystem)
    }
    val listState = rememberLazyListState()

    fun save(selectedTag: String, selectedNumeral: NumeralSystem?) {
        val applied = appLocaleForLanguageChange(selectedTag, selectedNumeral)
        committed = applied.storedTag to applied.numeralSystem
        scope.launch { setAppLocale(applied) }
    }

    val selectedTag = committed.first
    val selectedNumeral = committed.second

    Box(
        Modifier.verticalFadingEdge(listState)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
        ) {
            itemsIndexed(appLanguageOptions) { _, option ->
                val v = option.tag
                val title = if (option.systemDefault) systemDefaultName else option.nativeName
                val isSelected = v == selectedTag
                val languageCode = if (v == LOCALE_DEFAULT) {
                    ""
                } else {
                    languageCodeOf(v)
                }
                val numeralItems = numeralSystemsForLanguage(languageCode)

                Column(Modifier.fillMaxWidth()) {
                    RadioItem(
                        titleStr = title,
                        selected = isSelected,
                        onClick = {
                            if (v != selectedTag) {
                                save(v, numeralItems.firstOrNull()?.first)
                            }
                        },
                    )

                    if (isSelected && numeralItems.isNotEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        ) {
                            NumeralSystemChipRow(
                                numeralItems = numeralItems,
                                selected = selectedNumeral,
                                onSelect = { save(selectedTag, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
