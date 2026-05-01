package com.quranapp.android.compose.screens.onboarding

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.screens.settings.NumeralSystemChipRow
import com.quranapp.android.compose.utils.NumeralSystem
import com.quranapp.android.compose.utils.appLocaleForLanguageChange
import com.quranapp.android.compose.utils.normalizedLanguageTag
import com.quranapp.android.compose.utils.numeralSystemsForLanguage
import com.quranapp.android.compose.utils.readAppLocale
import com.quranapp.android.compose.utils.setAppLocale
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import verticalFadingEdge
import java.util.Locale

@Composable
fun OnboardingLanguagePage() {
    val context = LocalContext.current
    val values = context.getStringArray(R.array.availableLocalesValues)
    val names = context.getStringArray(R.array.availableLocalesNames)
    var committed by remember(context) {
        mutableStateOf(
            SPAppConfigs.getLocale(context) to readAppLocale(context).numeralSystem
        )
    }
    val listState = rememberLazyListState()

    fun save(selectedTag: String, selectedNumeral: NumeralSystem?) {
        val applied = appLocaleForLanguageChange(context, selectedTag, selectedNumeral)
        setAppLocale(context, applied)
        committed = applied.languageTag to applied.numeralSystem
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
            itemsIndexed(values) { index, value ->
                val v = value.orEmpty()
                val isSelected = v == selectedTag
                val languageCode = if (v == SPAppConfigs.LOCALE_DEFAULT) {
                    ""
                } else {
                    Locale.forLanguageTag(v.normalizedLanguageTag()).language
                }
                val numeralItems = numeralSystemsForLanguage(languageCode)

                Column(Modifier.fillMaxWidth()) {
                    RadioItem(
                        titleStr = names[index].orEmpty(),
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
