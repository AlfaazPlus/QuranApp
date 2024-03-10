package com.quranapp.android.ui.components.homepage.featuredDua

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.ui.components.common.ExclusiveVersesText

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeaturedDuaCard(featuredItem: ExclusiveVerse, onClick: () -> Unit) {
    val context = LocalContext.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateDpAsState(targetValue = if (isPressed) 4.dp else 0.dp)

    val excluded = featuredItem.id in arrayOf(1, 2)
    val count = featuredItem.verses.size

    val title = if (!excluded) context.getString(R.string.strMsgDuaFor, featuredItem.name)
    else featuredItem.name

    val subTitle = if (featuredItem.id == 1) null
    else if (count > 1) context.getString(R.string.places, count)
    else context.getString(R.string.place, count)

    val inChapters = if (featuredItem.id == 1) null else featuredItem.inChapters

    Box(
        modifier = Modifier
            .padding(start = 10.dp, top = 15.dp, end = 2.dp, bottom = 15.dp)
            .height(150.dp)
            .width(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .height(150.dp - scale)
                .width(200.dp - scale),
            shape = RoundedCornerShape(10.dp),
            backgroundColor = colorResource(R.color.black),
            onClick = { onClick() },
            interactionSource = interactionSource
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.quran_wallpaper2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )

                ExclusiveVersesText(
                    title = title,
                    subTitle = subTitle,
                    inChapters = inChapters
                )

            }

        }

    }
}
