import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalFadingEdge(
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    length: Dp = 24.dp,
    color: Color
): Modifier = this.then(
    Modifier.drawWithContent {
        val lengthPx = length.toPx()

        drawContent()

        if (canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, Color.Transparent),
                    startY = 0f,
                    endY = lengthPx
                ),
                size = Size(size.width, lengthPx)
            )
        }

        if (canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, color),
                    startY = size.height - lengthPx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - lengthPx),
                size = Size(size.width, lengthPx)
            )
        }
    }
)

@Composable
fun Modifier.verticalFadingEdge(
    scrollState: ScrollState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier {
    val canScrollBackward by remember {
        derivedStateOf { scrollState.value > 0 }
    }
    val canScrollForward by remember {
        derivedStateOf { scrollState.value < scrollState.maxValue }
    }

    return this.verticalFadingEdge(
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        length = length,
        color = color
    )
}

@Composable
fun Modifier.verticalFadingEdge(
    listState: LazyListState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier = verticalFadingEdge(
    canScrollBackward = listState.canScrollBackward,
    canScrollForward = listState.canScrollForward,
    length = length,
    color = color
)

@Composable
fun Modifier.verticalFadingEdge(
    gridState: LazyGridState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier = verticalFadingEdge(
    canScrollBackward = gridState.canScrollBackward,
    canScrollForward = gridState.canScrollForward,
    length = length,
    color = color
)

fun Modifier.horizontalFadingEdge(
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    length: Dp = 24.dp,
    color: Color
): Modifier = drawWithContent {
    val lengthPx = length.toPx()

    drawContent()

    if (canScrollBackward) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(color, Color.Transparent),
                startX = 0f,
                endX = lengthPx
            ),
            size = Size(lengthPx, size.height)
        )
    }

    if (canScrollForward) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color),
                startX = size.width - lengthPx,
                endX = size.width
            ),
            topLeft = Offset(size.width - lengthPx, 0f),
            size = Size(lengthPx, size.height)
        )
    }
}

@Composable
fun Modifier.horizontalFadingEdge(
    scrollState: ScrollState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier {
    val canScrollBackward by remember {
        derivedStateOf { scrollState.value > 0 }
    }
    val canScrollForward by remember {
        derivedStateOf { scrollState.value < scrollState.maxValue }
    }

    return this.horizontalFadingEdge(
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        length = length,
        color = color
    )
}

@Composable
fun Modifier.horizontalFadingEdge(
    listState: LazyListState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier {
    val canScrollBackward by remember {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }

    return this.horizontalFadingEdge(
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        length = length,
        color = color
    )
}

@Composable
fun Modifier.horizontalFadingEdge(
    gridState: LazyGridState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier {
    val canScrollBackward by remember {
        derivedStateOf { gridState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { gridState.canScrollForward }
    }

    return this.horizontalFadingEdge(
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        length = length,
        color = color
    )
}