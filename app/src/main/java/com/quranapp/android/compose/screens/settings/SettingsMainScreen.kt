package com.quranapp.android.compose.screens.settings

import ThemeUtils
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.settings.DailyReminderSheet
import com.quranapp.android.compose.components.settings.ListItemCategoryLabel
import com.quranapp.android.compose.components.settings.ResourceDownloadSrcSheet
import com.quranapp.android.compose.components.settings.SettingsItem
import com.quranapp.android.compose.components.settings.TextSizeSheet
import com.quranapp.android.compose.navigation.LocalSettingsNavHostController
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.utils.app.DownloadSourceUtils
import com.quranapp.android.utils.extensions.getStringArray
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.getQuranScriptVariantName
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.launch

@Composable
fun SettingsMainScreen(
    showReaderSettingsOnly: Boolean
) {
    val context = LocalContext.current
    val navController = LocalSettingsNavHostController.current
    val coroutineScope = rememberCoroutineScope()

    var showDailyReminderSheet by remember { mutableStateOf(false) }
    var showTextSizesSheet by remember { mutableStateOf(false) }
    var showResourceDownloadSrcSheet by remember { mutableStateOf(false) }

    val selectedLanguage = remember(context) {
        val availableLocalesValues = context.getStringArray(R.array.availableLocalesValues)
        val availableLocaleNames = context.getStringArray(R.array.availableLocalesNames)
        val selectedLocaleIndex = SPAppConfigs.getLocale(context).let { languageCode ->
            availableLocalesValues.indexOfFirst { it == languageCode }
        }

        if (selectedLocaleIndex > -1) {
            availableLocaleNames[selectedLocaleIndex] ?: ""
        } else {
            ""
        }
    }

    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    val slugs = ReaderPreferences.observeTranslations()
    val tafsirId = ReaderPreferences.observeTafsirId()
    val selectedTafsirName by produceState("", context, tafsirId) {
        tafsirId?.let {
            TafsirManager.prepare(context, false) {
                value = TafsirManager.getModel(tafsirId)?.name ?: ""
            }
        }
    }

    val arabicTextSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationTextSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedScriptVariant = ReaderPreferences.observeQuranScriptVariant()

    Scaffold(
        topBar = { AppBar(title = stringResource(R.string.strTitleSettings)) }
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
                ListItemCategoryLabel(title = stringResource(R.string.strTitleAppSettings))

                SettingsItem(
                    title = R.string.strTitleAppLanguage,
                    subtitleStr = selectedLanguage,
                    icon = R.drawable.dr_icon_language,
                ) { navController.navigate(SettingRoutes.LANGUAGE) }

                SettingsItem(
                    title = R.string.strTitleTheme,
                    subtitle = ThemeUtils.resolveThemeModeLabel(ThemeUtils.observeThemeMode()),
                    icon = R.drawable.dr_icon_theme,
                ) { navController.navigate(SettingRoutes.THEME) }

                SettingsItem(
                    title = R.string.strTitleVOTD,
                    subtitle = if (votdEnabled) R.string.strLabelOn else R.string.strLabelOff,
                    iconImage = {
                        Image(
                            painter = painterResource(R.drawable.dr_icon_heart_filled),
                            contentDescription = null,
                        )
                    },
                ) { showDailyReminderSheet = true }

                SwitchItem(
                    title = R.string.titleArabicTextToggle,
                    subtitle = R.string.msgArabicTextToggle,
                    checked = ReaderPreferences.observeArabicTextEnabled(),
                ) {
                    coroutineScope.launch {
                        ReaderPreferences.setArabicTextEnabled(it)
                    }
                }
            }

            ListItemCategoryLabel(title = stringResource(R.string.strTitleReaderSettings))

            SettingsItem(
                title = R.string.textSizes,
                icon = R.drawable.icon_font_size,
                subtitleStr = "${stringResource(R.string.labelArabic)}: ${
                    ReaderTextSizeUtils.calculateProgressText(
                        arabicTextSizeMult
                    )
                }%, ${stringResource(R.string.labelTranslation)}: ${
                    ReaderTextSizeUtils.calculateProgressText(
                        translationTextSizeMult
                    )
                }%"
            ) { showTextSizesSheet = true }

            SettingsItem(
                title = R.string.strTitleTranslations,
                icon = R.drawable.dr_icon_translations,
                subtitleStr = stringResource(R.string.strLabelSelectedCount, slugs.size)
            ) {
                navController.navigate(SettingRoutes.TRANSLATIONS)
            }

            SettingsItem(
                title = R.string.strTitleTafsir,
                iconImage = {
                    Image(
                        painter = painterResource(R.drawable.dr_icon_tafsir),
                        contentDescription = null,
                    )
                },
                subtitleStr = selectedTafsirName
            ) {
                navController.navigate(SettingRoutes.TAFSIR)
            }

            SettingsItem(
                title = R.string.strTitleScripts,
                icon = R.drawable.dr_icon_quran_script,
                subtitleStr = selectedScript.getQuranScriptName() +
                        (selectedScriptVariant?.let { " | ${it.getQuranScriptVariantName()}" }
                            ?: "")

            ) {
                navController.navigate(SettingRoutes.SCRIPT)
            }

            SettingsItem(
                title = R.string.downloadRecitations,
                icon = R.drawable.dr_icon_download,
            ) {
                navController.navigate(SettingRoutes.RECITATION_DOWNLOAD)
            }

            if (!showReaderSettingsOnly) {
                ListItemCategoryLabel(title = stringResource(R.string.titleOtherSettings))

                SettingsItem(
                    title = R.string.titleResourceDownloadSource,
                    icon = R.drawable.dr_icon_download,
                    subtitleStr = DownloadSourceUtils.observeCurrentSourceName(),
                ) {
                    showResourceDownloadSrcSheet = true
                }

                SettingsItem(
                    title = R.string.appLogs,
                    icon = R.drawable.dr_icon_bug,
                    subtitle = R.string.crashLogs,
                ) {
                    navController.navigate(SettingRoutes.APP_LOGS)
                }
            }

            TextSizeSheet(isOpen = showTextSizesSheet) {
                showTextSizesSheet = false
            }

            ResourceDownloadSrcSheet(isOpen = showResourceDownloadSrcSheet) {
                showResourceDownloadSrcSheet = false
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