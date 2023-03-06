package com.quranapp.android.widgets.tablayout;


import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.quranapp.android.components.ComponentBase;
import com.quranapp.android.utils.univ.StringUtils;

public class BottomTab extends ComponentBase {
    private String tabLabel;
    @DrawableRes
    private int tabIcon;
    private BottomTabView tabView;
    private boolean mIsKingTab;
    private Fragment attachedFragment;

    public BottomTab(@Nullable String tabLabel) {
        this.tabLabel = tabLabel;
    }

    public BottomTab(@Nullable String tabLabel, @DrawableRes int tabIcon) {
        this.tabLabel = tabLabel;
        this.tabIcon = tabIcon;
    }

    public BottomTab(@DrawableRes int tabIcon) {
        this.tabIcon = tabIcon;
    }

    @Nullable
    public String getTabLabel() {
        String label = StringUtils.capitalize(tabLabel);
        if (label.isEmpty()) return null;
        return label;
    }

    public void setTabLabel(@Nullable String tabLabel) {
        this.tabLabel = tabLabel;
    }

    @DrawableRes
    public int getTabIcon() {
        return tabIcon;
    }

    public void setTabIcon(@DrawableRes int tabIcon) {
        this.tabIcon = tabIcon;
    }

    @Nullable
    public BottomTabView getTabView() {
        return tabView;
    }

    public void setTabView(@NonNull BottomTabView tabView) {
        this.tabView = tabView;
    }

    public boolean isKingTab() {
        return mIsKingTab;
    }

    public void setKingTab(boolean isKingTab) {
        mIsKingTab = isKingTab;
    }

    @NonNull
    public Fragment getAttachedFragment() {
        return attachedFragment;
    }

    public void setAttachedFragment(@NonNull Fragment attachedFragment) {
        this.attachedFragment = attachedFragment;
    }
}