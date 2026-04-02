package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.utils.reader.rememberQuranTextStyle

@Composable
fun QuranText(
    verse: Verse,
) {
    val style = rememberQuranTextStyle(verse)

    Text(
        text = verse.arabicText + " " + verse.endText,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}