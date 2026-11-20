package com.quranapp.android.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.app_name
import com.quranapp.android.generated.resources.dr_icon_search
import com.quranapp.android.generated.resources.dr_logo
import com.quranapp.android.generated.resources.strLabelNavSearch
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                    painter = painterResource(Res.drawable.dr_logo),
                    contentDescription = stringResource(Res.string.app_name),
                    tint = null,
                )

                Text(
                    text = stringResource(Res.string.app_name),
                    style = typography.titleLarge.copy(
                        fontFamily = FontFamily.Default
                    ),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        actions = {
            SimpleTooltip(text = stringResource(Res.string.strLabelNavSearch)) {
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = {
                        Platform.navigator.navigate(AppRoute.Screen(AppRoutes.SEARCH))
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            Res.drawable.dr_icon_search
                        ),
                        contentDescription = stringResource(Res.string.strLabelNavSearch),
                        tint = colorScheme.onSurface
                    )
                }
            }

            IndexMenuButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surfaceContainer),
    )
}
