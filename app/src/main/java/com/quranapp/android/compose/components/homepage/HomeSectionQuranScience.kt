package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityQuranScience

@Composable
fun HomeSectionQuranScience() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .height(150.dp)
            .padding(10.dp)
            .clip(shapes.medium)
            .clickable {
                context.startActivity(Intent(context, ActivityQuranScience::class.java))
            }
    ) {
        Image(
            painter = painterResource(R.drawable.quran_science_wallpaper),
            contentDescription = stringResource(R.string.quran_and_science),
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.16f))
        )

        Text(
            text = stringResource(R.string.quran_and_science),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp),
            style = typography.titleLarge,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}