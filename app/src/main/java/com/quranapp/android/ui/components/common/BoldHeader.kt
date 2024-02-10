package com.quranapp.android.ui.components.common

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R

@Composable
fun BoldHeader(
    text: String,
    deleteButton: @Composable() (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .background(colorResource(id = R.color.colorBGPage))
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp)
            .height(dimensionResource(id = R.dimen.dmnAppBarHeight)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        Image(
            painter = painterResource(id = R.drawable.dr_icon_arrow_left),
            contentDescription = stringResource(id = R.string.strLabelBack),
            modifier = Modifier
                .background(Color.Transparent)
                .height(25.dp)
                .width(25.dp)
                .rotate(integerResource(id = R.integer.intActionBtnRotation).toFloat())
                .clip(CircleShape)
                .clickable {
                    onBackPressedDispatcher?.onBackPressed()
                }
        )
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        if (deleteButton != null) {
            deleteButton()
        }
    }

}