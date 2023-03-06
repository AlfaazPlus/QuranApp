package com.peacedesign.android.utils;

import static android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;
import static android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;

import com.peacedesign.R;

public class WindowUtils {
    public static void setLightStatusBar(@NonNull Window window) {
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController wic = decorView.getWindowInsetsController();
            wic.setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flag = decorView.getSystemUiVisibility();
            flag |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flag);
        }
    }

    public static void clearLightStatusBar(@NonNull Window window) {
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController wic = decorView.getWindowInsetsController();
            wic.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.setSystemUiVisibility(0);
        }
    }

    public static boolean isNightMode(@NonNull Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == UI_MODE_NIGHT_YES;
    }

    public static boolean isNightUndefined(@NonNull Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == UI_MODE_NIGHT_UNDEFINED;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isLandscapeMode(@NonNull Context context) {
        return context.getResources().getBoolean(R.bool.isLandscape);
    }

    public static boolean isRTL(@NonNull Context context) {
        return context.getResources().getBoolean(R.bool.isRTL);
    }
}
