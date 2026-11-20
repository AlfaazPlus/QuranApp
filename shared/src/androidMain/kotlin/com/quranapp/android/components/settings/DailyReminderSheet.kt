package com.quranapp.android.components.settings

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
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dailyReminderMsg
import com.quranapp.android.generated.resources.dr_icon_heart_filled
import com.quranapp.android.generated.resources.msgVerseReminderNotifPermission
import com.quranapp.android.generated.resources.notification_permission
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelGotIt
import com.quranapp.android.generated.resources.strLabelOff
import com.quranapp.android.generated.resources.strLabelOn
import com.quranapp.android.generated.resources.strTitleVOTD
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.utils.scheduling.RecommendedReminderScheduler
import com.quranapp.android.utils.scheduling.VerseOfTheDayScheduler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
        Triple(true, Res.string.strLabelOn, Res.string.dailyReminderMsg),
        Triple(false, Res.string.strLabelOff, null),
    )

    LaunchedEffect(permissionState) {
        if (permissionState != null && !permissionState.status.isGranted) {
            VersePreferences.setVOTDReminderEnabled(false)
            VerseOfTheDayScheduler.cancelDailyNotification()
            RecommendedReminderScheduler.cancel()
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
        icon = Res.drawable.dr_icon_heart_filled,
        title = stringResource(Res.string.strTitleVOTD),
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
                                    VerseOfTheDayScheduler.scheduleDailyNotification()
                                    RecommendedReminderScheduler.schedule()
                                } else {
                                    VerseOfTheDayScheduler.cancelDailyNotification()
                                    RecommendedReminderScheduler.cancel()
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
        title = stringResource(Res.string.notification_permission),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelGotIt),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    permissionState?.let {
                        if (showPermissionDialog.second == true) {
                            Platform.links.openAppSettings()
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
                text = stringResource(Res.string.msgVerseReminderNotifPermission),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}
