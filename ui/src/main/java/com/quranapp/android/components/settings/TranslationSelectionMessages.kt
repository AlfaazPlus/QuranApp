package com.quranapp.android.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.quranapp.android.components.common.dialogs.MessageDialog
import com.quranapp.android.core.R
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.viewmodel.TranslationUiEvent
import com.quranapp.android.viewmodel.TranslationViewModel

@Composable
fun TranslationSelectionMessages(viewModel: TranslationViewModel) {
    var selectionLimitOpen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TranslationUiEvent.SelectionLimit -> selectionLimitOpen = true
            }
        }
    }

    if (selectionLimitOpen) {
        MessageDialog(
            title = stringResource(R.string.strTitleInfo),
            message = stringResource(
                R.string.strMsgTranslSelectionLimit,
                TranslUtils.TRANSL_MAX_SELECTION_LIMIT,
            ),
            confirmText = stringResource(R.string.strLabelGotIt),
            onClose = { selectionLimitOpen = false },
        )
    }
}
