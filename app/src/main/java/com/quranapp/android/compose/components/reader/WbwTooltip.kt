package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WbwTooltipFetched(val loading: Boolean, val wbw: WbwWordEntity?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WbwTooltip(
    word: AyahWordEntity,
    onDismiss: () -> Unit,
    textStyles: QuranTextStyle,
    onOpenSheet: () -> Unit,
    anchor: @Composable () -> Unit
) {
    val vm = LocalReaderViewModel.current
    val wbwIdRaw = ReaderPreferences.observeWbwId()
    val wbwId = wbwIdRaw.takeIf { it.isNotEmpty() }

    val fetched by produceState(
        initialValue = WbwTooltipFetched(loading = true, wbw = null),
        word.ayahId,
        word.wordIndex,
        wbwId,
    ) {
        value = WbwTooltipFetched(loading = true, wbw = null)

        val id = wbwId

        if (id == null) {
            value = WbwTooltipFetched(loading = false, wbw = null)
            return@produceState
        }

        val row = withContext(Dispatchers.IO) {
            vm.repository.getWbwWordsForAyahs(
                wbwId = id,
                ayahIds = listOf(word.ayahId),
                wbwTranslation = ReaderPreferences.getWbwTooltipShowTranslation(),
                wbwTransliteration = ReaderPreferences.getWbwTooltipShowTransliteration(),
            )[word.ayahId]?.get(word.wordIndex)
        }

        value = WbwTooltipFetched(loading = false, wbw = row)
    }

    val state = rememberTooltipState(isPersistent = true)

    LaunchedEffect(word.ayahId, word.wordIndex) {
        state.show()
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
            12.dp
        ),
        onDismissRequest = {
            onDismiss()
        },
        tooltip = {
            PlainTooltip(
                caretShape = TooltipDefaults.caretShape(DpSize(16.dp, 8.dp)),
                shape = shapes.small,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                containerColor = colorScheme.surface,
                modifier = Modifier
                    .clickable { onOpenSheet() }
            ) {
                val wbw = fetched.wbw

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fetched.loading) {
                        Loader(size = 20.dp)
                    } else if (wbw != null) {
                        val hasTransliteration = !wbw.transliteration.isNullOrBlank()
                        val hasTranslation = !wbw.translation.isNullOrBlank()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            if (hasTransliteration) {
                                Text(
                                    text = wbw.transliteration,
                                    style = textStyles.wbwTrltStyle ?: TextStyle.Default,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            if (hasTranslation) {
                                Text(
                                    text = wbw.translation,
                                    style = textStyles.wbwTrStyle ?: TextStyle.Default,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Icon(
                        painterResource(R.drawable.dr_icon_chevron_right),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        state = state,
        content = anchor,
        hasAction = true,
        focusable = false,
    )
}
