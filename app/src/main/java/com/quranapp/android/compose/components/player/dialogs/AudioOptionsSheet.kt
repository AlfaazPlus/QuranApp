package com.quranapp.android.compose.components.player.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationPreferences
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioOptionsSheet(
    controller: RecitationController,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeRecitationAudioOption()
    val selectedVerseGroupSize = RecitationPreferences.observeVerseGroupSize()
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Pair(RecitationUtils.AUDIO_OPTION_ONLY_QURAN, R.string.audioOnlyArabic),
        Pair(RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION, R.string.audioOnlyTranslation),
        Pair(RecitationUtils.AUDIO_OPTION_BOTH, R.string.audioBothArabicTranslation),
    )

    val verseGroupSizes = listOf(1, 2, 3, 5, 10)
    val sizeOptionEnabled = selectedAudioOption == RecitationUtils.AUDIO_OPTION_BOTH
    val sizeOptionOpacity = if (sizeOptionEnabled) 1f else 0.5f

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.dr_icon_settings,
        title = stringResource(R.string.audioOption),
    ) {
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
        ) {
            items.forEach { (option, title) ->
                RadioItem(
                    title = title,
                    selected = option == selectedAudioOption,
                    onClick = {
                        coroutineScope.launch {
                            RecitationPreferences.setRecitationAudioOption(option)
                            controller.setAudioOption(option)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.titleRecitationGroupSize),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .alpha(sizeOptionOpacity),
                color = colorScheme.primary
            )

            AlertCard(
                modifier = Modifier.alpha(sizeOptionOpacity)
            ) {
                Text(
                    text = stringResource(R.string.msgRecitationGroupSize),
                    style = typography.bodyMedium
                )
            }

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(sizeOptionOpacity)
            ) {
                verseGroupSizes.forEach { size ->
                    FilterChip(
                        selected = size == selectedVerseGroupSize,
                        enabled = sizeOptionEnabled,
                        onClick = {
                            if (sizeOptionEnabled) {
                                coroutineScope.launch {
                                    RecitationPreferences.setVerseGroupSize(size)
                                    controller.setVerseGroupSize(size)
                                }
                            }
                        },
                        label = { Text(size.toString()) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.primary,
                            selectedLabelColor = colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = size == selectedVerseGroupSize,
                            borderColor = colorScheme.outlineVariant,
                        ),
                    )
                }
            }
        }
    }
}