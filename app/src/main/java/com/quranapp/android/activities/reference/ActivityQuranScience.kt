package com.quranapp.android.activities.reference

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.adapters.reference.ADPQuranScience
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.views.BoldHeader
import org.json.JSONArray

class ActivityQuranScience : BaseActivity() {
    override fun getLayoutResource() = R.layout.activity_exclusive_verses

    override fun shouldInflateAsynchronously() = true
    @SuppressLint("DiscouragedApi")
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

        val items = mutableListOf<QuranScienceItem>()

        assets.open("science/index.json").use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(QuranScienceItem(
                    obj.getString("title"),
                    obj.getInt("referencesCount"),
                    obj.getString("path"),
                    resources.getIdentifier(obj.getString("image"), "drawable", packageName)
                ))
            }
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = ADPQuranScience(items)
    }

    private fun showInfoDialog() {
        PeaceDialog.newBuilder(this)
            .setTitle("About this page")
            .setMessage(
                "The contents on this page are taken from the book by Dr. Zakir Naik called " +
                        "\"The Quran and Modern Science: Compatible or Incompatible\"\n\n" +
                        "The contents in the app are currently available only in English."
            )
            .setNeutralButton("Close", null)
            .show()
    }
}