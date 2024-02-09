package com.quranapp.android.ui.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R

@Composable
fun ButtonActionAlpha(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    TextButton(
        modifier = modifier.padding(15.dp, 8.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = R.color.colorPrimaryAlpha20),
        ),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = dimensionResource(id = R.dimen.dmnCommonSize1_5).value.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun ButtonActionAlphaSmall(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    TextButton(
        modifier = modifier.padding(10.dp, 5.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = R.color.colorPrimaryAlpha20)
        ),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = dimensionResource(id = R.dimen.dmnCommonSize3).value.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
            )
        )
    }

}