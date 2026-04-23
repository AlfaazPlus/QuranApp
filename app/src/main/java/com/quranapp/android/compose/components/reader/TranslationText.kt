package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.univ.StringUtils


@Composable
fun TranslationText(
    verseUi: ReaderLayoutItem.VerseUI,
) {
    val texts = verseUi.parsedTranslationTexts
    if (texts.isEmpty()) {
        return
    }
    val isDark = isSystemInDarkTheme()

    SelectionContainer {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            texts.forEach {
                val langCode = it.first
                val text = it.second

                Text(
                    text,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) colorScheme.onBackground.alpha(0.8f) else colorScheme.onBackground,
                    style = TextStyle(
                        textDirection = if (StringUtils.isRtlLanguage(langCode)) TextDirection.Rtl else TextDirection.Ltr,
                    )
                )
            }
        }
    }
}
