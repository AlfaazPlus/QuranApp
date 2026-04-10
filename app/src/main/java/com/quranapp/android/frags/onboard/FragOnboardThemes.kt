package com.quranapp.android.frags.onboard

import ThemeUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.R
import com.quranapp.android.databinding.LytThemeExplorerBinding
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
            it.check(resolveThemeIdFromMode())
            it.onCheckChangedListener = { button, checkedId ->
                val themeMode = resolveThemeModeFromId(checkedId)
                setThemeMode(button.context, themeMode)
                AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeForDelegate())
            }
        }
    }

    fun resolveThemeIdFromMode(): Int {
        return when (ThemeUtils.getThemeMode()) {
            ThemeUtils.THEME_MODE_DARK -> R.id.themeDark
            ThemeUtils.THEME_MODE_LIGHT -> R.id.themeLight
            ThemeUtils.THEME_MODE_DEFAULT -> R.id.systemDefault
            else -> R.id.systemDefault
        }
    }

    fun resolveThemeModeFromId(id: Int): String {
        return when (id) {
            R.id.themeDark -> {
                ThemeUtils.THEME_MODE_DARK
            }

            R.id.themeLight -> {
                ThemeUtils.THEME_MODE_LIGHT
            }

            else -> {
                ThemeUtils.THEME_MODE_DEFAULT
            }
        }
    }

}
