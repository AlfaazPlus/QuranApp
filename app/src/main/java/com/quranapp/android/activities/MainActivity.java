package com.quranapp.android.activities;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.databinding.ActivityMainBinding;
import com.quranapp.android.suppliments.IndexMenu;
import com.quranapp.android.utils.app.AppActions;
import com.quranapp.android.utils.app.UpdateManager;
import com.quranapp.android.utils.sharedPrefs.SPAppActions;
import com.quranapp.android.frags.main.FragMain;
import com.quranapp.android.widgets.tablayout.BottomTab;
import com.quranapp.android.widgets.tablayout.BottomTabLayout;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding mBinding;
    private IndexMenu mIndexMenu;
    private UpdateManager mUpdateManager;

    @Override
    protected int getStatusBarBG() {
        return ContextCompat.getColor(this, R.color.colorBGHomePageItem);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIndexMenu != null) {
            mIndexMenu.close();
        }
    }

    @Override
    protected void onPause() {
        if (mUpdateManager != null) {
            mUpdateManager.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUpdateManager != null) {
            mUpdateManager.onResume();
        }
    }

    @Override
    protected void initCreate(Bundle savedInstanceState) {
        if (isOnboardingRequired()) {
            initOnboarding();
            return;
        }


        mUpdateManager = new UpdateManager(this, null);
        mUpdateManager.refreshAppUpdatesJson();

        if (mUpdateManager.check4CriticalUpdate()) {
            return;
        }


        super.initCreate(savedInstanceState);
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityMainBinding.bind(activityView);

        if (isOnboardingRequired()) {
            return;
        }

        init();
    }

    private void init() {
        initContent();
        initActions();
    }

    private void initHeader() {
        initMenu();

        mBinding.header.indexMenu.setOnClickListener(v -> mIndexMenu.open());
    }

    private void initActions() {
        AppActions.checkForResourcesVersions(this);
        AppActions.scheduleActions(this);
        AppActions.checkForCrashLogs(this);
    }

    private void initContent() {
        initHeader();
        initViewPager();
        initBottomNavigation();
    }

    private void initViewPager() {
        ViewPager2 viewPager = mBinding.viewPager;
        ViewPagerAdapter2 mViewPagerAdapter = new ViewPagerAdapter2(this);
        mViewPagerAdapter.addFragment(new FragMain(), str(R.string.strLabelNavHome));
        viewPager.setAdapter(mViewPagerAdapter);
        viewPager.setOffscreenPageLimit(mViewPagerAdapter.getItemCount());
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mBinding.header.getRoot().setExpanded(true);

                boolean notMainPage = position == 1;
                mBinding.header.getRoot().setElevation(notMainPage ? 0 : dp2px(4));
            }
        });
    }

    private void initBottomNavigation() {
        BottomTabLayout bottomTabLayout = mBinding.bottomTabLayout;
        bottomTabLayout.setTabs(getBottomTabs());
        bottomTabLayout.setKingTab(new BottomTab(R.drawable.quran_kareem),
            kingTab -> launchActivity(ActivityReaderIndexPage.class));

        bottomTabLayout.setSelectionChangeListener(tab -> {
            if (tab.getId() == R.id.navSearch) {
                launchActivity(ActivitySearch.class);
            }
        });
    }

    private ArrayList<BottomTab> getBottomTabs() {
        @DrawableRes int[] bottomTabsIcons = {R.drawable.dr_icon_home, R.drawable.dr_icon_search};
        @StringRes int[] bottomTabsLabels = {R.string.strLabelNavHome, R.string.strLabelNavSearch};
        @IdRes int[] ids = {R.id.navHome, R.id.navSearch};

        ArrayList<BottomTab> bottomTabs = new ArrayList<>();
        for (int i = 0; i < bottomTabsIcons.length; i++) {
            BottomTab bottomTab = new BottomTab(getString(bottomTabsLabels[i]), bottomTabsIcons[i]);
            bottomTab.setId(ids[i]);
            bottomTabs.add(bottomTab);
        }

        return bottomTabs;
    }

    private void initMenu() {
        mIndexMenu = new IndexMenu(this, mBinding.getRoot());
    }

    private boolean isOnboardingRequired() {
        return SPAppActions.getRequireOnboarding(this);
    }

    private void initOnboarding() {
        launchActivity(ActivityOnboarding.class);
        finish();
    }
}