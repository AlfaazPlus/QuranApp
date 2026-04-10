package com.quranapp.android.frags.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.quranapp.android.compose.screens.HomeScreen
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.viewModels.HomeViewModel

class FragMain : BaseFragment() {
    private val viewModel: HomeViewModel by viewModels()

    override fun networkReceiverRegistrable(): Boolean {
        return true
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        viewModel.detachUpdateManager()
        super.onDestroyView()
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
                    HomeScreen(homeVm = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.attachUpdateManager(requireContext())
    }
}
