package com.quranapp.android.ui.components.homepage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.FeaturedQuranModel


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomepageCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "")

    Card(
        modifier = Modifier
            .height(150.dp)
            .width(200.dp)
            .scale(scale),
        shape = RoundedCornerShape(10.dp),
        backgroundColor = colorResource(R.color.black),
        onClick = onClick,
        interactionSource = interactionSource,
        content = content
    )
}