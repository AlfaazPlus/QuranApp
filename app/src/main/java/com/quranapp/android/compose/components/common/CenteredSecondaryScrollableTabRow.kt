package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CenteredSecondaryScrollableTabRow(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    tabCount: Int,
    containerColor: Color,
    minTabWidth: Dp = TabRowDefaults.ScrollableTabRowMinTabWidth,
    tab: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val tabWidthsPx = remember(tabCount) { mutableStateMapOf<Int, Int>() }

        val fallbackTotalWidth = minTabWidth * tabCount
        val measuredTotalWidth = with(density) { tabWidthsPx.values.sum().toDp() }

        val hasAllTabWidths = tabWidthsPx.size == tabCount
        val totalTabWidth = if (hasAllTabWidths) measuredTotalWidth else fallbackTotalWidth

        val edgePadding = ((maxWidth - totalTabWidth) / 2).coerceAtLeast(0.dp)

        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = containerColor,
            edgePadding = edgePadding
        ) {
            repeat(tabCount) { index ->
                tab(
                    index,
                    Modifier.onSizeChanged { size ->

                        if (tabWidthsPx[index] != size.width) {
                            tabWidthsPx[index] = size.width
                        }
                    }
                )
            }
        }
    }
}
