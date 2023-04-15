package com.quranapp.android.frags.settings.appLogs

import android.os.Bundle
import android.view.View
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.utils.univ.FileUtils

class FragAppLogsCrash : BaseFragment() {
    private lateinit var binding: FragSettingsTranslBinding
    private lateinit var fileUtils: FileUtils

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileUtils = FileUtils.newInstance(view.context)
        binding = FragSettingsTranslBinding.bind(view)

        init()
    }

    private fun init() {

    }
}