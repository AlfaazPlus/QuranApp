package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.compose.screens.chapterInfo.ChapterInfoScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.utils.IntentUtils.INTENT_ACTION_OPEN_CHAPTER_INFO
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.flow.MutableStateFlow

class ActivityChapInfo : BaseActivity() {
    val intentFlow = MutableStateFlow<Intent?>(null)

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(isTaskRoot) {
            override fun handleOnBackPressed() {
                launchMainActivity()
                finish()
            }
        })


        intentFlow.value = intent

        setContent {
            val currentIntent by intentFlow.collectAsState()

            if (currentIntent == null) {
                return@setContent
            }

            val (initialChapterNo, initialLanguage) =
                resolveChapterInfoStartArgs(currentIntent!!, appLocale().language)

            QuranAppTheme {
                ChapterInfoScreen(
                    initialChapterNo = initialChapterNo,
                    initialLanguage = initialLanguage,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        intentFlow.value = intent
    }

    private fun resolveChapterInfoStartArgs(
        intent: Intent,
        defaultAppLanguage: String,
    ): Pair<Int, String?> = try {
        when {
            intent.action.equals(INTENT_ACTION_OPEN_CHAPTER_INFO, ignoreCase = true) -> {
                val language = intent.getStringExtra("language")
                val chapterNo = intent.getIntExtra("chapterNo", -1)
                chapterNo to language
            }

            intent.action == Intent.ACTION_VIEW -> {
                val url = intent.data ?: throw IllegalArgumentException("Invalid params")
                if (!url.host.equals("quran.com", ignoreCase = true)) {
                    throw IllegalArgumentException("Invalid params")
                }
                val pathSegments = url.pathSegments
                val lang = url.getQueryParameter("language") ?: url.getQueryParameter("lang")
                val chapterNo = pathSegments.getOrNull(1)?.toIntOrNull() ?: -1
                chapterNo to lang
            }

            else -> {
                val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, -1)
                chapterNo to defaultAppLanguage
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        -1 to null
    }

}
