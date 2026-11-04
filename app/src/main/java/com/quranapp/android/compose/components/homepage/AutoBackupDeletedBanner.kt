package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityExportImport
import com.quranapp.android.backup.BackupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AutoBackupDeletedBanner() {
    val visible = BackupPreferences.observeFileDeletedAlert()
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        onClick = {
            context.startActivity(Intent(context, ActivityExportImport::class.java))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_backup),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )

            Text(
                text = stringResource(R.string.autoBackupFileDeleted),
                modifier = Modifier.weight(1f),
                style = typography.bodyMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            IconButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        BackupPreferences.dismissFileDeletedAlert()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_close),
                    contentDescription = stringResource(R.string.strLabelClose),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
