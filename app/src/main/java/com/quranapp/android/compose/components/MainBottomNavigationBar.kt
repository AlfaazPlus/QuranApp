package com.quranapp.android.compose.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReaderIndexPage
import com.quranapp.android.activities.ActivitySearch
import com.quranapp.android.compose.theme.alpha

val MainBottomNavBarHeight = 70.dp

@Composable
fun mainBottomNavigationOuterHeight(): Dp {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return MainBottomNavBarHeight + navBarBottom
}

@Composable
fun MainBottomNavigationBar() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MainBottomNavBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomItem(R.string.strLabelNavHome, R.drawable.dr_icon_home, true) {
                // noop
            }

            Box(
                modifier = Modifier
                    .height(52.dp)
                    .width(140.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .clickable {
                        context.startActivity(Intent(context, ActivityReaderIndexPage::class.java))
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.quran_kareem),
                    contentDescription = stringResource(R.string.strTitleQuran),
                    tint = colorScheme.onPrimary
                )
            }

            BottomItem(R.string.strLabelNavSearch, R.drawable.dr_icon_search, false) {
                context.startActivity(Intent(context, ActivitySearch::class.java))
            }
        }
    }
}

@Composable
private fun RowScope.BottomItem(
    title: Int,
    icon: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isActive) colorScheme.primary else colorScheme.onSurface.alpha(0.75f)

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(title),
            tint = tint
        )
        Text(
            stringResource(title),
            style = typography.labelSmall,
            color = tint
        )
    }
}