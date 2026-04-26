package com.quranapp.android.compose.screens.settings

import ThemeUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.Chip
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.extensions.getDimenPx
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.getQuranScriptFontPackSizeMb
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.getQuranScriptVariantName
import com.quranapp.android.utils.reader.getQuranScriptVerseTextSizeMediumRes
import com.quranapp.android.utils.reader.getScriptPreviewText
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.viewModels.ScriptEvent
import com.quranapp.android.viewModels.ScriptsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ScriptsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<ScriptsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDownloadAlert by remember {
        mutableStateOf<Pair<String, QuranScriptUtils.DownloadedFontsInfo>?>(
            null
        )
    }

    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedVariant = ReaderPreferences.observeQuranScriptVariant()


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(stringResource(R.string.strTitleScripts))
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.scripts.keys.toList()) { script ->
                ScriptItem(
                    script = script,
                    variants = uiState.scripts.getOrDefault(script, emptyList()),
                    selectedScript = selectedScript,
                    selectedVariant = selectedVariant,
                    downloadStates = uiState.downloadStates,
                    onCancelDownload = { key ->
                        viewModel.onEvent(ScriptEvent.CancelDownload(key))
                    }
                ) { newScript, newVariant ->
                    if (script.isKFQPCScript()) {
                        val fontDownloadedCount =
                            QuranScriptUtils.getKFQPCFontDownloadedCount(context, script)

                        if (fontDownloadedCount.remaining > 0) {
                            showDownloadAlert = script to fontDownloadedCount
                            return@ScriptItem
                        }
                    }

                    scope.launch {
                        ReaderPreferences.setQuranScriptWithVariant(newScript, newVariant)
                    }
                }
            }
        }
    }

    ScriptDownloadRequestAlert(
        viewModel,
        info = showDownloadAlert
    ) {
        showDownloadAlert = null
    }
}

@Composable
private fun ScriptItem(
    script: String,
    variants: List<QuranScriptVariant>,
    selectedScript: String,
    selectedVariant: QuranScriptVariant?,
    downloadStates: Map<String, ResourceDownloadStatus>,
    onCancelDownload: (String) -> Unit,
    onSelect: (String, QuranScriptVariant?) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()

    val previewStyle = TextStyle(
        fontFamily = FontFamily(Font(script.getQuranScriptFontRes(isDark))),
        fontSize = with(density) {
            context.getDimenPx(script.getQuranScriptVerseTextSizeMediumRes()).toSp()
        }
    )

    val isSelected = script == selectedScript
    val downloadState = downloadStates[script] ?: ResourceDownloadStatus.Idle
    val isDownloading =
        downloadState is ResourceDownloadStatus.Started || downloadState is ResourceDownloadStatus.InProgress

    Surface(
        Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.medium)
            .border(1.dp, colorScheme.outline.alpha(0.2f), MaterialTheme.shapes.medium)
            .clickable {
                if (!isDownloading && script != selectedScript) {
                    onSelect(script, variants.firstOrNull())
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    text = script.getScriptPreviewText(),
                    style = previewStyle,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(
                color = colorScheme.outline.alpha(0.2f),
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isDownloading) {
                        Loader(
                            size = 20.dp
                        )
                    } else {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (!isDownloading) {
                                    onSelect(script, null)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        script.getQuranScriptName(),
                        modifier = Modifier.weight(1f),
                        style = typography.titleMedium,
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                    )
                }

                if (isDownloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (downloadState) {
                                is ResourceDownloadStatus.InProgress -> if (downloadState.progress <= 100) {
                                    String.format(
                                        Locale.getDefault(),
                                        $$"%1$s (%2$d%%)",
                                        stringResource(R.string.msgDownloadingFonts),
                                        downloadState.progress
                                    )
                                } else {
                                    stringResource(R.string.msgExtractingFonts)
                                }

                                else -> stringResource(R.string.textDownloading)
                            },
                            modifier = Modifier.weight(1f),
                            style = typography.bodyMedium.copy(
                                color = colorScheme.onSurface.alpha(0.75f),
                                fontStyle = FontStyle.Italic
                            ),
                        )
                        TextButton(onClick = { onCancelDownload(script) }) {
                            Text(stringResource(R.string.strLabelCancel))
                        }
                    }
                }

                if (isSelected && variants.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        variants.forEach { variant ->
                            Chip(
                                label = {
                                    Text(variant.getQuranScriptVariantName())
                                },
                                selected = selectedVariant == variant
                            ) {
                                onSelect(script, variant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptDownloadRequestAlert(
    viewModel: ScriptsViewModel,
    info: Pair<String, QuranScriptUtils.DownloadedFontsInfo>?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        isOpen = info != null,
        onClose = onDismiss,
        title = stringResource(R.string.titleDownloadScriptResources),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(R.string.labelDownload),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    viewModel.onEvent(ScriptEvent.DownloadScript(info!!.first))
                }
            )
        )
    ) {
        if (info == null) return@AlertDialog

        val msg = StringBuilder(stringResource(R.string.msgDownloadKFQPCResources)).append("\n")
        val downloadSize = info.first.getQuranScriptFontPackSizeMb()

        if (info.second.remaining > 0) {
            msg.append("\n").append(
                stringResource(
                    R.string.msgDownloadFontsSize,
                    downloadSize.first,
                    downloadSize.second
                )
            )
        }

        Text(msg.toString())
    }
}