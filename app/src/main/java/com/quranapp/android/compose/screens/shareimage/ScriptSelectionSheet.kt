package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.utils.reader.getQuranScriptName

@Composable
fun ScriptSelectionSheet(
    isOpen: Boolean,
    scripts: List<String>,
    selectedScript: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.dr_icon_quran_script,
        title = stringResource(R.string.strTitleScripts),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (scripts.isEmpty()) {
                Text(
                    text = stringResource(R.string.noItems),
                    style = typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                scripts.forEach { script ->
                    RadioItem(
                        titleStr = script.getQuranScriptName(),
                        selected = script == selectedScript,
                        onClick = {
                            onSelect(script)
                            onClose()
                        },
                    )
                }
            }
        }
    }
}
