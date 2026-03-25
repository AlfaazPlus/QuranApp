/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.frags.settings.translation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.compose.screens.settings.TranslationSelectionScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.Codes
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.TranslationEvent
import com.quranapp.android.viewModels.TranslationViewModel
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.TreeSet

class FragSettingsTranslation : FragSettingsBase(), FragmentResultListener {
    private val translationViewModel: TranslationViewModel by viewModels()

    override fun onPause() {
        beforeFinish()
        super.onPause()
    }

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleTranslations)

    override val layoutResource = 0 // Not using XML layout

    public override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)

        header.setCallback(object : BoldHeaderCallback {
            override fun onBackIconClick() {
                activity.onBackPressedDispatcher.onBackPressed()
            }

            override fun onRightIconClick() {
                translationViewModel.onEvent(TranslationEvent.Refresh)
            }

            override fun onSearchRequest(searchBox: EditText?, newText: CharSequence) {
                search(newText)
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
                    TranslationSelectionScreen(
                        onNavigateToDownload = {
                            launchFrag(
                                FragSettingsTranslationsDownload::class.java,
                                null
                            )
                        },
                    )
                }
            }
        }
    }


    public override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        val args = getArgs()
        var initialSlugs: Set<String>

        val requestedTranslSlugs = args.getStringArray(Keys.READER_KEY_TRANSL_SLUGS)
        if (requestedTranslSlugs == null) {
            initialSlugs = SPReader.getSavedTranslations(ctx)
        } else {
            initialSlugs = TreeSet<String>(Arrays.asList(*requestedTranslSlugs))
        }

        val saveTranslChanges = args.getBoolean(Keys.READER_KEY_SAVE_TRANSL_CHANGES, true)

        translationViewModel.onEvent(
            TranslationEvent.Initialize(
                initialSlugs = initialSlugs,
                saveTranslationChanges = saveTranslChanges
            )
        )

        // Observe loading state to toggle header refresh icon visibility
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                translationViewModel.uiState.collect { state ->
                    activity()?.header?.let {
                        it.setShowSearchIcon(!state.isLoading)
                        it.setShowRightIcon(!state.isLoading)
                    }

                    setArguments(getArgs().apply {
                        putStringArray(
                            Keys.READER_KEY_TRANSL_SLUGS,
                            state.selectedSlugs.toTypedArray<String>()
                        )
                    })
                }
            }
        }

        parentFragmentManager.setFragmentResultListener(
            Codes.SETTINGS_TRANSLATION_DOWNLOAD_CODE.toString(),
            getViewLifecycleOwner(),
            this
        )
    }

    private fun search(query: CharSequence) {
        translationViewModel.onEvent(
            TranslationEvent.Search(
                query.toString().lowercase()
            )
        )
    }

    public override fun getFinishingResult(ctx: Context): Bundle {
        val slugs = translationViewModel.uiState.value.selectedSlugs

        val data = Bundle()
        data.putStringArray(Keys.READER_KEY_TRANSL_SLUGS, slugs.toTypedArray<String>())
        return data
    }

    private fun beforeFinish() {
        val context = getContext() ?: return

        parentFragmentManager.setFragmentResult(
            Codes.SETTINGS_LAUNCHER_RESULT_CODE.toString(),
            getFinishingResult(context)
        )
    }


    override fun onFragmentResult(requestKey: String, result: Bundle) {
        if (Codes.SETTINGS_TRANSLATION_DOWNLOAD_CODE.toString() != requestKey) {
            return
        }

        translationViewModel.loadTranslations(true)
    }
}
