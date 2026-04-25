@file:Suppress("UNCHECKED_CAST")

package com.quranapp.android.utils.reader

import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


data class VerseModeConfig(
    val script: QuranScript,
    val translations: Set<String>,
    val arabicSize: Float,
    val translationSize: Float,
    val arabicEnabled: Boolean,
    val wbwId: String,
)

data class MushafModeConfig(
    val script: QuranScript,
)

data class TranslationModeConfig(
    val script: QuranScript,
    val translations: Set<String>,
    val translationSize: Float,
) {
    fun toCacheKey(): String {
        return "${script.scriptCode}_${script.variant}_${translations.joinToString()}_$translationSize"
    }
}


sealed interface ReaderObserveAction {
    data class BuildVerse(
        val cfg: VerseModeConfig
    ) : ReaderObserveAction

    data class SwitchMushaf(
        val cfg: MushafModeConfig
    ) : ReaderObserveAction

    data class BuildTranslation(
        val cfg: TranslationModeConfig
    ) : ReaderObserveAction
}

object ReaderChangeManager {

    fun verseModeFlow(): Flow<ReaderObserveAction> {
        return combine(
            scriptFlow(),
            translationSlugsFlow(),
            arabicSizeFlow(),
            translationSizeFlow(),
            arabicEnabledFlow(),
            wbwIdFlow(),
        ) { values ->
            ReaderObserveAction.BuildVerse(
                VerseModeConfig(
                    script = values[0] as QuranScript,
                    translations = values[1] as Set<String>,
                    arabicSize = values[2] as Float,
                    translationSize = values[3] as Float,
                    arabicEnabled = values[4] as Boolean,
                    wbwId = values[5] as String,
                )
            )
        }.distinctUntilChanged()
    }

    fun mushafModeFlow(): Flow<ReaderObserveAction> {
        return scriptFlow()
            .map {
                ReaderObserveAction.SwitchMushaf(
                    MushafModeConfig(it)
                )
            }
            .distinctUntilChanged()
    }

    fun translationModeFlow(): Flow<ReaderObserveAction> {
        return combine(
            scriptFlow(),
            translationSlugsFlow(),
            translationSizeFlow(),
        ) { script, slugs, size ->

            ReaderObserveAction.BuildTranslation(
                TranslationModeConfig(
                    script = script,
                    translations = slugs,
                    translationSize = size,
                )
            )
        }.distinctUntilChanged()
    }


    private fun scriptFlow(): Flow<QuranScript> {
        return DataStoreManager.flowMultiple(
            ReaderPreferences.KEY_SCRIPT,
            ReaderPreferences.KEY_SCRIPT_VARIANT,
        ).map { prefs ->
            val script = QuranScriptUtils.validatePreferredScript(
                prefs.get(ReaderPreferences.KEY_SCRIPT)
            )

            val variant = prefs.get(ReaderPreferences.KEY_SCRIPT_VARIANT)

            QuranScript.fromRawValues(script, variant)
        }.distinctUntilChanged()
    }

    private fun translationSlugsFlow(): Flow<Set<String>> {
        return ReaderPreferences.translationsFlow()
            .distinctUntilChanged()
    }

    private fun arabicSizeFlow(): Flow<Float> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC
        ).distinctUntilChanged()
    }

    private fun translationSizeFlow(): Flow<Float> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL
        ).distinctUntilChanged()
    }

    private fun arabicEnabledFlow(): Flow<Boolean> {
        return DataStoreManager.flow(
            ReaderPreferences.KEY_ARABIC_TEXT_ENABLED
        ).distinctUntilChanged()
    }

    private fun wbwIdFlow(): Flow<String> {
        return DataStoreManager.flowMultiple(
            ReaderPreferences.KEY_WBW,
            ReaderPreferences.KEY_WBW_CONTENT_EPOCH,
        ).map {
            it.get(ReaderPreferences.KEY_WBW)
        }.distinctUntilChanged()
    }
}