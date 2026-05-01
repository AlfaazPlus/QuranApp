package com.alfaazplus.sunnah.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.quranapp.android.R

val appFontFamilyTitle = FontFamily.Default
val appFontFamily = FontFamily.Default

val fontUrdu by lazy {
    FontFamily(
        Font(
            R.font.noto_nastaliq_urdu_regular,
            weight = FontWeight.Normal,
        ),
    )
}

val fontCommon by lazy {
    FontFamily(
        Font(
            R.font.quran_common,
            weight = FontWeight.Normal,
        ),
    )
}

val fontSurah by lazy {
    FontFamily(
        Font(
            R.font.suracon,
            weight = FontWeight.Normal,
        ),
    )
}

val tightTextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle.Default.copy(
        mode = LineHeightStyle.Mode.Tight,
        alignment = LineHeightStyle.Alignment.Center,
    )
)

private val platformTextStyle = PlatformTextStyle(false)

@Composable
fun getAppTypography(): Typography {
    val baseFont = appFontFamily
    val titleFont = appFontFamilyTitle

    return Typography(
        bodyLarge = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        bodySmall = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        labelLarge = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        labelMedium = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = baseFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        ),
        displayLarge = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            platformStyle = platformTextStyle,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
    )
}

val type @Composable get() = MaterialTheme.typography
