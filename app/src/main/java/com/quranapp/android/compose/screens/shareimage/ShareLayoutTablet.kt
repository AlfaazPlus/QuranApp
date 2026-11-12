package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TabletShareLayout(
    modifier: Modifier = Modifier,
) {
    val session = LocalShareAsImage.current

    BoxWithConstraints(modifier = modifier) {
        val roomy = maxWidth >= 840.dp && maxHeight >= 480.dp
        val columnCount = if (maxWidth >= 840.dp) 4 else 3
        val outerInset = if (roomy) 32.dp else 16.dp

        Row(modifier = Modifier.fillMaxSize()) {
            ShareOptionsPane(
                columnCount = columnCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.56f),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f),
            ) {
                ShareAsImagePreview(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(outerInset),
                )

                ShareDownloadBar(
                    canExport = session.canExport,
                    isExporting = session.isExporting,
                    onShare = session.onShare,
                    onDownload = session.onDownload,
                    contentPadding = PaddingValues(
                        start = outerInset,
                        end = outerInset,
                        bottom = outerInset,
                    ),
                )
            }
        }
    }
}
