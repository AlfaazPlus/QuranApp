package com.quranapp.android.components.homepage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.core.R
import com.quranapp.android.utils.app.UpdateManager

@Composable
fun AppUpdateRequiredDialog() {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager.getInstance(context) }
    val message = stringResource(R.string.strMsgUpdateAvailable2Continue)
    val splitAt = message.indexOf('\n')
    val title = if (splitAt >= 0) message.substring(0, splitAt) else message
    val body = if (splitAt >= 0) message.substring(splitAt + 1).trim() else null

    AlertDialog(
        isOpen = true,
        onClose = {},
        title = title,
        properties = DialogProperties(
            windowTitle = title,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelUpdate),
                style = AlertDialogActionStyle.Danger,
                dismissOnClick = false,
                onClick = updateManager::openPlayStore,
            ),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedUpdateAppIcon(Modifier.size(70.dp))

            if (!body.isNullOrEmpty()) {
                Text(
                    text = body,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface,
                )
            }
        }
    }
}
