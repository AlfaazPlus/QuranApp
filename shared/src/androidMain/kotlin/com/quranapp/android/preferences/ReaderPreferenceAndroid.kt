package com.quranapp.android.preferences

import android.content.Context
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.QuranScriptVariant
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.isQuranAtlasScript
import com.quranapp.android.utils.tafsir.TafsirUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val LEGACY_SP_READER = "sp_reader"
private const val LEGACY_SP_TEXT_STYLE = "sp_reader_text"
private const val LEGACY_SP_TRANSL = "sp_reader_translations"
private const val LEGACY_SP_SCRIPT = "sp_reader_script"
private const val LEGACY_SP_TAFSIR = "sp_reader_tafsir"

fun ReaderPreferences.migrateFromLegacyIfNeeded(context: Context) {
    runBlocking {
        if (DataStoreManager.readFirst(KEY_LEGACY_MIGRATED)) return@runBlocking

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
}

fun ReaderPreferences.repairStoredPreferencesIfNeeded(context: Context) {
    runBlocking {
        withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
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
                val installed = DatabaseProvider.getExternalQuranDatabase(appCtx)
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
                DatabaseProvider.getTranslationRepository(appCtx).getBooks().keys
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
    }
}
