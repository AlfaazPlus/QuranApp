package com.quranapp.android.components.miscellenous

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.core.R
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.sharedPrefs.SPLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LastCrashReportDialog() {
    var crashLog by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        val logText = withContext(Dispatchers.IO) { Log.getLastCrashLog(context) }

        if (logText.isNullOrBlank()) {
            SPLog.removeLastCrashLogFilename(context)
            return@LaunchedEffect
        }

        crashLog = logText
    }

    fun dismiss() {
        crashLog = null
        SPLog.removeLastCrashLogFilename(context)
    }

    AlertDialog(
        isOpen = crashLog != null,
        onClose = { dismiss() },
        title = stringResource(R.string.lastCrashLog),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCopy),
                style = AlertDialogActionStyle.Primary,
                dismissOnClick = false,
                onClick = {
                    val log = crashLog ?: return@AlertDialogAction

                    dismiss()
                    context.copyToClipboard(log)
                    PlatformServices.messages.showClipboard(
                        resources.getString(R.string.copiedToClipboard),
                    )
                },
            ),
            AlertDialogAction(
                text = stringResource(R.string.createIssue),
                style = AlertDialogActionStyle.Primary,
                dismissOnClick = false,
                onClick = {
                    val log = crashLog ?: return@AlertDialogAction

                    dismiss()
                    context.copyToClipboard(log)
                    PlatformServices.messages.show(
                        context.getString(R.string.pasteCrashLogGithubIssue),
                        long = true
                    )
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
