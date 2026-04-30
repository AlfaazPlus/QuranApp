package com.quranapp.android.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.search.SearchScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.appLocale
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

class ActivitySearch : BaseActivity() {
    private val _voiceQueryFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val voiceSearchFlow = _voiceQueryFlow.asSharedFlow()

    override fun getLayoutResource(): Int = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    QuranAppTheme {
                        val voiceLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult

                            val matches =
                                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                            val text =
                                matches?.firstOrNull() ?: return@rememberLauncherForActivityResult

                            _voiceQueryFlow.tryEmit(text)
                        }

                        val supportsVoice = packageManager.queryIntentActivities(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                            PackageManager.MATCH_DEFAULT_ONLY,
                        ).isNotEmpty()


                        SearchScreen(
                            supportsVoiceSearch = supportsVoice,
                            voiceSearchFlow = voiceSearchFlow,
                            onVoiceSearchClick = { inQuranText ->
                                runCatching {
                                    val locale = if (!inQuranText) appLocale()
                                    else Locale.forLanguageTag("ar-SA")


                                    val intent =
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH,
                                            )

                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                getString(if (inQuranText) R.string.searchTipArabic else R.string.searchInQuran)
                                            )

                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE,
                                                locale.toLanguageTag()
                                            )

                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                                                locale.toLanguageTag()
                                            )

                                            putExtra(
                                                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                                                1500
                                            )

                                            putExtra(
                                                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                                                1000
                                            )
                                        }

                                    voiceLauncher.launch(intent)
                                }
                            },
                        )
                    }
                }
            },
        )
    }
}
