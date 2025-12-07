package com.quranapp.android.composables.verse

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.utils.univ.MessageUtils

@Composable
fun VerseHeader(
    verse: Verse,
    isBookmarked: Boolean,
    isReciting: Boolean,
    isReference: Boolean = false,
    onActionSelected: (VerseActionType) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        // Quick Actions (Left flows to Right)
        if (!isReference) {
            VerseActionBar(
                verse = verse,
                isBookmarked = isBookmarked,
                isReciting = isReciting,
                onActionSelected = onActionSelected
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Verse Serial Badge (Right Aligned)
        val chapterNo = verse.chapterNo
        val verseNo = verse.verseNo
        val verseSerial = stringResource(R.string.strLabelVerseSerial, chapterNo, verseNo)

        Surface(
            color = colorResource(R.color.colorDividerVerse),
            contentColor = colorResource(R.color.colorIcon),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .clickable {
                    MessageUtils.showRemovableToast(
                        context,
                        "Verse ID copied: " + verse.id,
                        Toast.LENGTH_SHORT
                    )
                }
        ) {
            Text(
                text = verseSerial,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = dimensionResource(R.dimen.dmnCommonSize2).value.sp,
                fontFamily = FontFamily.SansSerif,
                style = androidx.compose.ui.text.TextStyle(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            )
        }
    }
}