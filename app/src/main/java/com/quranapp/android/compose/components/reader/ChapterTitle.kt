package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.compose.components.ChapterIcon

@Composable
fun ChapterTitle(
    chapterNo: Int,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            Modifier.weight(1f),
            thickness = 1.dp,
            color = colorScheme.primary,
        )
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.quran_frame3),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorScheme.primary),
                modifier = Modifier.height(55.dp)
            )

            ChapterIcon(chapterNo, fontSize = 31.sp, modifier = Modifier.padding(top = 10.dp))
        }
        HorizontalDivider(
            Modifier.weight(1f),
            thickness = 1.dp,
            color = colorScheme.primary,
        )
    }
}