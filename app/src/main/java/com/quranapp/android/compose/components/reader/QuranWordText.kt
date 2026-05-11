package com.quranapp.android.compose.components.reader

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.LocalQuranAtlasBundle

@Composable
fun QuranWordText(
    modifier: Modifier = Modifier,
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    style: TextStyle = TextStyle.Default,
) {
    val bundle = LocalQuranAtlasBundle.current

    if (bundle != null && atlasPlacements != null) {
        QuranAtlasText(
            modifier = modifier,
            placements = atlasPlacements,
            bundle = bundle,
            fontSize = style.fontSize,
            lineHeight = style.lineHeight,
            color = style.color,
        )
    } else {
        Text(
            text = word.text,
            style = style,
            modifier = modifier,
            maxLines = 1,
            softWrap = false,
        )
    }
}
