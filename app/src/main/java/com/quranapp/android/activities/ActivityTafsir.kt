package com.quranapp.android.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.compose.screens.tafsir.TafsirReaderScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.databinding.LytTafsirTextSizeBinding
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.simplified.SimpleSeekbarChangeListener
import com.quranapp.android.utils.univ.Codes
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.TafsirReaderEvent
import com.quranapp.android.viewModels.TafsirReaderViewModel
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet

class ActivityTafsir : BaseActivity() {

    private val viewModel: TafsirReaderViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initContent(intent)
    }

    override fun getLayoutResource() = 0
    override fun getThemeId() = R.style.Theme_QuranApp_ComposeActivity

    override fun adjustSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.getDecorView())
        val isLight = isStatusBarLight()

        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    override fun onActivityInflated(
        activityView: View,
        savedInstanceState: Bundle?
    ) {
        setupComposeContent()

        QuranMeta.prepareInstance(this, object : OnResultReadyCallback<QuranMeta?> {
            override fun onReady(r: QuranMeta?) {
                if (r != null) {
                    TafsirManager.prepare(this@ActivityTafsir, false) {
                        viewModel.setQuranMeta(r)
                        initContent(intent)
                    }
                }
            }
        })
    }


    private fun setupComposeContent() {
        setContent {
            QuranAppTheme {
                TafsirReaderScreen(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    showFontSizeDialog = { showFontSizeDialog() },
                    onOpenSettings = { openTafsirSettings() },
                )
            }
        }
    }

    private fun initContent(intent: Intent) {
        val tafsirKey = intent.getStringExtra("tafsirKey")
        val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, 1)
        val verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, 1)

        if (chapterNo < 1 || verseNo < 1) {
            finish()
            return
        }

        viewModel.onEvent(
            TafsirReaderEvent.Init(
                tafsirKey ?: SPReader.getSavedTafsirKey(this),
                chapterNo,
                verseNo
            )
        )
    }

    private fun showFontSizeDialog() {
        val binding = LytTafsirTextSizeBinding.inflate(layoutInflater)

        PeaceBottomSheet().apply {
            params.apply {
                headerTitleResource = R.string.titleReaderTextSizeTafsir
                contentView = binding.root
            }
        }.show(supportFragmentManager, "TafsirFontSize")

        val multiplier = SPReader.getSavedTextSizeMultTafsir(this)

        val text = "${ReaderTextSizeUtils.calculateProgressText(multiplier)}%"
        binding.progressText.text = text

        binding.seekBar.apply {
            max = ReaderTextSizeUtils.getMaxProgress()
            progress = ReaderTextSizeUtils.calculateProgress(multiplier)
            setOnSeekBarChangeListener(object : SimpleSeekbarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val nProgress = ReaderTextSizeUtils.normalizeProgress(progress)
                    val text = "$nProgress%"
                    binding.progressText.text = text

                    viewModel.onEvent(
                        TafsirReaderEvent.UpdateTextSize(
                            ReaderTextSizeUtils.calculateMultiplier(nProgress)
                        )
                    )
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    SPReader.setSavedTextSizeMultTafsir(
                        seekBar.context,
                        ReaderTextSizeUtils.calculateMultiplier(
                            ReaderTextSizeUtils.normalizeProgress(
                                seekBar.progress
                            )
                        )
                    )
                }
            })
        }
    }

    private fun openTafsirSettings() {
        val intent = Intent(this, ActivitySettings::class.java).apply {
            putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, ActivitySettings.SETTINGS_TAFSIR)
        }
        startActivity4Result(intent, null)
    }

    override fun onActivityResult2(result: ActivityResult?) {
        super.onActivityResult2(result)

        if (result?.resultCode == Codes.SETTINGS_LAUNCHER_RESULT_CODE) {
            val updatedTafsirKey = SPReader.getSavedTafsirKey(this)
            if (updatedTafsirKey != null && updatedTafsirKey != viewModel.uiState.value.tafsirKey) {
                viewModel.onEvent(TafsirReaderEvent.ChangeTafsir(updatedTafsirKey))
            }
        }
    }
}
