/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.utils.univ.ActivityBuffer;
import com.quranapp.android.views.BoldHeader;

public abstract class FragSettingsBase extends BaseFragment {
    private final ActivityBuffer<ActivitySettings> mActivityBuffer = new ActivityBuffer<>();

    @CallSuper
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ActivitySettings) {
            mActivityBuffer.onActivityGained((ActivitySettings) context);
        }
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        mActivityBuffer.onActivityLost();
    }

    protected void getActivitySafely(ActivityBuffer.ActivityAvailableListener<ActivitySettings> listener) {
        mActivityBuffer.safely(listener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (shouldCreateScroller()) {
            NestedScrollView scroller = new NestedScrollView(inflater.getContext());
            scroller.setId(R.id.scrollView);
            scroller.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            View mainView = getFragView(inflater.getContext());

            if (mainView == null) {
                mainView = inflater.inflate(getLayoutResource(), scroller, false);
            }

            scroller.addView(mainView);

            return scroller;
        } else {
            View mainView = getFragView(inflater.getContext());

            if (mainView == null) {
                mainView = inflater.inflate(getLayoutResource(), container, false);
            }
            return mainView;
        }
    }

    protected boolean shouldCreateScroller() {
        return false;
    }

    protected View getFragView(Context ctx) {
        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(getPageBackgroundColor(view.getContext()));

        if (shouldCreateScroller()) {
            onViewReady(view.getContext(), ((ViewGroup) view).getChildAt(0), savedInstanceState);
        } else {
            onViewReady(view.getContext(), view, savedInstanceState);
        }
    }

    @NonNull
    public Bundle getArgs() {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        return args;
    }

    public abstract String getFragTitle(Context ctx);

    @LayoutRes
    public abstract int getLayoutResource();

    @ColorInt
    public int getPageBackgroundColor(Context ctx) {
        return color(ctx, R.color.colorBGPage);
    }

    public abstract void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState);

    @CallSuper
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        header.setTitleText(getFragTitle(activity));
    }

    public void launchFrag(Class<? extends FragSettingsBase> cls, Bundle args) {
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction t = fm.beginTransaction();
        /*t.setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter
                R.anim.slide_out_right  // popExit
        );*/
        t.replace(R.id.frags_container, cls, args, cls.getSimpleName());
        t.setReorderingAllowed(true);
        t.addToBackStack(cls.getSimpleName());
        t.commit();
    }

    public Bundle getFinishingResult(Context ctx) {
        return null;
    }

    @CallSuper
    public void onNewArguments(Bundle args) {
        setArguments(args);
    }
}
