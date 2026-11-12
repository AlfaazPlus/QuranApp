package com.quranapp.android.compose.screens.shareimage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet

enum class ShareContentOption(val value: String, val labelRes: Int) {
    BOTH("both", R.string.arabic_and_translation),
    ARABIC("arabic", R.string.arabic_only),
    TRANSLATION("translation", R.string.translation_only);

    val showArabic: Boolean
        get() = this != TRANSLATION

    val showTranslation: Boolean
        get() = this != ARABIC

    companion object {
        val DEFAULT = BOTH

        fun fromValue(value: String): ShareContentOption {
            return entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }
}

@Composable
fun ShareContentOptionSheet(
    isOpen: Boolean,
    selected: ShareContentOption,
    onSelect: (ShareContentOption) -> Unit,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.dr_icon_read_quran,
        title = stringResource(R.string.content),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
        ) {
            ShareContentOption.entries.forEach { option ->
                RadioItem(
                    title = option.labelRes,
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        onClose()
                    },
                )
            }
        }
    }
}
