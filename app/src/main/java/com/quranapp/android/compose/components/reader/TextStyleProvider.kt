package com.quranapp.android.compose.components.reader

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.appFontFamily
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.isUrduLanguageCode
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.wbw.WbwManager


data class QuranTextStyle(
    val quran: (Int) -> TextStyle?,
    val wbwTrltStyle: TextStyle?,
    val wbwTrStyle: TextStyle?,
    val wbwMaxWith: Dp,
)

val LocalQuranTextStyle = staticCompositionLocalOf<QuranTextStyle> {
    error("QuranTextStyle not provided")
}

@Composable
fun TextStyleProvider(
    pageTextStyles: Map<Int, TextStyle>,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val wbwMult = ReaderPreferences.observeWbwTextSizeMultiplier()
    val wbwId = ReaderPreferences.observeWbwId()

    val wbwInfo by produceState<WbwLanguageInfo?>(null, wbwId) {
        value = WbwManager.getAvailable(context, false)?.wbw?.firstOrNull {
            it.id == wbwId
        }
    }

    val transliterationBase = typography.bodySmall.copy(
        color = colorScheme.onSurface.alpha(0.65f)
    )

    val translationBase = typography.bodySmall.copy(
        color = colorScheme.onSurface
    )

    val fontFamily =
        if (wbwInfo?.langCode?.isUrduLanguageCode() == true) fontUrdu else appFontFamily

    val transliterationStyle = remember(translationBase, wbwMult, fontFamily) {
        val fs = (transliterationBase.fontSize.value * wbwMult).sp

        transliterationBase.copy(
            fontSize = fs,
            lineHeight = fs * 1.35f,
            fontFamily = fontFamily
        )
    }

    val translationStyle = remember(translationBase, wbwMult, fontFamily) {
        val fs = (translationBase.fontSize.value * wbwMult).sp

        translationBase.copy(
            fontSize = fs,
            lineHeight = fs * 1.35f,
            fontFamily = fontFamily
        )
    }


    CompositionLocalProvider(
        LocalQuranTextStyle provides QuranTextStyle(
            quran = { pageTextStyles[it] },
            wbwTrltStyle = transliterationStyle,
            wbwTrStyle = translationStyle,
            wbwMaxWith = (90 * wbwMult / 100).coerceAtLeast(90F).dp
        )
    ) {
        content()
    }
}