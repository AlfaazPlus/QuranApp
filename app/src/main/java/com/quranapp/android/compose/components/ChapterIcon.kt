package com.quranapp.android.compose.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.quranapp.android.R
import com.quranapp.android.utils.quran.QuranUtils

//@font/suracon
val fontFamilyChapterIcon = FontFamily(
    Font(
        resId = R.font.suracon,
        weight = FontWeight.Normal,
        loadingStrategy = FontLoadingStrategy.Async
    )
)

@Composable
fun ChapterIcon(
    chapterNo: Int,
    modifier: Modifier = Modifier,
    color: Color = colorScheme.onBackground,
    fontSize: TextUnit = MaterialTheme.typography.headlineSmall.fontSize,
    withPrefix: Boolean = true,
) {
    val text = remember(chapterNo, withPrefix) {
        val base = QuranUtils.getChapterIconUnicode(chapterNo)

        if (base != null) {
            if (withPrefix) {
                base + (QuranUtils.getChapterIconUnicode(0) ?: "")
            } else {
                base
            }
        } else {
            ""
        }
    }

    if (text.isEmpty()) return

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fontFamilyChapterIcon,
        fontSize = fontSize,
    )
}

