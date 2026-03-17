package com.quranapp.android.frags.settings.translation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.screens.settings.TranslationDownloadScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.utils.univ.Codes
import com.quranapp.android.viewModels.TranslationDownloadEvent
import com.quranapp.android.viewModels.TranslationDownloadViewModel
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import kotlinx.coroutines.launch

class FragSettingsTranslationsDownload :
    FragSettingsBase() {

    private val viewModel: TranslationDownloadViewModel by viewModels()

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleDownloadTranslations)

    override val layoutResource = 0

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.setCallback(object : BoldHeaderCallback {
            override fun onBackIconClick() {
                activity.onBackPressedDispatcher.onBackPressed()
            }

            override fun onRightIconClick() {
                viewModel.onEvent(TranslationDownloadEvent.Refresh)
            }
        })

        header.setShowSearchIcon(false)
        header.setShowRightIcon(false)
        header.setSearchHint(R.string.strHintSearchTranslation)
        header.setRightIconRes(
            R.drawable.dr_icon_refresh,
            activity.getString(R.string.strLabelRefresh)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    TranslationDownloadScreen()
                }
            }
        }
    }


    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    activity()?.header?.setShowRightIcon(!state.isLoading)

                    // Notify the list fragment to refresh its data
                    parentFragmentManager.setFragmentResult(
                        Codes.SETTINGS_TRANSLATION_DOWNLOAD_CODE.toString(),
                        Bundle()
                    )
                }
            }
        }

    }
}
