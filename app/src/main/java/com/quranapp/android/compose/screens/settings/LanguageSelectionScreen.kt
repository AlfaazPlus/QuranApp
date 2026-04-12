package com.quranapp.android.compose.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.utils.normalizedLanguageTag
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs

@Composable
fun LanguageSelectionScreen() {
    val context = LocalContext.current
    val availableLocalesValues = context.getStringArray(R.array.availableLocalesValues)
    val availableLocaleNames = context.getStringArray(R.array.availableLocalesNames)

    val initialLocale = remember { SPAppConfigs.getLocale(context) }
    var selectedLocale by remember { mutableStateOf(initialLocale) }

    fun save(locale: String) {
        SPAppConfigs.setLocale(context, locale)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(locale.normalizedLanguageTag())
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                stringResource(R.string.strTitleAppLanguage),
                actions = {
                    IconButton(
                        painterResource(R.drawable.dr_icon_check),
                        enabled = selectedLocale != initialLocale
                    ) {

                        save(selectedLocale)
                    }
                }
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 96.dp
            ),
        ) {
            itemsIndexed(
                items = availableLocalesValues,
                key = { _, value -> value!! }
            ) { index, localeValue ->
                LanguageItem(
                    languageName = availableLocaleNames[index]!!,
                    localeValue = localeValue!!,
                    isSelected = localeValue == selectedLocale,
                    onSelect = {
                        selectedLocale = localeValue
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    languageName: String,
    localeValue: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onSelect
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = languageName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
