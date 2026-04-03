package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel

@Composable
fun ReaderNavigator(
    readerVm: ReaderViewModel,
    isInBottomSheet: Boolean,
    onClose: () -> Unit,
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
}
