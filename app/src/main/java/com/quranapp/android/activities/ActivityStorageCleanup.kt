package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.databinding.ActivityStorageCleanupBinding
import com.quranapp.android.views.BoldHeader

class ActivityStorageCleanup : BaseActivity() {
    override fun getLayoutResource() = R.layout.activity_storage_cleanup

    override fun shouldInflateAsynchronously() = true

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityStorageCleanupBinding.bind(activityView)

        setupHeader(binding.header)
    }

    private fun setupHeader(header: BoldHeader) {
        header.setTitleText(R.string.titleStorageCleanup)
        header.setCallback { finish() }
    }
}