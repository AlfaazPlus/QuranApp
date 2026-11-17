package com.quranapp.android.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.msgResourceDownloadSource
import com.quranapp.android.generated.resources.titleResourceDownloadSource
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.app.ResourceDownloadProxy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ResourceDownloadSrcSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    val currentDownloadSrc = AppPreferences.observeResourceDownloadProxy()

    val items = listOf(
        Pair(ResourceDownloadProxy.ALFAAZ_PLUS, "AlfaazPlus"),
        Pair(ResourceDownloadProxy.GITHUB, "GitHub Raw"),
        Pair(ResourceDownloadProxy.JSDELIVR, "JsDelivr"),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_download,
        title = stringResource(Res.string.titleResourceDownloadSource),
    ) {
        AlertCard(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(Res.string.msgResourceDownloadSource),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (downloadSrc, title) ->
                RadioItem(
                    titleStr = title,
                    subtitleStr = DownloadSourceUtils.getDownloadSourceName(downloadSrc),
                    selected = currentDownloadSrc == downloadSrc,
                    onClick = {
                        scope.launch {
                            DownloadSourceUtils.setDownloadSource(downloadSrc)
                        }
                        onDismiss()
                    },
                )
            }
        }
    }
}
