package com.quranapp.android.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.adapters.utility.ViewPagerAdapter2
import com.quranapp.android.compose.components.IndexMenuButton
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.databinding.ActivityMainBinding
import com.quranapp.android.frags.main.FragMain
import com.quranapp.android.utils.app.AppActions.checkForCrashLogs
import com.quranapp.android.utils.app.AppActions.checkForResourcesVersions
import com.quranapp.android.utils.app.AppActions.scheduleActions
import com.quranapp.android.utils.app.UpdateManager
import com.quranapp.android.utils.sharedPrefs.SPAppActions.getRequireOnboarding
import com.quranapp.android.views.reader.updateAllVotdWidgets
import com.quranapp.android.widgets.tablayout.BottomTab
import com.quranapp.android.widgets.tablayout.BottomTabLayout.OnKingTabClickListener
import com.quranapp.android.widgets.tablayout.BottomTabLayout.OnTabSelectionChangeListener

class MainActivity : BaseActivity() {
    private var mBinding: ActivityMainBinding? = null
    private var mUpdateManager: UpdateManager? = null

    override fun getStatusBarBG() = ContextCompat.getColor(this, R.color.colorBGHomePageItem)

    override fun shouldInflateAsynchronously() = true

    override fun getLayoutResource() = R.layout.activity_main

    override fun onPause() {
        if (mUpdateManager != null) {
            mUpdateManager!!.onPause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (mUpdateManager != null) {
            mUpdateManager!!.onResume()
        }
    }

    override fun initCreate(savedInstanceState: Bundle?) {
        if (this.isOnboardingRequired) {
            initOnboarding()
            return
        }


        mUpdateManager = UpdateManager(this, null)
        mUpdateManager!!.refreshAppUpdatesJson()

        if (mUpdateManager!!.check4CriticalUpdate()) {
            return
        }


        super.initCreate(savedInstanceState)
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        mBinding = ActivityMainBinding.bind(activityView)

        if (this.isOnboardingRequired) {
            return
        }

        init()
    }

    private fun init() {
        initContent()
        initActions()

        updateAllVotdWidgets(this)
    }

    private fun initHeader() {
        mBinding!!.header.indexMenu.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                QuranAppTheme {
                    IndexMenuButton()
                }
            }
        }
    }

    private fun initActions() {
        checkForResourcesVersions(this)
        scheduleActions(this)
        checkForCrashLogs(this)
    }

    private fun initContent() {
        initHeader()
        initViewPager()
        initBottomNavigation()
    }

    private fun initViewPager() {
        val viewPager = mBinding!!.viewPager
        val mViewPagerAdapter = ViewPagerAdapter2(this)
        mViewPagerAdapter.addFragment(FragMain(), str(R.string.strLabelNavHome))
        viewPager.setAdapter(mViewPagerAdapter)
        viewPager.setOffscreenPageLimit(mViewPagerAdapter.getItemCount())
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER)

        viewPager.setUserInputEnabled(false)

        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                mBinding!!.header.getRoot().setExpanded(true)

                val notMainPage = position == 1
                mBinding!!.header.getRoot()
                    .setElevation((if (notMainPage) 0 else dp2px(4f)).toFloat())
            }
        })
    }

    private fun initBottomNavigation() {
        val bottomTabLayout = mBinding!!.bottomTabLayout
        bottomTabLayout.setTabs(this.bottomTabs)
        bottomTabLayout.setKingTab(
            BottomTab(R.drawable.quran_kareem),
            OnKingTabClickListener { kingTab: BottomTab? -> launchActivity(ActivityReaderIndexPage::class.java) })

        bottomTabLayout.setSelectionChangeListener(OnTabSelectionChangeListener { tab: BottomTab? ->
            if (tab!!.id == R.id.navSearch) {
                launchActivity(ActivitySearch::class.java)
            }
        })
    }

    private val bottomTabs: ArrayList<BottomTab?>
        get() {
            @DrawableRes val bottomTabsIcons =
                intArrayOf(
                    R.drawable.dr_icon_home,
                    R.drawable.dr_icon_search
                )
            @StringRes val bottomTabsLabels =
                intArrayOf(
                    R.string.strLabelNavHome,
                    R.string.strLabelNavSearch
                )
            @IdRes val ids = intArrayOf(
                R.id.navHome,
                R.id.navSearch
            )

            val bottomTabs = java.util.ArrayList<BottomTab?>()
            for (i in bottomTabsIcons.indices) {
                val bottomTab =
                    BottomTab(getString(bottomTabsLabels[i]), bottomTabsIcons[i])
                bottomTab.id = ids[i]
                bottomTabs.add(bottomTab)
            }

            return bottomTabs
        }


    private val isOnboardingRequired: Boolean
        get() = getRequireOnboarding(this)

    private fun initOnboarding() {
        launchActivity(ActivityOnboarding::class.java)
        finish()
    }
}