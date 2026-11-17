package com.quranapp.android.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.WaitingDialog
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelClose
import com.quranapp.android.generated.resources.strMsgCouldNotOpenPage
import com.quranapp.android.generated.resources.strMsgSomethingWrong
import com.quranapp.android.generated.resources.strMsgTryLater
import com.quranapp.android.utils.app.InfoUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Opens an [InfoUtils] URL and hosts the waiting and error dialogs.
 * Call this above any early return so the dialogs stay composed while a link loads.
 */
@Composable
fun rememberInfoLinks(): (urlKey: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var showError by remember { mutableStateOf(false) }

    WaitingDialog(
        isOpen = job != null,
        onClose = {
            job?.cancel()
            job = null
        },
        actions = listOf(AlertDialogAction(stringResource(Res.string.strLabelCancel))),
    )

    AlertDialog(
        isOpen = showError,
        onClose = { showError = false },
        title = stringResource(Res.string.strMsgSomethingWrong),
        actions = listOf(AlertDialogAction(stringResource(Res.string.strLabelClose))),
    ) {
        Text(
            stringResource(Res.string.strMsgCouldNotOpenPage) + " " +
                    stringResource(Res.string.strMsgTryLater)
        )
    }

    return { urlKey ->
        job?.cancel()
        val next = scope.launch {
            try {
                InfoUtils.open(context, urlKey)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showError = true
            }
        }
        next.invokeOnCompletion {
            if (job === next) job = null
        }
        if (next.isActive) job = next
    }
}
