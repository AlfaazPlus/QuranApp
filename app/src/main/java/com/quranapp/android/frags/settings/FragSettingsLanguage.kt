package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.screens.settings.LanguageSelectionScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback

class FragSettingsLanguage : FragSettingsBase() {
    private var selectedLocale: String? = null

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleAppLanguage)

    override val layoutResource: Int = 0 // Not using XML layout

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    changeCheckpoint()
                }
            })

            setShowRightIcon(true)
            disableRightBtn(true)
            setRightIconRes(R.drawable.dr_icon_check, activity.getString(R.string.strLabelDone))
            setShowSearchIcon(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val initialLocale = SPAppConfigs.getLocale(requireContext())

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    LanguageSelectionScreen(
                        onLocaleSelected = { locale ->
                            selectedLocale = locale
                            activity()?.header?.disableRightBtn(locale == initialLocale)
                        }
                    )
                }
            }
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        // Locale is initialized in onCreateView
    }

    private fun changeCheckpoint() {
        val locale = selectedLocale ?: return

        restartApp(requireContext(), locale)
    }

    private fun restartApp(ctx: Context, locale: String) {
        SPAppConfigs.setLocale(ctx, locale)
        restartMainActivity(ctx)
    }
}
