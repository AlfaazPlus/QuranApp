/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */

package com.quranapp.android.activities.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater.OnInflateFinishedListener;

import com.quranapp.android.activities.MainActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected final AsyncLayoutInflater mAsyncInflater = new AsyncLayoutInflater(this);

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(initBeforeBaseAttach(base));
    }

    private Context initBeforeBaseAttach(Context base) {
        adjustFontScale(base);
        return base;
    }

    private void adjustFontScale(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        if (configuration.fontScale != 1) {
            configuration.fontScale = 1.00f;
            DisplayMetrics metrics = resources.getDisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.fontScale * metrics.density;
            resources.updateConfiguration(configuration, metrics);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        initCreate(savedInstanceState);
    }

    protected void initCreate(@Nullable Bundle savedInstanceState) {
        preActivityInflate(savedInstanceState);
        OnInflateFinishedListener inflateCallback = (view, resId, parent) -> {
            setContentView(view);

            if (savedInstanceState == null && shouldInflateAsynchronously()) {
                view.post(() -> onActivityInflated(view, null));
            } else {
                onActivityInflated(view, savedInstanceState);
            }
        };

        final int layoutRes = getLayoutResource();

        if (layoutRes != 0) {
            if (savedInstanceState == null && shouldInflateAsynchronously()) {
                mAsyncInflater.inflate(layoutRes, null, inflateCallback);
            } else {
                View view = getLayoutInflater().inflate(layoutRes, null);
                inflateCallback.onInflateFinished(view, layoutRes, null);
            }
        } else {
            onActivityInflated(new View(this), savedInstanceState);
        }
    }

    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @LayoutRes
    protected abstract int getLayoutResource();

    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
    }

    protected abstract void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState);

    public void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void restartMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
