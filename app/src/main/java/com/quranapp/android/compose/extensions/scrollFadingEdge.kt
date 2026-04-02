import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalFadingEdge(
    scrollState: ScrollState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier = composed {
    val lengthPx = with(LocalDensity.current) { length.toPx() }

    drawWithContent {
        drawContent()

        val topFade = minOf(scrollState.value.toFloat(), lengthPx)
        val bottomFade = minOf(
            (scrollState.maxValue - scrollState.value).toFloat(),
            lengthPx
        )

        if (scrollState.value > 0) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, Color.Transparent),
                    startY = 0f,
                    endY = topFade
                ),
                size = Size(size.width, topFade)
            )
        }

        if (scrollState.value < scrollState.maxValue) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, color),
                    startY = size.height - bottomFade,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - bottomFade),
                size = Size(size.width, bottomFade)
            )
        }
    }
}

@Composable
fun Modifier.horizontalFadingEdge(
    scrollState: ScrollState,
    length: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.background
): Modifier = composed {
    val lengthPx = with(LocalDensity.current) { length.toPx() }

    drawWithContent {
        drawContent()

        val leftFade = minOf(scrollState.value.toFloat(), lengthPx)
        val rightFade = minOf(
            (scrollState.maxValue - scrollState.value).toFloat(),
            lengthPx
        )

        if (scrollState.value > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(color, Color.Transparent),
                    startX = 0f,
                    endX = leftFade
                ),
                size = Size(leftFade, size.height)
            )
        }

        if (scrollState.value < scrollState.maxValue) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color),
                    startX = size.width - rightFade,
                    endX = size.width
                ),
                topLeft = Offset(size.width - rightFade, 0f),
                size = Size(rightFade, size.height)
            )
        }
    }
}