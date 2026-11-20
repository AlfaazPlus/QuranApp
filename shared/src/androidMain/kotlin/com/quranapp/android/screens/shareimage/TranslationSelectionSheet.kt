package com.quranapp.android.screens.shareimage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_translations
import com.quranapp.android.generated.resources.noItems
import com.quranapp.android.generated.resources.strTitleTranslations
import org.jetbrains.compose.resources.stringResource

@Composable
fun TranslationSelectionSheet(
    isOpen: Boolean,
    books: List<TranslationBookInfoModel>,
    selectedSlug: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_translations,
        title = stringResource(Res.string.strTitleTranslations),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (books.isEmpty()) {
                Text(
                    text = stringResource(Res.string.noItems),
                    style = typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                books.forEach { book ->
                    RadioItem(
                        titleStr = book.displayName.ifBlank { book.bookName },
                        subtitleStr = book.langName.ifBlank { null },
                        selected = book.slug == selectedSlug,
                        onClick = {
                            onSelect(book.slug)
                            onClose()
                        },
                    )
                }
            }
        }
    }
}
