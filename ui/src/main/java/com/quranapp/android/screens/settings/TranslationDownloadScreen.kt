package com.quranapp.android.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.IconButton
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.MessageDialog
import com.quranapp.android.components.settings.TranslationDownloadList
import com.quranapp.android.core.R
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.viewmodel.TranslationDownloadEvent
import com.quranapp.android.viewmodel.TranslationDownloadUiEvent
import com.quranapp.android.viewmodel.TranslationDownloadViewModel
import com.quranapp.android.viewmodel.TranslationEvent
import com.quranapp.android.viewmodel.TranslationViewModel

@Composable
fun TranslationDownloadScreen() {
    val navController = LocalSettingsNavController.current
    val viewModel = appViewModel<TranslationDownloadViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var message by remember { mutableStateOf<TranslationDownloadUiEvent.ShowMessage?>(null) }

    DisposableEffect(navController) {
        onDispose {
            runCatching {
                val entry = navController.getBackStackEntry(SettingRoutes.TRANSLATIONS)
                val translationVm = ViewModelProvider(entry)[TranslationViewModel::class.java]
                translationVm.onEvent(TranslationEvent.RefreshQuiet)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TranslationDownloadUiEvent.ShowMessage -> message = event
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                stringResource(R.string.strTitleDownloadTranslations),
                searchPlaceholder = stringResource(R.string.strHintSearch),
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = {
                    viewModel.onEvent(TranslationDownloadEvent.Search(it))
                },
                actions = {
                    IconButton(
                        painterResource(R.drawable.dr_icon_refresh)
                    ) {
                        viewModel.onEvent(TranslationDownloadEvent.Refresh)
                    }
                }
            )
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            when {
                uiState.isLoading -> Loader(true)
                uiState.error != null -> ErrorMessageCard(
                    error = uiState.error!!,
                    onRetry = { viewModel.onEvent(TranslationDownloadEvent.Refresh) }
                )

                else -> TranslationDownloadList(
                    groups = uiState.groups,
                    searchQuery = uiState.searchQuery,
                    downloadStates = uiState.downloadStates,
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }

    val pending = message
    if (pending != null) {
        MessageDialog(
            title = pending.title,
            message = pending.message,
            confirmText = stringResource(R.string.strLabelClose),
            onClose = { message = null },
        )
    }
}
