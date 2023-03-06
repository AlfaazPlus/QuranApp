package com.quranapp.android.adapters.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter2 extends FragmentStateAdapter {
    private final List<Fragment> fragments = new ArrayList<>();
    private final List<String> fragmentTitles = new ArrayList<>();

    public ViewPagerAdapter2(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(@NonNull Fragment fragment, @Nullable String title) {
        fragments.add(fragment);
        fragmentTitles.add(title);
    }

    public List<Fragment> getFragments() {
        return fragments;
    }

    public Fragment getFragment(int index) {
        return fragments.get(index);
    }

    public Fragment getFragmentSafely(int index) {
        try {
            return fragments.get(index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public CharSequence getPageTitle(int position) {
        return fragmentTitles.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}