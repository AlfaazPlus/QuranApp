package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.utility.ViewPagerAdapter2
import com.quranapp.android.databinding.FragSettingsRecitationsBinding
import com.quranapp.android.databinding.LytReaderIndexTabBinding
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback

class FragSettingsRecitations : FragSettingsBase() {

    private lateinit var binding: FragSettingsRecitationsBinding

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectReciter)

    override val layoutResource = R.layout.frag_settings_recitations

    override fun getFinishingResult(ctx: Context): Bundle? {
        val bundle = Bundle()

        childFragmentManager.fragments.forEach {
            if (it is FragSettingsBase) {
                it.getFinishingResult(ctx)?.let { result ->
                    bundle.putAll(result)
                }
            }
        }

        return if (bundle.isEmpty) null else bundle
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onRightIconClick() {
                    // refresh child fragments
                    childFragmentManager.fragments.forEach {
                        if (it is FragSettingsRecitationsBase) {
                            it.refresh(activity, true)
                        }
                    }
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                    val currentFragment = childFragmentManager.fragments[binding.viewPager.currentItem]
                    if (currentFragment !is FragSettingsRecitationsBase) return

                    currentFragment.search(newText)
                }
            })

            disableRightBtn(false)
            setSearchHint(R.string.strHintSearchReciter)
            setRightIconRes(
                R.drawable.dr_icon_refresh,
                activity.getString(R.string.strLabelRefresh)
            )
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        binding = FragSettingsRecitationsBinding.bind(view)

        init(ctx)
    }

    private fun init(ctx: Context) {
        binding.let {
            it.viewPager.adapter = ViewPagerAdapter2(requireActivity()).apply {
                addFragment(FragSettingsRecitationsArabic(), "Arabic")
                addFragment(FragSettingsRecitationsTranslation(), "Translation")
            }
            it.viewPager.offscreenPageLimit = it.viewPager.adapter!!.itemCount
            it.viewPager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        }

        binding.tabLayout.setTabSetupCallback { viewPager, tab, position ->
            tab.customView = LytReaderIndexTabBinding.inflate(LayoutInflater.from(ctx)).apply {
                tabTitle.text = (viewPager.adapter as ViewPagerAdapter2).getPageTitle(position)
            }.root
        }
        binding.tabLayout.populateFromViewPager(binding.viewPager)
    }
}
