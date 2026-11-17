package com.quranapp.android.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quranapp.android.components.common.dialogs.MessageDialog
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelGotIt
import com.quranapp.android.generated.resources.strMsgTranslSelectionLimit
import com.quranapp.android.generated.resources.strTitleInfo
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.viewmodel.TranslationUiEvent
import com.quranapp.android.viewmodel.TranslationViewModel
import org.jetbrains.compose.resources.stringResource

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
            title = stringResource(Res.string.strTitleInfo),
            message = stringResource(
                Res.string.strMsgTranslSelectionLimit,
                TranslUtils.TRANSL_MAX_SELECTION_LIMIT,
            ),
            confirmText = stringResource(Res.string.strLabelGotIt),
            onClose = { selectionLimitOpen = false },
        )
    }
}
