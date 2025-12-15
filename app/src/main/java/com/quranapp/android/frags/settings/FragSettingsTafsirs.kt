package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.screens.settings.TafsirSelectionScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.viewModels.TafsirEvent
import com.quranapp.android.viewModels.TafsirViewModel
import com.quranapp.android.views.BoldHeader
import kotlinx.coroutines.launch

class FragSettingsTafsirs : FragSettingsBase() {

    private val tafsirViewModel: TafsirViewModel by viewModels()

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectTafsir)

    override val layoutResource: Int = 0 // Not using XML layout

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (tafsirViewModel.hasTafsirSelectionChanged()) {
            return bundleOf(ActivityReader.KEY_TAFSIR_CHANGED to true)
        }
        return null
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeader.BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    tafsirViewModel.onEvent(TafsirEvent.Refresh)
                }
            })

            setShowRightIcon(true)
            setRightIconRes(
                R.drawable.dr_icon_refresh,
                activity.getString(R.string.strLabelRefresh)
            )
        }
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
                    val uiState by tafsirViewModel.uiState.collectAsState()

                    TafsirSelectionScreen(
                        uiState = uiState,
                    )
                }
            }
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        // Observe loading state to toggle header refresh icon visibility
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                tafsirViewModel.uiState.collect { state ->
                    activity()?.header?.setShowRightIcon(!state.isLoading)
                }
            }
        }
    }
}
