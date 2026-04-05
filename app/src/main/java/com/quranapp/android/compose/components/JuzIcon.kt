package com.quranapp.android.compose.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.alfaazplus.sunnah.ui.theme.fontCommon
import com.alfaazplus.sunnah.ui.theme.fontSurah
import com.quranapp.android.utils.quran.QuranGlyphs

@Composable
fun JuzIcon(
    juzNo: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = MaterialTheme.typography.headlineSmall.fontSize,
    withPrefix: Boolean = true,
) {
    Text(
        text = QuranGlyphs.Juz.get(juzNo),
        modifier = modifier,
        color = color,
        fontFamily = fontCommon,
        fontSize = fontSize,
        fontWeight = FontWeight.Normal,
    )
}

