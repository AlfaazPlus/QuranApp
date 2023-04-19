package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.viewpager2.widget.ViewPager2
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
    private var pageAdapter: ViewPagerAdapter2? = null

    override fun getFragTitle(ctx: Context): String = ctx.getString(R.string.strTitleSelectReciter)

    override val layoutResource = R.layout.frag_settings_recitations

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (pageAdapter?.fragments == null || pageAdapter!!.fragments.isEmpty()) return null

        val bundle = Bundle()

        pageAdapter!!.fragments.forEach {
            if (it is FragSettingsRecitationsBase) {
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
                    if (pageAdapter?.fragments == null || pageAdapter!!.fragments.isEmpty()) return
                    // refresh child fragments
                    pageAdapter!!.fragments.forEach {
                        if (it is FragSettingsRecitationsBase && !it.isRefreshing) {
                            it.refresh(activity, true)
                        }
                    }
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                    if (pageAdapter?.fragments == null || pageAdapter!!.fragments.isEmpty()) return

                    val currentFragment = pageAdapter!!.fragments[binding.viewPager.currentItem]
                    if (currentFragment !is FragSettingsRecitationsBase) return

                    currentFragment.search(newText)
                }
            })

            setShowSearchIcon(true)
            setShowRightIcon(true)
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
            pageAdapter = ViewPagerAdapter2(requireActivity())
            it.viewPager.adapter = pageAdapter!!.apply {
                addFragment(FragSettingsRecitationsArabic(), ctx.getString(R.string.strTitleQuran))
                addFragment(FragSettingsRecitationsTranslation(), ctx.getString(R.string.labelTranslation))
            }
            it.viewPager.offscreenPageLimit = pageAdapter!!.itemCount
            it.viewPager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        }

        binding.tabLayout.let {
            it.setTabSetupCallback { viewPager, tab, position ->
                tab.customView = LytReaderIndexTabBinding.inflate(LayoutInflater.from(ctx)).apply {
                    tabTitle.text = (viewPager.adapter as ViewPagerAdapter2).getPageTitle(position)
                }.root
            }
            it.populateFromViewPager(binding.viewPager)
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activity()?.header?.checkSearchShown()

                if (pageAdapter?.fragments == null || pageAdapter!!.fragments.isEmpty()) return

                pageAdapter!!.fragments.forEachIndexed { index, fragment ->
                    if (fragment is FragSettingsRecitationsBase && index != position) {
                        fragment.search("")
                    }
                }
            }
        })
    }
}
