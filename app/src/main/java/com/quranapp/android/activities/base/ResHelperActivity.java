/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */

package com.quranapp.android.activities.base;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.ArrayRes;
import androidx.annotation.BoolRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ContextKt;

public class ResHelperActivity extends AppCompatActivity {
    static class ActivityState {
        static int CREATED, RESUMED = 1, STARTED = 2, PAUSED = 3, STOPPED = 4, DESTROYED = 5;
    }

    private int mPrimaryClr = -213;
    private AsyncLayoutInflater mAsyncInflater;
    int mActivityState = -1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityState = ActivityState.CREATED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityState = ActivityState.RESUMED;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mActivityState = ActivityState.STARTED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityState = ActivityState.PAUSED;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mActivityState = ActivityState.STOPPED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityState = ActivityState.DESTROYED;
    }

    @NonNull
    public AsyncLayoutInflater getAsyncLayoutInflater() {
        if (mAsyncInflater == null) {
            mAsyncInflater = new AsyncLayoutInflater(this);
        }
        return mAsyncInflater;
    }

    public Configuration getConfiguration() {
        return getResources().getConfiguration();
    }

    @ColorInt
    public int primaryClr() {
        if (mPrimaryClr == -213) {
            mPrimaryClr = color(R.color.colorPrimary);
        }

        return mPrimaryClr;
    }

    public String appName() {
        return str(R.string.app_name);
    }

    public boolean bool(@BoolRes int boolResId) {
        try {
            return getResources().getBoolean(boolResId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String str(@StringRes int stringResId) {
        try {
            return getString(stringResId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String str(@StringRes int stringResId, Object... formatArgs) {
        try {
            return getString(stringResId, formatArgs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String[] strArray(@ArrayRes int arrayResId) {
        return getResources().getStringArray(arrayResId);
    }

    public int[] intArray(@ArrayRes int arrayResId) {
        return getResources().getIntArray(arrayResId);
    }

    public TypedArray typedArray(@ArrayRes int arrayResId) {
        return getResources().obtainTypedArray(arrayResId);
    }

    @ColorInt
    public int color(@ColorRes int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    public ColorStateList colorStateList(@ColorRes int colorResId) {
        return ContextCompat.getColorStateList(this, colorResId);
    }

    public Typeface font(@FontRes int fontResId) {
        return ContextKt.getFont(this, fontResId);
    }

    public Drawable drawable(@DrawableRes int drawableResId) {
        return ContextKt.drawable(this, drawableResId);
    }

    public int dimen(@DimenRes int dimenResId) {
        return ContextKt.getDimenPx(this, dimenResId);
    }

    public int dp2px(@Dimension(unit = Dimension.DP) float dpValue) {
        return Dimen.dp2px(this, dpValue);
    }
}
