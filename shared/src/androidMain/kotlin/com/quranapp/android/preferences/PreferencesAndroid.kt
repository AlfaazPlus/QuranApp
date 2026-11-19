package com.quranapp.android.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationVersionStore
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.utils.translation.TranslationVersionStore
import com.quranapp.android.utils.wbw.WbwVersionStore
import com.quranapp.android.utils.tafsir.TafsirUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val LEGACY_SP_APP_CONFIGS = "sp_app_configs"
private const val LEGACY_KEY_APP_THEME = "key.app.theme"
private const val LEGACY_KEY_APP_LANGUAGE = "key.app.language"
private const val LEGACY_KEY_NUMERAL_SYSTEM = "key.app.numeral_system"

private const val LEGACY_SP_APP_ACTION = "sp_app_action"
private const val LEGACY_KEY_ONBOARDING_REQUIRED = "app.action.onboarding_required"

private const val LEGACY_SP_READER = "sp_reader"
private const val LEGACY_SP_TEXT_STYLE = "sp_reader_text"
private const val LEGACY_SP_TRANSL = "sp_reader_translations"
private const val LEGACY_SP_SCRIPT = "sp_reader_script"
private const val LEGACY_SP_TAFSIR = "sp_reader_tafsir"

fun migrateLegacySharedPreferences(context: Context) {
    runBlocking {
        withContext(Dispatchers.IO) {
            AppActionsPreferences.migrateFromLegacyIfNeeded(context)
            ReaderPreferences.migrateFromLegacyIfNeeded(context)
            ReaderPreferences.repairStoredPreferencesIfNeeded(context)
            migrateVersionStoresIfNeeded(context)
        }
    }
}

private suspend fun AppActionsPreferences.migrateFromLegacyIfNeeded(context: Context) {
    if (DataStoreManager.readFirst(KEY_LEGACY_MIGRATED)) return

    val sp = context.applicationContext.getSharedPreferences(
        LEGACY_SP_APP_ACTION,
        Context.MODE_PRIVATE,
    )

    DataStoreManager.edit {
        if (sp.contains(LEGACY_KEY_ONBOARDING_REQUIRED)) {
            this[KEY_ONBOARDING_REQUIRED.key] =
                sp.getBoolean(LEGACY_KEY_ONBOARDING_REQUIRED, true)
        }

        this[KEY_LEGACY_MIGRATED.key] = true
    }
}

private suspend fun AppPreferences.migrateFromLegacyIfNeeded(context: Context) {
    if (DataStoreManager.readFirst(KEY_LEGACY_APP_CONFIGS_MIGRATED)) return

    val sp = context.applicationContext.getSharedPreferences(
        LEGACY_SP_APP_CONFIGS,
        Context.MODE_PRIVATE,
    )
    val locale = sp.getString(LEGACY_KEY_APP_LANGUAGE, null)
    val numeral = sp.getString(LEGACY_KEY_NUMERAL_SYSTEM, null)
    val theme = sp.getString(LEGACY_KEY_APP_THEME, null)
    val themeAlreadyStored = DataStoreManager.contains(ThemeUtils.KEY_THEME_MODE.key)

    DataStoreManager.edit {
        if (!locale.isNullOrEmpty()) {
            this[KEY_APP_LANGUAGE.key] = locale
        }

        if (!numeral.isNullOrEmpty()) {
            this[KEY_NUMERAL_SYSTEM.key] = numeral
        }

        if (!themeAlreadyStored && !theme.isNullOrEmpty()) {
            this[ThemeUtils.KEY_THEME_MODE.key] = theme
        }

        this[KEY_LEGACY_APP_CONFIGS_MIGRATED.key] = true
    }
}

private suspend fun ReaderPreferences.migrateFromLegacyIfNeeded(context: Context) {
    if (DataStoreManager.readFirst(KEY_LEGACY_MIGRATED)) return

    val appCtx = context.applicationContext

    val spReader = appCtx.getSharedPreferences(LEGACY_SP_READER, Context.MODE_PRIVATE)
    if (spReader.contains(Keys.READER_KEY_ARABIC_TEXT_ENABLED)) {
        DataStoreManager.write(
            KEY_ARABIC_TEXT_ENABLED,
            spReader.getBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, true),
        )
    }
    if (spReader.contains(Keys.READER_KEY_AUTO_SCROLL_SPEED)) {
        DataStoreManager.write(
            KEY_AUTO_SCROLL_SPEED,
            spReader.getFloat(Keys.READER_KEY_AUTO_SCROLL_SPEED, 7f),
        )
    }

    val spTextStyle = appCtx.getSharedPreferences(LEGACY_SP_TEXT_STYLE, Context.MODE_PRIVATE)
    if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC)) {
        DataStoreManager.write(
            KEY_TEXT_SIZE_MULT_ARABIC,
            spTextStyle.getFloat(
                ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC,
                ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT,
            ),
        )
    }
    if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL)) {
        DataStoreManager.write(
            KEY_TEXT_SIZE_MULT_TRANSL,
            spTextStyle.getFloat(
                ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TRANSL,
                ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT,
            ),
        )
    }
    if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR)) {
        DataStoreManager.write(
            KEY_TEXT_SIZE_MULT_TAFSIR,
            spTextStyle.getFloat(
                ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_TAFSIR,
                ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT,
            ),
        )
    }

    val spTransl = appCtx.getSharedPreferences(LEGACY_SP_TRANSL, Context.MODE_PRIVATE)
    if (spTransl.contains(TranslUtils.KEY_TRANSLATIONS)) {
        val legacy = spTransl.getStringSet(TranslUtils.KEY_TRANSLATIONS, null)
        if (legacy != null) {
            DataStoreManager.write(KEY_TRANSLATIONS, HashSet(legacy))
        }
    }

    val spScript = appCtx.getSharedPreferences(LEGACY_SP_SCRIPT, Context.MODE_PRIVATE)
    if (spScript.contains(QuranScriptUtils.KEY_SCRIPT)) {
        val script = spScript.getString(QuranScriptUtils.KEY_SCRIPT, null)
        DataStoreManager.write(KEY_SCRIPT, script ?: QuranScriptUtils.SCRIPT_DEFAULT)
    }

    val spTafsir = appCtx.getSharedPreferences(LEGACY_SP_TAFSIR, Context.MODE_PRIVATE)
    if (spTafsir.contains(TafsirUtils.KEY_TAFSIR)) {
        val key = spTafsir.getString(TafsirUtils.KEY_TAFSIR, null)
        if (key != null) {
            DataStoreManager.write(KEY_TAFSIR, key)
        }
    }

    DataStoreManager.write(KEY_LEGACY_MIGRATED, true)
}

private suspend fun ReaderPreferences.repairStoredPreferencesIfNeeded(context: Context) {
    val storedScript = DataStoreManager.readFirst(KEY_SCRIPT)
    val storedVariant = DataStoreManager.readFirst(KEY_SCRIPT_VARIANT)
    val storedTranslations = DataStoreManager.readFirst(KEY_TRANSLATIONS)

    var working = QuranScriptUtils.validatePreferredScript(storedScript)

    if (working.isKFQPCScript()) {
        if (QuranScriptUtils.getKFQPCFontDownloadedCount(working).remaining > 0) {
            working = QuranScriptUtils.SCRIPT_DEFAULT
        }
    }

    if (working.isQuranAtlasScript()) {
        val installed = DatabaseProvider.getExternalQuranDatabase()
            .atlasWordShapeDao()
            .getBundleByKey(working) != null

        if (!installed) {
            working = QuranScriptUtils.SCRIPT_DEFAULT
        }
    }

    val allowedVariants = QuranScriptUtils.availableScripts()[working].orEmpty()
    val parsedVariant = QuranScriptVariant.fromValue(storedVariant)
    val targetVariant = when {
        parsedVariant != null && parsedVariant in allowedVariants -> storedVariant
        else -> ""
    }

    val availableTranslationSlugs =
        DatabaseProvider.getTranslationRepository().getBooks().keys
    val repairedTranslations = storedTranslations.filterTo(hashSetOf()) {
        it in availableTranslationSlugs
    }.ifEmpty {
        TranslUtils.defaultTranslationSlugs()
    }

    if (working != storedScript ||
        targetVariant != storedVariant ||
        repairedTranslations != storedTranslations
    ) {
        DataStoreManager.edit {
            if (working != storedScript) {
                this[KEY_SCRIPT.key] = working
            }
            if (targetVariant != storedVariant) {
                this[KEY_SCRIPT_VARIANT.key] = targetVariant
            }
            if (repairedTranslations != storedTranslations) {
                this[KEY_TRANSLATIONS.key] = repairedTranslations
            }
        }
    }
}

private val KEY_VERSION_STORES_MIGRATED =
    PrefKey(booleanPreferencesKey("version_stores.legacy_migrated_v1"), false)

private suspend fun migrateVersionStoresIfNeeded(context: Context) {
    if (DataStoreManager.readFirst(KEY_VERSION_STORES_MIGRATED)) return

    val versions = context.applicationContext.getSharedPreferences(
        "sp_wbw_versions",
        Context.MODE_PRIVATE,
    )
    val translation = legacyStoreText(AppFiles.savedFile("other", "translation_versions.json"))
    val recitation = legacyStoreText(
        RecitationModelManager.getRecitationsDir() / "reciter_versions.json",
    )

    DataStoreManager.edit {
        for ((key, value) in versions.all) {
            if (key.startsWith(WbwVersionStore.KEY_PREFIX) && value is Int) {
                this[intPreferencesKey(key)] = value
            }
        }

        if (!translation.isNullOrEmpty()) {
            this[TranslationVersionStore.KEY.key] = translation
        }

        if (!recitation.isNullOrEmpty()) {
            this[RecitationVersionStore.KEY.key] = recitation
        }

        this[KEY_VERSION_STORES_MIGRATED.key] = true
    }
}

private fun legacyStoreText(path: okio.Path): String? {
    if (!AppFiles.exists(path) || AppFiles.size(path) <= 0L) return null
    return runCatching { AppFiles.readText(path) }.getOrNull()
}
