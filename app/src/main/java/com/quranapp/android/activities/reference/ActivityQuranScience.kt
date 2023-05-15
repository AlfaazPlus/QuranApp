package com.quranapp.android.activities.reference

import android.os.Bundle
import android.view.View
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.views.BoldHeader

class ActivityQuranScience : BaseActivity() {
    override fun getLayoutResource() = R.layout.activity_exclusive_verses

    override fun shouldInflateAsynchronously() = true
    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityExclusiveVersesBinding.bind(activityView)

        binding.header.let {
            it.setBGColor(R.color.colorBGPage)
            it.setTitleText("Quran & Science")
            it.setShowRightIcon(true)
            it.setRightIconRes(R.drawable.dr_icon_info)
            it.setCallback(object : BoldHeader.BoldHeaderCallback {
                override fun onBackIconClick() {
                    onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    showInfoDialog()
                }
            })
        }
    }

    private fun showInfoDialog() {
        PeaceDialog.newBuilder(this)
            .setTitle("About this page")
            .setMessage(
                "The contents on this page are taken from the book by Dr. Zakir Naik called " +
                        "\"The Quran and Modern Science: Compatible or Incompatible\"\n\n" +
                        "The contents are only available in English."
            )
            .setNeutralButton("Close", null)
            .show()
    }
}