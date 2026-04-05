package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.fontCommon
import com.quranapp.android.utils.quran.QuranGlyphs

@Composable
fun Bismillah(
    modifier: Modifier = Modifier
) {
    Text(
        QuranGlyphs.Special.BISMILLAH,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp),
        fontSize = 36.sp,
        fontFamily = fontCommon,
        textAlign = TextAlign.Center,
    )
}