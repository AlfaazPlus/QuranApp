import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.composables.verse.VerseActionType
import com.quranapp.android.composables.verse.VerseHeader
import com.quranapp.android.reader_managers.ReaderVerseDecorator
import com.quranapp.android.utils.univ.SelectableLinkMovementMethod


@Composable
fun VerseItem(
    verse: Verse,
    verseDecorator: ReaderVerseDecorator,
    isBookmarked: Boolean,
    isReciting: Boolean,
    isReference: Boolean = false,
    highlightColor: Int? = null,
    onActionSelected: (VerseActionType) -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (highlightColor != null) Color(highlightColor) else Color.Transparent,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(top = 10.dp)
    ) {
        VerseHeader(
            verse = verse,
            isBookmarked = isBookmarked,
            isReciting = isReciting,
            isReference = isReference,
            onActionSelected = onActionSelected
        )

        if (verse.arabicTextSpannable != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                factory = { ctx ->
                    AppCompatTextView(ctx).apply {
                        setTextIsSelectable(true)
                        textDirection = android.view.View.TEXT_DIRECTION_RTL
                        gravity = android.view.Gravity.START
                    }
                },
                update = { textView ->
                    textView.text = verse.arabicTextSpannable
                    verseDecorator.setTextColorArabic(textView)
                    verseDecorator.setTextSizeArabic(textView)
                }
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 5.dp),
            factory = { ctx ->
                AppCompatTextView(ctx).apply {
                    setTextIsSelectable(true)
                    movementMethod = SelectableLinkMovementMethod.getInstance()
                }
            },
            update = { textView ->
                textView.text = verse.translTextSpannable
                verseDecorator.setTextColorNonArabic(textView)
                verseDecorator.setTextSizeTransl(textView)
            }
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(R.color.colorDividerVerse)
        )
    }
}