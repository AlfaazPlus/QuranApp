package com.quranapp.android.compose.extensions

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable

fun LazyGridScope.fullWidthColumn(
    span: Int,
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(
        span = { GridItemSpan(span) },
        content = content,
    )
}