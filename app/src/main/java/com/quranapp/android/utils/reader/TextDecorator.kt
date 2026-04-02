package com.quranapp.android.utils.reader

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfaazplus.sunnah.ui.theme.fontFamily
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.extensions.getDimension
import com.quranapp.android.viewModels.VerseViewModel

@Composable
fun translationTextStyle(
    slug: String?
): TextStyle {
    val isUrdu = slug != null && TranslUtils.isUrdu(slug)
    val baseStyle = LocalTextStyle.current
    val textSizeMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier()

    val resolvedFontSize = if (baseStyle.fontSize != TextUnit.Unspecified) {
        baseStyle.fontSize
    } else {
        16.sp
    } * textSizeMultiplier

    return baseStyle.copy(
        textDirection = if (isUrdu) TextDirection.Rtl else TextDirection.Ltr,
        fontFamily = if (isUrdu) fontUrdu else fontFamily,
        platformStyle = PlatformTextStyle(
            includeFontPadding = true
        ),
        lineHeight = if (isUrdu) resolvedFontSize * 2.5f else resolvedFontSize * 1.5
    )
}

@Composable
fun rememberQuranTextStyle(
    verse: Verse,
): TextStyle {
    val viewModel = viewModel<VerseViewModel>()
    val context = LocalContext.current
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val script = ReaderPreferences.observeQuranScript()
    val textSizeMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier()

    val fontFamily = remember(script, verse, verse.pageNo) {
        viewModel.fontFamily(script, verse)
    }

    val fontSize = remember(script, textSizeMultiplier, density) {
        val basePx = context.getDimension(
            script.getQuranScriptVerseTextSizeMediumRes()
        )
        with(density) { (basePx * textSizeMultiplier).toSp() }
    }

    return remember(fontFamily, fontSize) {
        typography.headlineSmall.copy(
            fontFamily = fontFamily,
            fontSize = fontSize,
            color = colorScheme.onBackground,
            textDirection = TextDirection.Rtl,

            lineHeight = fontSize * 1.8f
        )
    }
}