package com.quranapp.android.widgets.tablayout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;

import java.util.ArrayList;
import java.util.List;

public class BottomTabLayout extends LinearLayout {
    private final ArrayList<BottomTab> mTabs = new ArrayList<>();
    boolean mProtectFromViewPagerChange;
    private LinearLayout mTabsContainer;
    private BottomTab mSelectedTab;
    private ViewPager2 mViewPager;
    private ViewPagerAdapter2 mViewPagerAdapter;
    private OnTabSelectionChangeListener mSelectionChangeListener;
    private OnTabSelectionUpdateListener mSelectionUpdateListener;
    private BottomTab mKingTab;
    private OnKingTabClickListener mKingTabClickCallback;
    private int mPreSelectedTabIndex;
    private boolean firstTime = true;

    public BottomTabLayout(@NonNull Context context) {
        this(context, null);
    }

    public BottomTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initThis();
        initTabsContainer();
    }

    private void initThis() {
        setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        setGravity(Gravity.CENTER);

        int bgColor = ContextCompat.getColor(getContext(), R.color.colorBGBottomNavigation);
        int borderColor = ContextCompat.getColor(getContext(), R.color.colorBottomNavigationBorder);

        int[] borderWidths = Dimen.createBorderWidthsForBG(0, Dimen.dp2px(getContext(), 1), 0, 0);
        Drawable background = DrawableUtils.createBackgroundStroked(bgColor, borderColor, borderWidths, null);
        setBackground(background);
    }

    private void initTabsContainer() {
        mTabsContainer = new LinearLayout(getContext());
        mTabsContainer.setOrientation(HORIZONTAL);
        mTabsContainer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        mTabsContainer.setLayoutParams(params);

        setVisibility(INVISIBLE);
        mTabsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                resetTabsParams();
                mTabsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        addView(mTabsContainer);
    }

    private void initKingTab(BottomTab kingTab) {
        if (kingTab == null) return;

        int index = getTabsCount() / 2;
        BottomTabView tabView = new BottomTabView(this, kingTab);
        mTabsContainer.addView(tabView, index);

        resetTabsParams();
    }

    void resetTabsParams() {
        int childCount = mTabsContainer.getChildCount();
        int tabWidth = mTabsContainer.getWidth() / Math.max(childCount, 1);
        for (int i = 0; i < childCount; i++) {
            View tabView = mTabsContainer.getChildAt(i);
            LinearLayout.LayoutParams params = (LayoutParams) tabView.getLayoutParams();
            params.width = tabWidth - (params.leftMargin + params.rightMargin);
            tabView.requestLayout();
        }

        setVisibility(VISIBLE);
    }

    private boolean hasKingTab() {
        return mKingTab != null && mKingTab.getTabView() != null && mKingTab.getTabView().getParent() != null;
    }


    private void setupViewPager(ViewPager2 viewPager) {
        if (mPreSelectedTabIndex != -1) {
            viewPager.setCurrentItem(mPreSelectedTabIndex, false);
        }
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                selectTabAtInternal(position);
            }
        });

        /*viewPager.addOnAdapterChangeListener((viewPager1, oldAdapter, newAdapter) -> {
            setTabs(mTabs);
        });*/
    }

    private void drainTabs() {
        mTabs.clear();
        mTabsContainer.removeAllViews();
    }


    public void addTab(@NonNull BottomTab tab, int position) {
        if (position > mTabs.size()) {
            String msg = String.format(
                "position cannot be larger than size of tabLayout items. Position: %s , Size: %s", position,
                mTabs.size());
            throw new IllegalArgumentException(msg);
        }

        tab.setPosition(position);
        mTabs.add(position, tab);
        addTabView(tab, position);
    }

    public void addTab(@NonNull BottomTab tab) {
        addTab(tab, mTabs.size());
    }


    private BottomTabView createTabView(BottomTab tab) {
        return new BottomTabView(this, tab);
    }

    private void addTabView(BottomTab tab, int position) {
        addViewToLayout(createTabView(tab), position);
    }

    private void addViewToLayout(BottomTabView tabView, int position) {
        mTabsContainer.addView(tabView, position);
    }

    void onKingTabClick(@NonNull BottomTab tab) {
        if (mKingTabClickCallback != null) mKingTabClickCallback.onKingTabClick(tab);
    }

    void selectTab(BottomTab tabToSelect) {
        if (tabToSelect == null) return;

        BottomTab currentSelectedTab = mSelectedTab;
        tabToSelect.setSelected(true);

        if (!mProtectFromViewPagerChange && mViewPager != null) mViewPager.setCurrentItem(tabToSelect.getPosition());

        //        if (currentSelectedTab != tabToSelect) unselectTabView(currentSelectedTab);
        selectTabView(tabToSelect);

        //        mSelectedTab = tabToSelect;
    }

    private void selectTabView(BottomTab tabToSelect) {
        if (tabToSelect == null) return;

        if (mSelectedTab == tabToSelect) {
            if (mSelectionUpdateListener != null) {
                mSelectionUpdateListener.onTabReselect(tabToSelect);
            }
            return;
        } else {
            if (mSelectionUpdateListener != null) {
                mSelectionUpdateListener.onTabSelect(tabToSelect);
            }
            if (mSelectionChangeListener != null) {
                mSelectionChangeListener.onTabSelect(tabToSelect);
            }
        }

        BottomTabView viewToSelect = tabToSelect.getTabView();
        if (firstTime && viewToSelect != null) {
            firstTime = false;
            viewToSelect.selectView();
            //            scrollToView(viewToSelect);
        }
    }

    private void unselectTabView(BottomTab currentSelectedTab) {
        if (currentSelectedTab == null) return;

        BottomTabView viewToUnselect = currentSelectedTab.getTabView();
        if (viewToUnselect != null) {
            viewToUnselect.unselectView();
            if (mSelectionUpdateListener != null) mSelectionUpdateListener.onTabUnselect(currentSelectedTab);
        }
    }

    void selectTabAtInternal(int position) {
        if (getTabsCount() == 0) return;

        BottomTab tabToSelect = mTabs.get(position);
        if (tabToSelect.getSelected()) return;

        mProtectFromViewPagerChange = true;
        selectTab(tabToSelect);
        mProtectFromViewPagerChange = false;
    }

    public void setTabs(@NonNull List<BottomTab> tabs) {
        drainTabs();

        int len = tabs.size();
        len = mViewPagerAdapter == null ? len : Math.min(mViewPagerAdapter.getItemCount(), len);

        for (int i = 0; i < len; i++) {
            BottomTab tab = tabs.get(i);
            if (mViewPagerAdapter != null) {
                tab.setAttachedFragment(mViewPagerAdapter.getFragment(i));
            }
            addTab(tab, i);
        }

        if (mPreSelectedTabIndex != -1 && tabs.size() > 1) {
            selectTabAtInternal(mPreSelectedTabIndex);
        }

        initKingTab(mKingTab);

        resetTabsParams();
    }

    public void setKingTab(@NonNull BottomTab kingTab, @Nullable OnKingTabClickListener clickCallback) {
        kingTab.setKingTab(true);

        mKingTab = kingTab;
        mKingTabClickCallback = clickCallback;

        initKingTab(kingTab);
    }

    public void setViewPager(@NonNull ViewPager2 viewPager) {
        mViewPager = viewPager;
        mViewPagerAdapter = (ViewPagerAdapter2) viewPager.getAdapter();
        setupViewPager(viewPager);
    }

    public void setSelectionChangeListener(@NonNull OnTabSelectionChangeListener listener) {
        mSelectionChangeListener = listener;
    }

    public void setSelectionUpdateListener(@NonNull OnTabSelectionUpdateListener listener) {
        mSelectionUpdateListener = listener;
    }

    public void selectTabAt(int position) {
        if (getTabsCount() == 0) return;

        selectTab(mTabs.get(position));
    }

    public int getTabsCount() {
        return mTabs.size();
    }

    public void preSelectedTab(int index) {
        mPreSelectedTabIndex = index;
    }

    public interface OnTabSelectionChangeListener {
        void onTabSelect(@NonNull BottomTab tab);
    }

    public static class OnTabSelectionUpdateListener {
        public void onTabSelect(@NonNull BottomTab tab) {

        }

        public void onTabUnselect(@NonNull BottomTab tab) {

        }

        public void onTabReselect(@NonNull BottomTab tab) {

        }
    }

    public interface OnKingTabClickListener {
        void onKingTabClick(@NonNull BottomTab kingTab);
    }
}