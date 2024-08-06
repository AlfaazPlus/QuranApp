package com.quranapp.android.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R

@Composable
fun ExclusiveVersesText(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    inChapters: String? = null
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //title
        Text(
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = dimensionResource(id = R.dimen.dmnCommonSizeLarge).value.sp,
            color = colorResource(id = R.color.white),
            textAlign = TextAlign.Center,
        )
        //subTitle
        if (subTitle != null) {
            Text(
                text = subTitle,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = dimensionResource(id = R.dimen.dmnCommonSize2).value.sp,
                color = Color(0xFFD0D0D0),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        //inChapters
        if (inChapters != null) {
            Text(
                text = inChapters,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Normal,
                fontSize = dimensionResource(id = R.dimen.dmnCommonSize2).value.sp,
                color = Color(0xFFD0D0D0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

    }
}