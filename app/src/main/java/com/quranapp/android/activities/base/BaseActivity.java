/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */

package com.quranapp.android.activities.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater.OnInflateFinishedListener;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import static com.quranapp.android.activities.base.BaseActivity.ActivityAnimationStyle.DEFAULT;
import static com.quranapp.android.activities.base.BaseActivity.ActivityAnimationStyle.NONE;
import static com.quranapp.android.activities.base.BaseActivity.ActivityAnimationStyle.SLIDE;
import static com.quranapp.android.utils.sharedPrefs.SPAppConfigs.LOCALE_DEFAULT;

import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.MainActivity;
import com.quranapp.android.interfaceUtils.ActivityResultStarter;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs;

import java.util.Locale;

public abstract class BaseActivity extends ResHelperActivity implements NetworkStateReceiverListener,
    ActivityResultStarter {
    private final ActivityResultLauncher<Intent> mActivityResultLauncher = activityResultHandler();
    protected final AsyncLayoutInflater mAsyncInflater = new AsyncLayoutInflater(this);
    private NetworkStateReceiver mNetworkReceiver;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(initBeforeBaseAttach(base));
    }

    private Context initBeforeBaseAttach(Context base) {
        adjustFontScale(base);
        return updateBaseContextLocale(base);
    }

    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);

        if (LOCALE_DEFAULT.equals(language)) {
            return context;
        }

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }
        return updateResourcesLocaleLegacy(context, locale);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            setLocale(overrideConfiguration.locale);
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    protected void setLocale(Locale locale) {
        SPAppConfigs.setLocale(this, locale.toLanguageTag());
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            resources.updateConfiguration(configuration, displayMetrics);
        }
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

    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNetworkReceiver = new NetworkStateReceiver();
        mNetworkReceiver.addListener(this);
        ContextCompat.registerReceiver(
            this,
            mNetworkReceiver,
            NetworkStateReceiver.getIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mNetworkReceiver != null) {
            mNetworkReceiver.removeListener(this);
            unregisterReceiver(mNetworkReceiver);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(getThemeId());
        super.onCreate(savedInstanceState);
        adjustStatusAndNavigationBar();
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

        if (savedInstanceState == null && shouldInflateAsynchronously()) {
            mAsyncInflater.inflate(getLayoutResource(), null, inflateCallback);
        } else {
            View view = getLayoutInflater().inflate(getLayoutResource(), null);
            inflateCallback.onInflateFinished(view, getLayoutResource(), null);
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

    public void adjustStatusAndNavigationBar() {
        Window window = getWindow();
        boolean isLight = isStatusBarLight();

        int statusBarBG = getStatusBarBG();
        int navBarBG = getNavBarBG();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();

            window.setStatusBarColor(statusBarBG);
            window.setNavigationBarColor(navBarBG);

            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);

            wic.setAppearanceLightStatusBars(isLight);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wic.setAppearanceLightNavigationBars(isLight);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(true);
            }
        } else {
            if (isLight) {
                window.setStatusBarColor(Color.BLACK);
            } else {
                window.setStatusBarColor(statusBarBG);
            }
        }
    }

    public void setStatusBarBG(@ColorInt int color, boolean isLight) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();

            window.setStatusBarColor(color);

            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(isLight);
        } else {
            if (isLight) {
                window.setStatusBarColor(Color.BLACK);
            } else {
                window.setStatusBarColor(color);
            }
        }
    }

    protected boolean isStatusBarLight() {
        return !WindowUtils.isNightMode(this) || WindowUtils.isNightUndefined(this);
    }

    @ColorInt
    protected int getStatusBarBG() {
        return getWindowBackgroundColor();
    }

    @StyleRes
    protected int getThemeId() {
        return R.style.Theme_QuranApp;
    }

    @ColorInt
    protected int getNavBarBG() {
        return getStatusBarBG();
    }

    private void setupInAnimation() {
        @AnimRes int inThisActivity;
        @AnimRes int outOldActivity;
        switch (getAnimationStyle()) {
            case SLIDE:
                inThisActivity = R.anim.slide_in_right;
                outOldActivity = R.anim.slide_out_left;
                break;
            case NONE:
                inThisActivity = 0;
                outOldActivity = 0;
                break;

            case DEFAULT:
            default:
                inThisActivity = -1;
                outOldActivity = -1;
                break;
        }

        if (inThisActivity == -1) return;

        overridePendingTransition(inThisActivity, outOldActivity);
    }

    private void setupOutAnimation() {
        @AnimRes int inOldActivity;
        @AnimRes int outThisActivity;
        switch (getAnimationStyle()) {
            case SLIDE:
                inOldActivity = R.anim.slide_in_left;
                outThisActivity = R.anim.slide_out_right;
                break;
            case NONE:
                inOldActivity = 0;
                outThisActivity = 0;
                break;

            case DEFAULT:
            default:
                inOldActivity = -1;
                outThisActivity = -1;
                break;
        }

        if (inOldActivity == -1) return;

        overridePendingTransition(inOldActivity, outThisActivity);
    }

    protected int getAnimationStyle() {
        return DEFAULT;
    }

    public void launchActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }

    protected void hideSystemBars() {
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        window.addFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Returns window background color for the current theme,
     * which is the value of {@code @android:attr/windowBackground}
     */
    @ColorInt
    private int getWindowBackgroundColor() {
        TypedArray attributes = obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        @ColorInt int backgroundColor = attributes.getColor(0, ContextCompat.getColor(this, R.color.colorBGPage));
        attributes.recycle();
        return backgroundColor;
    }

    public void launchMainActivity() {
        launchActivity(MainActivity.class);
    }

    public void restartMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public boolean isDestroyed2() {
        return isDestroyed() || isFinishing() || isChangingConfigurations();
    }

    protected static class ActivityAnimationStyle {
        public static final int DEFAULT = 0, SLIDE = 1, NONE = -1;
    }

    @Override
    public void startActivity4Result(Intent intent, ActivityOptionsCompat options) {
        mActivityResultLauncher.launch(intent, options);
    }

    private ActivityResultLauncher<Intent> activityResultHandler() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onActivityResult2);
    }

    protected void onActivityResult2(ActivityResult result) {
    }

    @Override
    public void networkAvailable() {
    }

    @Override
    public void networkUnavailable() {
    }
}
