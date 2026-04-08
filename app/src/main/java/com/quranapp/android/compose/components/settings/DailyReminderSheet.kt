package com.quranapp.android.compose.components.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.utils.NavigationHelper
import com.quranapp.android.compose.utils.VerseOfTheDayScheduler
import com.quranapp.android.compose.utils.preferences.VersePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DailyReminderSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    var showPermissionDialog by remember {
        mutableStateOf<Pair<Boolean, Boolean>>(
            Pair(
                false,
                false
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val items = listOf(
        Triple(true, R.string.strLabelOn, R.string.strMsgVOTDOn),
        Triple(false, R.string.strLabelOff, null),
    )

    LaunchedEffect(permissionState) {
        if (permissionState != null && !permissionState.status.isGranted) {
            VersePreferences.setVOTDReminderEnabled(false)
            VerseOfTheDayScheduler.cancelDailyNotification(context)
        }
    }

    suspend fun validate(newStatus: Boolean): Boolean {
        if (newStatus == false) {
            return true
        }

        if (permissionState != null) {
            if (!permissionState.status.isGranted) {
                showPermissionDialog = Pair(true, !permissionState.status.shouldShowRationale)
                return false
            }
        }

        return true
    }


    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.dr_icon_heart_filled,
        title = stringResource(R.string.strTitleVOTD),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (key, title, desc) ->
                RadioItem(
                    title = title,
                    subtitle = desc,
                    selected = key == votdEnabled,
                    onClick = {
                        coroutineScope.launch {
                            if (validate(key)) {
                                VersePreferences.setVOTDReminderEnabled(key)

                                if (key == true) {
                                    VerseOfTheDayScheduler.scheduleDailyNotification(context)
                                } else {
                                    VerseOfTheDayScheduler.cancelDailyNotification(context)
                                }
                            }
                        }

                        onClose()
                    },
                )
            }
        }
    }

    AlertDialog(
        isOpen = showPermissionDialog.first,
        onClose = { showPermissionDialog = showPermissionDialog.copy(false) },
        title = stringResource(R.string.notification_permission),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(R.string.strLabelGotIt),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    permissionState?.let {
                        if (showPermissionDialog.second == true) {
                            NavigationHelper.openAppSettings(context)
                        } else {
                            it.launchPermissionRequest()
                        }
                    }
                    showPermissionDialog = showPermissionDialog.copy(false)
                }
            )
        ),
        content = {
            Text(
                text = stringResource(R.string.msgVerseReminderNotifPermission),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}