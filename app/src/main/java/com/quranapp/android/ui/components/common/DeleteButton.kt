package com.quranapp.android.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = dialogTitle, textAlign = TextAlign.Center) },
            text = { Text(text = dialogText, textAlign = TextAlign.Center) },
            buttons = {
                Row(
                    modifier = Modifier.background(Color.White),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDialog = false }) {
                        Text(
                            text = stringResource(id = R.string.strLabelCancel),
                            color = Color.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Divider(
                        color = Color.Gray, modifier = Modifier
                            .height(12.dp)
                            .width(1.dp)
                    )
                    TextButton(onClick = { onDelete() }) {
                        Text(
                            text = deleteButtonText,
                            color = Color(ColorUtils.DANGER),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        )
    }
}