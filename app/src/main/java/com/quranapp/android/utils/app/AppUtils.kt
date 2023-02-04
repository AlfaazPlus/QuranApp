package com.quranapp.android.utils.app;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.BuildConfig;
import com.quranapp.android.R;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.sp.SPAppConfigs;
import com.quranapp.android.utils.univ.FileUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AppUtils {
    public static final String DEFAULT_DB_NAME = "QuranApp.db";
    public static final String BASE_APP_DOWNLOADED_SAVED_DATA_DIR = FileUtils.createPath("downloaded", "saved_data");

    public static String resolveThemeTextFromMode(Context context) {
        final int strId;
        switch (SPAppConfigs.getThemeMode(context)) {
            case SPAppConfigs.THEME_MODE_DARK:
                strId = R.string.strLabelThemeDark;
                break;
            case SPAppConfigs.THEME_MODE_LIGHT:
                strId = R.string.strLabelThemeLight;
                break;
            case SPAppConfigs.THEME_MODE_DEFAULT:
            default:
                strId = R.string.strLabelSystemDefault;
                break;
        }

        return context.getString(strId);
    }

    public static int resolveThemeIdFromMode(Context context) {
        switch (SPAppConfigs.getThemeMode(context)) {
            case SPAppConfigs.THEME_MODE_DARK:
                return R.id.themeDark;
            case SPAppConfigs.THEME_MODE_LIGHT:
                return R.id.themeLight;
            case SPAppConfigs.THEME_MODE_DEFAULT:
            default:
                return R.id.systemDefault;
        }
    }

    public static String resolveThemeModeFromId(int id) {
        final String themeMode;

        if (id == R.id.themeDark) {
            themeMode = SPAppConfigs.THEME_MODE_DARK;
        } else if (id == R.id.themeLight) {
            themeMode = SPAppConfigs.THEME_MODE_LIGHT;
        } else {
            themeMode = SPAppConfigs.THEME_MODE_DEFAULT;
        }
        return themeMode;
    }

    public static int resolveThemeModeFromSP(Context context) {
        final String themeMode = SPAppConfigs.getThemeMode(context);
        final int mode;
        switch (themeMode) {
            case SPAppConfigs.THEME_MODE_DARK:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case SPAppConfigs.THEME_MODE_LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case SPAppConfigs.THEME_MODE_DEFAULT:
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        return mode;
    }

    public static Map<String, Object> getAppConfigs(Context ctx) {
        Map<String, Object> map = new HashMap<>();
        map.put("locale", Locale.getDefault().toString());
        map.put("theme", SPAppConfigs.getThemeMode(ctx));
        map.put("orientation", WindowUtils.isLandscapeMode(ctx) ? "landscape" : "portrait");
        return map;
    }

    public static Map<String, Object> getDeviceInformation(Context ctx) {
        Map<String, Object> map = new HashMap<>();

        map.put("appVersionCode", BuildConfig.VERSION_CODE);
        map.put("appVersionName", BuildConfig.VERSION_NAME);

        map.put("screenSize", getScreenSize(ctx));

        TimeZone tz = TimeZone.getDefault();
        map.put("timeZone", tz.getDisplayName(false, TimeZone.SHORT) + " " + tz.getID());

        TelephonyManager tm = ContextCompat.getSystemService(ctx, TelephonyManager.class);
        map.put("country", tm != null ? tm.getNetworkCountryIso() : null);

        map.put("model", Build.MODEL);
        map.put("id", Build.ID);
        map.put("manufacturer", Build.MANUFACTURER);
        map.put("brand", Build.BRAND);
        map.put("type", Build.TYPE);
        map.put("user", Build.USER);
        map.put("base", Build.VERSION_CODES.BASE);
        map.put("incremental", Build.VERSION.INCREMENTAL);
        map.put("sdkInt", Build.VERSION.SDK_INT);
        map.put("board", Build.BOARD);
        map.put("host", Build.HOST);
        map.put("fingerprint", Build.FINGERPRINT);
        map.put("versionRelease", Build.VERSION.RELEASE);
        return map;
    }

    private static String getScreenSize(Context ctx) {
        WindowManager wm = ContextCompat.getSystemService(ctx, WindowManager.class);
        if (wm == null) {
            return null;
        }

        Point out = new Point();
        wm.getDefaultDisplay().getRealSize(out);
        return out.x + "*" + out.y;
    }

    public static boolean isUpdateAvailable(Context ctx) {
        long latestVerFromSP = SPAppActions.getLatestAppVersion(ctx);
        if (latestVerFromSP == -1) {
            return false;
        }

        return latestVerFromSP > BuildConfig.VERSION_CODE;
    }
}
