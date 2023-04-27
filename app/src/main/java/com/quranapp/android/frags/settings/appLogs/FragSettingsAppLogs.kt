package com.quranapp.android.frags.settings.appLogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.quranapp.android.R
import com.quranapp.android.adapters.utility.ViewPagerAdapter2
import com.quranapp.android.databinding.FragSettingsRecitationsBinding
import com.quranapp.android.databinding.LytReaderIndexTabBinding
import com.quranapp.android.frags.settings.FragSettingsBase

class FragSettingsAppLogs : FragSettingsBase() {
    private lateinit var binding: FragSettingsRecitationsBinding

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.appLogs)

    override val layoutResource = R.layout.frag_settings_recitations

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        binding = FragSettingsRecitationsBinding.bind(view)

        init(ctx)
    }

    private fun init(ctx: Context) {
        binding.let {
            val pageAdapter = ViewPagerAdapter2(requireActivity())
            it.viewPager.adapter = pageAdapter.apply {
                addFragment(FragAppLogsCrash(), ctx.getString(R.string.crashLogs))
                addFragment(FragAppLogsSuppressed(), ctx.getString(R.string.suppressedLogs))
            }
            it.viewPager.offscreenPageLimit = pageAdapter.itemCount
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