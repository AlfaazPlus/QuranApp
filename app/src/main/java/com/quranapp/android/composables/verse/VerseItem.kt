package com.quranapp.android.composables.verse

import androidx.compose.runtime.Composable
import com.quranapp.android.components.quran.subcomponents.Verse


// TODO: isShowingAsReference

@Composable
fun VerseView(
    verse: Verse,
    isBookmarked: Boolean,
    isReciting: Boolean,
    onOptions: (Verse) -> Unit,
    onRecite: (Verse) -> Unit,
    onTafsir: (Verse) -> Unit,
    onBookmarkToggle: (Verse) -> Unit
) {}