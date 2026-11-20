package com.quranapp.android.components.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.autoBackupFileDeleted
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.ic_auto_backup
import com.quranapp.android.generated.resources.strLabelClose
import com.quranapp.android.preferences.BackupPreferences
import com.quranapp.android.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.navigation.AppRoutes

@Composable
fun AutoBackupDeletedBanner() {
    val visible = BackupPreferences.observeFileDeletedAlert()
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        onClick = {
            PlatformServices.navigator.navigate(AppRoute.Screen(AppRoutes.EXPORT_IMPORT))
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
                painter = painterResource(Res.drawable.ic_auto_backup),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )

            Text(
                text = stringResource(Res.string.autoBackupFileDeleted),
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
                    painter = painterResource(Res.drawable.dr_icon_close),
                    contentDescription = stringResource(Res.string.strLabelClose),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
