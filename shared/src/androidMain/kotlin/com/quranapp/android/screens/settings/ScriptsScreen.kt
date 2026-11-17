package com.quranapp.android.screens.settings

import android.text.format.Formatter
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.Chip
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.estimatedSize
import com.quranapp.android.generated.resources.labelDownload
import com.quranapp.android.generated.resources.msgDownloadFontsSize
import com.quranapp.android.generated.resources.msgDownloadKFQPCResources
import com.quranapp.android.generated.resources.msgDownloadingFonts
import com.quranapp.android.generated.resources.msgExtractingFonts
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strTitleScripts
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.generated.resources.titleDownloadScriptResources
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.alpha
import com.quranapp.android.theme.observeDarkTheme
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.getQuranScriptFontPackSizeMb
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.getQuranScriptPreview
import com.quranapp.android.utils.reader.getQuranScriptVariantName
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.viewmodel.ScriptEvent
import com.quranapp.android.viewmodel.ScriptsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScriptsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = appViewModel<ScriptsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDownloadAlert by remember {
        mutableStateOf<Pair<String, QuranScriptUtils.DownloadedFontsInfo>?>(
            null
        )
    }

    var showAtlasDownloadAlert by remember { mutableStateOf<String?>(null) }

    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedVariant = ReaderPreferences.observeQuranScriptVariant()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(stringResource(Res.string.strTitleScripts))
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
                            QuranScriptUtils.getKFQPCFontDownloadedCount(script)

                        if (fontDownloadedCount.remaining > 0) {
                            showDownloadAlert = script to fontDownloadedCount
                            return@ScriptItem
                        }
                    }

                    scope.launch {
                        if (script.isQuranAtlasScript() && !viewModel.isAtlasInstalled(script)) {
                            showAtlasDownloadAlert = script
                            return@launch
                        }

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

    ScriptAtlasDownloadRequestAlert(
        viewModel = viewModel,
        scriptKey = showAtlasDownloadAlert,
        onDismiss = { showAtlasDownloadAlert = null },
    )
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
    val appLocale = LocalAppLocale.current
    val isDark = ThemeUtils.observeDarkTheme()

    val isSelected = script == selectedScript
    val downloadState = downloadStates[script] ?: ResourceDownloadStatus.Idle
    val isDownloading =
        downloadState is ResourceDownloadStatus.Started || downloadState is ResourceDownloadStatus.InProgress
    val applyTint = script != QuranScriptUtils.SCRIPT_KFQPC_V4

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
            Image(
                painter = painterResource(script.getQuranScriptPreview(isDark)),
                contentDescription = script.getQuranScriptName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(12.dp),
                contentScale = ContentScale.Fit,
                colorFilter = if (applyTint) ColorFilter.tint(colorScheme.onSurface) else null
            )

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
                        style = typography.labelLarge,
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
                                    appLocale.format(
                                        "%1\$s (%2\$d%%)",
                                        stringResource(
                                            if (script.isQuranAtlasScript()) {
                                                Res.string.textDownloading
                                            } else {
                                                Res.string.msgDownloadingFonts
                                            },
                                        ),
                                        downloadState.progress
                                    )
                                } else {
                                    if (script.isQuranAtlasScript()) {
                                        stringResource(Res.string.textDownloading)
                                    } else {
                                        stringResource(Res.string.msgExtractingFonts)
                                    }
                                }

                                else -> stringResource(Res.string.textDownloading)
                            },
                            modifier = Modifier.weight(1f),
                            style = typography.bodyMedium.copy(
                                color = colorScheme.onSurface.alpha(0.75f),
                                fontStyle = FontStyle.Italic
                            ),
                        )
                        TextButton(onClick = { onCancelDownload(script) }) {
                            Text(stringResource(Res.string.strLabelCancel))
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
        title = stringResource(Res.string.titleDownloadScriptResources),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(Res.string.labelDownload),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    viewModel.onEvent(ScriptEvent.DownloadScript(info!!.first))
                }
            )
        )
    ) {
        if (info == null) return@AlertDialog

        val msg = StringBuilder(stringResource(Res.string.msgDownloadKFQPCResources)).append("\n")
        val downloadSize = info.first.getQuranScriptFontPackSizeMb()

        if (info.second.remaining > 0) {
            msg.append("\n").append(
                stringResource(
                    Res.string.msgDownloadFontsSize,
                    downloadSize.first / 1000,
                    downloadSize.second / 1000
                )
            )
        }

        Text(msg.toString())
    }
}

@Composable
private fun ScriptAtlasDownloadRequestAlert(
    viewModel: ScriptsViewModel,
    scriptKey: String?,
    onDismiss: () -> Unit,
) {
    val estimatedSizeBytes = scriptKey?.getQuranScriptFontPackSizeMb()?.first ?: 0
    val context = LocalContext.current

    AlertDialog(
        isOpen = scriptKey != null,
        onClose = onDismiss,
        title = stringResource(Res.string.titleDownloadScriptResources),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(Res.string.labelDownload),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    val key = scriptKey ?: return@AlertDialogAction
                    viewModel.onEvent(
                        ScriptEvent.DownloadAtlas(key, 6)
                    )
                }
            )
        )
    ) {
        if (scriptKey == null) return@AlertDialog

        Text(
            scriptKey.getQuranScriptName() + "\n" +
                    stringResource(
                        Res.string.estimatedSize,
                        Formatter.formatFileSize(context, estimatedSizeBytes * 1000L)
                    )
        )
    }
}
