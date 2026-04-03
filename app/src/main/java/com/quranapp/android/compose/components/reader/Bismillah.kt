package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.fontBismillah
import com.quranapp.android.R

@Composable
fun Bismillah(
    modifier: Modifier = Modifier
) {
    Text(
        stringResource(R.string.strBismillahEntity),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        fontSize = 40.sp,
        fontFamily = fontBismillah,
        textAlign = TextAlign.Center,
    )
}