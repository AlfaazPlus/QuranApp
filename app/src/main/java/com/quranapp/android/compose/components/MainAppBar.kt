package com.quranapp.android.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar() {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_logo),
                    contentDescription = stringResource(R.string.app_name),
                    tint = null,
                )

                Text(
                    text = stringResource(R.string.app_name),
                    style = typography.titleLarge,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        actions = {
            IndexMenuButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface),
    )
}