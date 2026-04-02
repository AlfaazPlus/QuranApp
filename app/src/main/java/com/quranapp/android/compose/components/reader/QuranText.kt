package com.quranapp.android.compose.components.reader

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.utils.reader.rememberQuranTextStyle
import com.quranapp.android.utils.univ.MessageUtils

@Composable
fun QuranText(
    verse: Verse,
) {
    val style = rememberQuranTextStyle(verse)
    val context = LocalContext.current

    Text(
        text = buildAnnotatedString {
            verse.segments.forEachIndexed { index, word ->
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "wbw",
                        styles = TextLinkStyles(
                            focusedStyle = SpanStyle(color = colorScheme.primary),
                            pressedStyle = SpanStyle(color = colorScheme.primary),
                            hoveredStyle = SpanStyle(color = colorScheme.primary),
                        )
                    ) {
                        // TODO
                        MessageUtils.showRemovableToast(context, word, Toast.LENGTH_LONG)
                    }
                ) {
                    append(word)
                }

                if (index != verse.segments.lastIndex) {
                    append(" ")
                }
            }

            if (!verse.endText.isNullOrEmpty()) {
                append("  " + verse.endText)
            }
        },
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}