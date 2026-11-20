package com.quranapp.android.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.SwitchItem
import com.quranapp.android.components.settings.DailyReminderSheet
import com.quranapp.android.components.settings.ListItemCategoryLabel
import com.quranapp.android.components.settings.SettingsItem
import com.quranapp.android.components.settings.TextSizeSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.advanced_settings
import com.quranapp.android.generated.resources.downloadRecitations
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_language
import com.quranapp.android.generated.resources.dr_icon_quran_script
import com.quranapp.android.generated.resources.dr_icon_theme
import com.quranapp.android.generated.resources.dr_icon_translations
import com.quranapp.android.generated.resources.ic_bell
import com.quranapp.android.generated.resources.ic_bell_ring
import com.quranapp.android.generated.resources.ic_book_open_text
import com.quranapp.android.generated.resources.ic_verse_end
import com.quranapp.android.generated.resources.ic_wrench
import com.quranapp.android.generated.resources.icon_font_size
import com.quranapp.android.generated.resources.labelArabic
import com.quranapp.android.generated.resources.labelTranslation
import com.quranapp.android.generated.resources.msgArabicTextToggle
import com.quranapp.android.generated.resources.msgKeepScreenOnWhileReading
import com.quranapp.android.generated.resources.strLabelOff
import com.quranapp.android.generated.resources.strLabelOn
import com.quranapp.android.generated.resources.strLabelSelectedCount
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strTitleAppLanguage
import com.quranapp.android.generated.resources.strTitleAppSettings
import com.quranapp.android.generated.resources.strTitleReaderSettings
import com.quranapp.android.generated.resources.strTitleScripts
import com.quranapp.android.generated.resources.strTitleSettings
import com.quranapp.android.generated.resources.strTitleTafsir
import com.quranapp.android.generated.resources.strTitleTheme
import com.quranapp.android.generated.resources.strTitleTranslations
import com.quranapp.android.generated.resources.strTitleVOTD
import com.quranapp.android.generated.resources.textSizes
import com.quranapp.android.generated.resources.titleArabicTextToggle
import com.quranapp.android.generated.resources.titleKeepScreenOnWhileReading
import com.quranapp.android.generated.resources.titleOtherSettings
import com.quranapp.android.generated.resources.wordByWord
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.appLanguageOptions

import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.preferences.VersePreferences
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.observeThemeMode
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.getQuranScriptVariantName
import com.quranapp.android.utils.tafsir.TafsirManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsMainScreen(
    showReaderSettingsOnly: Boolean
) {
    val context = LocalContext.current
    val navController = LocalSettingsNavController.current
    val coroutineScope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current

    var showDailyReminderSheet by rememberSaveable { mutableStateOf(false) }
    var showTextSizesSheet by rememberSaveable { mutableStateOf(false) }

    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)
    val selectedLanguage = remember(appLocale, systemDefaultName) {
        val selected = appLanguageOptions.firstOrNull { it.tag == appLocale.storedTag }

        when {
            selected == null -> ""
            selected.systemDefault -> systemDefaultName
            else -> selected.nativeName
        }
    }

    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    val slugs = ReaderPreferences.observeTranslations()
    val tafsirId = ReaderPreferences.observeTafsirId()
    val selectedTafsirName by produceState("", context, tafsirId) {
        tafsirId?.let {
            TafsirManager.prepare(false) {
                value = TafsirManager.getModel(tafsirId)?.name ?: ""
            }
        }
    }

    val arabicTextSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationTextSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedScriptVariant = ReaderPreferences.observeQuranScriptVariant()

    Scaffold(
        topBar = { AppBar(title = stringResource(Res.string.strTitleSettings)) }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 150.dp),
        ) {
            if (!showReaderSettingsOnly) {
                ListItemCategoryLabel(title = stringResource(Res.string.strTitleAppSettings))

                SettingsItem(
                    title = Res.string.strTitleAppLanguage,
                    subtitleStr = selectedLanguage,
                    icon = Res.drawable.dr_icon_language,
                ) { navController.navigate(SettingRoutes.LANGUAGE) }

                SettingsItem(
                    title = Res.string.strTitleTheme,
                    subtitle = ThemeUtils.resolveThemeModeLabel(ThemeUtils.observeThemeMode()),
                    icon = Res.drawable.dr_icon_theme,
                ) { navController.navigate(SettingRoutes.THEME) }

                SettingsItem(
                    title = Res.string.strTitleVOTD,
                    subtitle = if (votdEnabled) Res.string.strLabelOn else Res.string.strLabelOff,
                    icon = if (votdEnabled) Res.drawable.ic_bell_ring else Res.drawable.ic_bell,
                ) { showDailyReminderSheet = true }

                SwitchItem(
                    title = Res.string.titleArabicTextToggle,
                    subtitle = Res.string.msgArabicTextToggle,
                    checked = ReaderPreferences.observeArabicTextEnabled(),
                ) {
                    coroutineScope.launch {
                        ReaderPreferences.setArabicTextEnabled(it)
                    }
                }
            }

            ListItemCategoryLabel(title = stringResource(Res.string.strTitleReaderSettings))

            SettingsItem(
                title = Res.string.textSizes,
                icon = Res.drawable.icon_font_size,
                subtitleStr = appLocale.format(
                    $$"%1$s: %2$d%%, %3$s: %4$d%%",
                    stringResource(Res.string.labelArabic),
                    ReaderTextSizeUtils.calculateProgressText(arabicTextSizeMult),
                    stringResource(Res.string.labelTranslation),
                    ReaderTextSizeUtils.calculateProgressText(translationTextSizeMult)
                )
            ) { showTextSizesSheet = true }

            SettingsItem(
                title = Res.string.strTitleTranslations,
                icon = Res.drawable.dr_icon_translations,
                subtitleStr = stringResource(Res.string.strLabelSelectedCount, slugs.size)
            ) {
                navController.navigate(SettingRoutes.TRANSLATIONS)
            }

            SettingsItem(
                title = Res.string.strTitleTafsir,
                icon = Res.drawable.ic_book_open_text,
                subtitleStr = selectedTafsirName
            ) {
                navController.navigate(SettingRoutes.TAFSIR)
            }

            SettingsItem(
                title = Res.string.strTitleScripts,
                icon = Res.drawable.dr_icon_quran_script,
                subtitleStr = selectedScript.getQuranScriptName() +
                        (selectedScriptVariant?.let { " | ${it.getQuranScriptVariantName()}" }
                            ?: "")

            ) {
                navController.navigate(SettingRoutes.SCRIPT)
            }

            SettingsItem(
                title = Res.string.wordByWord,
                icon = Res.drawable.ic_verse_end,
            ) {
                navController.navigate(SettingRoutes.WWB)
            }

            SwitchItem(
                title = Res.string.titleKeepScreenOnWhileReading,
                subtitle = Res.string.msgKeepScreenOnWhileReading,
                checked = ReaderPreferences.observeKeepScreenOn(),
            ) {
                coroutineScope.launch {
                    ReaderPreferences.setKeepScreenOn(it)
                }
            }

            if (!showReaderSettingsOnly) {
                ListItemCategoryLabel(title = stringResource(Res.string.titleOtherSettings))

                SettingsItem(
                    title = Res.string.downloadRecitations,
                    icon = Res.drawable.dr_icon_download,
                ) {
                    navController.navigate(SettingRoutes.RECITATION_DOWNLOAD)
                }

                SettingsItem(
                    title = Res.string.advanced_settings,
                    icon = Res.drawable.ic_wrench,
                ) {
                    navController.navigate(SettingRoutes.ADVANCED_SETTINGS)
                }
            }

            TextSizeSheet(isOpen = showTextSizesSheet) {
                showTextSizesSheet = false
            }

            DailyReminderSheet(
                isOpen = showDailyReminderSheet,
                onClose = {
                    showDailyReminderSheet = false
                },
            )
        }
    }
}
