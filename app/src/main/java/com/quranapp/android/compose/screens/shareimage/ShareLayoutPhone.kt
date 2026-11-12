package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PhoneShareLayout(
    modifier: Modifier = Modifier,
) {
    val session = LocalShareAsImage.current

    BoxWithConstraints(modifier = modifier) {
        val previewWeight = if (maxHeight < 520.dp) 0.32f else 0.46f

        Column(modifier = Modifier.fillMaxSize()) {
            ShareAsImagePreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(previewWeight),
            )

            ShareOptionsPane(
                columnCount = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - previewWeight),
            )

            ShareDownloadBar(
                canExport = session.canExport,
                isExporting = session.isExporting,
                onShare = session.onShare,
                onDownload = session.onDownload,
            )
        }
    }
}
