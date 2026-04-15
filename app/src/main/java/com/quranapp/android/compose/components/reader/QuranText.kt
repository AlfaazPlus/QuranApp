package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun QuranText(
    verseUi: ReaderLayoutItem.VerseUI,
) {
    Text(
        text = verseUi.parsedQuranText ?: buildAnnotatedString { },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}