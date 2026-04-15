/*
 * --------------------------------------------------------------------------
 * Project: SunnahApp
 * Created by Faisal Khan (http://github.com/faisalcodes) on July 06, 2023
 *
 * Copyright (c) 2023 All rights reserved.
 * --------------------------------------------------------------------------
 */

package com.quranapp.android.compose.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun LoaderIndicator(
    size: Dp = 28.dp,
) {
    CircularProgressIndicator(
        strokeWidth = 2.dp,
        strokeCap = StrokeCap.Round,
        modifier = Modifier.size(size)
    )
}

@Composable
fun Loader(
    fill: Boolean = false,
    size: Dp = 28.dp,
) {
    if (fill) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoaderIndicator(size)
        }
    } else {
        LoaderIndicator(size)
    }
}