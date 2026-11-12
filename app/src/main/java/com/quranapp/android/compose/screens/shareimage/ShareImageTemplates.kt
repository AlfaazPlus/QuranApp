package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.quranapp.android.R
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.utils.share.VerseTemplate
import com.quranapp.android.utils.share.VerseTemplateManager

private val thumbScrim = Brush.verticalGradient(
    listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.55f),
    ),
)

@Composable
fun TemplatesGrid(
    templates: List<VerseTemplate>,
    columnCount: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    state: LazyGridState,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(columnCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = templates,
            key = { it.id },
        ) { item ->
            TemplateThumb(
                template = item,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TemplateThumb(
    template: VerseTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val downloadProxy = AppPreferences.observeResourceDownloadProxy()
    val thumbPx = with(density) { 112.dp.roundToPx() }.coerceIn(96, 256)

    val imageData = remember(template.id, template.bgSrc, downloadProxy, context) {
        VerseTemplateManager.resolveTemplateImageModel(context, template)
    }

    val imageRequest = remember(imageData, thumbPx, context) {
        ImageRequest.Builder(context)
            .data(imageData)
            .size(thumbPx)
            .build()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shapes.medium)
                .background(colorScheme.surfaceContainerHighest)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                    shape = shapes.medium,
                )
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = template.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(thumbScrim)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = template.layout.aspectRatioRaw,
                    style = typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_check),
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Text(
            text = template.name,
            style = typography.labelSmall,
            color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
