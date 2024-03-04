package com.quranapp.android.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.peacedesign.android.utils.ColorUtils
import com.quranapp.android.R

@Composable
fun DeleteButton(
    imageDescription: String = "",
    dialogTitle: String,
    dialogText: String,
    deleteButtonText: String,
    onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Image(
        painter = painterResource(id = R.drawable.dr_icon_delete),
        contentDescription = imageDescription,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                showDialog = !showDialog
            }
    )

    if (showDialog) {
        DeleteDialog(
            dialogTitle = dialogTitle,
            dialogText = dialogText,
            deleteButtonText = deleteButtonText,
            onDelete = onDelete,
            onDismiss = {showDialog = false}
        )
    }
}

@Composable
fun DeleteDialog(
    dialogTitle: String,
    dialogText: String,
    deleteButtonText: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = colorResource(id = R.color.colorBGHomePageItem),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorText),
                    modifier = Modifier.padding(10.dp)
                )
                Text(
                    text = dialogText,
                    color = colorResource(id = R.color.colorText),
                    modifier = Modifier.padding(bottom = 18.dp)
                )
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.strLabelCancel),
                            color = colorResource(id = R.color.colorText),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Divider(
                        color = Color.Gray, modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                    )
                    TextButton(
                        onClick = { onDelete() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = deleteButtonText,
                            color = Color(ColorUtils.DANGER),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }


    }
}