/*
 * (c) Faisal Khan. Created on 26/10/2021.
 */

package com.quranapp.android.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.databinding.ActivityOnboardBinding;
import com.quranapp.android.frags.onboard.FragOnBoardRecitation;
import com.quranapp.android.frags.onboard.FragOnBoardThemes;
import com.quranapp.android.frags.onboard.FragOnBoardTransls;
import com.quranapp.android.utils.app.ThemeUtils;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;
import com.quranapp.android.utils.sharedPrefs.SPAppActions;
import com.quranapp.android.utils.univ.SimpleTabSelectorListener;

public class ActivityOnboarding extends BaseActivity {
    private ActivityOnboardBinding mBinding;
    public boolean mThemeChanged;
    private int boardIndex;
    private String[] mTitles;
    private String[] mDescs;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_onboard;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("index", boardIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            boardIndex = savedInstanceState.getInt("index", 0);
        } else {
            boardIndex = 0;
        }
        super.preActivityInflate(savedInstanceState);
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityOnboardBinding.bind(activityView);

        prepare();
        navigate(boardIndex);

        for (View button : new View[]{mBinding.previous, mBinding.next}) {
            button.setOnTouchListener(new HoverPushOpacityEffect());
        }

        mBinding.skip.setOnClickListener(v -> finishSetup());
        mBinding.previous.setOnClickListener(v -> {
            if (boardIndex == 0) {
                return;
            }
            navigate(--boardIndex);
        });
        mBinding.next.setOnClickListener(v -> {
            if (boardIndex == getBoardLastIndex()) {
                finishSetup();
                return;
            }
            navigate(++boardIndex);
        });
    }

    private void prepare() {
        mTitles = strArray(R.array.arrOnboardingTitles);
        mDescs = strArray(R.array.arrOnboardingDescs);

        for (int i = 0, l = mTitles.length; i < l; i++) {
            TabLayout.Tab tab = mBinding.pagerIndicator.newTab();
            mBinding.pagerIndicator.addTab(tab);
        }

        mBinding.pagerIndicator.addOnTabSelectedListener(new SimpleTabSelectorListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                navigate(tab.getPosition());
            }
        });

        initViewPager(mBinding.board);
    }

    private void initViewPager(ViewPager2 viewPager) {
        ViewPagerAdapter2 adapter = new ViewPagerAdapter2(this);

        adapter.addFragment(FragOnBoardThemes.newInstance(), "");
        adapter.addFragment(FragOnBoardTransls.newInstance(), "");
        adapter.addFragment(FragOnBoardRecitation.newInstance(), "");

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getItemCount());
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewPager.setUserInputEnabled(false);
    }

    private void navigate(int index) {
        if (index < 0 || index > getBoardLastIndex()) {
            return;
        }

        boardIndex = index;

        mBinding.previous.setVisibility(index == 0 ? View.GONE : View.VISIBLE);
        mBinding.next.setText(index == getBoardLastIndex() ? R.string.strLabelStart : R.string.strLabelNext);
        mBinding.pagerIndicator.selectTab(mBinding.pagerIndicator.getTabAt(index));

        String title = getBoardTitle(index);
        if (!TextUtils.isEmpty(title)) {
            mBinding.title.setText(title);
            mBinding.title.setVisibility(View.VISIBLE);
        } else {
            mBinding.title.setVisibility(View.GONE);
        }

        String desc = getBoardDesc(index);
        if (!TextUtils.isEmpty(desc)) {
            mBinding.desc.setText(desc);
            mBinding.desc.setVisibility(View.VISIBLE);
        } else {
            mBinding.desc.setVisibility(View.GONE);
        }

        mBinding.board.setCurrentItem(index, false);
    }

    private int getBoardLastIndex() {
        return mTitles.length - 1;
    }

    private String getBoardTitle(int index) {
        return mTitles[index];
    }

    private String getBoardDesc(int index) {
        return mDescs[index];
    }

    private void finishSetup() {
        SPAppActions.setRequireOnboarding(this, false);
        if (mThemeChanged) {
            AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeFromSP(this));
        }
        launchMainActivity();
        finish();
    }
}
