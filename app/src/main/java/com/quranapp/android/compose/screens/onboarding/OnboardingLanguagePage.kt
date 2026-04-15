package com.quranapp.android.compose.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.quranapp.android.compose.utils.setAppLocale
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import verticalFadingEdge

@Composable
fun OnboardingLanguagePage() {
    val context = LocalContext.current
    val values = context.getStringArray(R.array.availableLocalesValues)
    val names = context.getStringArray(R.array.availableLocalesNames)
    var selectedLocale by remember(context) { mutableStateOf(SPAppConfigs.getLocale(context)) }
    val listState = rememberLazyListState()

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

                RadioItem(
                    titleStr = names[index].orEmpty(),
                    selected = v == selectedLocale,
                    onClick = {
                        if (v != selectedLocale) {
                            selectedLocale = v
                            setAppLocale(context, v)
                        }
                    },
                )
            }
        }
    }
}
