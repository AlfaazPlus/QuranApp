package com.quranapp.android.compose.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.components.quran.Quran2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SpotlightVerseLines(val arabic: String, val translation: String?)

@Composable
fun SpotlightVersePanel(
    verse: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val lines by produceState<SpotlightVerseLines?>(
        initialValue = null,
        verse.chapterNo,
        verse.verseNo,
    ) {
        if (!verse.isValid) {
            value = SpotlightVerseLines("", null)
            return@produceState
        }

        val quran = Quran2.prepareInstance(context)

        val arabic = quran.getVerse(verse.chapterNo, verse.verseNo)?.arabicText.orEmpty()

        val translation = withContext(Dispatchers.IO) {
            QuranTranslationFactory(context).use { factory ->
                factory.getTranslationsSingleVerse(verse.chapterNo, verse.verseNo)
                    .firstOrNull()
                    ?.let { StringUtils.removeHTML(it.text, false) }
                    ?.takeIf { it.isNotBlank() }
            }
        }

        value = SpotlightVerseLines(arabic, translation)
    }

    val scroll = rememberScrollState()

    Box(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val content = lines) {
            null -> CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = colorScheme.primary,
                strokeWidth = 3.dp,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = content.arabic,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                    ),
                    color = PlayerContentColor,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = content.translation
                        ?: stringResource(R.string.strMsgTranslNoneSelected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (content.translation != null) {
                        PlayerContentColor.alpha(0.82f)
                    } else {
                        PlayerContentColor.alpha(0.45f)
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
