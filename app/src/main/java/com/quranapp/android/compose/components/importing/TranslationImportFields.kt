package com.quranapp.android.compose.components.importing

import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.AlertType
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.importing.ImportFieldId
import com.quranapp.android.utils.reader.translation.importLanguages
import com.quranapp.android.utils.reader.translation.isCustomImportSlug
import com.quranapp.android.utils.reader.translation.translationSlug

fun translationImportFields() = listOf(
    ImportField(ImportFieldId.BOOK, "Book", required = true),
    ImportField(ImportFieldId.AUTHOR, "Author", required = true),
    ImportField(ImportFieldId.DISPLAY_NAME, "Short name", required = true),
    languageImportField(),
)

fun tafsirImportFields() = listOf(
    ImportField(ImportFieldId.BOOK, "Book", required = true),
    ImportField(ImportFieldId.AUTHOR, "Author", required = true),
    languageImportField(),
)

private fun languageImportField() = ImportField(
    id = ImportFieldId.LANG_CODE,
    label = "Language",
    required = true,
    choices = importLanguages().map { ImportChoice(it.code, it.name) },
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
            text = "This translation already exists and will be replaced.",
            style = typography.bodyMedium,
        )
    }
}

@Composable
fun TafsirImportDetails(
    values: Map<ImportFieldId, String>,
    existingKeys: Set<String>,
) {
    val slug = translationSlug(
        values[ImportFieldId.LANG_CODE].orEmpty(),
        values[ImportFieldId.BOOK].orEmpty(),
    ) ?: return
    
    if (slug !in existingKeys || !isCustomImportSlug(slug)) return

    AlertCard(type = AlertType.Warning) {
        Text(
            text = "This tafsir already exists and will be replaced.",
            style = typography.bodyMedium,
        )
    }
}
