package com.quranapp.android.compose.components.reader.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.components.common.Chip
import com.quranapp.android.compose.components.dialogs.BottomSheetHeader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.utils.univ.ResUtils
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.launch
import java.util.Locale

private val footnoteTagPattern = Regex("""(?s)<fn.*?>(.*?)<.*?fn>""")

private data class VerseShareState(
    val useVerseRange: Boolean = false,
    val fromVerseText: String = "",
    val toVerseText: String = "",
    val whatsappStyling: Boolean = false,
    val includeArabic: Boolean = true,
    val includeFootnotes: Boolean = false,
    val includeBookmarkNote: Boolean = false,
    val selectedSlugs: Set<String> = setOf(),
)

private typealias UpdateVerseShareState = (VerseShareState.() -> VerseShareState) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseShareSheet(
    vwd: VerseWithDetails?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (vwd == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val translFactory = QuranTranslationFactory.remember(context)

    val translationBooks = remember(translFactory) {
        translFactory.getAvailableTranslationBooksInfo()
    }

    var state by remember { mutableStateOf(VerseShareState()) }

    val updateState: UpdateVerseShareState = { transform ->
        state = state.transform()
    }

    LaunchedEffect(vwd) {
        updateState { VerseShareState() }
    }

    LaunchedEffect(translationBooks) {
        val first = translationBooks.keys.firstOrNull()

        updateState {
            copy(
                selectedSlugs = if (first != null) setOf(first) else emptySet()
            )
        }
    }

    val clipboardMsg = stringResource(R.string.copiedToClipboard)
    val shareTitle = stringResource(R.string.strTitleShareVerse)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = stringResource(R.string.strTitleShareVerse),
                hasDragHandle = true,
            )

            Column(
                modifier = Modifier
                    .weight(1f, false)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AdvancedShareForm(
                    vwd = vwd,
                    translationBooks = translationBooks,
                    state,
                    updateState = updateState
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.strLabelCancel))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val text = buildShareText(
                                context,
                                translFactory,
                                state = state,
                                vwd = vwd,
                            )

                            if (text.isNullOrEmpty()) return@launch

                            context.copyToClipboard(text)

                            MessageUtils.showClipboardMessage(
                                context,
                                clipboardMsg
                            )
                        }

                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.strLabelCopy))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val text = buildShareText(
                                context,
                                translFactory = translFactory,
                                state = state,
                                vwd = vwd,
                            )

                            if (text.isNullOrEmpty()) return@launch

                            AppBridge.newSharer(context)
                                .setData(text)
                                .setChooserTitle(shareTitle)
                                .setPlatform(AppBridge.Platform.SYSTEM_SHARE)
                                .share()

                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                    )
                ) {
                    Text(stringResource(R.string.strLabelShare))
                }
            }
        }
    }
}

@Composable
private fun AdvancedShareForm(
    vwd: VerseWithDetails,
    translationBooks: Map<String, TranslationBookInfoModel>,
    state: VerseShareState,
    updateState: UpdateVerseShareState,
) {
    Text(
        text = stringResource(R.string.strLabelSelectVerse),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        Chip(
            selected = !state.useVerseRange,
            label = { Text(stringResource(R.string.strLabelCurrentVerse)) },
        ) {
            updateState {
                copy(useVerseRange = false)
            }
        }
        Chip(
            selected = state.useVerseRange,
            label = { Text(stringResource(R.string.strLabelVerseRange)) },
        ) {
            updateState {
                copy(useVerseRange = true)
            }
        }
    }

    if (state.useVerseRange) {
        Text(
            text = formattedStringResource(
                R.string.strMsgShareRange,
                1,
                vwd.chapter.surah.ayahCount
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            style = typography.bodyMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.fromVerseText,
                onValueChange = {
                    updateState {
                        copy(fromVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.strHintFromVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = state.toVerseText,
                onValueChange = {
                    updateState {
                        copy(toVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.strHintToVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }

    CheckboxRow(
        checked = state.whatsappStyling,
        onCheckedChange = {
            updateState {
                copy(whatsappStyling = it)
            }
        },
        label = stringResource(R.string.strLabelWhatsappStyling),
    )

    CheckboxRow(
        checked = state.includeArabic,
        onCheckedChange = {
            updateState {
                copy(includeArabic = it)
            }
        },
        label = stringResource(R.string.strLabelIncludeArabic),
    )

    CheckboxRow(
        checked = state.includeFootnotes,
        onCheckedChange = {
            updateState {
                copy(includeFootnotes = it)
            }
        },
        label = stringResource(R.string.strLabelIncludeFootnotes),
    )

    CheckboxRow(
        checked = state.includeBookmarkNote,
        onCheckedChange = {
            updateState {
                copy(includeBookmarkNote = it)
            }
        },
        label = stringResource(R.string.includeBookmarkNote),
    )

    Text(
        text = stringResource(R.string.strLabelSelectTranslations),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    translationBooks.forEach { (slug, book) ->
        val checked = slug in state.selectedSlugs


        CheckboxRow(
            checked,
            onCheckedChange = {
                updateState {
                    copy(
                        selectedSlugs = if (it) selectedSlugs + slug else selectedSlugs - slug,
                    )
                }
            },
            label = book.getDisplayName(true),
        )
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(0.dp)
        )

        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun resolveVerseRange(
    state: VerseShareState,
    defaultVerse: Int,
): Pair<Int?, Int?> {
    if (!state.useVerseRange) {
        return defaultVerse to defaultVerse
    }

    val from = state.fromVerseText.trim().toIntOrNull()
    val to = state.toVerseText.trim().toIntOrNull()

    if (from == null || to == null) return null to null

    return from to to
}

private suspend fun buildShareText(
    context: Context,
    translFactory: QuranTranslationFactory,
    state: VerseShareState,
    vwd: VerseWithDetails,
): String? {
    val proceed = state.selectedSlugs.isNotEmpty() || state.includeArabic

    if (!proceed) return null


    val (fromVerse, toVerse) = resolveVerseRange(
        state = state,
        defaultVerse = vwd.verseNo,
    )

    if (fromVerse == null || toVerse == null) {
        Toast.makeText(
            context,
            R.string.strMsgEnterValidRange,
            Toast.LENGTH_LONG,
        ).show()
        return null
    }

    if (!vwd.chapter.isVerseRangeValid(fromVerse, toVerse)) {
        Toast.makeText(
            context,
            R.string.strMsgEnterValidRange,
            Toast.LENGTH_LONG,
        ).show()
        return null
    }

    val sb = StringBuilder()

    for (verseNo in fromVerse..toVerse) {
        appendVerseBlock(
            context = context,
            sb = sb,
            translFactory = translFactory,
            chapterNo = vwd.chapterNo,
            verseNo = verseNo,
            isSingleVerse = fromVerse == toVerse,
            state,
        )

        if (verseNo < toVerse) {
            sb.append("\n\n")
            StringUtils.replicate(sb, StringUtils.HYPHEN, 10)
            sb.append("\n\n")
        }
    }

    return sb.toString()
}

private suspend fun appendVerseBlock(
    context: Context,
    sb: StringBuilder,
    translFactory: QuranTranslationFactory,
    chapterNo: Int,
    verseNo: Int,
    isSingleVerse: Boolean,
    state: VerseShareState,
) {
    val repository = DatabaseProvider.getQuranRepository(context)

    val transls = translFactory.getTranslationsSingleVerse(state.selectedSlugs, chapterNo, verseNo)
    val hasMultiTransls = transls.size > 1

    if (!isSingleVerse || hasMultiTransls) {
        sb.append("(Quran, Verse ").append(chapterNo).append(":").append(verseNo).append(")")
    }

    if (state.includeArabic) {
        val words = repository.getWordsForAyah(chapterNo, verseNo, QuranScriptUtils.SCRIPT_UTHMANI)
        val text = words.joinToString(" ") { it.text }

        if (text.isNotEmpty()) {
            sb.append("\n\n").append(text)
        }
    }

    for (i in transls.indices) {
        sb.append(
            makeTranslText(
                context,
                translFactory,
                transls[i],
                hasMultiTransls,
                state.includeFootnotes,
                state.whatsappStyling
            )
        )
    }

    if (isSingleVerse && !hasMultiTransls) {
        sb.append("\n\n").append("${StringUtils.HYPHEN} Quran $chapterNo:$verseNo")
    }

    if (state.includeBookmarkNote) {
        val userRepository = DatabaseProvider.getUserRepository(context)

        userRepository.getBookmark(chapterNo, verseNo, verseNo)?.let { bookmark ->
            val bookmarkNote = bookmark.note?.trim().orEmpty()

            if (bookmarkNote.isNotBlank()) {
                sb.append("\n\n")

                appendBold(sb, "Notes:", state.whatsappStyling)
                    .append("\n")
                    .append(bookmarkNote)
            }
        }
    }
}

private fun makeTranslText(
    context: Context,
    translFactory: QuranTranslationFactory,
    transl: Translation,
    inclAuthor: Boolean,
    inclFootnotes: Boolean,
    whatsappStyle: Boolean,
): CharSequence {
    val sb = StringBuilder()
    sb.append("\n\n").append(cleanHtml(transl.text, inclFootnotes))

    val footnotes = transl.footnotes
    val bookInfo = translFactory.getTranslationBookInfo(transl.bookSlug)
    val hasFootnotes = footnotes.isNotEmpty()

    if (inclAuthor || (inclFootnotes && hasFootnotes)) {
        val author = bookInfo.getDisplayName(false)

        sb.append("\n")

        appendItalic(sb, author, whatsappStyle)
    }

    if (inclFootnotes && hasFootnotes) {
        sb.append("\n\n")


        val title =
            ResUtils.getLocalizedString(
                context,
                R.string.strTitleFootnotes,
                Locale.forLanguageTag(bookInfo.langCode)
            ) ?: "FOOTNOTES"

        appendBold(sb, title, whatsappStyle)

        footnotes.entries.sortedBy { it.key }.forEach { (number, footnote) ->
            sb.append("\n\t\t[").append(number).append("] ")
            sb.append(
                HtmlCompat.fromHtml(footnote.text, HtmlCompat.FROM_HTML_MODE_LEGACY),
            )
        }
    }
    return sb
}

private fun cleanHtml(string: String, inclFootnotes: Boolean): String {
    val replaced = footnoteTagPattern.replace(string) { match ->
        if (inclFootnotes) "[${match.groupValues[1]}]" else ""
    }

    return HtmlCompat.fromHtml(replaced, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}

private fun appendBold(
    sb: StringBuilder,
    text: CharSequence,
    whatsappStyle: Boolean
): StringBuilder {
    if (whatsappStyle) {
        sb.append('*').append(text).append('*')
    } else {
        sb.append(text)
    }

    return sb
}

private fun appendItalic(
    sb: StringBuilder,
    text: CharSequence,
    whatsappStyle: Boolean
): StringBuilder {
    if (whatsappStyle) {
        sb.append('_').append(text).append('_')
    } else {
        sb.append(text)
    }

    return sb
}
