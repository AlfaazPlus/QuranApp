package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.screens.tafsir.TafsirReaderScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.databinding.LytTafsirTextSizeBinding
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.simplified.SimpleSeekbarChangeListener
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.TafsirReaderEvent
import com.quranapp.android.viewModels.TafsirReaderViewModel
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet
import java.util.Locale

class ActivityTafsir : BaseActivity() {

    private val viewModel: TafsirReaderViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initContent(intent)
    }

    override fun getLayoutResource() = 0

    override fun onActivityInflated(
        activityView: View,
        savedInstanceState: Bundle?
    ) {
        enableEdgeToEdge()

        setContent {
            QuranAppTheme {
                TafsirReaderScreen(
                    showFontSizeDialog = { showFontSizeDialog() },
                )
            }
        }

        TafsirManager.prepare(this, false) {
            initContent(intent)
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
                tafsirKey ?: ReaderPreferences.getTafsirId(),
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

        val multiplier = ReaderPreferences.getTafsirTextSizeMultiplier()

        val text = String.format(
            Locale.getDefault(),
            "%d%%",
            ReaderTextSizeUtils.calculateProgressText(multiplier)
        )
        binding.progressText.text = text

        binding.seekBar.apply {
            max = ReaderTextSizeUtils.maxProgress
            progress = ReaderTextSizeUtils.calculateProgress(multiplier)
            setOnSeekBarChangeListener(object : SimpleSeekbarChangeListener() {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val nProgress = ReaderTextSizeUtils.normalizeProgress(progress)
                    val text = String.format(Locale.getDefault(), "%d%%", nProgress)
                    binding.progressText.text = text

                    viewModel.onEvent(
                        TafsirReaderEvent.UpdateTextSize(
                            ReaderTextSizeUtils.calculateMultiplier(nProgress)
                        )
                    )
                }
            })
        }
    }
}
