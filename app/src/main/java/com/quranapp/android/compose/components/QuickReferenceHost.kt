package com.quranapp.android.compose.components

import android.app.Activity
import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceParsedVerses
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.factory.ReaderFactory

class QuickReferenceHost(
    private val activity: Activity,
    private val composeView: ComposeView,
) {
    private var quickRefData by mutableStateOf<QuickReferenceData?>(null)

    init {
        attach()
    }

    @JvmOverloads
    fun show(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        slugs: Set<String>? = null,
    ) {
        activity.runOnUiThread {
            quickRefData = QuickReferenceData(
                slugs ?: ReaderPreferences.getTranslations(),
                chapterNo,
                parsedVerses = QuickReferenceParsedVerses.Range(fromVerse..toVerse),
            )
        }
    }

    private fun attach() {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            QuranAppTheme {
                SideEffect {
                    composeView.visibility =
                        if (quickRefData == null) View.GONE else View.VISIBLE
                }

                QuickReference(
                    data = quickRefData,
                    onOpenInReader = { chapterNo, range ->
                        quickRefData = null
                        ReaderFactory.startVerseRange(activity, chapterNo, range.first, range.last)
                    },
                    onClose = { quickRefData = null },
                )
            }
        }
    }
}