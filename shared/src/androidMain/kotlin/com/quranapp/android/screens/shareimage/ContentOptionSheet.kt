package com.quranapp.android.screens.shareimage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.arabic_and_translation
import com.quranapp.android.generated.resources.arabic_only
import com.quranapp.android.generated.resources.content
import com.quranapp.android.generated.resources.dr_icon_read_quran
import com.quranapp.android.generated.resources.translation_only
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class ShareContentOption(val value: String, val labelRes: StringResource) {
    BOTH("both", Res.string.arabic_and_translation),
    ARABIC("arabic", Res.string.arabic_only),
    TRANSLATION("translation", Res.string.translation_only);

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
        icon = Res.drawable.dr_icon_read_quran,
        title = stringResource(Res.string.content),
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
