package com.quranapp.android.views.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.quranapp.android.R;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.utils.extensions.ContextKt;

public class TabLayout2 extends TabLayout {
    private ViewPagerAdapter2 mAdapter;
    private ViewPager2 mViewPager2;
    private TabSetupCallback mTabSetupCallback;
    private boolean mSmoothScroll = true;

    public TabLayout2(@NonNull Context context) {
        super(context);
        init();
    }

    public TabLayout2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TabLayout2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
    }

    public void populateFromViewPager(@NonNull ViewPager viewpager) {
        setupWithViewPager(viewpager);
    }

    public void populateFromViewPager(@NonNull ViewPager2 viewPager2) {
        mAdapter = (ViewPagerAdapter2) viewPager2.getAdapter();
        mViewPager2 = viewPager2;

        setMediator(viewPager2, mAdapter, mSmoothScroll);
    }

    private void setMediator(ViewPager2 viewPager2, ViewPagerAdapter2 adapter, boolean smoothScroll) {
        if (adapter == null) {
            throw new IllegalStateException("Trying to populate from ViewPager2 before it has an adapter.");
        }

        viewPager2.setUserInputEnabled(smoothScroll);
        TabLayoutMediator mediator = new TabLayoutMediator(this, viewPager2, true, smoothScroll, this::setupTab);
        mediator.attach();
    }

    private void setupTab(Tab tab, int position) {
        tab.setText(mAdapter.getPageTitle(position));
        if (mTabSetupCallback != null) {
            mTabSetupCallback.onTabSetup(mViewPager2, tab, position);
        }
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        super.addTab(tab, position, setSelected);
        setupTabAppearances(tab);
    }

    private void setupTabAppearances(Tab tab) {
        TooltipCompat.setTooltipText(tab.view, null);

        float textSize = ContextKt.getDimenSp(getContext(), R.dimen.dmnCommonSize);
        int childCount = tab.view.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = tab.view.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setAllCaps(false);
                textView.setTextSize(textSize);
                textView.setLetterSpacing(0);
            }
        }
    }

    public void setTabSetupCallback(TabSetupCallback callback) {
        mTabSetupCallback = callback;
    }

    public void setSmoothScroll(boolean smoothScroll) {
        mSmoothScroll = smoothScroll;

        if (mViewPager2 != null && mAdapter != null) {
            setMediator(mViewPager2, mAdapter, smoothScroll);
        }
    }

    public interface TabSetupCallback {
        void onTabSetup(ViewPager2 viewPager, Tab tab, int position);
    }
}
