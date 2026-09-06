package com.quranapp.android.compose.components.tafsir.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.utils.reader.ReaderTextSizeUtils

@Composable
fun TafsirTextSizeSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    textSizeMultiplier: Float,
    onUpdateTextSize: (Float) -> Unit,
) {
    val appLocale = LocalAppLocale.current
    val min = ReaderTextSizeUtils.TEXT_SIZE_MIN_PROGRESS.toFloat()
    val max = ReaderTextSizeUtils.TEXT_SIZE_MAX_PROGRESS.toFloat()
    val steps = max.toInt() - min.toInt()

    var sliderProgress by remember(textSizeMultiplier) {
        mutableFloatStateOf((textSizeMultiplier * 100).coerceIn(min, max))
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.icon_font_size,
        title = stringResource(R.string.titleReaderTextSizeTafsir),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Slider(
                value = sliderProgress,
                onValueChange = { newValue ->
                    sliderProgress = newValue

                    val nProgress = newValue.toInt()
                    val newMultiplier = ReaderTextSizeUtils.calculateMultiplier(nProgress)

                    if (newMultiplier != textSizeMultiplier) {
                        onUpdateTextSize(newMultiplier)
                    }
                },
                valueRange = min..max,
                steps = steps,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = String.format(appLocale.platformLocale, "%d%%", sliderProgress.toInt()),
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
