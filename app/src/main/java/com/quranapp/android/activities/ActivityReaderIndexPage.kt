package com.quranapp.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.adapters.utility.ViewPagerAdapter2
import com.quranapp.android.databinding.ActivityReaderIndexPageBinding
import com.quranapp.android.databinding.LytFavChaptersTabBtnBinding
import com.quranapp.android.databinding.LytReaderIndexTabBinding
import com.quranapp.android.frags.readerindex.FragReaderIndexChapters
import com.quranapp.android.frags.readerindex.FragReaderIndexFavChapters
import com.quranapp.android.frags.readerindex.FragReaderIndexJuz
import com.quranapp.android.interfaceUtils.readerIndex.FragReaderIndexCallback
import com.quranapp.android.utils.simplified.SimpleTabSelectorListener

class ActivityReaderIndexPage : BaseActivity() {
    private val fragCallbacks: MutableList<FragReaderIndexCallback> = ArrayList()
    private lateinit var binding: ActivityReaderIndexPageBinding

    override fun getLayoutResource(): Int {
        return R.layout.activity_reader_index_page
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        binding = ActivityReaderIndexPageBinding.bind(activityView)
        activityView.post { init() }
    }

    private fun init() {
        initHeader()
        initViewPager()
        initTabs()
        initSort()
    }

    private fun initHeader() {
        binding.header.let {
            it.back.setOnClickListener { finish() }
            it.search.setOnClickListener { startActivity(Intent(this, ActivitySearch::class.java)) }
        }
    }

    private fun initViewPager() {
        val adapter = ViewPagerAdapter2(this).apply {
            addFragment(FragReaderIndexChapters.newInstance(), getString(R.string.strTitleReaderChapters))
            addFragment(FragReaderIndexJuz.newInstance(), getString(R.string.strTitleReaderJuz))
            addFragment(FragReaderIndexFavChapters(), "")
        }

        binding.viewPager.let {
            it.adapter = adapter
            it.offscreenPageLimit = adapter.itemCount
            it.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
            it.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.sort.visibility = if (position == 2) View.GONE else View.VISIBLE
                }
            })
        }
    }

    private fun initTabs() {
        binding.header.let {
            it.readerTabLayout.setTabSetupCallback { viewPager: ViewPager2, tab: TabLayout.Tab, position: Int ->
                val inflater = LayoutInflater.from(this)
                if (position == 2) {
                    tab.customView = LytFavChaptersTabBtnBinding.inflate(inflater).root
                } else {
                    tab.customView = LytReaderIndexTabBinding.inflate(inflater).apply {
                        tabTitle.text = (viewPager.adapter as? ViewPagerAdapter2)?.getPageTitle(position)
                    }.root
                }
            }
            it.readerTabLayout.populateFromViewPager(binding.viewPager)
            it.readerTabLayout.addOnTabSelectedListener(object : SimpleTabSelectorListener() {
                override fun onTabReselected(tab: TabLayout.Tab) {
                    scrollToTop()
                }
            })
        }
    }

    private fun initSort() {
        binding.let {
            it.sort.visibility = View.VISIBLE
            it.sort.setOnClickListener { sort() }
        }
    }

    private fun scrollToTop() {
        fragCallbacks[binding.viewPager.currentItem].scrollToTop(true)
    }

    private fun sort() {
        fragCallbacks[binding.viewPager.currentItem].sort(this)
    }

    fun addToCallbacks(callback: FragReaderIndexCallback) {
        fragCallbacks.add(callback)
    }
}
