package com.quranapp.android.components.miscellenous

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.copiedToClipboard
import com.quranapp.android.generated.resources.createIssue
import com.quranapp.android.generated.resources.lastCrashLog
import com.quranapp.android.generated.resources.pasteCrashLogGithubIssue
import com.quranapp.android.generated.resources.strLabelCopy
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.extensions.copyToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun LastCrashReportDialog() {
    var crashLog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val logText = withContext(Dispatchers.IO) { Logger.getLastCrashLog() }

        if (logText.isNullOrBlank()) {
            AppPreferences.removeLastCrashLogFilename()
            return@LaunchedEffect
        }

        crashLog = logText
    }

    fun dismiss() {
        crashLog = null

        scope.launch {
            AppPreferences.removeLastCrashLogFilename()
        }
    }

    AlertDialog(
        isOpen = crashLog != null,
        onClose = { dismiss() },
        title = stringResource(Res.string.lastCrashLog),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCopy),
                style = AlertDialogActionStyle.Primary,
                dismissOnClick = false,
                onClick = {
                    val log = crashLog ?: return@AlertDialogAction

                    dismiss()
                    context.copyToClipboard(log)
                    scope.launch {
                        PlatformServices.messages.showClipboard(
                            getString(Res.string.copiedToClipboard),
                        )
                    }
                },
            ),
            AlertDialogAction(
                text = stringResource(Res.string.createIssue),
                style = AlertDialogActionStyle.Primary,
                dismissOnClick = false,
                onClick = {
                    val log = crashLog ?: return@AlertDialogAction

                    dismiss()
                    context.copyToClipboard(log)
                    scope.launch {
                        PlatformServices.messages.show(
                            getString(Res.string.pasteCrashLogGithubIssue),
                            long = true
                        )
                    }
                    PlatformServices.links.browse(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
                },
            ),
        ),
        content = {
            val log = crashLog

            if (log != null) {
                Text(
                    text = log,
                    color = colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    style = typography.bodyMedium,
                )
            }
        },
    )
}
