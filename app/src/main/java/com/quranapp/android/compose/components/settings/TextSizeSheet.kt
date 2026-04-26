package com.quranapp.android.compose.components.settings

import ThemeUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MAX_PROGRESS
import com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MIN_PROGRESS
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeMediumRes
import com.quranapp.android.utils.reader.getScriptPreviewText
import kotlinx.coroutines.launch
import java.util.Locale

private enum class PreviewType {
    Arabic,
    Translation,
}

@Composable
fun TextSizeSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.icon_font_size,
        title = stringResource(R.string.textSizes),
    ) {
        AlertCard(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(R.string.textSizeVerseByVerseOnly),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ArabicTextSizeSlider()
            TranslationTextSizeSlider()
        }
    }
}


@Composable
private fun ArabicTextSizeSlider() {
    val coroutineScope = rememberCoroutineScope()
    val textSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()

    val min = TEXT_SIZE_MIN_PROGRESS.toFloat()
    val max = TEXT_SIZE_MAX_PROGRESS.toFloat()
    val steps = max.toInt() - min.toInt()
    val progress = textSizeMult * 100

    ListItemCategoryLabel(stringResource(R.string.strTitleReaderTextSizeArabic))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = progress,
            onValueChange = {
                coroutineScope.launch {
                    ReaderPreferences.setArabicTextSizeMultiplier(
                        ReaderTextSizeUtils.calculateMultiplier(it.toInt())
                    )
                }
            },
            valueRange = min..max,
            steps = steps
        )
        Text(
            text = String.format(Locale.getDefault(), "%d%%", progress.toInt()),
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        ArabicTextPreview(textSizeMult)
    }
}

@Composable
private fun ArabicTextPreview(textSizeMult: Float) {
    val script = ReaderPreferences.observeQuranScript()
    val isDark = ThemeUtils.observeDarkTheme()
    val density = LocalDensity.current

    val sizeRes = script.getQuranScriptVerseTextSizeMediumRes()

    val fontSize = with(density) {
        (LocalResources.current.getDimensionPixelSize(sizeRes) * textSizeMult).toSp()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            script.getScriptPreviewText(),
            modifier = Modifier.fillMaxWidth(),
            fontSize = fontSize,
            lineHeight = fontSize * 1.8,
            fontFamily = FontFamily(
                Font(
                    script.getQuranScriptFontRes(isDark),
                    FontWeight.Normal
                )
            ),
        )
    }
}

@Composable
private fun TranslationTextSizeSlider() {
    val coroutineScope = rememberCoroutineScope()
    val textSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()

    val min = TEXT_SIZE_MIN_PROGRESS.toFloat()
    val max = TEXT_SIZE_MAX_PROGRESS.toFloat()
    val steps = max.toInt() - min.toInt()
    val progress = textSizeMult * 100

    ListItemCategoryLabel(stringResource(R.string.strTitleReaderTextSizeTransl))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = progress,
            onValueChange = {
                coroutineScope.launch {
                    ReaderPreferences.setTranslationTextSizeMultiplier(
                        ReaderTextSizeUtils.calculateMultiplier(
                            it.toInt()
                        )
                    )
                }
            },
            valueRange = min..max,
            steps = steps
        )
        Text(
            text = String.format(Locale.getDefault(), "%d%%", progress.toInt()),
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        TranslationTextPreview(textSizeMult)
    }
}

@Composable
private fun TranslationTextPreview(textSizeMult: Float) {
    val fontSize = 16.sp * textSizeMult

    Text(
        stringResource(R.string.strPreviewTextTransl),
        fontSize = fontSize,
        lineHeight = fontSize * 1.5,
    )
}
