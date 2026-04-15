package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.fontCommon
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.utils.quran.QuranGlyphs

@Composable
fun ChapterTitle(
    chapterNo: Int,
    withinBox: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (withinBox) 0.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {

        Text(
            text = QuranGlyphs.Special.TITLE_FRAME,
            modifier = Modifier
                .fillMaxWidth(),
            style = TextStyle(
                fontFamily = fontCommon,
                fontSize = 46.sp,
                textAlign = TextAlign.Center,
                color = colorScheme.primary,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )

        ChapterIcon(chapterNo, fontSize = 30.sp, modifier = Modifier.padding(top = 8.dp))
    }
}