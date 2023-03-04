package com.peacedesign.android.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FontRes;
import androidx.annotation.FractionRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.peacedesign.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Locale;

public class ResUtils {
    @Nullable
    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        try {
            return AppCompatResources.getDrawable(context, resId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Dimension
    public static int getDimenPx(@NonNull Context context, @DimenRes int dimenRes) {
        return context.getResources().getDimensionPixelSize(dimenRes);
    }

    @Dimension(unit = Dimension.SP)
    public static float getDimenSp(@NonNull Context context, @DimenRes int dimenRes) {
        return Dimen.px2sp(context, context.getResources().getDimensionPixelSize(dimenRes));
    }

    public static float getFraction(@NonNull Context context, @FractionRes int dimenRes) {
        return context.getResources().getFraction(dimenRes, 1, 1);
    }

    @NonNull
    public static Resources getLocalizedResources(Context context, Locale locale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(locale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }

    public static String getLocalizedString(Context context, @StringRes int resId, Locale locale) {
        try {
            return getLocalizedResources(context, locale).getString(resId);
        } catch (Exception e) {
            return null;
        }
    }


    public static InputStream getBitmapInputStream(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return new ByteArrayInputStream(stream.toByteArray());
    }

    public static String readAssetsTextFile(Context context, String filename) {
        String text = "";
        try (InputStream is = context.getAssets().open(filename);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }

            text = os.toString();
        } catch (IOException e) {e.printStackTrace();}
        return text;
    }
}
