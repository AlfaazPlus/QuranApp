package com.quranapp.android.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R


@Composable
fun ChapterCard(
    chapterNo: Int,
    chapterName: String,
    chapterTrans: String?,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFavourite: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val showFavouriteIcon = onToggleFavourite != null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    start = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = if (showFavouriteIcon) 0.dp else 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chapterNo.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chapterName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!chapterTrans.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapterTrans,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ChapterIcon(chapterNo = chapterNo)

            if (showFavouriteIcon) {
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        painter = painterResource(
                            if (isFavourite) R.drawable.icon_star_filled
                            else R.drawable.icon_star_outlined
                        ),
                        contentDescription = null,
                        tint = if (isFavourite) colorScheme.primary
                        else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}