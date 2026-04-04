package com.quranapp.android.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@Composable
fun AutoScrollSheet(
    readerVm: ReaderViewModel,
    isOpen: Boolean,
    onClose: () -> Unit
) {
    val savedAutoScrollSpeed = ReaderPreferences.observeAutoScrollSpeed()
    val scope = rememberCoroutineScope()

    var autoScrollSpeed by readerVm.autoScrollSpeed
    val isScrolling = autoScrollSpeed != null

    BottomSheet(
        isOpen,
        onClose,
        title = stringResource(R.string.autoScroll)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.autoScrollSpeed),
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Slider(
                    enabled = !isScrolling,
                    value = savedAutoScrollSpeed,
                    onValueChange = {
                        scope.launch {
                            ReaderPreferences.setAutoScrollSpeed(it)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${savedAutoScrollSpeed.toInt()}x",
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }

            Button(
                onClick = {
                    if (!isScrolling) {
                        autoScrollSpeed = savedAutoScrollSpeed
                        onClose()
                    } else {
                        autoScrollSpeed = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScrolling) colorScheme.errorContainer
                    else colorScheme.primary,
                    contentColor = if (isScrolling) colorScheme.onErrorContainer
                    else colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isScrolling) stringResource(R.string.stopAutoScroll)
                    else stringResource(R.string.startAutoScroll),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}