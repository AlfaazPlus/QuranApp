package com.quranapp.android.compose.components.common

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.SimpleTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    bgColor: Color? = null,
    color: Color? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bgColor ?: Color.Unspecified,
            navigationIconContentColor = color ?: Color.Unspecified,
            titleContentColor = color ?: Color.Unspecified,
            actionIconContentColor = color ?: Color.Unspecified,
        ),
        title = {
            Text(
                text = title,
            )
        },
        navigationIcon = {
            SimpleTooltip(
                text = stringResource(R.string.strLabelBack)
            ) {
                IconButton(
                    onClick = { backPressedDispatcher?.onBackPressed() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_arrow_left),
                        contentDescription = stringResource(R.string.strLabelBack),
                    )
                }
            }
        },
        actions = actions,
    )
}