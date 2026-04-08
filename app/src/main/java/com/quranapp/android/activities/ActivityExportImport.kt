package com.quranapp.android.activities


import ThemeUtils
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.safeBoolean
import com.quranapp.android.api.safeFloat
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeJsonArray
import com.quranapp.android.api.safeJsonObject
import com.quranapp.android.api.safeString
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.screens.ExportImportScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.preferences.AppPreferences
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.db.bookmark.BookmarkDbHelper
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.app.ResourceDownloadProxy
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ActivityExportImport : BaseActivity() {
    private val bookmarkDbHeader = BookmarkDbHelper(this)
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportDataToFile(uri)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importData(uri)
            }
        }
    }

    private var importScopes = mapOf<String, Boolean>()
    private var importFailuresMap = mutableMapOf<String, String>()

    private var exportContent = ""

    override fun onDestroy() {
        bookmarkDbHeader.close()

        super.onDestroy()
    }

    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    ExportImportScreen(
                        importCallback = { scopes ->
                            importScopes = scopes
                            launchImportFilePicker()
                        },
                        exportCallback = { scopes ->
                            CoroutineScope(Dispatchers.IO).launch {
                                exportData(scopes)

                                withContext(Dispatchers.Main) {
                                    launchExportFilePicker()
                                }
                            }
                        },
                    )
                }
            }
        })
    }

    private fun launchImportFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importLauncher.launch(intent)
    }

    private fun launchExportFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "quranapp-exported-data-v1.json")
        }
        exportLauncher.launch(intent)
    }

    private fun importData(uri: Uri) {
        importFailuresMap = mutableMapOf()
        CoroutineScope(Dispatchers.IO).launch {
            val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: ""


            val jsonObject = JsonHelper.json.parseToJsonElement(content).jsonObject
            // val version = jsonObject.safeInt(ExportKeys.VERSION, 1)

            importV1(jsonObject, importScopes)

            // restart after 1 second
            delay(1000)

            withContext(Dispatchers.Main) {
                if (
                    importScopes.get(ExportKeys.SETTINGS) == true &&
                    jsonObject.safeJsonObject(ExportKeys.SETTINGS) != null
                ) {
                    restartMainActivity()
                }
            }
        }
    }

    private suspend fun importV1(
        jsonObject: JsonObject,
        scopes: Map<String, Boolean>
    ) {
        if (scopes.get(ExportKeys.BOOKMARKS) == true) {
            jsonObject.safeJsonArray(ExportKeys.BOOKMARKS)?.let { bookmarks ->
                importBookmarks(bookmarks) { error ->
                    importFailuresMap[ExportKeys.BOOKMARKS] = error
                }
            }

        }

        if (scopes.get(ExportKeys.SETTINGS) == true) {
            jsonObject.safeJsonObject(ExportKeys.SETTINGS)?.let { settings ->
                importSettings(settings)
            }
        }
    }

    private suspend fun importBookmarks(
        jsonArray: JsonArray,
        failureCallback: (String) -> Unit
    ) {
        try {
            val bookmarks = BookmarkModel.fromJson(jsonArray)
            bookmarkDbHeader.addMultipleBookmarks(bookmarks)
        } catch (e: Exception) {
            failureCallback(e.message ?: "Unknown error")
            withContext(Dispatchers.Main) {
                Logger.d(e)
                Log.saveError(e, "parseBookmarksAndSave")
            }
        }

    }

    private suspend fun importSettings(jsonObject: JsonObject) {

        jsonObject.safeString(ExportKeys.LOCALE)?.let {
            SPAppConfigs.setLocale(this, it)
        }

        jsonObject.safeString(ExportKeys.THEME)?.let {
            ThemeUtils.setThemeMode(it)

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(
                    when (it) {
                        ThemeUtils.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        ThemeUtils.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        ThemeUtils.THEME_MODE_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        else -> {
                            AppCompatDelegate.getDefaultNightMode()
                        }
                    }
                )
            }
        }

        jsonObject.safeString(ExportKeys.DL_SRC)?.let {
            AppPreferences.setResourceDownloadProxy(ResourceDownloadProxy.fromValue(it))
        }

        jsonObject.safeFloat(ExportKeys.READER_AUTO_SCROLL_SPEED)?.let {
            ReaderPreferences.setAutoScrollSpeed(it)
        }

        jsonObject.safeBoolean(ExportKeys.READER_ARABIC_TEXT_ENABLED)?.let {
            ReaderPreferences.setArabicTextEnabled(it)
        }

        jsonObject.safeInt(ExportKeys.READER_STYLE)?.let {
            ReaderPreferences.setReaderMode(ReaderMode.fromLegacyStyleInt(it))
        }


        jsonObject.safeFloat(ExportKeys.RECITATION_SPEED)?.let {
            RecitationPreferences.setSpeed(it)
        }

        jsonObject.safeString(ExportKeys.RECITATION_RECITER)?.let {
            RecitationPreferences.setReciterId(it)
        }

        jsonObject.safeString(ExportKeys.RECITATION_RECITER_TRANSLATION)?.let {
            RecitationPreferences.setTranslationReciterId(it)
        }

        jsonObject.safeString(ExportKeys.RECITATION_OPTION_AUDIO)?.let {
            RecitationPreferences.setAudioOption(AudioOption.fromValue(it))
        }


        jsonObject.safeFloat(ExportKeys.TEXT_SIZE_MULT_TAFSIR)?.let {
            ReaderPreferences.setTafsirTextSizeMultiplier(it)
        }

        jsonObject.safeFloat(ExportKeys.TEXT_SIZE_MULT_ARABIC)?.let {
            ReaderPreferences.setArabicTextSizeMultiplier(it)
        }

        jsonObject.safeFloat(ExportKeys.TEXT_SIZE_MULT_TRANSLATION)?.let {
            ReaderPreferences.setTranslationTextSizeMultiplier(it)
        }

        jsonObject.safeString(ExportKeys.SCRIPT_CURRENT)?.let {
            ReaderPreferences.setQuranScript(it)
        }

        jsonObject.safeString(ExportKeys.SCRIPT_VARIANT_CURRENT)?.let {
            ReaderPreferences.setQuranScriptVariant(QuranScriptVariant.fromValue(it))
        }

        jsonObject.safeString(ExportKeys.TAFSIR_CURRENT)?.let {
            ReaderPreferences.setTafsirId(it)
        }

        jsonObject.safeJsonArray(ExportKeys.TRANSLATION_CURRENT)?.let { translations ->
            val translationList = mutableSetOf<String>()
            for (i in 0 until translations.size) {
                translations[i].jsonPrimitive.contentOrNull?.let { translation ->
                    translationList.add(translation)
                }
            }

            ReaderPreferences.setTranslations(translationList)
        }

    }


    private fun exportData(scopes: Map<String, Boolean>) {
        val obj = JSONObject()

        // bookmarks
        if (scopes.get(ExportKeys.BOOKMARKS) == true) {
            val bookmarks = prepareBookmarksForExport()
            if (bookmarks != null) {
                obj.put(ExportKeys.BOOKMARKS, bookmarks)
            }
        }

        // settings
        if (scopes.get(ExportKeys.SETTINGS) == true) {
            val settings = prepareSettingsForExport()
            obj.put(ExportKeys.SETTINGS, settings)
        }

        // version
        obj.put(ExportKeys.VERSION, 1)

        exportContent = obj.toString()

    }

    private fun prepareBookmarksForExport(): JSONArray? {
        val bookmarks = bookmarkDbHeader.getBookmarks()

        if (bookmarks.isEmpty()) {
            return null
        }

        return BookmarkModel.toJson(bookmarks)
    }

    private fun prepareSettingsForExport(): JSONObject {
        val settings = JSONObject()

        settings.put(ExportKeys.LOCALE, SPAppConfigs.getLocale(this))
        settings.put(ExportKeys.THEME, ThemeUtils.getThemeMode())
        settings.put(ExportKeys.DL_SRC, AppPreferences.getResourceDownloadProxy().value)

        settings.put(ExportKeys.READER_AUTO_SCROLL_SPEED, SPReader.getAutoScrollSpeed(this))
        settings.put(ExportKeys.READER_ARABIC_TEXT_ENABLED, SPReader.getArabicTextEnabled(this))
        settings.put(ExportKeys.READER_STYLE, SPReader.getSavedReaderStyle(this))

        settings.put(ExportKeys.RECITATION_REPEAT, SPReader.getRecitationRepeatVerse(this))
        settings.put(ExportKeys.RECITATION_SPEED, SPReader.getRecitationSpeed(this))
        settings.put(ExportKeys.RECITATION_RECITER, SPReader.getSavedRecitationSlug(this))
        settings.put(
            ExportKeys.RECITATION_RECITER_TRANSLATION,
            SPReader.getSavedRecitationTranslationSlug(this)
        )
        settings.put(ExportKeys.RECITATION_SCROLL_SYNC, SPReader.getRecitationScrollSync(this))
        settings.put(ExportKeys.RECITATION_OPTION_AUDIO, SPReader.getRecitationAudioOption(this))
        settings.put(
            ExportKeys.RECITATION_CONTINUE_CHAPTER,
            SPReader.getRecitationContinueChapter(this)
        )

        settings.put(ExportKeys.TEXT_SIZE_MULT_TAFSIR, SPReader.getSavedTextSizeMultTafsir(this))
        settings.put(ExportKeys.TEXT_SIZE_MULT_ARABIC, SPReader.getSavedTextSizeMultArabic(this))
        settings.put(
            ExportKeys.TEXT_SIZE_MULT_TRANSLATION,
            SPReader.getSavedTextSizeMultTransl(this)
        )

        settings.put(ExportKeys.SCRIPT_CURRENT, SPReader.getSavedScript(this))
        settings.put(ExportKeys.TAFSIR_CURRENT, SPReader.getSavedTafsirKey(this))

        val translations = JSONArray()

        for (translation in SPReader.getSavedTranslations(this)) {
            translations.put(translation)
        }

        settings.put(ExportKeys.TRANSLATION_CURRENT, translations)

        // Add settings to the JSON object
        return settings
    }

    private fun exportDataToFile(uri: Uri) {
        if (exportContent.isBlank()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportContent.toByteArray())
            }

            withContext(Dispatchers.Main) {
                MessageUtils.showRemovableToast(
                    this@ActivityExportImport,
                    R.string.msgExportSuccess,
                    Toast.LENGTH_LONG
                )
            }
        }
    }
}

class ExportKeys {
    companion object {
        const val VERSION = "version"
        const val BOOKMARKS = "bookmarks"
        const val SETTINGS = "settings"

        // item keys
        const val LOCALE = "config.lang"
        const val THEME = "config.theme"
        const val DL_SRC = "config.dlSrc"
        const val READER_AUTO_SCROLL_SPEED = "reader.autoScrollSpeed"
        const val READER_ARABIC_TEXT_ENABLED = "reader.arabicTextEnabled"
        const val READER_STYLE = "reader.style"
        const val RECITATION_REPEAT = "rec.repeat"
        const val RECITATION_SPEED = "rec.speed"
        const val RECITATION_RECITER = "rec.reciter"
        const val RECITATION_RECITER_TRANSLATION = "rec.reciter_translation"
        const val RECITATION_SCROLL_SYNC = "rec.scroll_sync"
        const val RECITATION_OPTION_AUDIO = "rec.option_audio"
        const val RECITATION_CONTINUE_CHAPTER = "rec.continue_chapter"
        const val TEXT_SIZE_MULT_TAFSIR = "text.size_mult_tafsir"
        const val TEXT_SIZE_MULT_TRANSLATION = "text.size_mult_translation"
        const val TEXT_SIZE_MULT_ARABIC = "text.size_mult_arabic"
        const val SCRIPT_CURRENT = "script.current"
        const val SCRIPT_VARIANT_CURRENT = "script_variant.current"
        const val TAFSIR_CURRENT = "tafsir.current"
        const val TRANSLATION_CURRENT = "translation.current"
    }
}