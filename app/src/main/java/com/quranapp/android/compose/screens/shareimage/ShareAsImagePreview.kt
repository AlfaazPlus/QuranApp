package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withScale
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.utils.share.VerseFitStage

@Composable
fun ShareAsImagePreview(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val session = LocalShareAsImage.current
    val template = session.selectedTemplate
    val scene = session.scene
    val previewBg = session.previewBgBitmap

    Surface(
        modifier = modifier,
        color = colorScheme.surfaceContainer,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            val aspect = (template?.layout?.aspectRatio ?: 1f).coerceIn(0.25f, 4f)
            val availW = maxWidth.coerceAtLeast(1.dp)
            val availH = maxHeight.coerceAtLeast(1.dp)

            val (cardW, cardH) = if (availW / availH > aspect) {
                (availH * aspect) to availH
            } else {
                availW to (availW / aspect)
            }

            val density = LocalDensity.current
            val cardWidthPx = with(density) { cardW.roundToPx() }
            LaunchedEffect(cardWidthPx) {
                session.onPreviewWidthPx(cardWidthPx)
            }

            Box(
                modifier = Modifier
                    .size(width = cardW, height = cardH)
                    .shadow(elevation = 6.dp, shape = RectangleShape)
                    .background(colorScheme.surfaceContainerHighest)
                    .border(
                        width = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RectangleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    (session.isTemplatesLoading && template == null) ||
                        (template != null && scene == null && session.verse == null) -> {
                        Loader()
                    }

                    scene != null -> {
                        val activeBitmap = if (scene.templateId == template?.id) previewBg else null

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 0f && size.height > 0f) {
                                val sx = size.width / scene.canvasWidth.toFloat()
                                val sy = size.height / scene.canvasHeight.toFloat()

                                drawIntoCanvas { composeCanvas ->
                                    composeCanvas.nativeCanvas.withScale(sx, sy) {
                                        scene.draw(
                                            canvas = this,
                                            bgBitmap = activeBitmap,
                                            allowBlockingTextureLoad = false,
                                        )
                                    }
                                }
                            }
                        }

                        if (session.isPreviewBgLoading && activeBitmap == null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp),
                            ) {
                                Loader(size = 16.dp)
                            }
                        }

                        if (scene.fitResult.stage == VerseFitStage.REJECTED) {
                            PreviewStatusOverlay(
                                message = stringResource(R.string.shareImageFitRejected),
                                isError = true,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp),
                            )
                        }
                    }

                    else -> {
                        Loader()
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewStatusOverlay(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isError) {
        colorScheme.errorContainer.copy(alpha = 0.94f)
    } else {
        Color.Black.copy(alpha = 0.72f)
    }
    val textColor = if (isError) {
        colorScheme.onErrorContainer
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = message,
            style = typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
