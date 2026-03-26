package com.quranapp.android.activities.reference

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.adapters.reference.ADPQuranScience
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.databinding.ActivityExclusiveVersesBinding
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
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
            it.setTitleText(getString(R.string.quran_and_science))
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

        val locale = SPAppConfigs.getLocale(this)
        val indexPath = if (locale == "en" || locale == SPAppConfigs.LOCALE_DEFAULT) {
            "science/en/index.json"
        } else {
            val langPath = "science/$locale/index.json"
            try {
                assets.open(langPath).close()
                langPath
            } catch (e: Exception) {
                "science/en/index.json"
            }
        }

        assets.open(indexPath).use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    QuranScienceItem(
                        obj.getString("title"),
                        obj.getInt("referencesCount"),
                        obj.getString("path"),
                        getDrawableRes(obj.getInt("id"))
                    )
                )
            }
        }

        binding.list.addItemDecoration(GapedItemDecoration(dp2px(5F)))
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = ADPQuranScience(items)
    }

    private fun getDrawableRes(id: Int): Int {
        return when (id) {
            1 -> R.drawable.ic_science_astronomy
            2 -> R.drawable.ic_science_physics
            3 -> R.drawable.ic_science_geography
            4 -> R.drawable.ic_science_geology
            5 -> R.drawable.ic_science_oceanography
            6 -> R.drawable.ic_science_biology
            7 -> R.drawable.ic_science_botany
            8 -> R.drawable.ic_science_zoology
            9 -> R.drawable.ic_science_medicine
            10 -> R.drawable.ic_science_physiology
            11 -> R.drawable.ic_science_embryology
            12 -> R.drawable.ic_science_general
            else -> 0
        }
    }

    private fun showInfoDialog() {
        PeaceDialog.newBuilder(this)
            .setTitle(getString(R.string.about_this_page))
            .setMessage(getString(R.string.about_quran_science_msg))
            .setNeutralButton(getString(R.string.strLabelClose), null)
            .show()
    }
}