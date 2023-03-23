package com.quranapp.android.frags.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.R
import com.quranapp.android.databinding.LytThemeExplorerBinding
import com.quranapp.android.utils.app.ThemeUtils.resolveThemeIdFromMode
import com.quranapp.android.utils.app.ThemeUtils.resolveThemeModeFromId
import com.quranapp.android.utils.app.ThemeUtils.resolveThemeModeFromSP
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs.setThemeMode

class FragOnboardThemes : FragOnboardBase() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.lyt_theme_explorer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LytThemeExplorerBinding.bind(view).themeGroup.let {
            it.check(resolveThemeIdFromMode(view.context))
            it.onCheckChangedListener = { button, checkedId ->
                val themeMode = resolveThemeModeFromId(checkedId)
                setThemeMode(button.context, themeMode)
                AppCompatDelegate.setDefaultNightMode(resolveThemeModeFromSP(button.context))
            }
        }
    }
}
