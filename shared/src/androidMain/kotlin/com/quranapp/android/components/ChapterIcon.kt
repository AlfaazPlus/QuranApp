package com.quranapp.android.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.quranapp.android.theme.fontSurah
import com.quranapp.android.utils.quran.QuranGlyphs

@Composable
fun ChapterIcon(
    chapterNo: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = MaterialTheme.typography.headlineSmall.fontSize,
    withPrefix: Boolean = true,
) {
    val text = remember(chapterNo, withPrefix) {
        var base = QuranGlyphs.Chapter.get(chapterNo)

        if (withPrefix) {
            base += QuranGlyphs.Chapter.getPrefix()
        }

        base
    }

    if (text.isEmpty()) return

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fontSurah,
        fontSize = fontSize,
        fontWeight = FontWeight.Normal,
        style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = false,
            ),
        )
    )
}

