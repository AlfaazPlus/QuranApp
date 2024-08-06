package com.quranapp.android.ui.components.common

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R

@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    @ColorRes iconColor: Int? = null,
    @StringRes title: Int,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .padding(top = 10.dp, start = 15.dp, end = 15.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (iconColor != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = colorResource(id = iconColor),
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp)
                )
            }

            Text(
                text = stringResource(id = title),
                modifier = Modifier.padding(start = 10.dp),
                color = colorResource(id = R.color.colorText),
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (onClick != null) {
            ButtonActionAlphaSmall(
                text = stringResource(id = R.string.strLabelViewAll),
                onClick = onClick
            )
        }
    }
}
