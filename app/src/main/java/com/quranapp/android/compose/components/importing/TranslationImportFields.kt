package com.quranapp.android.compose.components.importing

import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.AlertType
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.importing.ImportFieldId
import com.quranapp.android.utils.reader.translation.importLanguages
import com.quranapp.android.utils.reader.translation.translationSlug

fun translationImportFields() = listOf(
    ImportField(ImportFieldId.BOOK, R.string.labelBook, required = true),
    ImportField(ImportFieldId.AUTHOR, R.string.labelAuthor, required = true),
    ImportField(ImportFieldId.DISPLAY_NAME, R.string.labelDisplayName, required = true),
    ImportField(
        id = ImportFieldId.LANG_CODE,
        label = R.string.labelLanguage,
        required = true,
        choices = importLanguages().map { ImportChoice(it.code, it.name) },
    ),
)

@Composable
fun TranslationImportDetails(
    values: Map<ImportFieldId, String>,
    existingSlugs: Set<String>,
) {
    val slug = translationSlug(
        values[ImportFieldId.LANG_CODE].orEmpty(),
        values[ImportFieldId.BOOK].orEmpty(),
    ) ?: return
    if (slug !in existingSlugs || TranslUtils.isPrebuilt(slug)) return

    AlertCard(type = AlertType.Warning) {
        Text(
            text = stringResource(R.string.importTranslationReplace),
            style = typography.bodyMedium,
        )
    }
}
