package com.quranapp.android.compose.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySearch
import com.quranapp.android.compose.components.dialogs.SimpleTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar() {
    val context = LocalContext.current

    TopAppBar(
        modifier = Modifier.shadow(4.dp),
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
                    style = typography.titleLarge.copy(
                        fontFamily = FontFamily.Default
                    ),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        actions = {
            SimpleTooltip(text = stringResource(R.string.strLabelNavSearch)) {
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = {
                        context.startActivity(Intent(context, ActivitySearch::class.java))
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.dr_icon_search
                        ),
                        contentDescription = stringResource(R.string.strLabelNavSearch),
                        tint = colorScheme.onSurface
                    )
                }
            }

            IndexMenuButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surfaceContainer),
    )
}
